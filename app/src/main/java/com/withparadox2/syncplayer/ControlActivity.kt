package com.withparadox2.syncplayer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.withparadox2.syncplayer.connection.Client
import com.withparadox2.syncplayer.connection.Server

private const val RC_ENABLE_BLE = 0

val PLAY_LIST = mapOf(
  "26427666" to "我说今晚月光那么美,你说是的",
  "137703" to "晚安",
  "32628933" to "孙大剩",
  "407761964" to "历历万乡",
  "549320309" to "克林",
  "252951" to "谢谢你让我这么爱你",
  "422429521" to "迷藏",
  "30031580" to "原来你也在这里",
  "28606468" to "老情歌",
  "1336871780" to "恋曲2018",
  "29414037" to "走马",
  "30953645" to "离别的车站",
  "544713530" to "如常"
)

open class ControlActivity : AppCompatActivity() {
  private var bltAdapter: BluetoothAdapter? = null
  private var isClient: Boolean = true
  private var client: Client? = null
  private var server: Server? = null
  private lateinit var layoutDevices: ViewGroup
  private lateinit var tvMessage: TextView
  private lateinit var etMessage: EditText
  private val messageStr = StringBuilder()
  private var isRunning = false
  private val deviceList = ArrayList<String>()

  private var controlPlayId: String? = null
  private var curPlayId: String? = null

  private val handler = Handler()
  private val playerManager = PlayManager(this, object : PlayManager.PlayerDelegate {
    override fun onCompletion() {
    }

    override fun onPrepared() {
      if (curPlayId == controlPlayId) {
        sendMessage(curPlayId!!)
      } else {
        sendMessage("go")
        start()
      }
    }
  })
  lateinit var playLayout: LinearLayout

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

