package resaver.pex

import PlatformByteBuffer
import java.io.IOException
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * VData that stores an identifier.
 */
class VDataID internal constructor(`val`: TString?) : VData() {
    @Throws(IOException::class)
    override fun write(output: PlatformByteBuffer?) {
        output?.put(type.ordinal.toByte())
        value.write(output)
    }

    override fun calculateSize(): Int {
        return 3
    }

    override fun collectStrings(strings: MutableSet<TString?>?) {
        strings?.add(value)
    }

    override val type: DataType
        get() = DataType.IDENTIFIER

    override fun toString(): String {
        //return String.format("ID[%s]", this.VALUE);
        return value.toString()
    }

    val isTemp: Boolean
        get() = (TEMP_PATTERN.test(value.toString())
                && !AUTOVAR_PATTERN.test(value.toString())
                && !NONE_PATTERN.test(value.toString()))
    val isAutovar: Boolean
        get() = AUTOVAR_PATTERN.test(value.toString())
    val isNonevar: Boolean
        get() = NONE_PATTERN.test(value.toString())

    override fun hashCode(): Int {
        var hash = 7
        hash = 83 * hash + value.hashCode()
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
        val other2 = other as VDataID
        return value == other2.value
    }

    var value: TString = `val`!!

    companion object {
        val TEMP_PATTERN: Predicate<String> = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate()
        val NONE_PATTERN: Predicate<String> = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate()
        val AUTOVAR_PATTERN: Predicate<String> = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate()
    }

}