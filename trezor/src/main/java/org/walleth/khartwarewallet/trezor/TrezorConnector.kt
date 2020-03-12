package org.walleth.khartwarewallet.trezor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.delay
import org.walleth.khartwarewallet.trezor.messages.MessageExchangeManager
import org.walleth.khartwarewallet.trezor.model.USBTransport
import org.walleth.khartwarewallet.trezor.usb.UsbPermissionReceiver
import org.walleth.khartwarewallet.trezor.usb.withAddress
import timber.log.Timber

suspend fun tryConnectTrezor(
    context: Context,
    onPermissionDenied: () -> Unit,
    onDeviceConnected: (device: MessageExchangeManager) -> Unit
) {
    val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    while (true) {
        val device = tryConnectTrezor(context, usbManager)
        if (device != null) {
            if (!usbManager.hasPermission(device)) {
                onPermissionDenied.invoke()
            } else {
                connect(device, usbManager)?.let {
                    onDeviceConnected(MessageExchangeManager((it)))
                }
            }

            return
        }
        delay(10)
    }
}

private fun connect(device: UsbDevice, usbManager: UsbManager): USBTransport? {
    val usbInterface = device.getInterface(0)
    val endpoints = (0 until usbInterface.endpointCount).map {
        usbInterface.getEndpoint(it)
    }.filter {
        it.type == UsbConstants.USB_ENDPOINT_XFER_INT
    }
    val readEndpoint = endpoints.withAddress(0x81)
    val writeEndpoint = endpoints.withAddress(0x01) ?: endpoints.withAddress(0x02)

    val connection = usbManager.openDevice(device)
    val error = when {
        readEndpoint == null -> "Could not find read endpoint"
        writeEndpoint == null -> " Could not find write endpoint"
        readEndpoint.maxPacketSize != 64 -> "Wrong packet size for read endpoint"
        writeEndpoint.maxPacketSize != 64 -> "Wrong packet size for write endpoint"
        !connection.claimInterface(usbInterface, true) -> "could not claim interface"
        else -> return USBTransport(connection, writeEndpoint, readEndpoint)
    }

    Timber.e("tryConnectTrezor: $error")
    return null
}

private suspend fun UsbManager.waitForCompatibleDevice(): UsbDevice {
    while (true) {
        deviceList.filter {
            it.value.isTREZORorSemiCompatible()
        }.values.firstOrNull()?.let {
            return it
        }

        delay(10)
    }
}

internal suspend fun tryConnectTrezor(context: Context, usbManager: UsbManager): UsbDevice? {

    val device = usbManager.waitForCompatibleDevice()

    if (!usbManager.hasPermission(device)) {
        val intent = Intent(UsbPermissionReceiver.ACTION)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        usbManager.requestPermission(device, pendingIntent)

        val permissionReceiver = UsbPermissionReceiver(context)
        permissionReceiver.register()
        while (permissionReceiver.permissionResult == null) {
            if (usbManager.hasPermission(device)) { // fallback for broadcast
                permissionReceiver.permissionResult == true
            }
            delay(100)
        }
        permissionReceiver.unRegister()
        if (permissionReceiver.permissionResult == false) {
            return device
        }
    }

    return device
}