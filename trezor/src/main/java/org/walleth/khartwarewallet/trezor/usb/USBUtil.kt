package org.walleth.khartwarewallet.trezor.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbRequest
import timber.log.Timber

/**
 * cares for opening and closing an USB request
 */
internal fun <T> withUSBRequest(
    usbConnection: UsbDeviceConnection,
    endpoint: UsbEndpoint,
    action: (usbRequest: UsbRequest) -> T
): T? {
    val request = UsbRequest()
    return if (!request.initialize(usbConnection, endpoint)) {
        Timber.e("writeMessage: could not initialize USB writeEndpoint")
        null
    } else {
        action.invoke(request).also {
            request.close()
        }
    }
}

fun List<UsbEndpoint>.withAddress(address: Int)  = firstOrNull {
    it.address == address
}