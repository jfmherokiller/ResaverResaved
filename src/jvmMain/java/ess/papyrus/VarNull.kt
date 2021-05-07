package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Variable that stores nothing.
 */
final public class VarNull extends Variable {

    public VarNull(@NotNull ByteBuffer input) {
        this.VALUE = input.getInt();
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
        return VarType.NULL;
    }

    @NotNull
    @Override
    public String toValueString() {
        return "NULL";
    }

    @NotNull
    @Override
    public String toString() {
        return "NULL";
    }

    final private int VALUE;
}
