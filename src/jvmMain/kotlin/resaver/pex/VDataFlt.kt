package resaver.pex

import kotlin.Throws
import java.io.IOException
import java.nio.ByteBuffer

/**
 * VData that stores a float.
 */
class VDataFlt internal constructor(val value: Float) : VData() {
    @Throws(IOException::class)
    override fun write(output: ByteBuffer?) {
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
                val other2 = other as VDataFlt
                return value == other2.value
            }
        }
    }

}