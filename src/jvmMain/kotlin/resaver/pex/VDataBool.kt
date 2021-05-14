package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * VData that stores a boolean.
 */
class VDataBool internal constructor(val value: Boolean) : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
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
        hash = 83 * hash + value.hashCode()
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
                val other2 = other as VDataBool
                value == other2.value
            }
        }
    }

}