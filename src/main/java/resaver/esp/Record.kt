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
import resaver.esp.Entry.Companion.advancingSlice
import resaver.esp.RecordBasic.Companion.skimRecord
import resaver.ess.papyrus.EID.Companion.pad8
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.DataFormatException

/**
 * Base class for the records of an ESP file.
 *
 * @author Mark Fairchild
 */
abstract class Record : Entry {
    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    abstract val code: RecordCode?

    /**
     * Header represents the standard header for all records except GRUP.
     *
     * @author Mark Fairchild
     */
    class Header(input: ByteBuffer, ctx: ESPContext) : Entry {
        /**
         * @see Entry.write
         */
        override fun write(output: ByteBuffer) {
            output.putInt(FLAGS)
            output.putInt(ID)
            output.putInt(REVISION)
            output.putShort(VERSION)
            output.putShort(UNKNOWN)
        }

        /**
         * @return The calculated size of the field.
         * @see Entry.calculateSize
         */
        override fun calculateSize(): Int {
            return 16
        }

        /**
         * Checks if the header indicates a compressed record.
         *
         * @return True if the field data is compressed, false otherwise.
         */
        val isCompressed: Boolean
            get() = FLAGS and 0x00040000 != 0

        /**
         * Checks if the header indicates localization (TES4 record only).
         *
         * @return True if the record is a TES4 and localization is enabled.
         */
        val isLocalized: Boolean
            get() = FLAGS and 0x00000080 != 0
        val FLAGS: Int
        val ID: Int
        val REVISION: Int
        val VERSION: Short
        val UNKNOWN: Short

        /**
         * Creates a new Header by reading it from a LittleEndianInput.
         *
         * @param input The LittleEndianInput to readFully.
         * @param ctx The mod descriptor.
         */
        init {
            FLAGS = input.int
            val id = input.int
            ID = ctx.remapFormID(id)
            REVISION = input.int
            VERSION = input.short
            UNKNOWN = input.short
        }
    }

