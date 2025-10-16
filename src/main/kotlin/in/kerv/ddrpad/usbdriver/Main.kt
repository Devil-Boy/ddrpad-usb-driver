package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.toControlBytes
import `in`.kerv.ddrpad.usbdriver.VirtualDdrPadMappings.toEventCode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbDirection
import net.codecrete.usb.UsbException
import net.codecrete.usb.linux.LinuxUsbException
import uk.co.bithatch.linuxio.InputDevice
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private val unhandledDdrPads = Channel<UsbDevice>(Channel.UNLIMITED)

private val logger = KotlinLogging.logger { }

/** Main entry point for the DDRPad USB driver. */
@OptIn(ExperimentalUnsignedTypes::class)
fun main() = runBlocking {
  collectAnyAlreadyConnectedDdrPads()
  listenForAndCollectNewlyConnectedDdrPads()

  // Using a Kotlin Channel, we can block/suspend the main thread on waiting for new DdrPad devices
  logger.debug { "Waiting on unhandled DDRPads..." }
  for (unhandledDdrPad in unhandledDdrPads) {
    logger.debug { "Received USB device: ${unhandledDdrPad.debugIdentifierString()}" }
    launch(Dispatchers.IO) {
      logger.debug { "Dispatched coroutine for: ${unhandledDdrPad.debugIdentifierString()}" }
      continuouslyMapDdrPadInputsToVirtualGamepad(unhandledDdrPad)
    }
  }
}

private fun collectAnyAlreadyConnectedDdrPads() {
  for (ddrPad in Usb.findDevices(DdrPadUsbIds::matches)) {
    unhandledDdrPads.trySend(ddrPad)
  }
}

/**
 * Sets up a listener to collect newly connected DDRPads.
 */
private fun listenForAndCollectNewlyConnectedDdrPads() {
  Usb.setOnDeviceConnected {
    if (DdrPadUsbIds.matches(it)) {
      unhandledDdrPads.trySend(it)
    }
  }
}

/**
 * Continuously maps inputs from a physical DDRPad to a virtual gamepad.
 *
 * This function handles the entire lifecycle of a single DDRPad device, from claiming its interface
 * to continuously reading its input and translating it into virtual gamepad events. It also manages
 * error handling and resource cleanup for the device.
 *
 * @param ddrPadHandle The [UsbDevice] representing the physical DDRPad.
 */
private suspend fun continuouslyMapDdrPadInputsToVirtualGamepad(ddrPadHandle: UsbDevice) {
  customDriverContext(ddrPadHandle) {
    deviceCommunicationContext(ddrPadHandle) {
      logger.debug { "Interfaces: ${ddrPadHandle.interfaces}" }

      if (ddrPadHandle.interfaces.size != 1) {
        // As far as we know, the DDRPad only has a single interface on it
        return@deviceCommunicationContext
      }

      val usbInterface = try {
        ddrPadHandle.interfaces.single()
      } catch (exception: UsbException) {
        logger.debug(exception) { "Failed to grab interface!" }
        return@deviceCommunicationContext
      }

      logger.debug { "Alternates: ${usbInterface.alternates}" }

      if (usbInterface.alternates.size != 1) {
        // As far as we know, the DDRPad only has a single alternate interface on it
        return@deviceCommunicationContext
      }

      val usbInterfaceAlternate = usbInterface.alternates.single()

      logger.debug { "Endpoints: ${usbInterfaceAlternate.endpoints.joinToString { "${it.direction}@${it.number}" }}" }

      val usbInboundEndpoints = usbInterfaceAlternate.endpoints.filter { it.direction == UsbDirection.IN }
      if (usbInboundEndpoints.size != 1) {
        // As far as we know, the DDRPad only has a single input endpoint on it
        return@deviceCommunicationContext
      }

      val usbInboundEndpointNumber = usbInboundEndpoints.single().number

      interfaceContext(ddrPadHandle, usbInterface.number) {
        virtualControllerContext { virtualDdrPad ->
          var previousControlBytes = try {
            ddrPadHandle.transferIn(usbInboundEndpointNumber).toControlBytes()
          } catch (exception: UsbException) {
            logger.error(exception) { "Failed first attempt to read the control bytes! Maybe this isn't a DDRPad?" }
            return@virtualControllerContext
          }

          while (true) {
            val currentControlBytes = try {
              ddrPadHandle.transferIn(usbInboundEndpointNumber).toControlBytes()
            } catch (exception: UsbException) {
              logger.debug(exception) { "If the DDRPad was unplugged, this is intended. Otherwise, something went wrong." }
              break
            }

            // Used to discover the dance pad raw input values
            //logger.debug { currentControlBytes }

            if (currentControlBytes.changedFrom(previousControlBytes)) {
              handleInputChange(currentControlBytes, previousControlBytes, virtualDdrPad)
              previousControlBytes = currentControlBytes
            }

            // This is just an arbitrarily low value right now. We can probably go lower before failing reads, but this works fine based on my limited testing. (A better DDR player can correct me.)
            delay(2.milliseconds)
          }
        }
      }
    }
  }
}

