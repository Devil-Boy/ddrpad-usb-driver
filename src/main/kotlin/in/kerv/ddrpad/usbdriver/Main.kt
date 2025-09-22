package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.toControlBytes
import `in`.kerv.ddrpad.usbdriver.VirtualDdrPadMappings.toEventCode
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import net.codecrete.usb.Usb
import net.codecrete.usb.UsbDevice
import net.codecrete.usb.UsbDirection
import net.codecrete.usb.UsbEndpoint
import uk.co.bithatch.linuxio.InputDevice
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

val unhandledDdrPads = Channel<UsbDevice>(Channel.UNLIMITED)
val inputProcessingThread: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

/** Main entry point for the DDRPad USB driver. */
@OptIn(ExperimentalUnsignedTypes::class)
suspend fun main() {
  collectAnyAlreadyConnectedDdrPads()
  listenForAndCollectNewlyConnectedDdrPads()

  // Using a Kotlin Channel, we can block/suspend the main thread on waiting for new DdrPad devices
  for (unhandledDdrPad in unhandledDdrPads) {
    handleDdrPad(unhandledDdrPad)
  }
}

private fun collectAnyAlreadyConnectedDdrPads() {
  for (ddrPad in Usb.findDevices(DdrPadUsbIds::matches)) {
    unhandledDdrPads.trySend(ddrPad)
  }
}

private fun listenForAndCollectNewlyConnectedDdrPads() {
  Usb.setOnDeviceConnected {
    if (DdrPadUsbIds.matches(it)) {
      unhandledDdrPads.trySend(it)
    }
  }
}

private fun handleDdrPad(ddrPadHandle: UsbDevice) {
  customDriverContext(ddrPadHandle) {
    deviceCommunicationContext(ddrPadHandle) {
      val iface = ddrPadHandle.interfaces[0]
      val inboundEndpointNumber = iface.currentAlternate.endpoints.getSingleInbound().number

      interfaceContext(ddrPadHandle, iface.number) {
        virtualControllerContext { virtualDdrPad ->
          var previousControlBytes = ddrPadHandle.transferIn(inboundEndpointNumber).toControlBytes()

          inputProcessingThread.scheduleAtFixedRate({
            val currentControlBytes =
              ddrPadHandle.transferIn(inboundEndpointNumber).toControlBytes()

            if (currentControlBytes.changedFrom(previousControlBytes)) {
              handleInputChange(currentControlBytes, previousControlBytes, virtualDdrPad)
              previousControlBytes = currentControlBytes
            }
          }, /* initialDelay = */ 0, /* period = */ 10, TimeUnit.MILLISECONDS).get()
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

  for (newlyPressedButton in newlyPressedButtons) {
    virtualDdrPad.pressKey(newlyPressedButton.toEventCode())
  }

  for (newlyReleasedButtons in newlyReleasedButtons) {
    virtualDdrPad.releaseKey(newlyReleasedButtons.toEventCode())
  }
}

/**
 * Extension function to find the single inbound endpoint from a list of USB endpoints.
 *
 * @return The single inbound [UsbEndpoint].
 * @throws NoSuchElementException if no inbound endpoint is found.
 * @throws IllegalArgumentException if more than one inbound endpoint is found.
 */
private fun List<UsbEndpoint>.getSingleInbound() = single { it.direction == UsbDirection.IN }

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
    usbDeviceHandle.releaseInterface(interfaceNumber)
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
