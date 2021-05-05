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
package resaver.ess.papyrus

import java.nio.ByteBuffer
import java.util.*

/**
 * Describes an EID of a papyrus element.
 *
 * @author Mark Fairchild
 */
abstract class EID
/**
 * Creates a new `EID`.
 */
private constructor() : PapyrusElement, Comparable<EID> {
    /**
     * An implementation of EID for 32 bit IDs.
     */
    class EID32(val VALUE: Int) : EID() {
        override fun longValue(): Long {
            return VALUE.toLong()
        }

        override fun write(output: ByteBuffer?) {
            output!!.putInt(VALUE)
        }

        override fun calculateSize(): Int {
            return 4
        }

        override fun toString(): String {
            return pad8(VALUE)
        }

        override fun is4Byte(): Boolean {
            return true
        }

        override fun derive(id: Long): EID {
            return EID32(id.toInt())
        }
    }

    /**
     * An implementation of EID for 64 bit IDs.
     */
    class EID64(val VALUE: Long) : EID() {
        override fun longValue(): Long {
            return VALUE
        }

        override fun write(output: ByteBuffer?) {
            output!!.putLong(VALUE)
        }

        override fun is8Byte(): Boolean {
            return true
        }

        override fun calculateSize(): Int {
            return 8
        }

        override fun toString(): String {
            return pad16(VALUE)
        }

        override fun derive(id: Long): EID {
            return EID64(id)
        }
    }

    /**
     * @return The `EID` as a 64bit int.
     */
    abstract fun longValue(): Long

    /**
     * @return A flag indicating if the `EID` is undefined.
     */
    val isUndefined: Boolean
        get() = isZero

    /**
     * @return A flag indicating if the `EID` is zero.
     */
    val isZero: Boolean
        get() = longValue() == 0L

    /**
     * Creates a new `EID` of the same size using a new value.
     *
     * @param id
     * @return
     */
    abstract fun derive(id: Long): EID

    /**
     * @return A flag indicating if the `EID` is 4-byte.
     */
    open fun is4Byte(): Boolean {
        return false
    }

    /**
     * @return A flag indicating if the `EID` is 8-byte.
     */
    open fun is8Byte(): Boolean {
        return false
    }

    override fun compareTo(other: EID): Int {
        Objects.requireNonNull(other)
        return java.lang.Long.compareUnsigned(longValue(), other.longValue())
    }

    override fun hashCode(): Int {
        return java.lang.Long.hashCode(longValue())
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (obj !is EID) {
            return false
        }
        return longValue() == obj.longValue()
    }

    companion object {
        /**
         * Reads a four-byte `EID` from a `ByteBuffer`.
         *
         * @param input The input stream.
         * @param pap The `Papyrus` structure to which the
         * `EID` belongs.
         * @return The `EID`.
         */
        @JvmStatic
        fun read4byte(input: ByteBuffer, pap: Papyrus): EID {
            Objects.requireNonNull(input)
            val VAL = input.int
            return make4byte(VAL, pap)
        }

        /**
         * Reads an eight-byte `EID` from a `ByteBuffer`.
         *
         * @param input The input stream.
         * @param pap The `Papyrus` structure to which the
         * `EID` belongs.
         * @return The `EID`.
         */
        @JvmStatic
        fun read8byte(input: ByteBuffer, pap: Papyrus): EID {
            Objects.requireNonNull(input)
            val VAL = input.long
            return make8Byte(VAL, pap)
        }

        /**
         * Makes a four-byte `EID` from an int.
         *
         * @param val The id value.
         * @param pap The `Papyrus` structure to which the
         * `EID` belongs.
         * @return The `EID`.
         */
        @JvmStatic
        fun make4byte(`val`: Int, pap: Papyrus): EID {
            return pap.EIDS.computeIfAbsent(`val`) { v: Number? -> EID32(`val`) }
        }

        /**
         * Makes an eight-byte `EID` from a long.
         *
         * @param val The id value.
         * @param pap The `Papyrus` structure to which the
         * `EID` belongs.
         * @return The `EID`.
         */
        @JvmStatic
        fun make8Byte(`val`: Long, pap: Papyrus): EID {
            return pap.EIDS.computeIfAbsent(`val`) { v: Number? -> EID64(`val`) }
        }

        /**
         * Pads an EID to return an 8 character hexadecimal string.
         *
         * @param id
         * @return
         */
        @JvmStatic
        fun pad8(id: Int): String {
            val hex = Integer.toHexString(id)
            val length = hex.length
            return ZEROES[8 - length].toString() + hex
        }

        /**
         * Pads an EID to return an 8 character hexadecimal string.
         *
         * @param id
         * @return
         */
        fun pad16(id: Long): String {
            val hex = java.lang.Long.toHexString(id)
            val length = hex.length
            return ZEROES[16 - length].toString() + hex
        }

        /**
         * An array of strings of zeroes with the length matching the index.
         */
        private val ZEROES = makeZeroes()
        private fun makeZeroes(): Array<String?> {
            val zeroes = arrayOfNulls<String>(16)
            zeroes[0] = ""
            for (i in 1 until zeroes.size) {
                zeroes[i] = zeroes[i - 1].toString() + "0"
            }
            return zeroes
        }
    }
}