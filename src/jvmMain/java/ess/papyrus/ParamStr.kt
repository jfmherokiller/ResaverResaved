package ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * An opcode parameter that stores a string.
 */
class ParamStr(`val`: TString?) : Parameter() {
    override val type: ParamType
        get() = ParamType.STRING

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
        return VALUE.toString().replace("\n", "\\n")
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
        val other = obj as ParamStr
        return VALUE.equals(other.VALUE)
    }

    val VALUE: TString

    init {
        VALUE = Objects.requireNonNull(`val`)!!
    }
}