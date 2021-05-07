package resaver.esp;

import java.nio.ByteBuffer;

/**
 * Header represents the standard header for all records except GRUP.
 *
 * @author Mark Fairchild
 */
public class RecordHeader implements Entry {

    /**
     * Creates a new Header by reading it from a LittleEndianInput.
     *
     * @param input The LittleEndianInput to readFully.
     * @param ctx   The mod descriptor.
     */
    public RecordHeader(ByteBuffer input, ESPContext ctx) {
        this.FLAGS = input.getInt();

        int id = input.getInt();
        int newID = ctx.remapFormID(id);
        this.ID = newID;

        this.REVISION = input.getInt();
        this.VERSION = input.getShort();
        this.UNKNOWN = input.getShort();
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.FLAGS);
        output.putInt(this.ID);
        output.putInt(this.REVISION);
        output.putShort(this.VERSION);
        output.putShort(this.UNKNOWN);
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        return 16;
    }

    /**
     * Checks if the header indicates a compressed record.
     *
     * @return True if the field data is compressed, false otherwise.
     */
    public boolean isCompressed() {
        return (this.FLAGS & 0x00040000) != 0;
    }

    /**
     * Checks if the header indicates localization (TES4 record only).
     *
     * @return True if the record is a TES4 and localization is enabled.
     */
    public boolean isLocalized() {
        return (this.FLAGS & 0x00000080) != 0;
    }

    final public int FLAGS;
    final public int ID;
    final public int REVISION;
    final public short VERSION;
    final public short UNKNOWN;

}
