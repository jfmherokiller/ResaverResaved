package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Variable that stores an integer.
 */
final public class VarInt extends Variable {

    public VarInt(@NotNull ByteBuffer input) {
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
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        output.putInt(this.VALUE);
    }

    @NotNull
    @Override
    public VarType getType() {
        return VarType.INTEGER;
    }

    @NotNull
    @Override
    public String toValueString() {
        //return String.format("%d", this.VALUE);
        return Integer.toString(this.VALUE);
    }

    @NotNull
    @Override
    public String toString() {
        //return String.format("%s:%d", this.getType(), this.VALUE);
        return this.getType() + ":" + this.toValueString();
    }

    final private int VALUE;
}
