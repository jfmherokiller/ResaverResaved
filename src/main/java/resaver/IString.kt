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
package resaver

import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * A case-insensitive version of the String class.
 *
 * IString has value semantics. It's `equals(obj)` method will return
 * true for any String or IString that contains a matching string.
 *
 * @author Mark Fairchild
 */
open class IString : CharSequence, Serializable, Comparable<IString> {
    /**
     * Creates a new `IString` with a specified value.
     *
     * @param val The value to store, as a `String`.
     */
    protected constructor(`val`: String) {
        STRING = `val`
        HASHCODE = STRING.toLowerCase().hashCode()
    }

    /**
     * Creates a new `IString` with a specified value.
     *
     * @param val The value to store, as a `String`.
     */
    protected constructor(`val`: CharSequence) {
        STRING = `val`.toString()
        HASHCODE = STRING.toLowerCase().hashCode()
    }

    /**
     * Copy constructor.
     *
     * @param other The original `IString`.
     */
    protected constructor(other: IString) {
        STRING = other.STRING
        HASHCODE = other.HASHCODE
    }

    /**
     * Creates a new blank `IString`.
     */
    private constructor() : this("")

    /**
     * @see java.lang.String.isEmpty
     * @return True if the `IString` is empty, false otherwise.
     */
    override fun isEmpty(): Boolean {
        return STRING.isEmpty()
    }

    /**
     * @return The length of the `IString`.
     * @see java.lang.String.length
     */
    override val length: Int
        get() = STRING.length

    /**
     * @see kotlin.String.get
     */
    override fun get(index: Int): Char {
        return STRING[index]
    }

    /**
     * @see java.lang.String.subSequence
     */
    override fun subSequence(startIndex: Int, endIndex: Int): IString {
        return IString(STRING.substring(startIndex, endIndex))
    }

    /**
     * @see java.lang.String.getBytes
     * @return An array of bytes representing the `IString`.
     */
    open val uTF8: ByteArray?
        get() = STRING.toByteArray(StandardCharsets.UTF_8)

    /**
     * Returns the `String` value of the `IString`.
     *
     * @return
     */
    override fun toString(): String {
        return STRING
    }

    /**
     * Calculates a case-insensitive hashcode for the `IString`.
     *
     * @see java.lang.String.hashCode
     */
    override fun hashCode(): Int {
        return HASHCODE
    }

    /**
     * Tests for case-insensitive value-equality with another
     * `IString` or a `String`.
     *
     * @param other The object to which to compare.
     * @see java.lang.String.equalsIgnoreCase
     */
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> {
                true
            }
            other is IString -> {
                this.equals(other)
            }
            other is String -> {
                this.equals(other as String?)
            }
            else -> {
                super.equals(other)
            }
        }
    }

    /**
     * Tests for case-insensitive value-equality with a `String`.
     *
     * @param other The `String` to which to compare.
     * @return
     * @see java.lang.String.equalsIgnoreCase
     */
    fun equals(other: String?): Boolean {
        return STRING.equals(other, ignoreCase = true)
    }

    /**
     * Tests for case-insensitive value-equality with an `IString`.
     *
     * @param other The `IString` to which to compare.
     * @return
     * @see java.lang.String.equalsIgnoreCase
     */
    fun equals(other: IString): Boolean {
        return HASHCODE == other.hashCode() && STRING.equals(other.STRING, ignoreCase = true)
    }

    /**
     * @see java.lang.Comparable.compareTo
     * @param other
     * @return
     */
    override fun compareTo(other: IString): Int {
        return compare(this, other)
    }

    /**
     * Performs case-insensitive regular-expression matching on the
     * `IString`.
     *
     * @param regex The regular expression.
     * @see java.lang.String.matches
     * @return True if the `IString` matches the regex, false
     * otherwise.
     */
    fun matches(regex: String): Boolean {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(STRING).matches()
    }

    private var STRING: String = ""
    private val HASHCODE: Int

    companion object {
        private val CACHE = mutableMapOf<String, IString>()

        /**
         * A re-usable blank `IString`.
         */
        @JvmField
        val BLANK = IString()

        /**
         * Creates a new `IString` with a specified value.
         *
         * @param val The value to store, as a `String`.
         * @return The new `IString`.
         */
        @JvmStatic
        operator fun get(`val`: String): IString {
            //return CACHE.computeIfAbsent(val, v -> new IString(v.intern()));
            return CACHE.getOrPut(`val`) { IString(`val`) }
            //return new IString(val);
        }

        /**
         * @see java.lang.String.format
         * @param format The format string.
         * @param args The arguments to the format string.
         * @return A formatted `IString`
         */
        @JvmStatic
        fun format(format: String?, vararg args: Any?): IString {
            return IString(String.format(format!!, *args))
        }

        @JvmStatic
        fun compare(s1: IString, s2: IString): Int {
            return compareValuesBy(s1.STRING, s2.STRING) {
                s1.STRING.compareTo(
                    s2.STRING, ignoreCase = true
                )
            }
        }
    }
}