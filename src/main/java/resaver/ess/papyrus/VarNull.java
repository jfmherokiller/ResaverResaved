package resaver.ess.papyrus;

import java.nio.ByteBuffer;

/**
 * Variable that stores nothing.
 */
final public class VarNull extends Variable {

    public VarNull(ByteBuffer input) {
        this.VALUE = input.getInt();
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @Override
    public void write(ByteBuffer output) {
        this.getType().write(output);
        output.putInt(this.VALUE);
    }

    @Override
    public VarType getType() {
        return VarType.NULL;
    }

    @Override
    public String toValueString() {
        return "NULL";
    }

    @Override
    public String toString() {
        return "NULL";
    }

    final private int VALUE;
}
