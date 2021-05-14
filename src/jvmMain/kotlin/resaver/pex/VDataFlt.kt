package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * VData that stores a float.
 */
class VDataFlt internal constructor(val value: Float) : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        output?.put(type.ordinal.toByte())
        output?.putFloat(value)
    }

    override fun calculateSize(): Int {
        return 5
    }

    override val type: DataType
        get() = DataType.FLOAT

    override fun toString(): String {
        return String.format("%g", value)
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
                val other2 = other as VDataFlt
                value == other2.value
            }
        }
    }

}