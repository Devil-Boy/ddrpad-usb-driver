package `in`.kerv.ddrpad.usbdriver

import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbDirection
import net.codecrete.usb.UsbEndpoint

fun main() {
  val ddrPadHandle = Usb.findDevice(ddrPadIds.vendorId, ddrPadIds.productId).getOrNull()
  checkNotNull(ddrPadHandle) { "DDRPad device not found." }

  customDriverContext(ddrPadHandle) {
    deviceCommunicationContext(ddrPadHandle) {
      val iface = ddrPadHandle.interfaces[0]
      val inboundEndpointNumber = iface.currentAlternate.endpoints.getSingleInbound().number

      interfaceContext(ddrPadHandle, iface.number) {
        repeat(600) {
          val ddrPadInput = DdrPadInput(ddrPadHandle.transferIn(inboundEndpointNumber))
          println(ddrPadInput)
          TimeUnit.MILLISECONDS.sleep(500)
        }
      }
    }
  }

  println("FINISH!")
}

private fun List<UsbEndpoint>.getSingleInbound() = single { it.direction == UsbDirection.IN }

private inline fun interfaceContext(
    usbDeviceHandle: UsbDevice,
    interfaceNumber: Int,
    block: () -> Unit,
) {
  try {
    usbDeviceHandle.claimInterface(interfaceNumber)
    block()
  } finally {
    usbDeviceHandle.releaseInterface(interfaceNumber)
  }
}

private inline fun deviceCommunicationContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  try {
    usbDeviceHandle.open()
    block()
  } finally {
    usbDeviceHandle.close()
  }
}

private inline fun customDriverContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  try {
    usbDeviceHandle.detachStandardDrivers()
    block()
  } finally {
    usbDeviceHandle.attachStandardDrivers()
  }
}

data class UsbDeviceIds(val vendorId: Int, val productId: Int)

val ddrPadIds = UsbDeviceIds(0x054c, 0x0268)

class DdrPadInput(bytes: ByteArray) {
  private val mainArrowsByte = bytes[2].toUByte()
  private val cornerArrowsByte = bytes[3].toUByte()

  fun isUpActive() = (0b00010000.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isDownActive() = (0b01000000.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isLeftActive() = (0b10000000.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isRightActive() = (0b00100000.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isStartActive() = (0b00001000.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isSelectActive() = (0b00000001.toUByte() and mainArrowsByte) != 0.toUByte()

  fun isUpLeftActive() = (0b00100000.toUByte() and cornerArrowsByte) != 0.toUByte()

  fun isUpRightActive() = (0b01000000.toUByte() and cornerArrowsByte) != 0.toUByte()

  fun isDownLeftActive() = (0b10000000.toUByte() and cornerArrowsByte) != 0.toUByte()

  fun isDownRightActive() = (0b00010000.toUByte() and cornerArrowsByte) != 0.toUByte()

  override fun toString() =
      listOfNotNull(
              if (isUpActive()) "up" else null,
              if (isDownActive()) "down" else null,
              if (isLeftActive()) "left" else null,
              if (isRightActive()) "right" else null,
              if (isUpLeftActive()) "up-left" else null,
              if (isUpRightActive()) "up-right" else null,
              if (isDownLeftActive()) "down-left" else null,
              if (isDownRightActive()) "down-right" else null,
              if (isStartActive()) "start" else null,
              if (isSelectActive()) "select" else null,
      ).joinToString(" + ").ifEmpty { "none" }
}
