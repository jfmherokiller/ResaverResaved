package resaver.ess.papyrus;

import java.nio.ByteBuffer;

/**
 * Variable that stores an integer.
 */
final public class VarInt extends Variable {

    public VarInt(ByteBuffer input) {
        this.VALUE = input.getInt();
    }

    public VarInt(int val) {
        this.VALUE = val;
    }

    public int getValue() {
        return this.VALUE;
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
        return VarType.INTEGER;
    }

    @Override
    public String toValueString() {
        //return String.format("%d", this.VALUE);
        return Integer.toString(this.VALUE);
    }

    @Override
    public String toString() {
        //return String.format("%s:%d", this.getType(), this.VALUE);
        return this.getType() + ":" + this.toValueString();
    }

    final private int VALUE;
}
