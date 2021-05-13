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
package ess

import resaver.WString
import java.nio.ByteBuffer

/**
 * Extends `IString` by handling charsets and storing the original
 * byte sequence.
 *
 * @author Mark Fairchild
 */
class WStringElement : WString, Element {
    /**
     * Copy constructor.
     *
     * @param other The original `WString`.
     */
    constructor(other: WStringElement?) : super(other) {}

    /**
     * Creates a new `WStringElement` from a character sequence; the
     * byte array is generated from the string using UTF-8 encoding.
     *
     * @param cs The `CharSequence`.
     */
    constructor(cs: CharSequence?) : super(cs) {}

    /**
     * Creates a new `WStringElement` from a character sequence and a
     * byte array.
     *
     * @param cs The `CharSequence`.
     * @param bytes The byte array.
     */
    private constructor(bytes: ByteArray) : super(bytes) {}

    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        if (output != null) {
            super.write(output)
        }
    }

    /**
     * @see Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return super.calculateSize()
    }

    companion object {
        /**
         * Creates a new `WStringElement` by reading from a
         * `ByteBuffer`.
         *
         * @param input The input stream.
         * @return The new `WStringElement`.
         */

        fun read(input: ByteBuffer?): WStringElement {
            val BYTES = input?.let { mf.BufferUtil.getWStringRaw(it) }
            return BYTES?.let { WStringElement(it) }!!
        }

        fun compare(w1: WStringElement?, w2: WStringElement?): Int {
            return w1!!.compareTo(w2!!)
        }
    }
}