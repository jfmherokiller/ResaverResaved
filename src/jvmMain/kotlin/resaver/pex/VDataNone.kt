package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * VData that stores nothing.
 */
class VDataNone internal constructor() : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        output?.put(type.ordinal.toByte())
    }

    override fun calculateSize(): Int {
        return 1
    }

    override val type: DataType
        get() = DataType.NONE

    override fun toString(): String {
        return "NONE"
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + VDataNone::class.hashCode()
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
            else -> javaClass == other.javaClass
        }
    }
}