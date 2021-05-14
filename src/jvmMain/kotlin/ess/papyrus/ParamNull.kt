package ess.papyrus

import PlatformByteBuffer


/**
 * An opcode parameter that stores Null.
 */
class ParamNull : Parameter() {
    override val type: ParamType
        get() = ParamType.NULL

    override fun write(output: PlatformByteBuffer?) {
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
        hash = 41 * hash + type.hashCode()
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
            else -> javaClass == other.javaClass
        }
    }
}