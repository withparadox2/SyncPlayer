package com.withparadox2.syncplayer.connection

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import java.lang.Exception
import java.util.*

private const val TAG = "[Client]"

class Client(
  private val context: Context,
  private val bltAdapter: BluetoothAdapter,
  private val delegate: ClientDelegate
) {
  private val handler = Handler()
  private var bluetoothGatt: BluetoothGatt? = null
  var curDevice: BluetoothDevice? = null
  private var readCharacteristic: BluetoothGattCharacteristic? = null
  private var writeCharacteristic: BluetoothGattCharacteristic? = null

  var isScanning = false
  var isConnectedServer = false

  fun scan() {
    if (isScanning) {
      return
    }
    isScanning = true
    val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID_SERVER)).build()

    bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
    bltAdapter.bluetoothLeScanner.startScan(
      listOf(filter),
      ScanSettings.Builder().build(),
      scanCallback
    )
    handler.postDelayed(stopScanAction, 100000)
    delegate.onStartScan()
    log("Start scan")
  }

  private val stopScanAction = Runnable {
    bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
    delegate.onStopScan()
    isScanning = false
    log("Stop scan")
  }

  private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
      result?.let {
        delegate.onAddDevice(it.device)
      }
    }
  }

  fun connect(device: BluetoothDevice) {
    curDevice = device
    bluetoothGatt = device.connectGatt(context, false, gattCallback)
  }

  private val gattCallback = object : BluetoothGattCallback() {
    override fun onCharacteristicRead(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?,
      status: Int
    ) {
      log("#onCharacteristicRead status = $status")
      if (status == BluetoothGatt.GATT_SUCCESS) {
        characteristic?.let {
          delegate.onReadMessage(String(it.value))
        }
      }
    }

    override fun onCharacteristicWrite(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?,
      status: Int
    ) {
      log("#onCharacteristicWrite status = $status")
    }

    override fun onCharacteristicChanged(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?
    ) {
      log("#onCharacteristicChanged characteristic: $characteristic")
      readMessage(characteristic)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        bluetoothGatt!!.discoverServices()
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        delegate.onDisconnected()
        isConnectedServer = false
      }

      log("#onConnectionStateChange newState: $newState")
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        try {
          initCharacteristic()
          isConnectedServer = true
          delegate.onConnected(curDevice!!)
        } catch (e: Exception) {
          log("#onServicesDiscovered error ${e.message}", e = e)
          delegate.onNotConnected(curDevice!!)
        }
      } else {
        delegate.onNotConnected(curDevice!!)
      }
      log("#onServicesDiscovered: status = $status")
    }
  }

  fun initCharacteristic() {
    val services = bluetoothGatt!!.services
    Log.e(TAG, services.toString())
    val service = bluetoothGatt!!.getService(UUID_SERVER)

    readCharacteristic = service.getCharacteristic(UUID_CHAR_READ)
    writeCharacteristic = service.getCharacteristic(UUID_CHAR_WRITE)

    val uuid = "00002902-0000-1000-8000-00805f9b34fb"
    bluetoothGatt!!.setCharacteristicNotification(readCharacteristic, true)
    val descriptor = readCharacteristic!!.getDescriptor(UUID.fromString(uuid))
    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    bluetoothGatt!!.writeDescriptor(descriptor)
  }

  fun readMessage(characteristic: BluetoothGattCharacteristic? = readCharacteristic) {
    if (bluetoothGatt != null && characteristic != null) {
      bluetoothGatt!!.readCharacteristic(characteristic)
    }
  }

  fun sendMessage(message: String): Boolean {
    if (bluetoothGatt != null && writeCharacteristic != null) {
      writeCharacteristic!!.setValue(message)
      writeCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
      return bluetoothGatt!!.writeCharacteristic(writeCharacteristic)
    }
    return false
  }

  fun close() {
    bluetoothGatt?.close()
    writeCharacteristic = null
    readCharacteristic = null
    curDevice = null
    if (isScanning) {
      stopScanAction.run()
    }
    handler.removeCallbacks(stopScanAction)
  }

  fun log(message: String, show: Boolean = true, e: Exception? = null) {
    e?.let { Log.e(com.withparadox2.syncplayer.connection.TAG, message, e) }
      ?: Log.i(com.withparadox2.syncplayer.connection.TAG, message)

    if (show) {
      delegate.onLog(message)
    }
  }

  interface ClientDelegate {
    fun onStartScan()
    fun onStopScan()
    fun onAddDevice(device: BluetoothDevice)
    fun onDisconnected()
    fun onNotConnected(device: BluetoothDevice)
    fun onConnected(device: BluetoothDevice)
    fun onReadMessage(message: String)
    fun onLog(log: String)
  }
}