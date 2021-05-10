package ess.papyrus

import java.nio.ByteBuffer

/**
 * An opcode parameter that stores a boolean.
 */
class ParamTerm(`val`: String) : Parameter() {
    override val type: ParamType
        get() = ParamType.TERM

    override fun write(output: ByteBuffer?) {
        throw IllegalStateException("Terms can't be written.")
    }

    override fun calculateSize(): Int {
        throw IllegalStateException("Terms don't have a serialized size.")
    }

    override fun toValueString(): String {
        return VALUE
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + type.hashCode()
        hash = 41 * hash + VALUE.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null) {
            return false
        } else if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as ParamTerm
        return VALUE == other2.VALUE
    }

    val VALUE: String = `val`

}