package com.withparadox2.syncplayer.connection

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

const val TAG = "[Server]"

val UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
val UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
val UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
private val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

class Server(
	private val context: Context,
	private val bltAdapter: BluetoothAdapter,
	private val delegate: ServerDelegate
) {

	private val bluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

	private lateinit var characteristicRead: BluetoothGattCharacteristic
	private lateinit var bluetoothGattServer: BluetoothGattServer

	init {
		initServer()
	}

	private fun initServer() {
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
				Log.d(TAG, "BLE advertisement added successfully")
				delegate.onLog("BLE advertisement added successfully")
				initServices()
			}

			override fun onStartFailure(errorCode: Int) {
				Log.e(TAG, "Failed to add BLE advertisement, reason: $errorCode")
				delegate.onLog("Failed to add BLE advertisement, reason: $errorCode")
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
			UUID_CHARREAD,
			BluetoothGattCharacteristic.PROPERTY_READ,
			BluetoothGattCharacteristic.PERMISSION_READ
		)
		//add a descriptor
		val descriptor =
			BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE)
		characteristicRead.addDescriptor(descriptor)
		service.addCharacteristic(characteristicRead)

		//add a write characteristic.
		val characteristicWrite = BluetoothGattCharacteristic(
			UUID_CHARWRITE,
			BluetoothGattCharacteristic.PROPERTY_WRITE or
					BluetoothGattCharacteristic.PROPERTY_READ or
					BluetoothGattCharacteristic.PROPERTY_NOTIFY,
			BluetoothGattCharacteristic.PERMISSION_WRITE
		)
		service.addCharacteristic(characteristicWrite)

		bluetoothGattServer.addService(service)
		Log.e(TAG, "2. initServices ok")
		delegate.onLog("2. initServices ok")
	}

	private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {

		override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
			Log.e(
				TAG,
				String.format(
					"onConnectionStateChange：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(
				TAG,
				String.format("onConnectionStateChange：status = %s, newState =%s ", status, newState)
			)

			delegate.onLog("onConnectionStateChange：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onConnectionStateChange：status = $status, newState = $newState}")
		}

		override fun onServiceAdded(status: Int, service: BluetoothGattService) {
			Log.e(TAG, String.format("onServiceAdded：status = %s", status))
			delegate.onLog("onServiceAdded：status = $status")
		}

		override fun onCharacteristicReadRequest(
			device: BluetoothDevice,
			requestId: Int,
			offset: Int,
			characteristic: BluetoothGattCharacteristic
		) {
			Log.e(
				TAG,
				String.format(
					"onCharacteristicReadRequest：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(
				TAG,
				String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset)
			)

			delegate.onLog("onCharacteristicReadRequest：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onCharacteristicReadRequest：requestId = $requestId, offset = $offset}")

			bluetoothGattServer.sendResponse(
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
			Log.e(
				TAG,
				String.format(
					"3.onCharacteristicWriteRequest：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(
				TAG,
				String.format(
					"3.onCharacteristicWriteRequest：requestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s",
					requestId,
					preparedWrite,
					responseNeeded,
					offset,
					String(requestBytes)
				)
			)


			delegate.onLog("onCharacteristicWriteRequest：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onCharacteristicWriteRequest：requestId = $requestId, preparedWrite = ${preparedWrite}, responseNeeded = ${responseNeeded}, offset = ${offset}, value = $requestBytes")


			bluetoothGattServer.sendResponse(
				device,
				requestId,
				BluetoothGatt.GATT_SUCCESS,
				offset,
				requestBytes
			)
			//4.处理响应内容
			onResponseToClient(requestBytes, device, requestId, characteristic)
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
			Log.e(
				TAG,
				String.format(
					"2.onDescriptorWriteRequest：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(
				TAG,
				String.format(
					"2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,",
					requestId,
					preparedWrite,
					responseNeeded,
					offset,
					String(value)
				)
			)


			delegate.onLog("onDescriptorWriteRequest：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onDescriptorWriteRequest：requestId = $requestId, preparedWrite = ${preparedWrite}, responseNeeded = ${responseNeeded}, offset = ${offset}, value = $value")


			// now tell the connected device that this was all successfull
			bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
		}

		override fun onDescriptorReadRequest(
			device: BluetoothDevice,
			requestId: Int,
			offset: Int,
			descriptor: BluetoothGattDescriptor
		) {
			Log.e(
				TAG,
				String.format(
					"onDescriptorReadRequest：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId))
			//            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

			delegate.onLog("onDescriptorReadRequest：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onDescriptorReadRequest：requestId = $requestId, offset = $offset}")


			bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
		}

		override fun onNotificationSent(device: BluetoothDevice, status: Int) {
			super.onNotificationSent(device, status)
			Log.e(
				TAG,
				String.format(
					"5.onNotificationSent：device name = %s, address = %s",
					device.name,
					device.address
				)
			)
			Log.e(TAG, String.format("5.onNotificationSent：status = %s", status))

			delegate.onLog("onNotificationSent：device name = ${device.name}, address = ${device.address}")
			delegate.onLog("onNotificationSent：status = $status")


		}

		override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
			super.onMtuChanged(device, mtu)
			Log.e(TAG, String.format("onMtuChanged：mtu = %s", mtu))
			delegate.onLog("onMtuChanged：mtu = $mtu")

		}

		override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
			super.onExecuteWrite(device, requestId, execute)
			Log.e(TAG, String.format("onExecuteWrite：requestId = %s", requestId))
			delegate.onLog("onExecuteWrite：requestId = $requestId")

		}
	}

	private fun onResponseToClient(
		requestBytes: ByteArray,
		device: BluetoothDevice,
		requestId: Int,
		characteristic: BluetoothGattCharacteristic
	) {
		Log.e(
			TAG,
			String.format(
				"4.onResponseToClient：device name = %s, address = %s",
				device.name,
				device.address
			)
		)
		Log.e(TAG, String.format("4.onResponseToClient：requestId = %s", requestId))
		val msg = String(requestBytes)


		delegate.onLog("onResponseToClient：device name = ${device.name}, address = ${device.address}")
		delegate.onLog("onResponseToClient：requestId = $requestId")

		println("4.收到:$msg")

		val str = String(requestBytes) + " hello back123>"
		characteristicRead.value = str.toByteArray()
		bluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false)

		println("4.响应:$str")
	}

	fun close() {
		bluetoothGattServer.close()
	}

	interface ServerDelegate {
		fun onLog(message: String)
	}
}