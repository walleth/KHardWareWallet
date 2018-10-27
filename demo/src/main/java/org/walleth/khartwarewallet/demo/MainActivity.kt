package org.walleth.khartwarewallet.demo

import android.nfc.NfcAdapter.getDefaultAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
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
            currentInfoText = "Card detected"

            channel.autoPair("WalletAppletTest")
            currentInfoText += "\nCard paired"

            channel.autoOpenSecureChannel()
            currentInfoText += "\nSecure channel established"

            when (mode_radio_group.checkedRadioButtonId) {
                R.id.mode_radio_check_status -> {

                    val status = channel.getStatus().toString()

                    currentInfoText += "\nCard status $status"

                    channel.verifyPIN("000000")
                }


                R.id.mode_radio_check_generate_mnemonic -> {

                    val mnemonic = channel.generateMnemonic(4, WORDLIST_ENGLISH)

                    currentInfoText += "\nGenerated Mnemonic $mnemonic"

                    channel.verifyPIN("000000")
                }


                R.id.mode_radio_new_key -> {

                    channel.verifyPIN("000000")

                    channel.initWithNewKey()

                    currentInfoText += "\nNew Key uploaded"
                }

                R.id.mode_radio_remove_key -> {

                    channel.verifyPIN("000000")

                    channel.removeKey()

                    currentInfoText += "\nKey removed"

                }

            }

            channel.unpairOthers()
            channel.autoUnpair()

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
