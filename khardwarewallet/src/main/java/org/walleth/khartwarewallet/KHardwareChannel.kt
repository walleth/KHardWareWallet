package org.walleth.khartwarewallet

import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlv
import com.payneteasy.tlv.BerTlvParser
import im.status.keycard.applet.ApplicationInfo
import im.status.keycard.applet.ApplicationStatus.TLV_APPLICATION_STATUS_TEMPLATE
import im.status.keycard.applet.KeycardCommandSet
import im.status.keycard.applet.KeycardCommandSet.GET_STATUS_P1_APPLICATION
import im.status.keycard.applet.TinyBERTLV
import im.status.keycard.io.CardChannel
import okio.buffer
import okio.source
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.extensions.toBigInteger
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.kethereum.model.SignedTransaction
import org.kethereum.model.Transaction
import org.walleth.khex.toHexString
import java.math.BigInteger

class KHardwareChannel(cardChannel: CardChannel) {

    var commandSet = KeycardCommandSet(cardChannel)
    private val blvParser by lazy { BerTlvParser() }

    fun getCardInfo() = ApplicationInfo(commandSet.select().checkOK().data)

    private fun ByteArray.toPublicKey(): PublicKey {

        if (first() != 4.toByte()) { // compression signaling
            throw java.lang.IllegalStateException("public key must start with 0x04 but was " + first() + " (full " + toHexString() + " size:$size )")
        }

        return PublicKey(copyOfRange(1, size))
    }

    fun generateMnemonic(checksumLength: Int, wordList: List<String>) =
        commandSet.generateMnemonic(checksumLength).checkOK().data.let { data ->

            if (wordList.size != 2048) {
                throw java.lang.IllegalArgumentException("Wordlist must have a size of 2048 - but was" + wordList.size)
            }

            if (data.size != 24) {
                throw java.lang.IllegalStateException("Expected the result data to be 24 bytes but was ${data.size}")
            }

            val buffer = data.inputStream().source().buffer()

            val mnemonicWords =  (0..11).map {
                wordList[buffer.readShort().toInt()]
            }

            MnemonicWords(mnemonicWords)
        }

    fun getStatus(): KhartwareStatus {
        val bytes = commandSet.getStatus(GET_STATUS_P1_APPLICATION).checkOK().data

        val tinyBERTLV = TinyBERTLV(bytes)
        tinyBERTLV.enterConstructed(TLV_APPLICATION_STATUS_TEMPLATE.toInt())

        return KhartwareStatus(
            tinyBERTLV.readInt(),
            tinyBERTLV.readInt(),
            tinyBERTLV.readBoolean()
        )
    }

    private var publicKey: PublicKey? = null

    fun toPublicKey(): PublicKey = commandSet.exportCurrentKey(true).checkOK().data.let {
        val parsed = blvParser.parse(it)
        publicKey = parsed.list.first().values.first().bytesValue.toPublicKey()
        publicKey!!
    }

    fun signText(string: String): SignatureData {

        val (leafList, recId) = sign(string.toByteArray().keccak())
        return SignatureData(
            r = BigInteger(leafList.first().bytesValue),
            s = BigInteger(leafList.last().bytesValue),
            v = recId.toBigInteger() + BigInteger.valueOf(27)
        )
    }

    fun sign(tx: Transaction): SignedTransaction {
        val chainId = tx.chain!!
        val encodeRLPHash = tx.encodeRLP(SignatureData().apply { v = chainId }).keccak()

        val (leafList, recId) = sign(encodeRLPHash)

        val signatureData = SignatureData(
            r = BigInteger(leafList.first().bytesValue),
            s = BigInteger(leafList.last().bytesValue),
            v = (recId.toBigInteger() + chainId * BigInteger.valueOf(2) + BigInteger.valueOf(8) + BigInteger.valueOf(27))
        )

        return SignedTransaction(tx, signatureData)
    }

    private fun sign(encodeRLPHash: ByteArray): Pair<MutableList<BerTlv>, Int> {
        val signedTransaction = commandSet.sign(encodeRLPHash).checkOK().data

        val parsed = blvParser.parse(signedTransaction)

        val rootList = parsed.list
        if (rootList.size != 1 || rootList.first().tag != BerTag(0xa0)) {
            throw IllegalArgumentException("Unexpected Signing result " + rootList)
        }

        val innerList = rootList.first().values

        if (innerList.size != 2 || innerList.last().tag != BerTag(0x30)) {
            throw IllegalArgumentException("Unexpected Signing result (level 2) " + innerList.size + " " + innerList.last().tag)
        }

        val leafList = innerList.last().values


        if (leafList.size != 2 || leafList.first().tag != BerTag(0x02) || leafList.last().tag != BerTag(0x02)) {
            throw IllegalArgumentException("Unexpected Signing result (leaf) $leafList")
        }

        val recId = ECDSASignature(
            leafList.first().bytesValue.toBigInteger(),
            leafList.last().bytesValue.toBigInteger()
        ).determineRecId(encodeRLPHash, publicKey!!)
        return Pair(leafList, recId)
    }

}