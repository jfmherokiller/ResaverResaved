package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Variable that stores a boolean.
 */
final public class VarBool extends Variable {

    public VarBool(@NotNull ByteBuffer input) {
        Objects.requireNonNull(input);
        this.VALUE = input.getInt();
    }

    public VarBool(boolean val) {
        this.VALUE = (val ? 1 : 0);
    }

    public boolean getValue() {
        return this.VALUE != 0;
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
        return VarType.BOOLEAN;
    }

    @NotNull
    @Override
    public String toValueString() {
        //return String.format("%s", Boolean.toString(this.VALUE != 0));
        return Boolean.toString(this.VALUE != 0);
    }

    @NotNull
    @Override
    public String toString() {
        //return String.format("%s:%s", this.getType(), Boolean.toString(this.VALUE != 0));
        return this.getType() + ":" + this.toValueString();
    }

    final private int VALUE;
}
