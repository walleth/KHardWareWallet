package org.walleth.khartwarewallet.trezor.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class UsbPermissionReceiver(private val context: Context) : BroadcastReceiver() {

    var permissionResult: Boolean? = null

    fun register() {
        context.registerReceiver(this, IntentFilter(ACTION))
    }

    fun unRegister() {
        context.unregisterReceiver(this)
    }

    companion object {
        val ACTION = UsbPermissionReceiver::class.java.name + ".ACTION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        permissionResult = intent?.extras?.get("permission") as? Boolean ?: false
    }
}