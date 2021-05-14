package resaver.pex

import PlatformByteBuffer
import java.io.IOException



/**
 * VData that stores a string.
 */
class VDataStr internal constructor(`val`: TString?) : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        output?.put(type.ordinal.toByte())
        string.write(output)
    }

    override fun calculateSize(): Int {
        return 3
    }

    override fun collectStrings(strings: MutableSet<TString?>?) {
        strings?.add(string)
    }

    override val type: DataType
        get() = DataType.STRING

    override fun toString(): String {
        return "\"$string\""
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + string.hashCode()
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
            this::class != other::class -> {
                false
            }
            else -> {
                val other2 = other as VDataStr
                string == other2.string
            }
        }
    }

    val string: TString = `val`!!

}