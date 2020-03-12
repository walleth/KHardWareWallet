package org.walleth.khartwarewallet.trezor.model

import io.trezor.deviceprotocol.MessageType

class TrezorMessage(
    val type: MessageType,
    val size: Int,
    var data: ByteArray = ByteArray(size)
)