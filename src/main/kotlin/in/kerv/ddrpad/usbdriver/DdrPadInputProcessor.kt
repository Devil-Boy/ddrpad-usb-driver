package `in`.kerv.ddrpad.usbdriver

@OptIn(ExperimentalUnsignedTypes::class)
object DdrPadInputProcessor {

  /** Converts a raw byte array received from the DDRPad USB device into a [ControlBytes] object. */
  fun ByteArray.toControlBytes() = ControlBytes(this.toUByteArray())

  /**
   * A value class representing the two control bytes received from the DDRPad. These bytes encode
   * the state of the buttons.
   *
   * Using a value class helps us avoid initializing a new objects in the heap while also allowing
   * us to code object-orientedly.
   */
  @JvmInline
  value class ControlBytes(val bytes: UByteArray) {

    /**
     * Returns a set of [DdrPadButton]s that are currently pressed, based on the control bytes.
     *
     * @return A [Set] of [DdrPadButton]s that are pressed.
     */
    fun getPressedButtons(): Set<DdrPadButton> {
      val controlScheme = inputBytesCountToControlSchemes[bytes.size]
      if (controlScheme == null) {
        throw AssertionError()
      }

      return controlScheme.filterValues { bytes[it.byteIndex] and it.bitMask != 0.toUByte() }.keys
    }

    /**
     * Checks if the button states represented by this [ControlBytes] object are different from
     * another [ControlBytes] object.
     *
     * This is how we efficiently detect changes.
     *
     * @param other The other [ControlBytes] object to compare against.
     * @return `true` if the button states are different, `false` otherwise.
     */
    fun changedFrom(other: ControlBytes) = !bytes.contentEquals(other.bytes)

    /**
     * Returns a string representation of the currently pressed buttons for debugging/development
     * purposes.
     */
    override fun toString() =
        "Raw: ${bytes.size}[${bytes.toBinaryStrings()}], Parsed: ${getPressedButtons()}"

    private fun UByteArray.toBinaryStrings() = this.joinToString { it.toString(2).padStart(8, '0') }
  }

  /**
   * Represents the individual buttons on a DDRPad, along with their control byte index and bitmask.
   */
  enum class DdrPadButton {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT,
    START,
    SELECT,
  }

  data class ControlSignature(val byteIndex: Int, val bitMask: UByte)

  val controlSchemeA =
      mapOf(
          DdrPadButton.UP to ControlSignature(2, 0b0000001.toUByte()),
          DdrPadButton.DOWN to ControlSignature(2, 0b0000010.toUByte()),
          DdrPadButton.LEFT to ControlSignature(2, 0b0000100.toUByte()),
          DdrPadButton.RIGHT to ControlSignature(2, 0b0001000.toUByte()),
          DdrPadButton.UP_LEFT to ControlSignature(4, 0b11111111.toUByte()),
          DdrPadButton.UP_RIGHT to ControlSignature(5, 0b11111111.toUByte()),
          DdrPadButton.DOWN_LEFT to ControlSignature(7, 0b11111111.toUByte()),
          DdrPadButton.DOWN_RIGHT to ControlSignature(6, 0b11111111.toUByte()),
          DdrPadButton.START to ControlSignature(2, 0b00010000.toUByte()),
          DdrPadButton.SELECT to ControlSignature(2, 0b00100000.toUByte()),
      )

  val controlSchemeB =
    mapOf(
      DdrPadButton.UP to ControlSignature(2, 0b0010000.toUByte()),
      DdrPadButton.DOWN to ControlSignature(2, 0b01000000.toUByte()),
      DdrPadButton.LEFT to ControlSignature(2, 0b10000000.toUByte()),
      DdrPadButton.RIGHT to ControlSignature(2, 0b00100000.toUByte()),
      DdrPadButton.UP_LEFT to ControlSignature(3, 0b00100000.toUByte()),
      DdrPadButton.UP_RIGHT to ControlSignature(3, 0b01000000.toUByte()),
      DdrPadButton.DOWN_LEFT to ControlSignature(3, 0b10000000.toUByte()),
      DdrPadButton.DOWN_RIGHT to ControlSignature(3, 0b00010000.toUByte()),
      DdrPadButton.START to ControlSignature(2, 0b00001000.toUByte()),
      DdrPadButton.SELECT to ControlSignature(2, 0b00000001.toUByte()),
    )

  // Right now, we're actually not sure why the input bytes seems to randomly change format.
  // Hopefully, we can identify which format is being used based on how many bytes we are receiving.
  val inputBytesCountToControlSchemes =
      mapOf(
          8 to controlSchemeA,
          // TODO: Figure out how many bytes control scheme A has
          -1 to controlSchemeB,
      )

  /**
   * Checks if the given number of bytes corresponds to a recognized control scheme.
   *
   * @param numberOfBytes The number of bytes received from the DDRPad.
   * @return `true` if the number of bytes is recognized, `false` otherwise.
   */
  fun isRecognizedNumberOfControlBytes(numberOfBytes: Int) =
      inputBytesCountToControlSchemes.containsKey(numberOfBytes)
}
