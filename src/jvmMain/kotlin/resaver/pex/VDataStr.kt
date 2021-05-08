package resaver.pex

import java.io.IOException
import java.nio.ByteBuffer


/**
 * VData that stores a string.
 */
class VDataStr internal constructor(`val`: TString?) : VData() {
    @Throws(IOException::class)
    override fun write(output: ByteBuffer?) {
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
        return String.format("\"%s\"", string)
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
            javaClass != other.javaClass -> {
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