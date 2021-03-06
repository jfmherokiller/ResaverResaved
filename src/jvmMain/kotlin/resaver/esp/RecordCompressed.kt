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

import PlatformByteBuffer
import mf.BufferUtil
import java.nio.charset.StandardCharsets
import java.util.zip.DataFormatException

/**
 * RecordCompressed represents all records that are compressed.
 *
 * @author Mark Fairchild
 */
class RecordCompressed(recordCode: RecordCode, header: RecordHeader, input: PlatformByteBuffer, ctx: ESPContext) : Record() {
    /**
     *
     * @return The total size of the uncompressed data in the
     * `Record`.
     */
    private val uncompressedSize: Int
        private get() {
            return FIELDS
                .mapNotNull { it?.calculateSize() }
                .sum()
        }

    /**
     */
    private val uncompressedData: PlatformByteBuffer
        private get() {
            val DATA = PlatformByteBuffer.allocate(uncompressedSize)
            FIELDS.forEach { field: Field? ->
                field?.write(DATA)
            }
            return DATA
        }

    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: PlatformByteBuffer?) {
        output?.put(this.code.toString().toByteArray(StandardCharsets.UTF_8))
        val UNCOMPRESSED = uncompressedData
        val UNCOMPRESSED_SIZE = UNCOMPRESSED.capacity()
        (UNCOMPRESSED).flip()
        val COMPRESSED = BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_SIZE)
        val COMPRESSED_SIZE = COMPRESSED.limit()
        output?.putInt(4 + COMPRESSED_SIZE)
        HEADER.write(output)
        output?.putInt(UNCOMPRESSED_SIZE)
        output?.put(COMPRESSED)
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        val UNCOMPRESSED = uncompressedData
        val UNCOMPRESSED_SIZE = UNCOMPRESSED.capacity()
        (UNCOMPRESSED).flip()
        val COMPRESSED = BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_SIZE)
        val COMPRESSED_SIZE = COMPRESSED.capacity()
        return 28 + COMPRESSED_SIZE
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

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    override val code: RecordCode
    private val HEADER: RecordHeader
    private val FIELDS: FieldList

    companion object {
        /**
         * Skims a RecordCompressed by reading it from a LittleEndianInput.
         *
         * @param recordCode The record code.
         * @param header The header.
         * @param input The `ByteBuffer` to read.
         * @param ctx The mod descriptor.
         */
        @Throws(DataFormatException::class)
        fun skimRecord(recordCode: RecordCode, header: RecordHeader, input: PlatformByteBuffer, ctx: ESPContext) {
            assert(input.hasRemaining())
            val DECOMPRESSED_SIZE = input.getInt()
            val uncompressed = BufferUtil.inflateZLIB(input, DECOMPRESSED_SIZE)
            uncompressed.makeLe()
            val FIELDS = FieldList()
            while (uncompressed.hasRemaining()) {
                val newFields = readField(recordCode, uncompressed, ctx)
                FIELDS.addAll(newFields)
            }
            ctx.PLUGIN_INFO.addRecord(header.ID, FIELDS)
        }
    }

    /**
     * Creates a new RecordCompressed by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param header The header.
     * @param input The LittleEndianInput to readFully.
     * @param ctx The mod descriptor.
     * @throws java.util.zip.DataFormatException
     */
    init {
        assert(input.hasRemaining())
        this.code = recordCode
        HEADER = header
        FIELDS = FieldList()
        val DECOMPRESSED_SIZE = input.getInt()
        val uncompressed = BufferUtil.inflateZLIB(input, DECOMPRESSED_SIZE)
        uncompressed.makeLe()
        while (uncompressed.hasRemaining()) {
            val newFields = readField(recordCode, uncompressed, ctx)
            FIELDS.addAll(newFields)
        }
    }
}