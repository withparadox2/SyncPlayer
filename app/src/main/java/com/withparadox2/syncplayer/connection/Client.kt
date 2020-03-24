package com.withparadox2.syncplayer.connection

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
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
	private var curDevice: BluetoothDevice? = null
	private var readCharacteristic: BluetoothGattCharacteristic? = null
	private var writeCharacteristic: BluetoothGattCharacteristic? = null

	var isScanning = false

	fun scan() {
		if (isScanning) {
			return
		}
		isScanning = true
		bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
		bltAdapter.bluetoothLeScanner.startScan(scanCallback)
		handler.postDelayed({
			bltAdapter.bluetoothLeScanner.stopScan(scanCallback)
			delegate.onStopScan()
			isScanning = false
			log("Stop scan")
		}, 100000)
		delegate.onStartScan()
		log("Start scan")
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
			}

			log("#onConnectionStateChange newState: $newState")
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				initCharacteristic()
//				try {
//					Thread.sleep(200)
//				} catch (e: InterruptedException) {
//					e.printStackTrace()
//				}
				delegate.onConnected(curDevice!!)
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

	fun sendMessage(message: String) {
		if (bluetoothGatt != null && writeCharacteristic != null) {
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
		fun onConnected(device: BluetoothDevice)
		fun onReadMessage(message: String)
		fun onLog(log: String)
	}
}