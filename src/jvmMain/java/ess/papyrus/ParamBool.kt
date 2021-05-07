package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * An opcode parameter that stores a boolean.
 */
class ParamBool(val VALUE: Byte) : Parameter() {
    override val type: ParamType
        get() = ParamType.BOOLEAN

    override fun write(output: ByteBuffer?) {
        type.write(output)
        output?.put(VALUE)
    }

    override fun calculateSize(): Int {
        return 2
    }

    override fun toValueString(): String {
        return java.lang.Boolean.toString(VALUE.toInt() != 0)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + Objects.hashCode(type)
        hash = 41 * hash + java.lang.Byte.hashCode(VALUE)
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
        val other = obj as ParamBool
        return VALUE == other.VALUE
    }
}