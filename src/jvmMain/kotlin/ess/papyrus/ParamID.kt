package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

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
        hash = 41 * hash + Objects.hashCode(type)
        hash = 41 * hash + Objects.hashCode(VALUE)
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
        val other = obj as ParamID
        return VALUE.equals(other.VALUE)
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
    val VALUE: TString

    init {
        VALUE = Objects.requireNonNull(`val`)!!
    }
}