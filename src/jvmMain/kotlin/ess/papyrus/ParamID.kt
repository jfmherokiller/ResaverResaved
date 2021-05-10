package ess.papyrus

import java.nio.ByteBuffer


/**
 * An opcode parameter that stores an identifier.
 */
class ParamID(`val`: TString?) : Parameter() {
    override val type: ParamType
        get() = ParamType.IDENTIFIER

    override fun write(output: ByteBuffer?) {
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
                val other2 = other as ParamID
                VALUE.equals(other2.VALUE)
            }
        }
    }

    override val isTemp: Boolean
        get() = (TEMP_PATTERN.test(VALUE.toString())
                && !AUTOVAR_PATTERN.test(VALUE.toString())
                && !NONE_PATTERN.test(VALUE.toString()))

    /**
     * @return A flag indicating if the parameter is an Autovariable.
     */
    override val isAutovar: Boolean
        get() = AUTOVAR_PATTERN.test(VALUE.toString())

    /**
     * @return A flag indicating if the parameter is an None variable.
     */
    override val isNonevar: Boolean
        get() = NONE_PATTERN.test(VALUE.toString())
    val VALUE: TString = `val`!!

}