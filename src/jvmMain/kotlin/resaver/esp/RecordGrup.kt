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


/**
 * Describes GRUP records.
 *
 * @author Mark Fairchild
 */
class RecordGrup(
    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    override val code: RecordCode, private val HEADER: ByteBuffer, input: ByteBuffer, ctx: ESPContext
) : Record() {
    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: ByteBuffer?) {
        output?.put(this.code.toString().toByteArray(StandardCharsets.UTF_8))
        output?.putInt(calculateSize())
        output?.put(HEADER)
        RECORDS.forEach { record: Record -> record.write(output) }
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        var sum = 24
        val result = RECORDS.sumOf { it.calculateSize() }
        sum += result
        return sum
    }

    /**
     * Returns a String representation of the Record, which will just be the
     * code string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        return this.code.toString()
    }

    private val RECORDS: MutableList<Record>

    /**
     * Creates a new RecordGRUP by reading it from a LittleEndianInput.
     *
     * @param code The record code, which must be RecordCode.GRUP.
     * @param headerData The header data (unused).
     * @param input The LittleEndianInput to read.
     * @param ctx The mod descriptor.
     */
    init {
        RECORDS = mutableListOf()
        while (input.hasRemaining()) {
            val record = readRecord(input, ctx)
            RECORDS.add(record)
        }
    }
}