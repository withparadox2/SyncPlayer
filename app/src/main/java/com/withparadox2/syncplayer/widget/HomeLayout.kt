package com.withparadox2.syncplayer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.withparadox2.syncplayer.R

class HomeLayout(context: Context, attributes: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
  FrameLayout(context, attributes, defStyleAttr, defStyleRes), View.OnClickListener {
  private lateinit var topBar: TopBar
  private lateinit var listView: View
  private lateinit var controller: View
  private lateinit var cover: View
  private var isInit = false

  constructor(context: Context, attributes: AttributeSet?, defStyleAttr: Int) : this(
    context,
    attributes,
    defStyleAttr,
    0
  )

  constructor(context: Context, attributes: AttributeSet?) : this(
    context,
    attributes,
    0
  )

  constructor(context: Context) : this(
    context, null
  )

  override fun onFinishInflate() {
    super.onFinishInflate()
    topBar = findViewById(R.id.top_bar)
    listView = findViewById(R.id.listview_songs)
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
    if (cover.visibility == View.VISIBLE) {
      return
    }
    topBar.animate().translationY(0f).setDuration(300)
      .setListener(object : AnimatorListenerAdapter() {

      }).start()
    cover.alpha = 0f
    cover.visibility = View.VISIBLE
    cover.animate().setListener(null).alpha(0.8f).setDuration(300).start()
  }

  fun hideTopBar() {
    topBar.animate().translationY(topBar.getDefaultTranslate().toFloat()).setDuration(300)
      .setListener(null).start()
    cover.animate().alpha(0f).setDuration(300)
      .setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          cover.visibility = View.GONE
        }
      }).start()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    val lvParams = listView.layoutParams as LayoutParams
    if (!isInit) {
      isInit = true
      controller.translationY = controller.height.toFloat()
      topBar.translationY = topBar.getDefaultTranslate().toFloat()
      lvParams.topMargin = topBar.getShowHeight()
      listView.requestLayout()
    }

    if (lvParams.bottomMargin != 0 && lvParams.bottomMargin != controller.height) {
      lvParams.bottomMargin = controller.height
      listView.requestLayout()
    }
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.cover) {
      hideTopBar()
    }
  }
}