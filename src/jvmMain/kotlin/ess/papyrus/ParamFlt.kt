package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * An opcode parameter that stores a float.
 */
class ParamFlt(val VALUE: Float) : Parameter() {
    override val type: ParamType
        get() = ParamType.FLOAT

    override fun write(output: ByteBuffer?) {
        type.write(output)
        output?.putFloat(VALUE)
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
        hash = 41 * hash + java.lang.Float.hashCode(VALUE)
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
        val other = obj as ParamFlt
        return VALUE == other.VALUE
    }
}