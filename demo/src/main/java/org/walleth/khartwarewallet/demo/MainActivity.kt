package org.walleth.khartwarewallet.demo

import android.nfc.NfcAdapter.getDefaultAdapter
import android.nfc.TagLostException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.walleth.khartwarewallet.KHardwareManager
import org.walleth.khartwarewallet.enableKhardwareReader

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val nfcAdapter by lazy {
        getDefaultAdapter(this)
    }

    private val cardManager by lazy { KHardwareManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardManager.onCardConnectedListener = { channel ->
            try {
                channel.autoPair("WalletAppletTest")
                channel.autoOpenSecureChannel()
                Log.i(TAG, channel.getStatus().toString())
            } catch (ignored: TagLostException) {} // this can happen - not a big deal

        }

        cardManager.start()
    }

    public override fun onResume() {
        super.onResume()
        nfcAdapter?.enableKhardwareReader(this, cardManager)
    }

    public override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}
