package resaver.ess.papyrus;

import org.jetbrains.annotations.Nullable;
import resaver.Analysis;
import resaver.ess.WStringElement;

import java.nio.ByteBuffer;

/**
 * TString implementation for 16 bit TStrings.
 */
final class TString16 extends TString {

    private final StringTable tStrings;

    /**
     * Creates a new <code>TString16</code> from a <code>WStringElement</code> and
     * an index.
     *
     * @param wstr  The <code>WStringElement</code>.
     * @param index The index of the <code>TString</code>.
     */
    TString16(StringTable tStrings, WStringElement wstr, int index) {
        super(wstr, index);
        this.tStrings = tStrings;
    }

    /**
     * Creates a new <code>TString16</code> from a character sequence and an
     * index.
     *
     * @param cs    The <code>CharSequence</code>.
     * @param index The index of the <code>TString</code>.
     */
    private TString16(StringTable tStrings, CharSequence cs, int index) {
        super(cs, index);
        this.tStrings = tStrings;
    }

    public TString16(StringTable tStrings, String val, int size) {
        super(val, size);
        this.tStrings = tStrings;
    }

    /**
     * @param output The output stream.
     * @see resaver.ess.Element#write(ByteBuffer)
     */
    @Override
    public void write(ByteBuffer output) {
        if (this.getIndex() > 0xFFF0 && !tStrings.STBCORRECTION) {
            output.putShort((short) 0xFFFF);
            output.putInt(this.getIndex());
        } else {
            output.putShort((short) this.getIndex());
        }
    }

    /**
     * @return The size of the <code>Element</code> in bytes.
     * @see resaver.ess.Element#calculateSize()
     */
    @Override
    public int calculateSize() {
        return (this.getIndex() > 0xFFF0 && !tStrings.STBCORRECTION ? 6 : 2);
    }

    @Override
    public boolean matches(@Nullable Analysis analysis, @Nullable String mod) {
        return false;
    }
}
