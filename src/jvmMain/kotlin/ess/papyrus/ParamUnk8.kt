package ess.papyrus

import java.nio.ByteBuffer


/**
 * An opcode parameter that stores a variant.
 */
class ParamUnk8(`val`: TString?) : Parameter() {
    override val type: ParamType
        get() = ParamType.UNKNOWN8

    override fun write(output: ByteBuffer?) {
        type.write(output)
        VALUE.write(output)
    }

    override fun calculateSize(): Int {
        return 1 + VALUE.calculateSize()
    }

    override fun toValueString(): String {
        return VALUE.toString().replace("\n", "\\n")
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + type.hashCode()
        hash = 41 * hash + VALUE.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        when {
            this === other -> {
                return true
            }
            other == null -> {
                return false
            }
            javaClass != other.javaClass -> {
                return false
            }
            else -> {
                val other2 = other as ParamUnk8
                return VALUE.equals(other2.VALUE)
            }
        }
    }

    val VALUE: TString = `val`!!

}