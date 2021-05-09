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

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import kotlin.experimental.and
/**
 * A Skyrim variable-size value.
 *
 * @author Mark Fairchild
 */
class VSVal : Element {
    /**
     * Creates a new `ChangeForm` from an integer.
     *
     * @param val The value.
     */
    constructor(`val`: Int) {
        var `val` = `val`
        when {
            `val` <= 0x40 -> {
                `val` = `val` shl 2
                DATA = ByteArray(1)
                DATA[0] = `val`.toByte()
            }
            `val` <= 0x4000 -> {
                `val` = `val` shl 2
                DATA = ByteArray(2)
                DATA[0] = (`val` or 0x1).toByte()
                DATA[1] = (`val` shr 8).toByte()
            }
            `val` <= 0x40000000 -> {
                `val` = `val` shl 2
                DATA = ByteArray(3)
                DATA[0] = (`val` or 0x2).toByte()
                DATA[1] = (`val` shr 8).toByte()
                DATA[2] = (`val` shr 16).toByte()
            }
            else -> {
                throw IllegalArgumentException("VSVal cannot stores values greater than 0x40000000: $`val`")
            }
        }
    }

    /**
     * Creates a new `ChangeForm` by reading from a
     * `ByteBuffer`.
     *
     * @param input The input stream.
     */
    constructor(input: ByteBuffer) {
        val firstByte = input.get()
        val size: Int = (firstByte and 0x3.toByte()).toInt()
        DATA = when (size) {
            0 -> byteArrayOf(firstByte)
            1 -> byteArrayOf(firstByte, input.get())
            else -> byteArrayOf(firstByte, input.get(), input.get())
        }
    }

    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output?.put(DATA)
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return DATA.size
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return String.format("%d", value)
    }

    /**
     * @return The value stored in the VSVal.
     */
    val value: Int
        get() {
            val size: Int = (DATA[0] and 0x3.toByte()).toInt()
            return when (size) {
                0 -> {
                    UtilityFunctions.toUnsignedInt(DATA[0]) ushr 2
                }
                1 -> {
                    (UtilityFunctions.toUnsignedInt(DATA[0])
                            or (UtilityFunctions.toUnsignedInt(DATA[1]) shl 8)) ushr 2
                }
                else -> {
                    (UtilityFunctions.toUnsignedInt(DATA[0])
                            or (UtilityFunctions.toUnsignedInt(DATA[1]) shl 8)
                            or (UtilityFunctions.toUnsignedInt(DATA[2]) shl 16)) ushr 2
                }
            }
        }

    override fun hashCode(): Int {
        return DATA.contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as VSVal
        return DATA.contentEquals(other2.DATA)
    }

    private val DATA: ByteArray
}