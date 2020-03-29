package com.withparadox2.syncplayer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.withparadox2.syncplayer.R

class TopBar(context: Context, attributes: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
  LinearLayout(context, attributes, defStyleAttr, defStyleRes), View.OnClickListener {
  lateinit var tvState: TextView

  private lateinit var decorLine: View
  private lateinit var tvServer: View
  private lateinit var tvClient: View
  lateinit var tvController: TextView

  var isCheckedServer = true
  private var delegate: Delegate? = null

  private var isStart: Boolean = false

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

  fun getShowHeight(): Int {
    return measuredHeight - tvState.top
  }

  fun getDefaultTranslate(): Int {
    return getShowHeight() - measuredHeight
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    setOnClickListener(this)
    tvState = findViewById(R.id.tv_state)
    decorLine = findViewById(R.id.decor_line)
    tvServer = findViewById(R.id.tv_server)
    tvClient = findViewById(R.id.tv_client)
    tvController = findViewById(R.id.tv_controller)

    tvClient.setOnClickListener(this)
    tvServer.setOnClickListener(this)
    tvController.setOnClickListener(this)

    setControllerState(isStart)
  }

  fun checkServer() {
    isCheckedServer = true
    decorLine.animate().translationX(0f).setDuration(300)
      .start()
  }

  fun checkClient() {
    isCheckedServer = false
    decorLine.animate().translationX((tvClient.left - tvServer.left).toFloat()).setDuration(300)
      .start()
  }

  override fun onClick(v: View?) {
    if (v?.id == R.id.tv_client) {
      if (isCheckedServer) {
        checkClient()
      }
    } else if (v?.id == R.id.tv_server) {
      if (!isCheckedServer) {
        checkServer()
      }
    } else if (v?.id == R.id.tv_controller) {
      isStart = !isStart
      delegate?.onClickController(isCheckedServer, isStart)
      setControllerState(isStart)
    }
  }

  fun setDelegate(delegate: Delegate) {
    this.delegate = delegate
  }

  private fun setControllerState(isStart: Boolean) {
    if (isStart) {
      tvController.text = "关闭"
    } else {
      tvController.text = "开启"
    }
  }

  interface Delegate {
    fun onClickController(isServer: Boolean, isStart: Boolean)
  }
}