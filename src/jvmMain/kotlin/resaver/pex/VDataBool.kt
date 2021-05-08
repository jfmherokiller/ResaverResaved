package resaver.pex

import java.io.IOException
import java.nio.ByteBuffer

/**
 * VData that stores a boolean.
 */
class VDataBool internal constructor(val value: Boolean) : VData() {
    @Throws(IOException::class)
    override fun write(output: ByteBuffer?) {
        output?.put(type.ordinal.toByte())
        output?.put(if (value) 1.toByte() else 0.toByte())
    }

    override fun calculateSize(): Int {
        return 2
    }

    override val type: DataType
        get() = DataType.BOOLEAN

    override fun toString(): String {
        return String.format("%b", value)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + java.lang.Boolean.hashCode(value)
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null) {
            return false
        } else if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as VDataBool
        return value == other2.value
    }

}