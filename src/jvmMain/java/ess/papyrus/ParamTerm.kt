package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

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
        hash = 41 * hash + Objects.hashCode(type)
        hash = 41 * hash + Objects.hashCode(VALUE)
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        } else if (obj == null) {
            return false
        } else if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ParamTerm
        return VALUE == other.VALUE
    }

    val VALUE: String = Objects.requireNonNull(`val`)

}