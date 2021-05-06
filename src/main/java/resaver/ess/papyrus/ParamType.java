package resaver.ess.papyrus;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Types of parameters. Not quite a perfect overlap with the other Type
 * class.
 */
public enum ParamType implements PapyrusElement {
    NULL,
    IDENTIFIER,
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    VARIANT,
    STRUCT,
    UNKNOWN8,
    TERM;

    static public ParamType read(ByteBuffer input) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        int val = Byte.toUnsignedInt(input.get());
        if (val < 0 || val >= VALUES.length) {
            throw new PapyrusFormatException("Invalid type: " + val);
        }
        return ParamType.values()[val];
    }

    @Override
    public void write(ByteBuffer output) {
        output.put((byte) this.ordinal());
    }

    @Override
    public int calculateSize() {
        return 1;
    }

    static final private ParamType[] VALUES = ParamType.values();
}
