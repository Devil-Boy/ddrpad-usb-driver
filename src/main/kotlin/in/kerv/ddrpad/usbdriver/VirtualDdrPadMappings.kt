package `in`.kerv.ddrpad.usbdriver

import `in`.kerv.ddrpad.usbdriver.DdrPadInputProcessor.DdrPadButton
import uk.co.bithatch.linuxio.EventCode

object VirtualDdrPadMappings {

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

  fun DdrPadButton.toEventCode() = realToVirtual[this]
}
