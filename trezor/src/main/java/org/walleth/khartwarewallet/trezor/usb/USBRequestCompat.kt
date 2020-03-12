package org.walleth.khartwarewallet.trezor.usb

import android.hardware.usb.UsbRequest
import android.os.Build
import java.nio.ByteBuffer

internal fun UsbRequest.queueCompat(data: ByteArray, size: Int) =
    queueCompat(ByteBuffer.wrap(data), size)

internal fun UsbRequest.queueCompat(data: ByteBuffer, size: Int) {
    if (Build.VERSION.SDK_INT > 25) {
        queue(data)
    } else {
        @Suppress("DEPRECATION")
        queue(data, size)
    }
}