package com.withparadox2.syncplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

@SuppressLint("ValidFragment")
class PermissionDialog constructor(private val confirmListener: DialogInterface.OnClickListener?) :
  DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity)
      .setTitle("Ask Permission")
      .setMessage("We need permissions, please!!!")
      .setPositiveButton("Ok", confirmListener)
      .create()
  }
}