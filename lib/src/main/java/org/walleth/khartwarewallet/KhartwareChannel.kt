package org.walleth.khartwarewallet

import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlvParser
import im.status.hardwallet_lite_android.io.CardChannel
import im.status.hardwallet_lite_android.wallet.WalletAppletCommandSet
import im.status.hardwallet_lite_android.wallet.WalletAppletCommandSet.GET_STATUS_P1_APPLICATION
import org.kethereum.bip39.model.MnemonicWords


class KhartwareChannel(cardChannel: CardChannel) {

    private var cmdSet = WalletAppletCommandSet(cardChannel)
    private val blvParser by lazy { BerTlvParser() }

    init {
        cmdSet.select().checkOK()
    }

    fun autoPair(password: String) = cmdSet.autoPair(password)

    fun autoOpenSecureChannel() = cmdSet.autoOpenSecureChannel()

    fun generateMnemonic(checksumLength: Int, wordList: List<String>) =
        cmdSet.generateMnemonic(checksumLength).checkOK().data.let { responseList ->

            if (wordList.size != 2048) {
                throw java.lang.IllegalArgumentException("Wordlist must have a size of 2048 - but was" + wordList.size)
            }

            if (responseList.size != 24) {
                throw java.lang.IllegalStateException("Expected the result data to be 24 bytes but was ${responseList.size}")
            }

            val indexList = (0..11).map {
                responseList[it * 2].toPositiveInt().shl(8) or responseList[it * 2 + 1].toPositiveInt()
            }

            MnemonicWords(indexList.map { wordList[it] })
        }

    fun getStatus(): KhartwareStatus {
        val bytes = cmdSet.getStatus(GET_STATUS_P1_APPLICATION).checkOK()

        val list = blvParser.parse(bytes.data).list

        if (list.size != 1 || !list.first().isTag(BerTag(0xa3))) {
            throw IllegalStateException("unexpected status response")
        }

        val valuesList = list.first().values
        return KhartwareStatus(
            valuesList[0].intValue,
            valuesList[1].intValue,
            valuesList[2].intValue == 0xff,
            valuesList[3].intValue == 0xff
        )


    }

    fun verifyPIN(pin: String) {
        cmdSet.verifyPIN(pin).checkOK()
    }

    fun unpairOthers() = cmdSet.unpairOthers()
    fun autoUnpair() = cmdSet.autoUnpair()

}

// TODO replace with native uint when migrating to Kotlin 1.3
fun Byte.toPositiveInt() = toInt() and 0xFF