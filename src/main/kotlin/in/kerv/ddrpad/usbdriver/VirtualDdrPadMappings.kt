package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.DdrPadButton
import uk.co.bithatch.linuxio.EventCode

object VirtualDdrPadMappings {

  val realToVirtual =
      mapOf(
          DdrPadButton.UP to EventCode.BTN_DPAD_UP,
          DdrPadButton.DOWN to EventCode.BTN_DPAD_DOWN,
          DdrPadButton.LEFT to EventCode.BTN_DPAD_LEFT,
          DdrPadButton.RIGHT to EventCode.BTN_DPAD_RIGHT,
          DdrPadButton.START to EventCode.BTN_START,
          DdrPadButton.SELECT to EventCode.BTN_SELECT,

          // Cross
          DdrPadButton.UP_LEFT to EventCode.BTN_SOUTH,

          // Circle
          DdrPadButton.UP_RIGHT to EventCode.BTN_EAST,

          // Triangle
          DdrPadButton.DOWN_LEFT to EventCode.BTN_NORTH,

          // Square
          DdrPadButton.DOWN_RIGHT to EventCode.BTN_WEST,
      )

  fun DdrPadButton.toEventCode() = realToVirtual[this]
}
