package ess.papyrus

import PlatformByteBuffer

/**
 * Variable that stores an integer.
 */
class VarInt : Variable {
    constructor(input: PlatformByteBuffer) {
        value = input.getInt()
    }

    constructor(`val`: Int) {
        value = `val`
    }

    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: PlatformByteBuffer?) {
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