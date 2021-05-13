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
        return when {
            this === other -> {
                true
            }
            other == null -> {
                false
            }
            javaClass != other.javaClass -> {
                false
            }
            else -> {
                val other2 = other as ParamTerm
                VALUE == other2.VALUE
            }
        }
    }

    val VALUE: String = `val`

}