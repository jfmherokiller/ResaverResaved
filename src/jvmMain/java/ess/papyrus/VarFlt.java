package ess.papyrus;

import java.nio.ByteBuffer;

/**
 * Variable that stores a float.
 */
final public class VarFlt extends Variable {

    public VarFlt(ByteBuffer input) {
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
    public void write(ByteBuffer output) {
        this.getType().write(output);
        output.putFloat(this.VALUE);
    }

    @Override
    public VarType getType() {
        return VarType.FLOAT;
    }

    @Override
    public String toValueString() {
        //return String.format("%f", this.VALUE);
        return Float.toString(this.VALUE);
    }

    @Override
    public String toString() {
        //return String.format("%s:%f", this.getType(), this.VALUE);
        return this.getType() + ":" + this.toValueString();
    }

    final private float VALUE;
}
