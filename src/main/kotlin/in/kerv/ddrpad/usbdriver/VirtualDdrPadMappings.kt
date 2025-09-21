package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.DdrPadButton
import uk.co.bithatch.linuxio.EventCode

object VirtualDdrPadMappings {
  /**
   * Maps physical DDR pad buttons to virtual gamepad button event codes.
   *
   * EventCode reference: https://web.archive.org/web/20250917115145/https://www.kernel.org/doc/html/v4.17/input/gamepad.html
   */
  val realToVirtual =
      mapOf(
          DdrPadButton.UP to EventCode.BTN_NORTH,
          DdrPadButton.DOWN to EventCode.BTN_SOUTH,
          DdrPadButton.LEFT to EventCode.BTN_WEST,
          DdrPadButton.RIGHT to EventCode.BTN_EAST,
          DdrPadButton.START to EventCode.BTN_START, // Project Outfox shows this as "Dpad Right"
          DdrPadButton.SELECT to EventCode.BTN_SELECT, // Project Outfox shows this as "Dpad Left"

          // Cross
          DdrPadButton.UP_LEFT to EventCode.BTN_TL, // Project Outfox shows this as "Back"

          // Circle
          DdrPadButton.UP_RIGHT to EventCode.BTN_TR, // Project Outfox shows this as "Start"

          // Triangle
          DdrPadButton.DOWN_LEFT to EventCode.BTN_TL2,

          // Square
          DdrPadButton.DOWN_RIGHT to EventCode.BTN_TR2,
      )

  /**
   * Converts a [DdrPadButton] to its corresponding [EventCode].
   */
  fun DdrPadButton.toEventCode() = realToVirtual[this]
}
