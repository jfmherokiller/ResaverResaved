package ess.papyrus

import java.nio.ByteBuffer


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
        hash = 41 * hash + type.hashCode()
        hash = 41 * hash + VALUE.hashCode()
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        return when {
            this === obj -> {
                true
            }
            obj == null -> {
                false
            }
            javaClass != obj.javaClass -> {
                false
            }
            else -> {
                val other = obj as ParamFlt
                VALUE == other.VALUE
            }
        }
    }
}