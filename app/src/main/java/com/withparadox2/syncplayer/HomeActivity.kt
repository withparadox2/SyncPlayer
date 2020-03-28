package com.withparadox2.syncplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.withparadox2.syncplayer.connection.BleManager
import com.withparadox2.syncplayer.widget.HomeLayout
import com.withparadox2.syncplayer.widget.TopBar
import java.util.concurrent.atomic.AtomicInteger

class HomeActivity : AppCompatActivity() {
  lateinit var topBar: TopBar
  lateinit var homeLayout: HomeLayout
  lateinit var rvSongs: RecyclerView
  lateinit var tvSongTitle: TextView
  lateinit var ivPlayController: ImageView
  lateinit var tvLeftTime: TextView
  lateinit var tvRightTime: TextView
  lateinit var seekBar: SeekBar

  private var curPlayIndex: Int? = null
  private lateinit var playManager: PlayManager

  private val bleManager = BleManager(this, object : BleManager.Delegate {
    override fun onReceiveMessage(isServer: Boolean, message: String, device: BluetoothDevice?) {
      syncHelper.onReceiveMessage(isServer, message, device)
    }

    override fun onStateChange(state: String) {
      topBar.tvState.text = state
    }
  })

  private val handler = Handler()
  private val syncHelper = SyncHelper()

