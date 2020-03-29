package com.withparadox2.syncplayer.connection

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.widget.Toast
import java.util.*

const val RC_ENABLE_BLE = 1

class BleManager(private val activity: Activity, private val delegate: Delegate) {
  private var bltAdapter: BluetoothAdapter? = null
  var isServer: Boolean = true
  private var client: Client? = null
  private var server: Server? = null
  private var isReady = false

  private val addressSet = HashSet<String>()
  private val deviceList = ArrayDeque<BluetoothDevice>()
  private var isConnectedToServer = false

  fun start(isServer: Boolean) {
    close()
    this.isServer = isServer
    if (!isReady) {
      setupBluetooth()
    } else {
      onAdapterReady()
    }
  }

  fun close() {
    client?.close()
    isConnectedToServer = false
    deviceList.clear()
    addressSet.clear()

    server?.close()
    delegate.onStateChange("未开启")
  }

  //TODO React to close and open action of bluetooth
  private fun setupBluetooth() {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    bltAdapter = adapter
    if (adapter == null) {
      toast("Bluetooth is not available")
      return
    }

    if (!adapter.isEnabled) {
      activity.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RC_ENABLE_BLE)
      return
    }

    onAdapterReady()
  }

  fun onAdapterReady() {
    isReady = true
    if (isServer) {
      startServer()
    } else {
      startClient()
    }
  }

  private fun startServer() {
    server = Server(activity, bltAdapter!!, object : Server.ServerDelegate {
      override fun onStartFailed() {
        delegate.onStateChange("服务启动失败！")
      }

      override fun onStartSuccess() {
        delegate.onStateChange("服务已启动！")
      }

      override fun onConnectedStateChanged(device: BluetoothDevice, isAdd: Boolean) {
        delegate.onStateChange("已连接${server?.deviceList?.size ?: 0}台设备")
      }

      override fun onReceiveMessage(
        device: BluetoothDevice, message: String
      ) {
        delegate.onReceiveMessage(true, message, device)
      }

      override fun onLog(message: String) {
      }
    })
    server?.initServer()
    delegate.onStateChange("正在启动服务...")
  }

  private fun startClient() {
    if (client == null) {
      client = Client(activity, bltAdapter!!, object : Client.ClientDelegate {
        override fun onNotConnected(device: BluetoothDevice) {
          client?.close()
          autoConnectServer()
        }

        //TODO Fail to find devices can be connected
        override fun onStartScan() {
          delegate.onStateChange("正在扫描...")
        }

        override fun onStopScan() {
        }

        override fun onAddDevice(device: BluetoothDevice) {
          if (!addressSet.contains(device.address)) {
            addressSet.add(device.address)
            deviceList.push(device)
            autoConnectServer()
          }
        }

        override fun onDisconnected() {
          close()
          delegate.onStateChange("已断开!")
          startClient()
        }

        override fun onConnected(device: BluetoothDevice) {
          delegate.onStateChange("已连接!")
          isConnectedToServer = true
        }

        override fun onReadMessage(message: String) {
          delegate.onReceiveMessage(false, message)
        }

        override fun onLog(log: String) {
        }
      })
    }

    client?.scan()
  }

  //TODO Extract toast
  private fun toast(message: String) {
    activity.runOnUiThread {
      Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
  }

  @Synchronized
  private fun autoConnectServer() {
    if (client?.curDevice != null || deviceList.size == 0) {
      return
    }
    val device = deviceList.pop()
    delegate.onStateChange("自动连接服务：${device.address}")
    client?.connect(device)
  }

  fun sendMessage(message: String, device: BluetoothDevice?): Boolean {
    return if (isServer) {
      server?.sendMessage(message, device) ?: false
    } else {
      client?.sendMessage(message) ?: false
    }
  }

  fun isServerReady(): Boolean {
    return isServer && server?.deviceList?.let { return it.size > 0 } ?: false
  }

  fun isClientReady(): Boolean {
    return !isServer && client?.isConnectedServer ?: false
  }

  fun getServerConnectedDeviceList(): ArrayList<BluetoothDevice>? {
    return server?.deviceList
  }

  interface Delegate {
    fun onStateChange(state: String)
    fun onReceiveMessage(isServer: Boolean, message: String, device: BluetoothDevice? = null)
  }
}