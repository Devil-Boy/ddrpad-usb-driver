package `in`.kerv.ddrpad.usbdriver

import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbException
import net.codecrete.usb.UsbDirection
import kotlin.jvm.optionals.getOrNull

fun main() {
  val ddrPadHandle = Usb.findDevice(ddrPadIds.vendorId, ddrPadIds.productId).getOrNull()
  checkNotNull(ddrPadHandle) { "DDRPad device not found." }

  ddrPadHandle.open()
  ddrPadHandle.claimInterface(0)

  try {
    while (true) {
      ddrPadHandle.transferIn(0).print()
    }
  } finally {
    ddrPadHandle.close()
  }
}
private fun ByteArray.print() {
  for (byte in this) {
    print(String.format("%02x ", byte))
  }
}

data class UsbDeviceIds(val vendorId: Int, val productId: Int)

val ddrPadIds = UsbDeviceIds(0x054c, 0x0268)