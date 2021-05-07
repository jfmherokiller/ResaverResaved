package ess.papyrus

import java.nio.ByteBuffer

/**
 * Variable that stores an integer.
 */
class VarInt : Variable {
    constructor(input: ByteBuffer) {
        value = input.int
    }

    constructor(`val`: Int) {
        value = `val`
    }

    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: ByteBuffer?) {
        type.write(output)
        output?.putInt(value)
    }

    override val type: VarType
        get() = VarType.INTEGER

    override fun toValueString(): String {
        //return String.format("%d", this.VALUE);
        return value.toString()
    }

    override fun toString(): String {
        //return String.format("%s:%d", this.getType(), this.VALUE);
        return "$type:${toValueString()}"
    }

    val value: Int
}