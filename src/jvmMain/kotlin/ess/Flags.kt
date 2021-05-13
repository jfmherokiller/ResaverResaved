/*
 * Copyright 2016 Mark.
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

import java.nio.ByteBuffer

import kotlin.experimental.and

/**
 * Stores binary flag fields.
 *
 * @author Mark
 */
abstract class Flags : Element {
    /**
     * @return An HTML representation of the `Flags`.
     */
    fun toHTML(): String {
        val ONES = charArrayOf(
            '\u2080',
            '\u2081',
            '\u2082',
            '\u2083',
            '\u2084',
            '\u2085',
            '\u2086',
            '\u2087',
            '\u2088',
            '\u2089'
        )
        //final char[] TENS = new char[]{'\u0000', '\u2081', '\u2082', '\u2083', '\u2084', '\u2085', '\u2086', '\u2087', '\u2088', '\u2089'};
        val BITS = 8 * calculateSize()
        val BUF = StringBuilder()

        //BUF.append("<p style=\"display:inline-table;\">");        
        //BUF.append("</p>");
        BUF.append("<code><table cellspacing=0 cellpadding=1 border=0.5 style=\"display:inline-table;\">")
        BUF.append("<tr align=center>")
        for (i in BITS - 1 downTo 0) {
            BUF.append("<td>")
            BUF.append(ONES[i / 10])
            BUF.append(ONES[i % 10])
            BUF.append("</td>")
        }
        BUF.append("</tr><tr align=center>")
        for (i in BITS - 1 downTo 0) {
            val flag = this.getFlag(i)
            BUF.append("<td><code>")
            BUF.append(if (flag) '1' else '0')
            BUF.append("</code></td>")
        }
        BUF.append("</tr></table></code>")
        return BUF.toString()
    }

    /**
     * Accesses the flag at a particular index in the field.
     *
     * @param index The index of the flag.
     * @return A boolean value representing the flag.
     */
    abstract fun getFlag(index: Int): Boolean

    /**
     * Accesses the flag corresponding to a ChangeFlagConstants.
     *
     * @param flag The ChangeFlagConstants.
     * @return A boolean value representing the flag.
     */
    fun getFlag(flag: ChangeFlagConstants): Boolean {
        return this.getFlag(flag.position)
    }

    /**
     * 8-bit array of flags.
     */
    class FlagsByte : Flags {
        constructor(input: ByteBuffer) {
            FLAGS = input.get()
        }

        constructor(`val`: Byte) {
            FLAGS = `val`
        }

        override fun write(output: ByteBuffer?) {
            output!!.put(FLAGS)
        }

        override fun calculateSize(): Int {
            return 1
        }

        override fun getFlag(index: Int): Boolean {
            require(!(index < 0 || index >= 8)) { "Invalid index: $index" }
            return 0x1 and (FLAGS.toInt() ushr index) != 0
        }

        fun checkMask(mask: Byte): Boolean {
            val i1 = mask.toInt() and 0xFF
            val i2 = FLAGS.toInt() and 0xFF
            val result = i1 and i2
            return result != 0
        }

        override fun toString(): String {
            val BITS = 8
            val binary = Integer.toBinaryString(FLAGS.toInt())
            val len = binary.length
            return ZEROS[BITS - len].toString() + binary
        }

        override fun hashCode(): Int {
            return Integer.hashCode(FLAGS.toInt())
        }

        override fun equals(other: Any?): Boolean {
            return other is FlagsByte && other.FLAGS == FLAGS
        }

        val FLAGS: Byte
    }

    /**
     * 16-bit array of flags.
     */
    class FlagsShort : Flags {
        constructor(input: ByteBuffer) {
            FLAGS = input.short
        }

        constructor(`val`: Short) {
            FLAGS = `val`
        }

        fun checkMask(mask: Short): Boolean {
            val result: Int = (FLAGS and mask).toInt()
            return result != 0
        }

        override fun write(output: ByteBuffer?) {
            output!!.putShort(FLAGS)
        }

        override fun calculateSize(): Int {
            return 2
        }

        override fun getFlag(index: Int): Boolean {
            require(!(index < 0 || index >= 16)) { "Invalid index: $index" }
            return 0x1 and (FLAGS.toInt() ushr index) != 0
        }

        override fun toString(): String {
            val BITS = 16
            val VAL: Int = (FLAGS.toInt() and 0xFFFF)
            val binary = Integer.toBinaryString(VAL)
            val len = binary.length
            return ZEROS[BITS - len].toString() + binary
        }

        override fun hashCode(): Int {
            return Integer.hashCode(FLAGS.toInt())
        }

        override fun equals(other: Any?): Boolean {
            return other is FlagsShort && other.FLAGS == FLAGS
        }

        val FLAGS: Short
    }

    /**
     * 32-bit array of flags.
     */
    class FlagsInt : Flags {
        constructor(input: ByteBuffer) {
            FLAGS = input.int
        }

        constructor(`val`: Int) {
            FLAGS = `val`
        }

        fun checkMask(mask: Short): Boolean {
            val result = FLAGS and mask.toInt()
            return result != 0
        }

        override fun write(output: ByteBuffer?) {
            output!!.putInt(FLAGS)
        }

        override fun calculateSize(): Int {
            return 4
        }

        override fun getFlag(index: Int): Boolean {
            require(!(index < 0 || index >= 32)) { "Invalid index: $index" }
            return 0x1 and (FLAGS ushr index) != 0
        }

        override fun toString(): String {
            val BITS = 32
            val binary = Integer.toBinaryString(FLAGS)
            val len = binary.length
            return ZEROS[BITS - len].toString() + binary
        }

        override fun hashCode(): Int {
            return FLAGS.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is FlagsInt && other.FLAGS == FLAGS
        }

        val FLAGS: Int
    }

    companion object {
        /**
         * Creates a new `Byte` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @return The `Byte` .
         */
        @JvmStatic
        fun readByteFlags(input: ByteBuffer): FlagsByte {
            return FlagsByte(input)
        }

        /**
         * Creates a new `Short` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @return The `Short` .
         */
        fun readShortFlags(input: ByteBuffer): FlagsShort {
            return FlagsShort(input)
        }

        /**
         * Creates a new `Int` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @return The `Int` .
         */
        fun readIntFlags(input: ByteBuffer): FlagsInt {
            return FlagsInt(input)
        }

        private val ZEROS = makeZeros()
        private fun makeZeros(): Array<String?> {
            val zeros = arrayOfNulls<String>(32)
            zeros[0] = ""
            for (i in 1..31) {
                zeros[i] = zeros[i - 1].toString() + "0"
            }
            return zeros
        }
    }
}