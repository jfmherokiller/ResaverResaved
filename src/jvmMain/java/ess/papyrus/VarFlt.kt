package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Variable that stores a float.
 */
final public class VarFlt extends Variable {

    public VarFlt(@NotNull ByteBuffer input) {
        this.VALUE = input.getFloat();
    }

    public VarFlt(float val) {
        this.VALUE = val;
    }

    public float getValue() {
        return this.VALUE;
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        output.putFloat(this.VALUE);
    }

    @NotNull
    @Override
    public VarType getType() {
        return VarType.FLOAT;
    }

    @Override
    public String toValueString() {
        //return String.format("%f", this.VALUE);
        return Float.toString(this.VALUE);
    }

    @NotNull
    @Override
    public String toString() {
        //return String.format("%s:%f", this.getType(), this.VALUE);
        return this.getType() + ":" + this.toValueString();
    }

    final private float VALUE;
}
