package org.walleth.khartwarewallet.demo

import android.nfc.NfcAdapter.getDefaultAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.walleth.khartwarewallet.KHardwareManager
import org.walleth.khartwarewallet.enableKhardwareReader

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val nfcAdapter by lazy {
        getDefaultAdapter(this)
    }

    private val cardManager by lazy { KHardwareManager() }

    private var currentInfoText: String? = null
        set(value) {
            field = value
            runOnUiThread {
                info_text.text = field
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardManager.onCardConnectedListener = { channel ->
            currentInfoText = "card detected"

            channel.autoPair("WalletAppletTest")
            currentInfoText += "\ncard paired"

            channel.autoOpenSecureChannel()
            currentInfoText += "\nsecure channel established"

            val status = channel.getStatus().toString()

            currentInfoText += "\ncard status $status"
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