  override fun setContentView(view: View?) {
    super.setContentView(view)
    setContentView(R.layout.activity_home)

    playManager = PlayManager(this, object : PlayManager.PlayerDelegate {
      override fun onCompletion() {
        if (bleManager.isClientReady()) {
          pauseOrPlay(curPlayIndex!!, false)
        } else {
          if (curPlayIndex!! >= PlayList.size - 1) {
            pauseOrPlay(0)
          } else {
            pauseOrPlay(curPlayIndex!! + 1)
          }
        }
      }

      override fun onPrepared() {
        tvRightTime.text = formatTime(playManager.getDuration())

        when {
          bleManager.isServerReady() -> syncHelper.requestPrepare()
          bleManager.isClientReady() -> syncHelper.tellPrepared()
          else -> startPlay()
        }
      }
    })

    homeLayout = findViewById(R.id.layout_home)
    topBar = findViewById(R.id.top_bar)
    topBar.setDelegate(object : TopBar.Delegate {
      override fun onClickController(isServer: Boolean, isStart: Boolean) {
        if (isStart) {
          bleManager.start(isServer)
        } else {
          bleManager.close()
        }
      }
    })
    topBar.tvState.setOnClickListener {
      homeLayout.showTopBar()
    }
    rvSongs = findViewById(R.id.listview_songs)
    tvSongTitle = findViewById(R.id.tv_song_name)
    ivPlayController = findViewById(R.id.btn_play_controller)
    tvLeftTime = findViewById(R.id.tv_left_time)
    tvRightTime = findViewById(R.id.tv_right_time)
    seekBar = findViewById(R.id.seek_bar)

    ivPlayController.setOnClickListener {
      curPlayIndex?.let {
        pauseOrPlay(it)
      }
    }

    rvSongs.adapter = SongsAdapter()
    rvSongs.layoutManager = LinearLayoutManager(this)

    seekBar.max = 1000
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (playManager.getDuration() > 0) {
          val percent = progress / seekBar.max
          val playPosition: Int = (percent * playManager.getDuration())

          if (fromUser) {
            if (bleManager.isClientReady()) {
              syncHelper.wantToSeekPosition(playPosition)
            } else {
              playManager.seekPosition(playPosition)
              if (bleManager.isServerReady()) {
                syncHelper.requestSeekTo(playPosition)
              }
            }
          }
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
      }
    })
  }

  private inner class SongsAdapter : RecyclerView.Adapter<SongViewHolder>() {
    val onClickListener = View.OnClickListener { view ->
      val position = view.tag as Int
      pauseOrPlay(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
      val textView = TextView(this@HomeActivity)
      textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
      textView.setPadding(60, 0, 60, 0)
      textView.setOnClickListener(onClickListener)
      return SongViewHolder(textView)
    }

    override fun getItemCount(): Int = PlayList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
      val pair = PlayList[position]
      holder.textView.text = "${if (curPlayIndex == position) "* " else ""}${pair.second}"
      holder.textView.tag = position
    }
  }

  private class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textView: TextView = itemView as TextView
  }

  private fun pauseOrPlay(index: Int, broadcast: Boolean = true) {
    tryPlayWithIndex(index, broadcast)
    updatePlayState()
  }

  private fun updatePlayState() {
    if (playManager.isPlaying) {
      ivPlayController.setImageDrawable(getDrawable(R.drawable.icon_pause))
    } else {
      ivPlayController.setImageDrawable(getDrawable(R.drawable.icon_play))
    }
    rvSongs.adapter?.notifyDataSetChanged()
  }

  private fun tryPlayWithIndex(index: Int, broadcast: Boolean) {
    if (index < 0 || index >= PlayList.size) {
      return
    }

    if (curPlayIndex == null) {
      homeLayout.showController()
    }

    val songItem = PlayList[index]

    if (curPlayIndex == index) {
      if (playManager.isPlaying) {
        if (broadcast && bleManager.isClientReady()) {
          // Ask server to dispatch command to pause
          syncHelper.wantToPause()
        } else {
          playManager.pause()
          stopTick()

          if (broadcast && bleManager.isServerReady()) {
            // Dispatch to pause
            syncHelper.requestPause()
          }
        }
      } else {
        if (broadcast && bleManager.isClientReady()) {
          syncHelper.wantToResume()
        } else {
          playManager.resume()
          startTick()

          if (broadcast && bleManager.isServerReady()) {
            // Dispatch to resume
            syncHelper.requestResume()
          }
        }
      }
    } else {
      if (broadcast && bleManager.isClientReady()) {
        syncHelper.wantToChangeSong(index)
      } else {
        curPlayIndex = index
        tvSongTitle.text = songItem.second
        playManager.play("https://music.163.com/song/media/outer/url?id=${songItem.first}.mp3")
        seekBar.isEnabled = false
      }
    }
  }

  private fun seekTo(position: Int) {
    if (position >= 0 && position <= playManager.getDuration()) {
      playManager.seekPosition(position)
      if (bleManager.isServer) {
        syncHelper.requestSeekTo(position)
      }
    }
  }

  private fun startPlay() {
    topBar.isEnabled = true
    playManager.resume()
    startTick()
    updatePlayState()
  }

  private fun startTick() {
    handler.post(tickAction)
  }

  private fun stopTick() {
    handler.removeCallbacks(tickAction)
  }

  private val tickAction = object : Runnable {
    override fun run() {
      if (playManager.getDuration() != 0) {
        val percent = playManager.currentPosition.toFloat() / playManager.getDuration()
        seekBar.progress = (percent * 1000).toInt()
      }
      tvLeftTime.text = formatTime(playManager.currentPosition)
      handler.postDelayed(this, 1000)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    playManager.destroy()
  }

  private fun formatTime(millis: Int): String {
    val m = millis / 60000
    val s = millis / 1000 % 60
    return "${if (m < 10) "0" else ""}$m:${if (s < 10) "0" else ""}$s"
  }

  private inner class SyncHelper {
    // Time consumed between calling resume() and playing actually
    var prepareTime: Int? = null

    // Used to calculate rtt
    var lastSendTime = 0L

    val answeredDeviceList = ArrayList<BluetoothDevice>()
    val lastServerPrepareRequestId = AtomicInteger(0)

    fun onReceiveMessage(isServer: Boolean, message: String?, device: BluetoothDevice? = null) {
      if (message == null || message.indexOf("$") < 0) {
        return
      }

      val arr = message.split("#")
      val msgSongIndex = arr[2].toInt()
      val content = arr[3]

      when (arr[0].toInt()) {
        // From Client
        0 -> {
          when (arr[1].toInt()) {
            // Client request to resume
            1 -> {
              if (msgSongIndex == curPlayIndex && playManager.isPaused) {
                pauseOrPlay(msgSongIndex)
              }
            }
            // Client request to pause
            2 -> {
              if (msgSongIndex == curPlayIndex && playManager.isPlaying) {
                pauseOrPlay(msgSongIndex)
              }
            }
            // Client request to change song
            3 -> {
              if (msgSongIndex != curPlayIndex) {
                pauseOrPlay(msgSongIndex)
              }
            }
            // Client request to seek to position
            4 -> {
              if (msgSongIndex == curPlayIndex) {
                seekTo(content.toInt())
              }
            }
            // Client request current position
            5 -> {
              if (msgSongIndex == curPlayIndex) {
                answerPosition(playManager.currentPosition, device)
              }
            }
            // Client is answering Server-3
            6 -> {
              val id = content.toInt()
              if (msgSongIndex == curPlayIndex && id == lastServerPrepareRequestId.get()) {
                if (device != null && answeredDeviceList.find { it.address == device.address } == null) {
                  answeredDeviceList.add(device)
                }
                bleManager.getServerConnectedDeviceList()?.let {
                  if (answeredDeviceList.size == it.size) {
                    startPlay()
                    requestResume()
                  }
                }
              }
            }
          }
        }
        // Server request to resume, client should sync
        1 -> {
          if (curPlayIndex == msgSongIndex) {
            startPlay()

            if (prepareTime == null) {
              val startResumePosition = playManager.currentPosition
              handler.postDelayed({
                prepareTime = startResumePosition + 500 - playManager.currentPosition

                lastSendTime = System.currentTimeMillis()
                wantToKnowCurrentPosition()
              }, 500)
            } else {
              lastSendTime = System.currentTimeMillis()
              wantToKnowCurrentPosition()
            }
          }
        }
        // Server request to pause
        2 -> {
          if (curPlayIndex == msgSongIndex) {
            pauseOrPlay(msgSongIndex, false)
          }
        }
        // Server request to prepare, client should answer Client-6 to server if ready
        3 -> {
          if (curPlayIndex != msgSongIndex) {
            pauseOrPlay(msgSongIndex, false)
          }
        }
        // Server request to seek to position
        4 -> {
          if (curPlayIndex == msgSongIndex) {
            seekTo(content.toInt())
          }
        }
        // Server answer request of Client-5
        5 -> {
          if (curPlayIndex == msgSongIndex) {
            val serverPosition = content.toInt()
            val ttr = System.currentTimeMillis() - lastSendTime
            val newPosition: Int = (serverPosition + ttr / 2 + prepareTime!!).toInt()
            seekTo(newPosition)
          }
        }
      }
    }

    // Server
    fun requestResume() {
      curPlayIndex?.let {
        sendMessage(1, 0, it)
      }
    }

    fun requestPause() {
      curPlayIndex?.let {
        sendMessage(2, 0, it)
      }
    }

    fun requestPrepare() {
      val id = lastServerPrepareRequestId.getAndIncrement()
      answeredDeviceList.clear()
      curPlayIndex?.let {
        sendMessage(3, 0, it, "$id")
      }
    }

    fun requestSeekTo(position: Int) {
      curPlayIndex?.let {
        sendMessage(4, 0, it, "$position")
      }
    }

    fun answerPosition(position: Int, device: BluetoothDevice?) {
      curPlayIndex?.let {
        sendMessage(5, 0, it, "$position", device)
      }
    }

    // Client
    fun wantToResume() {
      curPlayIndex?.let {
        sendMessage(0, 1, it)
      }
    }

    fun wantToPause() {
      curPlayIndex?.let {
        sendMessage(0, 2, it)
      }
    }

    fun wantToChangeSong(songIndex: Int) {
      sendMessage(0, 3, songIndex)
    }

    fun wantToSeekPosition(position: Int) {
      curPlayIndex?.let {
        sendMessage(0, 4, it)
      }
    }

    fun wantToKnowCurrentPosition() {
      curPlayIndex?.let {
        sendMessage(0, 5, it)
      }
    }

    fun tellPrepared() {
      curPlayIndex?.let {
        sendMessage(0, 6, it)
      }
    }

    fun sendMessage(
      command: Int,
      subCommand: Int,
      songIndex: Int,
      message: String = "",
      device: BluetoothDevice? = null
    ) {
      bleManager.sendMessage("$command#$subCommand#$songIndex$message", device)
    }
  }
}