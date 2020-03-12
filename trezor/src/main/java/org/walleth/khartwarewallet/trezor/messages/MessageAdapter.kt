package org.walleth.khartwarewallet.trezor.messages

import com.squareup.wire.Message
import io.trezor.deviceprotocol.MessageType
import okio.Buffer
import org.walleth.khartwarewallet.trezor.model.CHUNK_CONTENT_SIZE
import org.walleth.khartwarewallet.trezor.model.TrezorMessage
import kotlin.text.Charsets.US_ASCII

internal fun ByteArray.parseMessageWithType(type: MessageType): Message<*, *>? {
    val packageName = "io.trezor.deviceprotocol"
    val className = "$packageName.${type.name.replace("MessageType_", "")}"
    val cls = Class.forName(className)
    val field = cls.getField("ADAPTER")
    val method = field.type.getMethod("decode", ByteArray::class.java)
    val invoke = method.invoke(field.get(null), this)
    return invoke as Message<*, *>
}

internal fun getMessageId(message: Message<*, *>) =
    MessageType.valueOf(messageTypeName(message)).value

private fun messageTypeName(message: Message<*, *>) =
    "MessageType_" + message.javaClass.simpleName.replace("KeepKey", "")

internal fun Message<*, *>.withFrameAsBuffer() = encode().let { encoded ->
    Buffer().apply {
        writeString("##", US_ASCII)
        writeShort(getMessageId(this@withFrameAsBuffer))
        writeInt(encoded.size)
        write(encoded)
        write(ByteArray(CHUNK_CONTENT_SIZE - (size.toInt() % CHUNK_CONTENT_SIZE)))
    }
}

internal fun Buffer.readNextMessageChunk(msgFrame: TrezorMessage) {
    val byteCount = if (msgFrame.data.size + CHUNK_CONTENT_SIZE < msgFrame.size) {
        CHUNK_CONTENT_SIZE
    } else {
        msgFrame.size - msgFrame.data.size
    }

    msgFrame.data = msgFrame.data + readByteArray(byteCount.toLong())
}

internal fun Buffer.readFirstMessageChunk(): TrezorMessage {
    val type = MessageType.fromValue(readShort().toInt())
    val msgSize = readInt()
    val data = readByteArray(if (msgSize > size) size else msgSize.toLong())
    return TrezorMessage(type, msgSize, data)
}

internal fun TrezorMessage.isFullyRead() = data.size >= size
