package org.walleth.khartwarewallet.demo

import android.graphics.drawable.BitmapDrawable
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter.getDefaultAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import net.glxn.qrgen.android.QRCode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.DEFAULT_GAS_PRICE
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.functions.encodeRLP
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.khartwarewallet.KHardwareManager
import org.walleth.khartwarewallet.enableKhardwareReader
import org.walleth.khex.toHexString
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigInteger.ZERO
import java.math.BigInteger.valueOf
import java.security.Security

class MainActivity : AppCompatActivity() {

    private val nfcAdapter by lazy {
        getDefaultAdapter(this)
    }

    private val cardManager by lazy { KHardwareManager() }

    private var currentInfoText: String? = null
        set(value) {
            field = value
            runOnUiThread {
                info_text.text = Html.fromHtml(field)
                info_text.movementMethod = LinkMovementMethod()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
        setContentView(R.layout.activity_main)

        cardManager.onCardConnectedListener = { channel ->

            try {
                if (!channel.getCardInfo().isInitializedCard) {
                    currentInfoText = "Card detected but not initialized .."
                    val res = channel.commandSet.init("000000","123456789012","foo")

                    currentInfoText += ".. done $res"
                } else {
                    currentInfoText = "Card detected with version" + channel.getCardInfo().appVersionString

                    channel.commandSet.autoPair("foo")
                    currentInfoText += "\nCard paired"

                    channel.commandSet.autoOpenSecureChannel()
                    currentInfoText += "\nSecure channel established"

                    when (mode_radio_group.checkedRadioButtonId) {
                        R.id.mode_radio_set_ndef -> {

                            val status = channel.getStatus().toString()

                            currentInfoText += "\nCard status $status"

                            channel.commandSet.verifyPIN("000000")

                            val ndef = NdefMessage(NdefRecord.createApplicationRecord("org.walleth")).toByteArray()
                            val res = channel.commandSet.setNDEF(ndef)

                            currentInfoText += "\nNDEF isOK:${res.isOK}"
                        }
                        R.id.mode_radio_check_status -> {

                            val status = channel.getStatus().toString()

                            currentInfoText += "\nCard status $status"

                            channel.commandSet.verifyPIN("000000")
                        }
                        R.id.mode_radio_show_qr_code -> {
                            channel.commandSet.verifyPIN("000000")

                            val address = channel.toPublicKey().toAddress()

                            currentInfoText += "\nCard address $address"

                            runOnUiThread {
                                qrcode_image.setQRCode("ethereum:$address")
                                qrcode_image.visibility = View.VISIBLE
                            }
                        }

                        R.id.mode_radio_check_generate_mnemonic -> {

                            val mnemonic = channel.generateMnemonic(4, WORDLIST_ENGLISH)

                            currentInfoText += "\nGenerated Mnemonic $mnemonic"

                            channel.commandSet.verifyPIN("000000")
                        }


                        R.id.mode_radio_new_key -> {

                            channel.commandSet.verifyPIN("000000")

                            channel.commandSet.generateKey()

                            currentInfoText += "\nNew Key generated"
                        }

                        R.id.mode_radio_remove_key -> {

                            channel.commandSet.verifyPIN("000000")

                            channel.commandSet.removeKey()

                            currentInfoText += "\nKey removed"

                        }

                        R.id.mode_radio_sign_text -> {
                            channel.commandSet.verifyPIN("000000")
                            val address = channel.toPublicKey().toAddress()

                            val message = "foo"
                            val signText = channel.signText(message)

                            val revoceredAddress=signedMessageToKey("foo".toByteArray(),signText).toAddress()

                            currentInfoText += "\nsigned by:$address\nrecovered:$revoceredAddress\n$signText"
                        }
                        R.id.mode_radio_create_transaction -> {

                            channel.commandSet.verifyPIN("000000")

                            val address = channel.toPublicKey().toAddress()

                            val tx = Transaction(
                                chain = 5L,
                                creationEpochSecond = null,
                                from = address,
                                gasLimit = DEFAULT_GAS_LIMIT,
                                gasPrice = DEFAULT_GAS_PRICE,
                                input = emptyList(),
                                nonce = ZERO,
                                to = Address("0x381e247bef0ebc21b6611786c665dd5514dcc31f"),
                                txHash = null,
                                value = valueOf(42L)
                            )

                            val signedTx = channel.sign(tx)
                            val rlp = signedTx.encodeRLP().toHexString()

                            currentInfoText += "\n ${signedTx.signatureData} \n"
                            currentInfoText += "\nSigned transaction <a href='https://api-goerli.etherscan.io/api?module=proxy&action=eth_sendRawTransaction&hex=$rlp'>link</a>"

                            currentInfoText += "\n\n from <a href='https://goerli.etherscan.io/address/$address'>address</a>"

                        }
                    }

                    channel.commandSet.unpairOthers()
                    channel.commandSet.autoUnpair()
                }
            } catch (e: Exception) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val exceptionAsString = sw.toString()

                currentInfoText += "\n\nException: " + e.message
                currentInfoText += "\n\nTrace: $exceptionAsString"

                e.printStackTrace()
            }
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

fun ImageView.setQRCode(content: String) {
    val drawable = BitmapDrawable(resources, QRCode.from(content).bitmap())
    drawable.setAntiAlias(false)
    drawable.isFilterBitmap = false
    setImageDrawable(drawable)
}