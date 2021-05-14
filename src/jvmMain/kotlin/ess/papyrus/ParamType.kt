package ess.papyrus

import PlatformByteBuffer
import UtilityFunctions

/**
 * Types of parameters. Not quite a perfect overlap with the other Type
 * class.
 */
enum class ParamType : PapyrusElement {
    NULL, IDENTIFIER, STRING, INTEGER, FLOAT, BOOLEAN, VARIANT, STRUCT, UNKNOWN8, TERM;

    override fun write(output: PlatformByteBuffer?) {
        output?.put(ordinal.toByte())
    }

    override fun calculateSize(): Int {
        return 1
    }

    companion object {
        @Throws(PapyrusFormatException::class)
        fun read(input: PlatformByteBuffer): ParamType {
            val `val` = UtilityFunctions.toUnsignedInt(input.getByte())
            if (`val` < 0 || `val` >= VALUES.size) {
                throw PapyrusFormatException("Invalid type: $`val`")
            }
            return values()[`val`]
        }

        private val VALUES = values()
    }
}