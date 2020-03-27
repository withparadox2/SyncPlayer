package com.withparadox2.syncplayer

import android.content.Context
import android.media.MediaPlayer
import java.io.IOException

class PlayManager(context: Context, private val delegate: PlayerDelegate) :
  MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {


  private val player: MediaPlayer = MediaPlayer()

  var isPaused = false
    private set

  val isPlaying: Boolean
    get() = player.isPlaying

  val currentPosition: Int
    get() = if (player.isPlaying || isPaused) {
      player.currentPosition
    } else 0


  init {
    player.setOnCompletionListener(this)
    player.setOnPreparedListener(this)
  }

  fun play(path: String) {
    try {
      player.reset()
      player.setDataSource(path)
      player.prepareAsync()
      isPaused = false
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  fun pause() {
    if (player.isPlaying) {
      isPaused = true
      player.pause()
    }
  }

  fun resume() {
    player.start()
    isPaused = false
  }

  fun stop() {
    player.stop()
    isPaused = false
  }

  fun seekPosition(millis: Int) {
    player.seekTo(millis)
  }

  fun destroy() {
    player.release()
  }

  override fun onCompletion(mp: MediaPlayer) {
  }

  override fun onPrepared(mp: MediaPlayer) {
    delegate.onPrepared()
  }

  interface PlayerDelegate {
    fun onCompletion()
    fun onPrepared()
  }
}