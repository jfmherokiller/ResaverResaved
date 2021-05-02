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
package resaver.pex

import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.regex.Pattern

/**
 * Describes the data stored by a variable, property, or parameter.
 *
 * @author Mark Fairchild
 */
abstract class VData {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @param strings The string table.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    abstract fun write(output: ByteBuffer)

    /**
     * Calculates the size of the VData, in bytes.
     *
     * @return The size of the VData.
     */
    abstract fun calculateSize(): kotlin.Int

    /**
     * Collects all of the strings used by the VData and adds them to a set.
     *
     * @param strings The set of strings.
     */
    open fun collectStrings(strings: MutableSet<StringTable.TString>) {}

    /**
     * The `VData` is a `Term`, returns it encloded in
     * brackets. Otherwise it is identical to `toString()`.
     */
    open fun paren(): String {
        return this.toString()
    }

    /**
     * @return Returns the type of the VData.
     */
    abstract val type: DataType

    /**
     * VData that stores nothing.
     */
    class None : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
        }

        override fun calculateSize(): kotlin.Int {
            return 1
        }

        override val type: DataType
            get() = DataType.NONE

        override fun toString(): String {
            return "NONE"
        }

        override fun hashCode(): kotlin.Int {
            var hash = 7
            hash = 83 * hash + None::class.java.hashCode()
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
                else -> {
                    javaClass == other.javaClass
                }
            }
        }
    }

    /**
     * VData that stores an identifier.
     */
    class ID internal constructor(`val`: StringTable.TString?) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
            value.write(output)
        }

        override fun calculateSize(): kotlin.Int {
            return 3
        }

        override fun collectStrings(strings: MutableSet<StringTable.TString>) {
            strings.add(value)
        }

        override val type: DataType
            get() = DataType.IDENTIFIER

        override fun toString(): String {
            //return String.format("ID[%s]", this.VALUE);
            return value.toString()
        }

        fun getValue(): StringTable.TString {
            return value
        }

        fun setValue(`val`: StringTable.TString?) {
            value = Objects.requireNonNull(`val`)!!
        }

        val isTemp: Boolean
            get() = (TEMP_PATTERN.test(value.toString())
                    && !AUTOVAR_PATTERN.test(value.toString())
                    && !NONE_PATTERN.test(value.toString()))
        val isAutovar: Boolean
            get() = AUTOVAR_PATTERN.test(value.toString())
        val isNonevar: Boolean
            get() = NONE_PATTERN.test(value.toString())

        override fun hashCode(): kotlin.Int {
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
                    val other2 = other as ID
                    value == other2.value
                }
            }
        }

        private var value: StringTable.TString

        companion object {
            val TEMP_PATTERN = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate()!!
            val NONE_PATTERN = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate()!!
            val AUTOVAR_PATTERN = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate()!!
        }

        init {
            value = Objects.requireNonNull(`val`)!!
        }
    }

    /**
     * VData that stores a string.
     */
    class Str(`val`: StringTable.TString) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
            string.write(output)
        }

        override fun calculateSize(): kotlin.Int {
            return 3
        }

        override fun collectStrings(strings: MutableSet<StringTable.TString>) {
            strings.add(string)
        }

        override val type: DataType
            get() = DataType.STRING

        override fun toString(): String {
            return String.format("\"%s\"", string)
        }

        override fun hashCode(): kotlin.Int {
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
                    val other2 = other as Str
                    string == other2.string
                }
            }
        }

        val string: StringTable.TString = Objects.requireNonNull(`val`)

    }

    /**
     * VData that stores an integer.
     */
    class Int(val value: kotlin.Int) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
            output.putInt(value)
        }

        override fun calculateSize(): kotlin.Int {
            return 5
        }

        override val type: DataType
            get() = DataType.INTEGER

        override fun toString(): String {
            return String.format("%d", value)
        }

        override fun hashCode(): kotlin.Int {
            var hash = 7
            hash = 83 * hash + Integer.hashCode(value)
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
                    val other2 = other as Int
                    value == other2.value
                }
            }
        }

    }

    /**
     * VData that stores a float.
     */
    class Flt(val value: Float) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
            output.putFloat(value)
        }

        override fun calculateSize(): kotlin.Int {
            return 5
        }

        override val type: DataType
            get() = DataType.FLOAT

        override fun toString(): String {
            return String.format("%g", value)
        }

        override fun hashCode(): kotlin.Int {
            var hash = 7
            hash = 83 * hash + java.lang.Float.hashCode(value)
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
                    val other2 = other as Flt
                    value == other2.value
                }
            }
        }

    }

    /**
     * VData that stores a boolean.
     */
    class Bool(val value: Boolean) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            output.put(type.ordinal.toByte())
            output.put(if (value) 1.toByte() else 0.toByte())
        }

        override fun calculateSize(): kotlin.Int {
            return 2
        }

        override val type: DataType
            get() = DataType.BOOLEAN

        override fun toString(): String {
            return String.format("%b", value)
        }

        override fun hashCode(): kotlin.Int {
            var hash = 7
            hash = 83 * hash + java.lang.Boolean.hashCode(value)
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
                    val other2 = other as Bool
                    value == other2.value
                }
            }
        }

    }

    /**
     * VData that stores a "term", for disassembly purposes.
     */
    class Term(`val`: String) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            throw IllegalStateException("Not valid for Terms.")
        }

        override fun calculateSize(): kotlin.Int {
            throw IllegalStateException("Not valid for Terms.")
        }

        override fun collectStrings(strings: MutableSet<StringTable.TString>) {
            throw IllegalStateException("Not valid for Terms.")
        }


        override fun toString(): String {
            return value
        }

        override fun hashCode(): kotlin.Int {
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
                    val other2 = other as Term
                    value == other2.value
                }
            }
        }

        override fun paren(): String {
            return PVALUE
        }

        override val type: DataType
            get() = DataType.IDENTIFIER

        val value: String = Objects.requireNonNull(`val`)
        private val PVALUE: String = "($value)"

    }

    /**
     * VData that stores a string literal, for disassembly purposes.
     */
    internal class StrLit(`val`: String) : VData() {
        @Throws(IOException::class)
        override fun write(output: ByteBuffer) {
            throw IllegalStateException("Not valid for Terms.")
        }

        override fun calculateSize(): kotlin.Int {
            throw IllegalStateException("Not valid for Terms.")
        }

        override fun collectStrings(strings: MutableSet<StringTable.TString>) {
            throw IllegalStateException("Not valid for Terms.")
        }

        override val type: DataType
            get() = DataType.STRING

        override fun toString(): String {
            return ("\"" + value + "\"").replace("\n", "\\n")
        }

        override fun hashCode(): kotlin.Int {
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
                    val other2 = other as Term
                    value == other2.value
                }
            }
        }

        val value: String = Objects.requireNonNull(`val`)

    }

    companion object {
        /**
         * Creates a `VData` by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The string table.
         * @return The new `VData`.
         * @throws IOException Exceptions aren't handled.
         */
        @Throws(IOException::class)
        fun readVariableData(input: ByteBuffer, strings: StringTable): VData {
            val TYPE = DataType.read(input)
            return when (TYPE) {
                DataType.NONE -> None()
                DataType.IDENTIFIER -> {
                    val index = java.lang.Short.toUnsignedInt(input.short)
                    if (index < 0 || index >= strings.size) {
                        throw IOException()
                    }
                    ID(strings[index])
                }
                DataType.STRING -> {
                    val index = java.lang.Short.toUnsignedInt(input.short)
                    if (index < 0 || index >= strings.size) {
                        throw IOException()
                    }
                    Str(strings[index])
                }
                DataType.INTEGER -> {
                    val `val` = input.int
                    Int(`val`)
                }
                DataType.FLOAT -> {
                    val `val` = input.float
                    Flt(`val`)
                }
                DataType.BOOLEAN -> {
                    val `val` = input.get().toInt() != 0
                    Bool(`val`)
                }
            }
        }
    }
}