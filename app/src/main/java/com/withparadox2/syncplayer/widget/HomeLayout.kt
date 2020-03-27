package com.withparadox2.syncplayer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.withparadox2.syncplayer.R

class HomeLayout(context: Context) : FrameLayout(context), View.OnClickListener {
  private lateinit var topBar: TopBar
  private lateinit var listView: View
  private lateinit var controller: View
  private lateinit var cover: View
  private var isInit = false

  override fun onFinishInflate() {
    super.onFinishInflate()
    topBar = findViewById(R.id.topBar)
    listView = findViewById(R.id.listview)
    controller = findViewById(R.id.controller)
    cover = findViewById(R.id.cover)

    cover.setOnClickListener(this)
  }

  fun showController() {
    controller.animate().translationY(0f).setDuration(300).setListener(object :
      AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        (listView.layoutParams as LayoutParams).bottomMargin = controller.height
      }
    }).start()
  }

  fun showTopBar() {
    topBar.animate().translationY(0f).setDuration(300)
      .setListener(object : AnimatorListenerAdapter() {

      }).start()
    cover.alpha = 0f
    cover.visibility = View.VISIBLE
    cover.animate().alpha(0.8f).setDuration(300).start()
  }

  fun hideTopBar() {
    topBar.animate().translationY(topBar.getDefaultTranslate().toFloat()).setDuration(300)
      .setListener(object : AnimatorListenerAdapter() {

      }).start()
    cover.animate().alpha(0f).setDuration(300)
      .setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          cover.visibility = View.GONE
        }
      }).start()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    if (!isInit) {
      isInit = true
      controller.translationY = controller.height.toFloat()
      topBar.translationY = topBar.getDefaultTranslate().toFloat()
      (listView.layoutParams as LayoutParams).topMargin = topBar.getShowHeight()
    }
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.cover) {
      hideTopBar()
    }
  }
}