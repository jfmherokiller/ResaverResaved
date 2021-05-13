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

/**
 * Describes the eleven types of variable data.
 *
 * @author Mark Fairchild
 */
/**
 * Create a new `Type`.
 *
 * @param VALID
 */
enum class VarType(private val VALID: Boolean = true) : PapyrusElement {
    NULL,
    REF,
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    VARIANT,
    STRUCT,
    INVALID_8(false),
    INVALID_9(false),
    INVALID_10(false),
    REF_ARRAY,
    STRING_ARRAY,
    INTEGER_ARRAY,
    FLOAT_ARRAY,
    BOOLEAN_ARRAY,
    VARIANT_ARRAY,
    STRUCT_ARRAY;

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.put(CODE.toByte())
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 1
    }

    /**
     * @return True if the `Type` is an array type, false otherwise.
     */
    val isArray: Boolean
        get() = CODE >= 10//|| this == UNKNOWN6 || this == UNKNOWN6_ARRAY;

    /**
     * @return True iff the `Type` is a reference type.
     */
    val isRefType: Boolean
        get() = this == REF || this == REF_ARRAY || this == STRUCT || this == STRUCT_ARRAY

    //|| this == UNKNOWN6 || this == UNKNOWN6_ARRAY;
    private val CODE: Int = ordinal

    companion object {
        /**
         * Read a `Type` from an input stream.
         *
         * @param input The input stream.
         * @return The `Type`.
         * @throws PapyrusFormatException
         */

        @Throws(PapyrusFormatException::class)
        fun read(input: ByteBuffer): VarType {
            val `val` = input.get().toInt()
            if (`val` < 0 || `val` >= VALUES.size) {
                throw PapyrusFormatException("Invalid type value: $`val`")
            }
            val T = VALUES[`val`]
            if (!T.VALID) {
                throw PapyrusFormatException("Invalid type value: $T")
            }
            return T
        }

        private val VALUES = values()
    }

}