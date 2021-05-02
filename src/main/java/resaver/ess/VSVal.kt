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
package resaver.ess

import java.lang.IllegalArgumentException
import java.util.Arrays
import resaver.ess.VSVal
import java.lang.Byte
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
        val size: Int = (firstByte and 0x3).toInt()
        when (size) {
            0 -> DATA = byteArrayOf(firstByte)
            1 -> DATA = byteArrayOf(firstByte, input.get())
            else -> DATA = byteArrayOf(firstByte, input.get(), input.get())
        }
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        output.put(DATA)
    }

    /**
     * @see resaver.ess.Element.calculateSize
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
            val size: Int = (DATA[0] and 0x3).toInt()
            return when (size) {
                0 -> {
                    Byte.toUnsignedInt(DATA[0]) ushr 2
                }
                1 -> {
                    (Byte.toUnsignedInt(DATA[0])
                            or (Byte.toUnsignedInt(DATA[1]) shl 8)) ushr 2
                }
                else -> {
                    (Byte.toUnsignedInt(DATA[0])
                            or (Byte.toUnsignedInt(DATA[1]) shl 8)
                            or (Byte.toUnsignedInt(DATA[2]) shl 16)) ushr 2
                }
            }
        }

    override fun hashCode(): Int {
        return Arrays.hashCode(DATA)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as VSVal
        return DATA.contentEquals(other.DATA)
    }

    private val DATA: ByteArray
}