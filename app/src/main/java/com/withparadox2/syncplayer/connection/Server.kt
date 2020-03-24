package com.withparadox2.syncplayer.connection

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "[Server]"

val UUID_SERVER: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
val UUID_CHAR_READ: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
val UUID_CHAR_WRITE: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
private val UUID_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

class Server(
	private val context: Context,
	private val bltAdapter: BluetoothAdapter,
	private val delegate: ServerDelegate
) {

	private val bluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

	private var characteristicRead: BluetoothGattCharacteristic? = null
	private var bluetoothGattServer: BluetoothGattServer? = null

	private val deviceList = ArrayList<BluetoothDevice>()

	fun initServer() {
		val settings = AdvertiseSettings.Builder()
			.setConnectable(true)
			.build()

		val advertiseData = AdvertiseData.Builder()
			.setIncludeDeviceName(true)
			.setIncludeTxPowerLevel(true)
			.build()

		val scanResponseData = AdvertiseData.Builder()
			.addServiceUuid(ParcelUuid(UUID_SERVER))
			.setIncludeTxPowerLevel(true)
			.build()


		val callback = object : AdvertiseCallback() {

			override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
				log("AdvertiseCallback#onStartSuccess")
				try {
					initServices()
				} catch (e: Exception) {
					log("#initServices error ${e.message}", e = e)
				}
			}

			override fun onStartFailure(errorCode: Int) {
				log("AdvertiseCallback#onStartFailure")
			}
		}

		val bluetoothLeAdvertiser = bltAdapter.bluetoothLeAdvertiser
		bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback)
	}

	private fun initServices() {
		bluetoothGattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback)
		val service = BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY)

		//add a read characteristic.
		characteristicRead = BluetoothGattCharacteristic(
			UUID_CHAR_READ,
			BluetoothGattCharacteristic.PROPERTY_READ,
			BluetoothGattCharacteristic.PERMISSION_READ
		)
		//add a descriptor
		val descriptor =
			BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE)
		characteristicRead!!.addDescriptor(descriptor)
		service.addCharacteristic(characteristicRead)

		//add a write characteristic.
		val characteristicWrite = BluetoothGattCharacteristic(
			UUID_CHAR_WRITE,
			BluetoothGattCharacteristic.PROPERTY_WRITE or
					BluetoothGattCharacteristic.PROPERTY_READ or
					BluetoothGattCharacteristic.PROPERTY_NOTIFY,
			BluetoothGattCharacteristic.PERMISSION_WRITE
		)
		service.addCharacteristic(characteristicWrite)

		bluetoothGattServer!!.addService(service)
	}

	private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {

		override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
			log("#onConnectionStateChange：device name = ${device.name}, address = ${device.address} status = $status, newState = $newState}")
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				deviceList.add(device)
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				deviceList.remove(device)
			}
		}

		override fun onServiceAdded(status: Int, service: BluetoothGattService) {
			log("#onServiceAdded：status = $status")
		}

		override fun onCharacteristicReadRequest(
			device: BluetoothDevice,
			requestId: Int,
			offset: Int,
			characteristic: BluetoothGattCharacteristic
		) {
			log("#onCharacteristicReadRequest：device name = ${device.name}, address = ${device.address} requestId = $requestId, offset = $offset}")

			bluetoothGattServer!!.sendResponse(
				device,
				requestId,
				BluetoothGatt.GATT_SUCCESS,
				offset,
				characteristic.value
			)
		}

		override fun onCharacteristicWriteRequest(
			device: BluetoothDevice,
			requestId: Int,
			characteristic: BluetoothGattCharacteristic,
			preparedWrite: Boolean,
			responseNeeded: Boolean,
			offset: Int,
			requestBytes: ByteArray
		) {
			log("#onCharacteristicWriteRequest：device name = ${device.name}, address = ${device.address} requestId = $requestId, preparedWrite = ${preparedWrite}, responseNeeded = ${responseNeeded}, offset = ${offset}, value = $requestBytes")

			bluetoothGattServer!!.sendResponse(
				device,
				requestId,
				BluetoothGatt.GATT_SUCCESS,
				offset,
				requestBytes//TODO change this value to a random text
			)

			delegate.onReceiveMessage(device, String(requestBytes))
		}

		override fun onDescriptorWriteRequest(
			device: BluetoothDevice,
			requestId: Int,
			descriptor: BluetoothGattDescriptor,
			preparedWrite: Boolean,
			responseNeeded: Boolean,
			offset: Int,
			value: ByteArray
		) {
			log("#onDescriptorWriteRequest：device name = ${device.name}, address = ${device.address} requestId = $requestId, preparedWrite = ${preparedWrite}, responseNeeded = ${responseNeeded}, offset = ${offset}, value = $value")
			bluetoothGattServer!!.sendResponse(
				device,
				requestId,
				BluetoothGatt.GATT_SUCCESS,
				offset,
				value
			)
		}

		override fun onDescriptorReadRequest(
			device: BluetoothDevice,
			requestId: Int,
			offset: Int,
			descriptor: BluetoothGattDescriptor
		) {
			log("#onDescriptorReadRequest：device name = ${device.name}, address = ${device.address} requestId = $requestId")

			bluetoothGattServer!!.sendResponse(
				device,
				requestId,
				BluetoothGatt.GATT_SUCCESS,
				offset,
				null
			)
		}

		override fun onNotificationSent(device: BluetoothDevice, status: Int) {
			//TODO make sure this method has been invoked before sending further messages
			log("#onNotificationSent：device name = ${device.name}, address = ${device.address} status = $status")
		}

		override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
			log("#onMtuChanged：mtu = $mtu")
		}

		override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
			log("#onExecuteWrite：requestId = $requestId")
		}
	}

	fun close() {
		bluetoothGattServer?.close()
		bluetoothGattServer?.clearServices()
		deviceList.forEach {
			bluetoothGattServer?.cancelConnection(it)
		}
		bluetoothGattServer = null
		characteristicRead = null
		deviceList.clear()
		log("#close")
	}

	fun sendMessage(message: String): Boolean {
		if (characteristicRead != null && bluetoothGattServer != null) {
			characteristicRead!!.value = message.toByteArray()
			var result = true
			deviceList.forEach {
				result = result && bluetoothGattServer!!.notifyCharacteristicChanged(it, characteristicRead, false)
			}
			return result
		}
		return false
	}

	fun log(message: String, show: Boolean = true, e: Exception? = null) {
		e?.let { Log.e(TAG, message, e) } ?: Log.i(TAG, message)

		if (show) {
			delegate.onLog(message)
		}
	}

	fun getConnectedDeviceList(): ArrayList<BluetoothDevice> {
		return deviceList
	}

	interface ServerDelegate {
		fun onLog(message: String)
		fun onConnectedStateChanged(device: BluetoothDevice, isAdd: Boolean)
		fun onReceiveMessage(device: BluetoothDevice, message: String)
	}
}