/**
 * Handles changes in the DDRPad input, pressing or releasing virtual keys accordingly.
 *
 * @param currentControlBytes The current state of the DDRPad control bytes.
 * @param previousControlBytes The previous state of the DDRPad control bytes.
 * @param virtualDdrPad The virtual input device representing the DDRPad.
 */
private fun handleInputChange(
  currentControlBytes: DdrPadInputProcessor.ControlBytes,
  previousControlBytes: DdrPadInputProcessor.ControlBytes,
  virtualDdrPad: InputDevice,
) {
  val currentlyPressedButtons = currentControlBytes.getPressedButtons()
  val previouslyPressedButtons = previousControlBytes.getPressedButtons()

  val newlyPressedButtons = currentlyPressedButtons subtract previouslyPressedButtons
  val newlyReleasedButtons = previouslyPressedButtons subtract currentlyPressedButtons

  // The JIT should be able to compile this out at runtime and make it no-cost
  logger.debug { "Newly pressed: $newlyPressedButtons, Newly released: $newlyReleasedButtons" }

  for (newlyPressedButton in newlyPressedButtons) {
    virtualDdrPad.pressKey(newlyPressedButton.toEventCode())
  }

  for (newlyReleasedButton in newlyReleasedButtons) {
    virtualDdrPad.releaseKey(newlyReleasedButton.toEventCode())
  }
}

/**
 * Executes a block of code within the context of a virtual DDRPad controller.
 *
 * This is useful for manage the lifecycle of the virtual device. We don't want to leave a bunch of
 * dangling virtual controllers on people's systems.
 *
 * @param block The code block to execute, receiving the [InputDevice] for the virtual DDRPad.
 */
private inline fun virtualControllerContext(
  block: (virtualDdrPad: InputDevice) -> Unit,
) {
  val virtualDdrPad = InputDevice("Virtual DDRPad", 0xdead, 0xbeef)
  virtualDdrPad.capabilities += VirtualDdrPadMappings.realToVirtual.values

  try {
    virtualDdrPad.open()
  } catch (exception: IOException) {
    logger.error(exception) { "Failed to open virtual gamepad!" }
    return
  }

  try {
    block(virtualDdrPad)
  } finally {
    try {
      virtualDdrPad.close()
    } catch (exception: IOException) {
      logger.error(exception) { "Failed to close virtual gamepad. The virtual device is probably just going to dangle there, but there's nothing we can do about it." }
    }
  }

}

/**
 * Executes a block of code within the context of a claimed USB interface.
 *
 * This is useful for manage the lifecycle of the USB interface. We don't want to hog the interface
 * claim.
 *
 * @param usbDeviceHandle The [UsbDevice] handle.
 * @param interfaceNumber The number of the interface to claim.
 * @param block The code block to execute.
 */
private inline fun interfaceContext(
  usbDeviceHandle: UsbDevice,
  interfaceNumber: Int,
  block: () -> Unit,
) {
  usbDeviceHandle.claimInterface(interfaceNumber)
  try {
    block()
  } finally {
    try {
      usbDeviceHandle.releaseInterface(interfaceNumber)
    } catch (exception: LinuxUsbException) {
      // 19 means "No such device" which simply happens when devices are rapidly unplugged.
      if (exception.errorCode != 19) {
        throw exception
      }
    }
  }
}

/**
 * Executes a block of code within the context of an opened USB device.
 *
 * This is useful for manage the lifecycle of USB communication. We don't want to hold open the USB
 * connection.
 *
 * @param usbDeviceHandle The [UsbDevice] handle.
 * @param block The code block to execute.
 */
private inline fun deviceCommunicationContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  usbDeviceHandle.open()
  try {
    block()
  } finally {
    usbDeviceHandle.close()
  }
}

/**
 * Executes a block of code within a custom driver context, detaching and reattaching standard
 * drivers.
 *
 * Our driver exists because the standard drive is insufficient. However, out of respect, we should
 * return things to how they were before we executed.
 *
 * @param usbDeviceHandle The [UsbDevice] handle.
 * @param block The code block to execute.
 */
private inline fun customDriverContext(usbDeviceHandle: UsbDevice, block: () -> Unit) {
  usbDeviceHandle.detachStandardDrivers()
  try {
    block()
  } finally {
    usbDeviceHandle.attachStandardDrivers()
  }
}

/**
 * Returns a distinct identifier for the UsbDevice.
 *
 * Serial number should be distinct, but this can be improved in the future if needed.
 */
private fun UsbDevice.debugIdentifierString() = "<serialNumber:$serialNumber>"
