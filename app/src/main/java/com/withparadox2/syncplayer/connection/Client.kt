package com.withparadox2.syncplayer.connection

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.*


class Client(
	private val context: Context,
	private val bltAdapter: BluetoothAdapter,
	private val delegate: ClientDelegate
) {
	private val handler = Handler()
	private var bluetoothGatt: BluetoothGatt? = null
	private var curDevice: BluetoothDevice? = null
	private var readCharacteristic: BluetoothGattCharacteristic? = null
	private var writeCharacteristic: BluetoothGattCharacteristic? = null

	var isPrepared: Boolean = false

	fun scan() {
		bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
		bltAdapter.bluetoothLeScanner.startScan(scanCallback)
		handler.postDelayed({
			bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
			delegate.onStopScan()
		}, 100000)
		delegate.onStartScan()
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
		}

		override fun onCharacteristicWrite(
			gatt: BluetoothGatt?,
			characteristic: BluetoothGattCharacteristic?,
			status: Int
		) {
		}


		override fun onCharacteristicChanged(
			gatt: BluetoothGatt?,
			characteristic: BluetoothGattCharacteristic?
		) {
			Log.e(TAG, "onCharacteristicChanged characteristic: $characteristic")
			characteristic?.let { readCharacteristic(it) }
		}

		override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.e(TAG, "Connected to GATT server.")
				Log.e(
					TAG, "Attempting to start service discovery:" +
							bluetoothGatt!!.discoverServices()
				)
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				delegate.onDisconnect()
				isPrepared = false
				Log.e(TAG, "Disconnected from GATT server.")
			}
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.e(TAG, "onServicesDiscovered received:  SUCCESS")
				initCharacteristic()
				try {
					Thread.sleep(200)//延迟发送，否则第一次消息会不成功
				} catch (e: InterruptedException) {
					e.printStackTrace()
				}
				delegate.onConnect(curDevice!!)
				isPrepared = true
			} else {
				Log.e(TAG, "onServicesDiscovered error falure $status")
			}
		}
	}

	fun initCharacteristic() {
		val services = bluetoothGatt!!.services
		Log.e(TAG, services.toString())
		val service = bluetoothGatt!!.getService(UUID_SERVER)
		if (service == null) {
			delegate.onLog("service not found")
		} else {
			delegate.onLog("service found!")
		}
		readCharacteristic = service.getCharacteristic(UUID_CHARREAD)
		writeCharacteristic = service.getCharacteristic(UUID_CHARWRITE)

		val uuid = "00002902-0000-1000-8000-00805f9b34fb"
		bluetoothGatt!!.setCharacteristicNotification(readCharacteristic, true)
		val descriptor = readCharacteristic!!.getDescriptor(UUID.fromString(uuid))
		descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
		bluetoothGatt!!.writeDescriptor(descriptor)
	}

	fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
		bluetoothGatt!!.readCharacteristic(characteristic)
		val bytes = characteristic.value
		val str = String(bytes)
		Log.e(TAG, "## readCharacteristic, 读取到: $str")
		delegate.onReadMessage(str)
	}

	fun writeMessage(message: String) {
		if (isPrepared) {
			writeCharacteristic!!.setValue(message)
			writeCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
			bluetoothGatt!!.writeCharacteristic(writeCharacteristic)
		}
	}

	fun close() {
		bluetoothGatt?.close()
		writeCharacteristic = null
		readCharacteristic = null
		curDevice = null
	}

	interface ClientDelegate {
		fun onStartScan()
		fun onStopScan()
		fun onAddDevice(device: BluetoothDevice)
		fun onDisconnect()
		fun onConnect(device: BluetoothDevice)
		fun onReadMessage(message: String)
		fun onLog(log: String)
	}
}