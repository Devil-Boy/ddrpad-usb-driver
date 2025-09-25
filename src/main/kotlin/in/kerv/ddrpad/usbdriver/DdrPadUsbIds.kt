package `in`.kerv.ddrpad.usbdriver

import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbDevicePredicate

/**
 * Defines the USB Vendor ID and Product ID for the DDR Pad.
 */
object DdrPadUsbIds {
  val vendorId = 0x054c // Sony Corporation
  val productId = 0x0268 // PlayStation(R)3 Controller

  fun matches(device: UsbDevice) = device.vendorId == vendorId && device.productId == productId
}
