package ess.papyrus

import PlatformByteBuffer


/**
 * An opcode parameter that stores a boolean.
 */
class ParamBool(val VALUE: Byte) : Parameter() {
    override val type: ParamType
        get() = ParamType.BOOLEAN

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        output?.put(VALUE)
    }

    override fun calculateSize(): Int {
        return 2
    }

    override fun toValueString(): String {
        return (VALUE.toInt() != 0).toString()
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
                val other2 = other as ParamBool
                VALUE == other2.VALUE
            }
        }
    }
}