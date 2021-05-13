/*
 * Copyright 2016 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ess.papyrus

import java.nio.ByteBuffer
import java.util.function.Predicate

import java.util.regex.Pattern

/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
abstract class Parameter : PapyrusElement {
    /**
     * @return The type of the parameter.
     */
    abstract val type: ParamType

    /**
     * @return A flag indicating if the parameter is an identifier to a temp
     * variable.
     */
    open val isTemp: Boolean
        get() = false

    /**
     * @return A flag indicating if the parameter is an Autovariable.
     */
    open val isAutovar: Boolean
        get() = false

    /**
     * @return A flag indicating if the parameter is an None variable.
     */
    open val isNonevar: Boolean
        get() = false

    /**
     * @return Returns the identifier value of the `Parameter`, if
     * possible.
     */
    val iDValue: TString
        get() = if (this is ParamID) {
            this.VALUE
        } else {
            throw IllegalStateException()
        }

    /**
     * @return Returns the string value of the `Parameter`, if
     * possible.
     */
    val tStrValue: TString
        get() = if (this is ParamStr) {
            this.VALUE
        } else {
            throw IllegalStateException()
        }

    /**
     * @return Returns the integer value of the `Parameter`, if
     * possible.
     */
    val intValue: Int
        get() = if (this is ParamInt) {
            this.VALUE
        } else {
            throw IllegalStateException()
        }

    /**
     * @return Short string representation.
     */
    abstract fun toValueString(): String

    /**
     * An appropriately parenthesized string form of the parameter.
     *
     * @return
     */
    fun paren(): String {
        return if (type == ParamType.TERM) {
            "(${toValueString()})"
        } else {
            toValueString()
        }
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return type.toString() + ":" + toValueString()
    }

    companion object {
        /**
         * Creates a new `Parameter` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @return The new `Parameter`.
         * @throws PapyrusFormatException
         */
        @Throws(PapyrusFormatException::class)
        fun read(input: ByteBuffer, context: PapyrusContext): Parameter {
            val TYPE = ParamType.read(input)
            return when (TYPE) {
                ParamType.NULL -> ParamNull()
                ParamType.IDENTIFIER -> {
                    val id = context.readTString(input)
                    ParamID(id)
                }
                ParamType.STRING -> {
                    val str = context.readTString(input)
                    ParamStr(str)
                }
                ParamType.INTEGER -> {
                    val i = input.int
                    ParamInt(i)
                }
                ParamType.FLOAT -> {
                    val f = input.float
                    ParamFlt(f)
                }
                ParamType.BOOLEAN -> {
                    val b = input.get()
                    ParamBool(b)
                }
                ParamType.TERM -> throw IllegalStateException("Terms cannot be read.")
                ParamType.UNKNOWN8 -> {
                    val u8 = context.readTString(input)
                    ParamUnk8(u8)
                }
                else -> throw PapyrusFormatException("Illegal Parameter type: $TYPE")
            }
        }

        /**
         * Creates a term, a label for doing substitutions.
         *
         * @param value
         * @return
         */

        fun createTerm(value: String?): Parameter {
            return value?.let { ParamTerm(it) }!!
        }


        val TEMP_PATTERN: Predicate<String> = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate()

        val NONE_PATTERN: Predicate<String> = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate()

        val AUTOVAR_PATTERN: Predicate<String> = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate()
    }
}