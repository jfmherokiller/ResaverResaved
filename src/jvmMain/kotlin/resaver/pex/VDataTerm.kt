package resaver.pex

import PlatformByteBuffer
import java.io.IOException
import java.util.*

/**
 * VData that stores a "term", for disassembly purposes.
 */
class VDataTerm(`val`: String) : VData() {
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
        get() = DataType.IDENTIFIER

    override fun toString(): String {
        return value
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + Objects.hashCode(value)
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
                val other2 = other as VDataTerm
                value == other2.value
            }
        }
    }

    override fun paren(): String {
        return PVALUE
    }

    val value: String = Objects.requireNonNull(`val`)
    private val PVALUE: String = "($value)"

}