    etMessage = findViewById(R.id.et_message)
    etMessage.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val text: String = etMessage.text.toString()
        if (text.endsWith("\n")) {
          if (isClient) {
            client?.sendMessage(text)
          } else {
            server?.sendMessage(text)
          }
          etMessage.setText("")
        }
      }

      override fun afterTextChanged(s: Editable?) {
      }

    })

    playLayout = findViewById(R.id.layout_playlist)
    addPlayListView()
    findViewById<View>(R.id.btn_test_time).setOnClickListener {
      lastSendTime = System.currentTimeMillis()
//      sendMessage("time_request")
      playerManager.seekPosition(5000)
//      start2()
//      handler.post(checkPositionAction)
      addMessage("after seek ${playerManager.currentPosition}")
      handler.postDelayed(object : Runnable {
        override fun run() {

          addMessage("position after 8000 = ${playerManager.currentPosition} cost = ${System.currentTimeMillis() - lastSendTime}")
        }

      }, 3000)
    }
  }

  private fun reset() {
    tvMessage.text = ""
    messageStr.clear()
    layoutDevices.removeAllViews()
    client?.close()
    server?.close()
    deviceList.clear()
  }

  private fun addMessage(message: String) {
//    if (messageStr.length > 500) {
//      messageStr.clear()
//    }
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
          }

          override fun onStopScan() {
          }

          override fun onAddDevice(device: BluetoothDevice) {
            if (!deviceList.contains(device.address)) {
              deviceList.add(device.address)
              addDevice(device)
            }
          }

          override fun onDisconnected() {
            addMessage("onDisconnected")
//            handler.removeCallbacks(checkRttAction)
          }

          override fun onConnected(device: BluetoothDevice) {
            addMessage("onConnected ${device.name} ${device.address}")
//            handler.post(checkRttAction)
          }

          override fun onReadMessage(message: String) {
            addMessage("【Server】:${message}")
            parseMessage(message)
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
        override fun onConnectedStateChanged(device: BluetoothDevice, isAdd: Boolean) {
        }

        override fun onReceiveMessage(device: BluetoothDevice, message: String) {
          addMessage("【Client】:$message")
          parseMessage(message)
        }

        override fun onLog(message: String) {
          addMessage(message)
        }
      })
      server?.initServer()
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

  private fun checkPermission() {
    if (!PermissionManager.instance.hasPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
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
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )
  }

  private fun onGetPermission() {
    mGetPermissions = true
  }

  private fun addPlayListView() {
    PLAY_LIST.keys.forEach { key ->
      val name = PLAY_LIST[key]
      val view = TextView(this)
      view.setPadding(30, 15, 30, 15)
      view.text = name
      view.background = StateListDrawable().apply {
        this.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(Color.BLUE))
        this.addState(intArrayOf(android.R.attr.state_enabled), ColorDrawable(Color.RED))
      }
      view.tag = key
      view.setOnClickListener(itemClickListener)
      playLayout.addView(view)
    }
  }

  private val itemClickListener = View.OnClickListener {
    val id = it.tag as String
    controlPlayId = id
    playMusic(id)
  }

  private fun updateView() {
    playLayout.children.forEach {
      val id = it.tag as String
      val state = if (id == curPlayId) "[播放]${PLAY_LIST[id]}" else PLAY_LIST[id]
      (it as TextView).text = state
    }
  }

  private fun playMusic(id: String) {
    lastOffset = 0
    lastAppend = 0

    if (id == curPlayId) {
      playerManager.stop()
      curPlayId = null
      sendMessage("stop")
      handler.removeCallbacks(checkPositionAction)
    } else {
      if (curPlayId != null) {
        playerManager.stop()
      }
      curPlayId = id
      playerManager.play("https://music.163.com/song/media/outer/url?id=${id}.mp3")
    }
    updateView()
  }

  fun start2() {
  }

  fun start() {
    runOnUiThread {
      Toast.makeText(this, "start", Toast.LENGTH_SHORT).show()
    }

    playerManager.resume()

    if (isClient) {
      handler.postDelayed(checkPositionAction, 500)
    }
//    handler.postDelayed(object :Runnable {
//      override fun run() {
//        addMessage("seek position ${playerManager.currentPosition}")
//      }
//    }, 300)
  }

  private val checkPositionAction = object : Runnable {
    override fun run() {
      addMessage("position = " + playerManager.currentPosition)
      sendMessage("request_position")
      lastSendTime = System.currentTimeMillis()
//      handler.postDelayed(this, 500)
    }
  }

  private val checkRttAction = object : Runnable {
    override fun run() {
      lastSendTime = System.currentTimeMillis()
      sendMessage("time_request")
      handler.postDelayed(this, 1000)
    }
  }

  private var lastOffset = 0
  private var lastAppend = 0
  private var lastSendTime = 0L
  private var rtt = -1L

  private fun parseMessage(message: String) {
    when {
      message == "go" -> start()
      message == "stop" -> {
        curPlayId?.let { playMusic(it) }
      }
      message == "stop_sync" -> {
        handler.removeCallbacks(checkPositionAction)
      }
      message == "request_position" -> {
        sendMessage("check#" + playerManager.currentPosition)
      }
      message == "time_request" -> {
        sendMessage("time_response")
        addMessage("time_request")
      }
      message == "time_response" -> {
        val curRtt = (System.currentTimeMillis() - lastSendTime)
//        if (rtt < 0) {
//          rtt = curRtt
//        } else {
//          rtt = (rtt * 0.3f + curRtt * 0.7).toLong()
//        }
        rtt = curRtt
        addMessage("time_response curRtt = $curRtt rtt = $rtt")
      }
      message.startsWith("check") -> {
        val rtt = System.currentTimeMillis() - lastSendTime
        val position: Int = (message.substring(6).toInt() + (rtt / 2 + 200)).toInt()
        addMessage("remote ${message.substring(6)} local ${playerManager.currentPosition} rtt = $rtt")
        val offset = position - playerManager.currentPosition
        playerManager.seekPosition(position)
        addMessage("local ${playerManager.currentPosition}")

      }
      else -> {
        controlPlayId = null
        playMusic(message)
      }
    }
  }

  private fun sendMessage(message: String) {
    if (isClient) {
      client?.sendMessage(message)
    } else {
      server?.sendMessage(message)
    }
  }
}