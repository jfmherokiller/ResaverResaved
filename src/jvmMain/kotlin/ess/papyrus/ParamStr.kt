package ess.papyrus

import PlatformByteBuffer


/**
 * An opcode parameter that stores a string.
 */
class ParamStr(`val`: TString?) : Parameter() {
    override val type: ParamType
        get() = ParamType.STRING

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        VALUE.write(output)
    }

    override fun calculateSize(): Int {
        return 1 + VALUE.calculateSize()
    }

    /**
     * @return String representation.
     */
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
                val other2 = other as ParamStr
                VALUE.equals(other2.VALUE)
            }
        }
    }

    val VALUE: TString = `val`!!

}