package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * An opcode parameter that stores Null.
 */
class ParamNull : Parameter() {
    override val type: ParamType
        get() = ParamType.NULL

    override fun write(output: ByteBuffer?) {
        type.write(output)
    }

    override fun calculateSize(): Int {
        return 1
    }

    /**
     * @return String representation.
     */
    override fun toValueString(): String {
        return "NULL"
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + Objects.hashCode(type)
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null) {
            false
        } else javaClass == obj.javaClass
    }
}