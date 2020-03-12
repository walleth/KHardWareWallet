package org.walleth.khartwarewallet.trezor.messages

import com.squareup.wire.Message
import okio.Buffer
import org.walleth.khartwarewallet.trezor.model.*
import org.walleth.khartwarewallet.trezor.usb.queueCompat
import org.walleth.khartwarewallet.trezor.usb.withUSBRequest
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.text.Charsets.US_ASCII

class MessageExchangeManager(private val usb: USBTransport) {

    private val chunkMagic = ByteArray(1) { '?'.toByte() }

    fun exchangeMessage(m: Message<*, *>): Message<*, *>? {
        writeMessage(m)
        return readMessage()
    }

    fun disconnect() {
        usb.connection.close()
    }

    private fun readMessage() =
        withUSBRequest(usb.connection, usb.readEndpoint) { request ->

            var msg: TrezorMessage? = null
            var invalidChunks = 0
            while (invalidChunks < MAX_INVALID_CHUNKS && msg?.isFullyRead() != true) {
                val chunkBuffer = ByteBuffer.allocate(CHUNK_SIZE)
                request.queueCompat(chunkBuffer, CHUNK_SIZE)
                usb.connection.requestWait()
                val okioBuffer = Buffer().apply { write(chunkBuffer.array()) }
                if (okioBuffer.readByte() != '?'.toByte()) {
                    invalidChunks++
                } else {
                    if (msg == null) {
                        if (okioBuffer.readString(2, US_ASCII) != "##") {
                            invalidChunks++
                        } else {
                            msg = okioBuffer.readFirstMessageChunk()
                        }
                    } else {
                        okioBuffer.readNextMessageChunk(msg)
                    }
                }
            }

            msg?.let {
                msg.data.parseMessageWithType(it.type)
            }
        }

    private fun writeMessage(msg: Message<*, *>) {
        withUSBRequest(usb.connection, usb.writeEndpoint) { request ->
            val okIoBuffer = msg.withFrameAsBuffer()

            val chunks: Int = okIoBuffer.size.toInt() / CHUNK_CONTENT_SIZE
            Timber.i(String.format("writeMessage: Writing %d chunks", chunks))

            for (i in 0 until chunks) {
                val chunk = chunkMagic + okIoBuffer.readByteArray(CHUNK_CONTENT_SIZE_LONG)
                request.queueCompat(chunk, CHUNK_SIZE)
                usb.connection.requestWait()
            }
        }
    }
}