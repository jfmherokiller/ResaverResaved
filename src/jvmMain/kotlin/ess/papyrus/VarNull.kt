package ess.papyrus

import PlatformByteBuffer

/**
 * Variable that stores nothing.
 */
class VarNull(input: PlatformByteBuffer) : Variable() {
    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        output?.putInt(VALUE)
    }

    override val type: VarType
        get() = VarType.NULL

    override fun toValueString(): String {
        return "NULL"
    }

    override fun toString(): String {
        return "NULL"
    }

    private val VALUE: Int = input.getInt()

}