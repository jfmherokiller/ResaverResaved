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
package resaver.esp

import resaver.IString
import resaver.esp.ESPContext
import java.nio.ByteOrder
import java.lang.StringBuilder
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.Objects

/**
 * FieldSimple represents all fields that aren't a VMAD section.
 *
 * @author Mark Fairchild
 */
open class FieldSimple(code: IString, input: ByteBuffer, size: Int, big: Boolean, ctx: ESPContext?) : Field {
    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: ByteBuffer) {
        output.put(this.code.utF8)
        if (BIG) {
            val zero: Short = 0
            output.putShort(zero)
        } else {
            output.putShort(SIZE.toShort())
        }
        output.put(data)
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        return 6 + data.size
    }

    /**
     * Returns a copy of the data section in a `ByteBuffer`.
     *
     * @return A `ByteBuffer`
     */
    val byteBuffer: ByteBuffer
        get() {
            val buffer = ByteBuffer.allocate(SIZE)
            buffer.put(data)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            (buffer as Buffer).flip()
            return buffer
        }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        BUF.append(this.code).append("=")
        for (datum in data) {
            BUF.append(String.format("%02x", datum))
        }
        return BUF.toString()
    }

    private val SIZE: Int

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    final override val code: IString

    /**
     * @return The underlying byte array.
     */
    val data: ByteArray
    val BIG: Boolean

    /**
     * Creates a new FieldBasic by reading it from a `ByteBuffer`.
     *
     * @param code The field code.
     * @param input The `ByteBuffer` to read.
     * @param size The amount of data.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx
     */
    init {
        Objects.requireNonNull(input)
        SIZE = size
        this.code = code
        BIG = big
        data = ByteArray(size)
        input[data]
    }
}