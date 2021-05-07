package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * An opcode parameter that stores an integer.
 */
class ParamInt(val VALUE: Int) : Parameter() {
    override val type: ParamType
        get() = ParamType.INTEGER

    override fun write(output: ByteBuffer?) {
        type.write(output)
        output?.putInt(VALUE)
    }

    override fun calculateSize(): Int {
        return 5
    }

    override fun toValueString(): String {
        return VALUE.toString()
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + Objects.hashCode(type)
        hash = 41 * hash + Integer.hashCode(VALUE)
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
        val other = obj as ParamInt
        return VALUE == other.VALUE
    }
}