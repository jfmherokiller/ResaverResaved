package ess.papyrus

import java.nio.ByteBuffer


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
        val other2 = other as ParamBool
        return VALUE == other2.VALUE
    }
}