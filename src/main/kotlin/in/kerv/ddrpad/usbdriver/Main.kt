package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.toControlBytes
import `in`.kerv.ddrpad.usbdriver.VirtualDdrPadMappings.toEventCode
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.jvm.optionals.getOrNull
import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbDirection
import net.codecrete.usb.UsbEndpoint
import uk.co.bithatch.linuxio.InputDevice

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
  val ddrPadHandle = Usb.findDevice(DdrPadUsbIds.vendorId, DdrPadUsbIds.productId).getOrNull()
  checkNotNull(ddrPadHandle) { "DDRPad device not found." }

  customDriverContext(ddrPadHandle) {
    deviceCommunicationContext(ddrPadHandle) {
      val iface = ddrPadHandle.interfaces[0]
      val inboundEndpointNumber = iface.currentAlternate.endpoints.getSingleInbound().number

      interfaceContext(ddrPadHandle, iface.number) {
        virtualControllerContext { virtualDdrPad ->
          var previousControlBytes = ddrPadHandle.transferIn(inboundEndpointNumber).toControlBytes()

          fixedRateTimer(name = "Input processing loop", period = 10 /* milliseconds */) {
            val currentControlBytes =
                ddrPadHandle.transferIn(inboundEndpointNumber).toControlBytes()

            if (currentControlBytes.changedFrom(previousControlBytes)) {
              handleInputChange(currentControlBytes, previousControlBytes, virtualDdrPad)
              previousControlBytes = currentControlBytes
            }
          }

          blockThreadIndefinitely()
        }
      }
    }
  }
}

private fun handleInputChange(
    currentControlBytes: DdrPadInputProcessor.ControlBytes,
    previousControlBytes: DdrPadInputProcessor.ControlBytes,
    virtualDdrPad: InputDevice,
) {
  println(currentControlBytes)

  val currentlyPressedButtons = currentControlBytes.getPressedButtons()
  val previouslyPressedButtons = previousControlBytes.getPressedButtons()

  val newlyPressedButtons = currentlyPressedButtons subtract previouslyPressedButtons
  val newlyReleasedButtons = previouslyPressedButtons subtract currentlyPressedButtons

  for (newlyPressedButton in newlyPressedButtons) {
    virtualDdrPad.pressKey(newlyPressedButton.toEventCode())
  }

  for (newlyReleasedButtons in newlyReleasedButtons) {
    virtualDdrPad.releaseKey(newlyReleasedButtons.toEventCode())
  }
}

private fun blockThreadIndefinitely() {
  while (true) {
    TimeUnit.HOURS.sleep(1)
  }
}

private fun List<UsbEndpoint>.getSingleInbound() = single { it.direction == UsbDirection.IN }

private inline fun virtualControllerContext(
    block: (virtualDdrPad: InputDevice) -> Unit,
) {
  InputDevice("Virtual DDRPad", 0xdead, 0xbeef).use { virtualDdrPad ->
    virtualDdrPad.capabilities += VirtualDdrPadMappings.realToVirtual.values

    virtualDdrPad.open()
    try {
      block(virtualDdrPad)
    } finally {
      virtualDdrPad.close()
    }
  }
}

private inline fun interfaceContext(
    usbDeviceHandle: UsbDevice,
    interfaceNumber: Int,
    block: () -> Unit,
) {
  usbDeviceHandle.claimInterface(interfaceNumber)
  try {
    block()
  } finally {
    usbDeviceHandle.releaseInterface(interfaceNumber)
  }
}

private inline fun deviceCommunicationContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  usbDeviceHandle.open()
  try {
    block()
  } finally {
    usbDeviceHandle.close()
  }
}

private inline fun customDriverContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  usbDeviceHandle.detachStandardDrivers()
  try {
    block()
  } finally {
    usbDeviceHandle.attachStandardDrivers()
  }
}
