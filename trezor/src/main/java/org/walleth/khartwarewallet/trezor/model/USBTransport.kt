package org.walleth.khartwarewallet.trezor.model

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint

class USBTransport(
    val connection: UsbDeviceConnection,
    val writeEndpoint: UsbEndpoint,
    val readEndpoint: UsbEndpoint
)