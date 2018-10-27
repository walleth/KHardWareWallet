package org.walleth.khartwarewallet

import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlvParser
import im.status.hardwallet_lite_android.io.CardChannel
import im.status.hardwallet_lite_android.wallet.WalletAppletCommandSet
import im.status.hardwallet_lite_android.wallet.WalletAppletCommandSet.GET_STATUS_P1_APPLICATION


class KhartwareChannel(cardChannel: CardChannel) {

    private var cmdSet = WalletAppletCommandSet(cardChannel)
    private val blvParser by lazy { BerTlvParser() }

    init {
        cmdSet.select().checkOK()
    }

    fun autoPair(password: String) = cmdSet.autoPair(password)

    fun autoOpenSecureChannel() = cmdSet.autoOpenSecureChannel()

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

}