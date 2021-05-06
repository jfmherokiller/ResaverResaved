package resaver.ess.papyrus;

import org.jetbrains.annotations.Nullable;
import resaver.Analysis;
import resaver.ess.WStringElement;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * TString implementation for 32 bit TStrings.
 */
final class TString32 extends TString {

    /**
     * Creates a new <code>TString32</code> from a <code>WStringElement</code> and
     * an index.
     *
     * @param wstr  The <code>WStringElement</code>.
     * @param index The index of the <code>TString</code>.
     */
    TString32(WStringElement wstr, int index) {
        super(wstr, index);
    }

    /**
     * Creates a new <code>TString32</code> from a character sequence and an
     * index.
     *
     * @param cs    The <code>CharSequence</code>.
     * @param index The index of the <code>TString</code>.
     */
    private TString32(CharSequence cs, int index) {
        super(cs, index);
    }

    public TString32(String val, int size) {
        super(val, size);
    }

    /**
     * @param output The output stream.
     * @throws IOException
     * @see resaver.ess.Element#write(ByteBuffer)
     */
    @Override
    public void write(ByteBuffer output) {
        output.putInt(this.getIndex());
    }

    /**
     * @return The size of the <code>Element</code> in bytes.
     * @see resaver.ess.Element#calculateSize()
     */
    @Override
    public int calculateSize() {
        return 4;
    }

    @Override
    public boolean matches(@Nullable Analysis analysis, @Nullable String mod) {
        return false;
    }
}
