package com.withparadox2.syncplayer

import android.app.Activity
import android.content.Context
import android.widget.Toast

fun Context.toast(text: CharSequence) {
  (this as Activity).runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
  }
}