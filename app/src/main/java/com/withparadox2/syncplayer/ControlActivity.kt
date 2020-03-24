package com.withparadox2.syncplayer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.withparadox2.syncplayer.connection.Client
import com.withparadox2.syncplayer.connection.Server

private const val RC_ENABLE_BLE = 0

class ControlActivity : AppCompatActivity() {
	private var bltAdapter: BluetoothAdapter? = null
	private var isClient: Boolean = true
	private var client: Client? = null
	private var server: Server? = null
	private lateinit var layoutDevices: ViewGroup
	private lateinit var tvMessage: TextView
	private val messageStr = StringBuilder()
	private var isRunning = false
	private val deviceList = ArrayList<String>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_control)
		setupViews()
		checkPermission()
	}

	@SuppressLint("SetTextI18n")
	private fun setupViews() {
		findViewById<View>(R.id.tv_client).setOnClickListener {
			if (isRunning) {
				reset()
			}
			isClient = true
			setupBluetooth()
		}

		findViewById<View>(R.id.tv_server).setOnClickListener {
			if (isRunning) {
				reset()
			}
			isClient = false
			setupBluetooth()
		}

		findViewById<View>(R.id.tv_reset).setOnClickListener {
			reset()
		}

		tvMessage = findViewById(R.id.tv_message)
		layoutDevices = findViewById(R.id.layout_devices)
	}

	private fun reset() {
		tvMessage.text = ""
		messageStr.clear()
		layoutDevices.removeAllViews()
		if (isClient) {
			client?.close()
		} else {
			server?.close()
		}
		deviceList.clear()
	}

	private fun addMessage(message: String) {
		messageStr.append("\n")
		messageStr.append(message)
		runOnUiThread {
			tvMessage.text = messageStr
		}
	}


	private fun setupBluetooth() {
		val adapter = BluetoothAdapter.getDefaultAdapter()
		bltAdapter = adapter
		if (adapter == null) {
			toast("Bluetooth is not available")
			return
		}

		if (!adapter.isEnabled) {
			startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RC_ENABLE_BLE)
			return
		}

		onAdapterReady()
	}

	private fun onAdapterReady() {
		isRunning = true
		if (isClient) {
			addMessage("===Client====")
			if (client == null) {
				client = Client(this, bltAdapter!!, object : Client.ClientDelegate {
					override fun onStartScan() {
						addMessage("onStartScan")
					}

					override fun onStopScan() {
						addMessage("onStartScan")
					}

					override fun onAddDevice(device: BluetoothDevice) {
						if (!deviceList.contains(device.address)) {
							deviceList.add(device.address)
							addDevice(device)
						}
					}

					override fun onDisconnect() {
						addMessage("onDisconnect")
					}

					override fun onConnect(device: BluetoothDevice) {
						addMessage("onConnect ${device.name} ${device.address}")

					}

					override fun onReadMessage(message: String) {
						addMessage("read message:${message}")
					}

					override fun onLog(log: String) {
						addMessage(log)
					}
				})
			}

			client?.scan()
		} else {
			addMessage("===Server====")

			server = Server(this, bltAdapter!!, object : Server.ServerDelegate {
				override fun onLog(message: String) {
					addMessage(message)
				}
			})
		}
	}

	@SuppressLint("SetTextI18n")
	private fun addDevice(device: BluetoothDevice) {
		val tvDevice = TextView(this)
		tvDevice.tag = device
		tvDevice.text = "${layoutDevices.childCount} ${device.name} ${device.address}"
		layoutDevices.addView(tvDevice)
		tvDevice.setOnClickListener {
			client?.connect(device)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (resultCode == RESULT_OK) {
			if (requestCode == RC_ENABLE_BLE) {
				onAdapterReady()
			}
		}
	}

	var mGetPermissions = false

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		PermissionManager.instance.handlePermissionResult(requestCode, permissions, grantResults)
	}

	open fun checkPermission() {
		if (!PermissionManager.instance.hasPermission(
				this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.CAMERA
			)
		) {
			PermissionDialog(DialogInterface.OnClickListener { _, _ ->
				requestPermission()
			}).show(supportFragmentManager, "permission")
		} else {
			onGetPermission()
		}
	}

	private fun requestPermission() {
		PermissionManager.instance.requestPermission(
			this,
			object : PermissionManager.PermissionCallback {
				override fun onDenied() {
					toast("SimpleOCR can not work without necessary permissions")
				}

				override fun onGranted() {
					onGetPermission()
				}

			},
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION
		)
	}

	open fun onGetPermission() {
		mGetPermissions = true
	}
}