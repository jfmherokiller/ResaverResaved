package resaver.pex

import kotlin.Throws
import java.io.IOException
import java.nio.ByteBuffer

/**
 * VData that stores an integer.
 */
class VDataInt internal constructor(val value: Int) : VData() {
    @Throws(IOException::class)
    override fun write(output: ByteBuffer?) {
        output?.put(type.ordinal.toByte())
        output?.putInt(value)
    }

    override fun calculateSize(): Int {
        return 5
    }

    override val type: DataType
        get() = DataType.INTEGER

    override fun toString(): String {
        return String.format("%d", value)
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
                val other2 = other as VDataInt
                value == other2.value
            }
        }
    }

}