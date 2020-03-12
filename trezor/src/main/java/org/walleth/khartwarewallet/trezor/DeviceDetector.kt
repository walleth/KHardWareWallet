package org.walleth.khartwarewallet.trezor

import android.hardware.usb.UsbDevice

internal fun UsbDevice.isTREZOR() = interfaceCount > 0 && (isTREZOR1() || isTREZOR2())
internal fun UsbDevice.isTREZORorSemiCompatible() = isTREZOR() || isKeepKey()

private fun UsbDevice.isTREZOR1() = vendorId == 0x534c && productId == 0x0001
private fun UsbDevice.isTREZOR2() = vendorId == 0x1209 && (0x53c0..0x53c1).contains(productId)
private fun UsbDevice.isKeepKey() = vendorId == 0x2b24 && productId == 2
