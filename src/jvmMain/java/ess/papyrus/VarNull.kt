package ess.papyrus

import java.nio.ByteBuffer

/**
 * Variable that stores nothing.
 */
class VarNull(input: ByteBuffer) : Variable() {
    override fun calculateSize(): Int {
        return 5
    }

    override fun write(output: ByteBuffer?) {
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

    private val VALUE: Int = input.int

}