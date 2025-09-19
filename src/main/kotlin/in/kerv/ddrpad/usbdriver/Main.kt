package `in`.kerv.ddrpad.usbdriver

import org.usb4java.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

fun findDevice(vendorId: Short, productId: Short): Device? {
    val list = DeviceList()
    var result = LibUsb.getDeviceList(null, list)
    if (result < 0) {
        throw LibUsbException("Unable to get device list", result)
    }

    try {
        for (device in list) {
            val descriptor = DeviceDescriptor()
            result = LibUsb.getDeviceDescriptor(device, descriptor)
            if (result < 0) {
                throw LibUsbException("Unable to read device descriptor", result)
            }
            if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                return device
            }
        }
    } finally {
        LibUsb.freeDeviceList(list, true)
    }

    return null
}

fun main() {
    val vendorId: Short = 0x054c
    val productId: Short = 0x0268

    var result = LibUsb.init(null)
    if (result != LibUsb.SUCCESS) {
        throw LibUsbException("Unable to initialize libusb", result)
    }

    val device = findDevice(vendorId, productId)
    if (device == null) {
        System.err.println("Device not found.")
        return
    }

    val handle = DeviceHandle()
    result = LibUsb.open(device, handle)
    if (result != LibUsb.SUCCESS) {
        throw LibUsbException("Unable to open USB device", result)
    }

    try {
        // Detach the kernel driver
        result = LibUsb.detachKernelDriver(handle, 0)
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_NOT_SUPPORTED && result != LibUsb.ERROR_NOT_FOUND) {
            throw LibUsbException("Unable to detach kernel driver", result)
        }


        result = LibUsb.claimInterface(handle, 0)
        if (result != LibUsb.SUCCESS) {
            throw LibUsbException("Unable to claim interface", result)
        }

        val buffer = ByteBuffer.allocateDirect(8)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val transferred = IntBuffer.allocate(1)

        println("Listening for events...")
        while (true) {
            buffer.rewind()
            result = LibUsb.interruptTransfer(handle, 0x81.toByte(), buffer, transferred, 0)
            if (result == LibUsb.SUCCESS) {
                print("Received: ")
                for (i in 0 until transferred.get(0)) {
                    print(String.format("%02x ", buffer.get(i)))
                }
                println()
            } else {
                 throw LibUsbException("Interrupt transfer failed", result)
            }
        }
    } finally {
        LibUsb.releaseInterface(handle, 0)
        LibUsb.close(handle)
        LibUsb.exit(null)
    }
}
