package `in`.kerv.ddrpad.usbdriver

@OptIn(ExperimentalUnsignedTypes::class)
object DdrPadInputProcessor {

  /**
   * Converts a raw byte array received from the DDRPad USB device into a [ControlBytes] object.
   */
  fun ByteArray.toControlBytes() = ControlBytes(this.toUByteArray())

  /**
   * A value class representing the two control bytes received from the DDRPad.
   * These bytes encode the state of the buttons.
   *
   * Using a value class helps us avoid initializing a new objects in the heap while also allowing us to code object-orientedly.
   */
  @JvmInline
  value class ControlBytes(val bytes: UByteArray) {
    init {
      require(bytes.size == 8) { "The DDRPad always has 8 control bytes!" }
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
    override fun toString() = "Raw: [${bytes.toBinaryStrings()}], Parsed: ${getPressedButtons()}"

    private fun UByteArray.toBinaryStrings() = this.joinToString { it.toString(2).padStart(8, '0')  }
  }

  /** Represents the individual buttons on a DDRPad, along with their control byte index and bitmask. */
  enum class DdrPadButton(val controlByteIndex: Int, val bitMask: UByte) {
    UP(2, 0b00000001.toUByte()),
    DOWN(2, 0b00000010.toUByte()),
    LEFT(2, 0b00000100.toUByte()),
    RIGHT(2, 0b00001000.toUByte()),

    UP_LEFT(4, 0b11111111.toUByte()),
    UP_RIGHT(5, 0b11111111.toUByte()),
    DOWN_LEFT(7, 0b11111111.toUByte()),
    DOWN_RIGHT(6, 0b11111111.toUByte()),

    START(2, 0b00010000.toUByte()),
    SELECT(2, 0b00100000.toUByte()),
  }

}