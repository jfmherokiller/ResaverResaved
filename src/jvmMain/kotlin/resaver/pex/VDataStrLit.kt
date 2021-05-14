package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * VData that stores a string literal, for disassembly purposes.
 */
internal class VDataStrLit(`val`: String) : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        throw IllegalStateException("Not valid for Terms.")
    }

    override fun calculateSize(): Int {
        throw IllegalStateException("Not valid for Terms.")
    }

    override fun collectStrings(strings: MutableSet<TString?>?) {
        throw IllegalStateException("Not valid for Terms.")
    }

    override val type: DataType
        get() = DataType.STRING

    override fun toString(): String {
        return ("\"$value\"").replace("\n", "\\n")
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + value.hashCode()
        return hash
    }

    fun equals(obj: VDataTerm?): Boolean {
        return if (obj == null) {
            false
        } else value == obj.value
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
                val other2 = other as VDataStrLit
                value == other2.value
            }
        }
    }

    val value: String = `val`

}