package ess.papyrus

import java.nio.ByteBuffer


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
                val other2 = other as ParamInt
                VALUE == other2.VALUE
            }
        }
    }
}