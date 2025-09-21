package `in`.kerv.ddrpad.usbdriver

@OptIn(ExperimentalUnsignedTypes::class)
object DdrPadInputProcessor {

  fun ByteArray.toControlBytes() = ControlBytes(ubyteArrayOf(this[2].toUByte(), this[3].toUByte()))

  @JvmInline
  value class ControlBytes(val bytes: UByteArray) {
    init {
      require(bytes.size == 2) { "The DDRPad always has 2 control bytes!" }
    }

    fun getPressedButtons() = DdrPadButton.entries.filter {
      bytes[it.controlByteIndex] and it.bitMask != 0.toUByte()
    }.toSet()

    fun changedFrom(other: ControlBytes) = !bytes.contentEquals(other.bytes)

    override fun toString() = "Pressed buttons: ${getPressedButtons()}"
  }

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