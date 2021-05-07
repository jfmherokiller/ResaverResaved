package resaver.esp

import java.nio.ByteBuffer

/**
 * Header represents the standard header for all records except GRUP.
 *
 * @author Mark Fairchild
 */
class RecordHeader(input: ByteBuffer, ctx: ESPContext) : Entry {
    /**
     * @see Entry.write
     */
    override fun write(output: ByteBuffer?) {
        output!!.putInt(FLAGS)
        output.putInt(ID)
        output.putInt(REVISION)
        output.putShort(VERSION)
        output.putShort(UNKNOWN)
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        return 16
    }

    /**
     * Checks if the header indicates a compressed record.
     *
     * @return True if the field data is compressed, false otherwise.
     */
    val isCompressed: Boolean
        get() = FLAGS and 0x00040000 != 0

    /**
     * Checks if the header indicates localization (TES4 record only).
     *
     * @return True if the record is a TES4 and localization is enabled.
     */
    val isLocalized: Boolean
        get() = FLAGS and 0x00000080 != 0
    val FLAGS: Int
    @JvmField
    val ID: Int
    val REVISION: Int
    val VERSION: Short
    val UNKNOWN: Short

    /**
     * Creates a new Header by reading it from a LittleEndianInput.
     *
     * @param input The LittleEndianInput to readFully.
     * @param ctx   The mod descriptor.
     */
    init {
        FLAGS = input.int
        val id = input.int
        val newID = ctx.remapFormID(id)
        ID = newID
        REVISION = input.int
        VERSION = input.short
        UNKNOWN = input.short
    }
}