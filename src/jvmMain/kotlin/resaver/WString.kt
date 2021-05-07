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

import java.nio.ByteBuffer
import java.util.*

/**
 * Extends `IString` by handling charsets and storing the original
 * byte sequence.
 *
 * @author Mark Fairchild
 */
open class WString : IString {
    /**
     * Copy constructor.
     *
     * @param other The original `WString`.
     */
    constructor(other: WString) : super(other) {
        RAW_BYTES = other.RAW_BYTES
    }

    /**
     * Creates a new `WString` from a character sequence; the byte
     * array is generated from the string using UTF-8 encoding.
     *
     * @param cs The `CharSequence`.
     */
    constructor(cs: CharSequence?) : super(cs!!) {
        RAW_BYTES = null
    }

    /**
     * Creates a new `WString` from a character sequence and a byte
     * array.
     *
     * @param bytes The byte array.
     */
    protected constructor(bytes: ByteArray?) : super(mf.BufferUtil.mozillaString(bytes!!)) {
        RAW_BYTES = if (Arrays.equals(super.uTF8, bytes)) null else bytes
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    open fun write(output: ByteBuffer?) {
        if (output != null) {
            val BYTES = uTF8
            if (BYTES!!.size > 0xFFFF) {
                output.putShort(0xFFFF.toShort())
                output.put(BYTES, 0, 0xFFFF)
            } else {
                output.putShort(BYTES.size.toShort())
                output.put(BYTES)
            }
        }
    }

    /**
     * @return The size of the `WString` in bytes.
     */
    open fun calculateSize(): Int {
        val BYTES = uTF8
        return if (BYTES!!.size > 0xFFFF) 2 + 0xFFFF else 2 + BYTES.size
    }

    /**
     * @see java.lang.String.getBytes
     * @see resaver.IString.getUTF8
     * @return An array of bytes representing the `IString`.
     */
    override val uTF8: ByteArray?
        get() = RAW_BYTES ?: super.uTF8
    private val RAW_BYTES: ByteArray?

    /**
     * Tests for case-insensitive value-equality with another
     * `TString`, `IString`, or `String`.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String.equalsIgnoreCase
     */
    //@Override
    //public boolean equals(Object obj) {
    //    return super.equals(obj);
    //}
    companion object {
        /**
         * Creates a new `WString` by reading from a `ByteBuffer`.
         *
         * @param input The input stream.
         * @return The new `WString`.
         */
        fun read(input: ByteBuffer?): WString {
            val BYTES = input?.let { mf.BufferUtil.getWStringRaw(it) }
            return WString(BYTES)
        }
    }
}