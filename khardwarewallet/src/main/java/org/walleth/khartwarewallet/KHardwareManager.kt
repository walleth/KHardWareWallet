package org.walleth.khartwarewallet

import android.content.ContentValues.TAG
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.util.Log
import im.status.keycard.android.NFCCardChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.security.Security

class KHardwareManager : NfcAdapter.ReaderCallback {

    private var isoDep: IsoDep? = null
    private var isInvokingListener: Boolean = false

    var onCardConnectedListener: ((channel: KHardwareChannel) -> Unit)? = null

    fun isConnected() = isoDep != null && isoDep!!.isConnected

    init {
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

    override fun onTagDiscovered(tag: Tag) {
        isoDep = IsoDep.get(tag)

        try {
            isoDep = IsoDep.get(tag)
            isoDep!!.connect()
            isoDep!!.timeout = 120000
        } catch (e: IOException) {
            Log.e(TAG, "error connecting to tag")
        }

    }

    fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            startSuspended()
        }
    }

    private suspend fun startSuspended() {
        var connected = isConnected()

        while (true) {
            val newConnected = isConnected()
            if (newConnected != connected) {
                connected = newConnected
                Log.i(TAG, "tag " + if (connected) "connected" else "disconnected")

                if (connected && !isInvokingListener) {
                    onCardConnected()
                } else {
                    onCardDisconnected()
                }
            }

            delay(50)
        }
    }

    private fun onCardConnected() {
        isInvokingListener = true

        try {
            val channel = KHardwareChannel(NFCCardChannel(isoDep))
            onCardConnectedListener?.invoke(channel)

        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (ignored: TagLostException) {
        } // this can happen - not a big deal

        isInvokingListener = false
    }

    private fun onCardDisconnected() {
        isInvokingListener = false
        isoDep = null
    }

}
