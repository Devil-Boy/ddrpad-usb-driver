package `in`.kerv.ddrpad.usbdriver

@OptIn(ExperimentalUnsignedTypes::class)
object DdrPadInputProcessor {

  /**
   * Converts a raw byte array received from the DDRPad USB device into a [ControlBytes] object.
   * Based on testing, DDRPad's control bytes are always at indices 2 and 3 of the raw input report.
   */
  fun ByteArray.toControlBytes() = ControlBytes(ubyteArrayOf(this[2].toUByte(), this[3].toUByte()))

  /**
   * A value class representing the two control bytes received from the DDRPad.
   * These bytes encode the state of the buttons.
   *
   * Using a value class helps us avoid initializing a new objects in the heap while also allowing us to code object-orientedly.
   */
  @JvmInline
  value class ControlBytes(val bytes: UByteArray) {
    init {
      // Ensure that exactly two bytes are provided, as per DDRPad specification.
      require(bytes.size == 2) { "The DDRPad always has 2 control bytes!" }
    }

    /**
     * Returns a set of [DdrPadButton]s that are currently pressed, based on the control bytes.
     *
     * @return A [Set] of [DdrPadButton]s that are pressed.
     */
    fun getPressedButtons() = DdrPadButton.entries.filter {
      bytes[it.controlByteIndex] and it.bitMask != 0.toUByte()
    }.toSet()

    /**
     * Checks if the button states represented by this [ControlBytes] object
     * are different from another [ControlBytes] object.
     *
     * This is how we efficiently detect changes.
     *
     * @param other The other [ControlBytes] object to compare against.
     * @return `true` if the button states are different, `false` otherwise.
     */
    fun changedFrom(other: ControlBytes) = !bytes.contentEquals(other.bytes)

    /**
     * Returns a string representation of the currently pressed buttons for debugging/development purposes.
     */
    override fun toString() = "Pressed buttons: ${getPressedButtons()}"
  }

  /** Represents the individual buttons on a DDRPad, along with their control byte index and bitmask. */
  enum class DdrPadButton(val controlByteIndex: Int, val bitMask: UByte) {
    UP(0, 0b00010000.toUByte()),
    DOWN(0, 0b01000000.toUByte()),
    LEFT(0, 0b10000000.toUByte()),
    RIGHT(0, 0b00100000.toUByte()),

    UP_LEFT(1, 0b00100000.toUByte()),
    UP_RIGHT(1, 0b01000000.toUByte()),
    DOWN_LEFT(1, 0b10000000.toUByte()),
    DOWN_RIGHT(1, 0b00010000.toUByte()),

    START(0, 0b00001000.toUByte()),
    SELECT(0, 0b00000001.toUByte()),
  }

}