    companion object {
        /**
         * Reads a field from an ESP file input and returns it. Usually only one
         * field is readFully, but if the field is an XXXX type, the next field will
         * be returned as well.
         *
         * @param parentCode The recordcode of the containing record.
         * @param input The `ByteBuffer` to read.
         * @param ctx The mod descriptor.
         * @return A list of fields that were readFully.
         */
        fun readField(parentCode: RecordCode, input: ByteBuffer, ctx: ESPContext): FieldList {
            return readFieldAux(parentCode, input, 0, ctx)
        }

        /**
         * Reads a field from an ESP file input and returns it. Usually only one
         * field is readFully, but if the field is an XXXX type, the next field will
         * be returned as well.
         *
         * @param parentCode The recordcode of the containing record.
         * @param input The LittleEndianInput to readFully.
         * @param bigSize The size of the field, if it was specified externally (a
         * "XXXX" record).
         * @param ctx The mod descriptor.
         * @return A list of fields that were readFully.
         */
        private fun readFieldAux(parentCode: RecordCode, input: ByteBuffer, bigSize: Int, ctx: ESPContext): FieldList {
            assert(input.hasRemaining())

            // Read the record identification code.
            val CODEBYTES = ByteArray(4)
            input[CODEBYTES]
            val CODE = IString.get(String(CODEBYTES))
            ctx.pushContext(CODE)

            // Read the record size.
            val BIG = bigSize > 0
            val DATASIZE = java.lang.Short.toUnsignedInt(input.short)
            val ACTUALSIZE = if (BIG) bigSize else DATASIZE

            // This list will hold between zero and two fields that are read.
            val FIELDS = FieldList()
            if (ACTUALSIZE == 0) {
                ctx.popContext()
                return FIELDS
            }
            val FIELDINPUT = advancingSlice(input, ACTUALSIZE)

            // Depending on what code we found, pick a subclass to readFully in the
            // rest of the data.
            if (CODE.equals(IString.get("XXXX"))) {
                val xxxx = FieldXXXX(CODE, FIELDINPUT)
                val fieldsRead = readFieldAux(parentCode, input, xxxx.data, ctx)
                FIELDS.add(xxxx)
                FIELDS.addAll(fieldsRead)
            } else if (CODE.equals(IString.get("VMAD"))) {
                val field = FieldVMAD(parentCode, CODE, FIELDINPUT, BIG, ctx)
                FIELDS.add(field)
            } else if (CODE.equals(IString.get("EDID"))) {
                val field = FieldEDID(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx)
                FIELDS.add(field)
            } else if (CODE.equals(IString.get("FULL"))) {
                val field = FieldFull(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx)
                FIELDS.add(field)
            } else if (CODE.equals(IString.get("NAME")) && (parentCode === RecordCode.ACHR
                        || parentCode === RecordCode.REFR)
            ) {
                val field = FieldName(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx)
                FIELDS.add(field)
            } else {
                val field: Field = FieldSimple(CODE, FIELDINPUT, ACTUALSIZE, BIG, ctx)
                FIELDS.add(field)
            }
            ctx.popContext()
            return FIELDS
        }

        /**
         * Reads a record from an ESP file input and returns it.
         *
         * @param input The LittleEndianInput to readFully.
         * @param ctx The mod descriptor.
         * @return The next Record from input.
         */
        fun readRecord(input: ByteBuffer, ctx: ESPContext): Record {
            // Read the record identification code.
            val CODEBYTES = ByteArray(4)
            input[CODEBYTES]
            val CODESTRING = String(CODEBYTES)
            val CODE = RecordCode.valueOf(CODESTRING)

            // Read the record size.
            val DATASIZE = input.int

            // GRUPs get handled differently than other records.
            return if (CODE === RecordCode.GRUP) {
                // Read the header.
                val HEADER = advancingSlice(input, 16)

                // Read the record data.
                val RECORDINPUT = advancingSlice(input, DATASIZE - 24)

                // Read the rest of the record.
                RecordGrup(CODE, HEADER, RECORDINPUT, ctx)
            } else {
                // Read the header.
                val HEADER = Header(input, ctx)

                // Read the record data.
                val RECORDINPUT = advancingSlice(input, DATASIZE)

                // Read the rest of the record. Handle compressed records separately.
                if (HEADER.isCompressed) {
                    try {
                        RecordCompressed(CODE, HEADER, RECORDINPUT, ctx)
                    } catch (ex: DataFormatException) {
                        throw IllegalStateException("Failed to read compressd record. $ctx", ex)
                    }
                } else {
                    RecordBasic(CODE, HEADER, RECORDINPUT, ctx)
                }
            }
        }

        /**
         * Reads a record from an ESP file input and returns it.
         *
         * @param input The LittleEndianInput to readFully.
         * @param ctx The mod descriptor.
         */
        fun skimRecord(input: ByteBuffer, ctx: ESPContext) {
            // Read the record identification code.
            val CODEBYTES = ByteArray(4)
            input[CODEBYTES]
            val CODESTRING = String(CODEBYTES)
            val CODE: RecordCode
            CODE = try {
                RecordCode.valueOf(CODESTRING)
            } catch (ex: Exception) {
                throw ex
            }

            // Read the record size.
            val DATASIZE = input.int

            // GRUPs get handled differently than other records.
            if (CODE === RecordCode.GRUP) {
                val HEADER = advancingSlice(input, 16)
                val PREFIX = HEADER.int
                val TYPE = HEADER.int
                when (TYPE) {
                    0 -> {
                        val TOP = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(PREFIX)
                        (TOP as Buffer).flip()
                        val tops = String(TOP.array())
                        ctx.pushContext(tops)
                    }
                    4, 5 -> {
                        val X = PREFIX and 0xFFFF
                        val Y = PREFIX ushr 4
                        ctx.pushContext("$X, $Y")
                    }
                    2 -> ctx.pushContext("Block $PREFIX")
                    3 -> ctx.pushContext("SubBlock $PREFIX")
                    else -> ctx.pushContext(pad8(PREFIX))
                }

                // Get the record data.
                val RECORDINPUT = advancingSlice(input, DATASIZE - 24)

                // Read the rest of the record.
                while (RECORDINPUT.hasRemaining()) {
                    skimRecord(RECORDINPUT, ctx)
                }
                ctx.popContext()
            } else {
                // Read the header.
                val HEADER = Header(input, ctx)
                ctx.pushContext(pad8(HEADER.ID))

                // Read the record data.
                val RECORDINPUT = advancingSlice(input, DATASIZE)

                // Read the rest of the record. Handle compressed records separately.
                if (HEADER.isCompressed) {
                    try {
                        RecordCompressed.skimRecord(CODE, HEADER, RECORDINPUT, ctx)
                    } catch (ex: DataFormatException) {
                        throw IllegalStateException("Failed to read compressd record. $ctx", ex)
                    }
                } else {
                    skimRecord(CODE, HEADER, RECORDINPUT, ctx)
                }
                ctx.popContext()
            }
        }
    }
}