package org.walleth.khartwarewallet

data class KhartwareCardInfo(
    val instanceUID: ByteArray,
    val pubKey : ByteArray,
    val version : String,
    val remainingPairingSlots: Byte,
    val keyUID: ByteArray
)