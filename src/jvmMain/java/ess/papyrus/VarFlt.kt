package ess.papyrus

import java.nio.ByteBuffer

/**
 * Variable that stores a float.
 */
class VarFlt : Variable {
    constructor(input: ByteBuffer) {
        value = input.float
    }

    constructor(`val`: Float) {
        value = `val`
    }

    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: ByteBuffer?) {
        type.write(output)
        output?.putFloat(value)
    }

    override val type: VarType
        get() = VarType.FLOAT

    override fun toValueString(): String {
        //return String.format("%f", this.VALUE);
        return value.toString()
    }

    override fun toString(): String {
        //return String.format("%s:%f", this.getType(), this.VALUE);
        return type.toString() + ":" + toValueString()
    }

    val value: Float
}