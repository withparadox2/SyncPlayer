package com.withparadox2.syncplayer.widget

import android.content.Context
import android.widget.LinearLayout

class TopBar(context: Context) : LinearLayout(context) {
  fun getShowHeight(): Int {
    return 0
  }

  fun getDefaultTranslate(): Int {
    return getShowHeight() - measuredHeight
  }
}