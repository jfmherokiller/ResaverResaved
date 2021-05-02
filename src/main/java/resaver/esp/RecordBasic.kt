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

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

/**
 * RecordBasic represents all records that are not a GRUP and do not contain
 * compressed fields.
 *
 * @author Mark Fairchild
 */
class RecordBasic(
    private val CODE: RecordCode,
    /**
     * @return The record header.
     */
    val header: Header, input: ByteBuffer, ctx: ESPContext
) : Record() {
    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: ByteBuffer) {
        output.put(CODE.toString().toByteArray(StandardCharsets.UTF_8))
        output.putInt(calculateSize() - 24)
        header.write(output)
        FIELDS.forEach(Consumer { field: Field -> field.write(output) })
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        var sum = 24
        sum += FIELDS.stream().mapToInt { obj: Field -> obj.calculateSize() }.sum()
        return sum
    }

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    override val code: RecordCode
        get() = CODE


    /**
     * Returns a String representation of the Record, which will just be the
     * code string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        return code.toString()
    }

    private val FIELDS: FieldList = FieldList()

    companion object {
        /**
         * Skims a RecordBasic by reading it from a LittleEndianInput.
         *
         * @param recordCode The record code.
         * @param header The header.
         * @param input The `ByteBuffer` to read.
         * @param ctx The mod descriptor.
         */

        @JvmStatic
        fun skimRecord(CODE: RecordCode, HEADER: Header, RECORDINPUT: ByteBuffer, ctx: ESPContext) {
            val FIELDS = FieldList()
            while (RECORDINPUT.hasRemaining()) {
                val newFields = readField(CODE, RECORDINPUT, ctx)
                FIELDS.addAll(newFields)
            }
            ctx.PLUGIN_INFO.addRecord(HEADER.ID, FIELDS)
        }
    }

    /**
     * Creates a new RecordBasic by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The `ByteBuffer` to read.
     * @param ctx The mod descriptor.
     */
    init {
        while (input.hasRemaining()) {
            val newFields = readField(CODE, input, ctx)
            FIELDS.addAll(newFields)
        }
    }
}