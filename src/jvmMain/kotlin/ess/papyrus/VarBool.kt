package ess.papyrus

import PlatformByteBuffer

/**
 * Variable that stores a boolean.
 */
class VarBool : Variable {
    constructor(input: PlatformByteBuffer) {
        VALUE = input.getInt()
    }

    constructor(`val`: Boolean) {
        VALUE = if (`val`) 1 else 0
    }

    val value: Boolean
        get() = VALUE != 0

    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        output?.putInt(VALUE)
    }

    override val type: VarType
        get() = VarType.BOOLEAN

    override fun toValueString(): String {
        //return String.format("%s", Boolean.toString(this.VALUE != 0));
        return java.lang.Boolean.toString(VALUE != 0)
    }

    override fun toString(): String {
        //return String.format("%s:%s", this.getType(), Boolean.toString(this.VALUE != 0));
        return "$type:${toValueString()}"
    }

    private val VALUE: Int
}