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
package ess

import ess.ESS.ESSContext
import resaver.Analysis
import java.nio.Buffer
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

import java.util.logging.Logger
import java.util.zip.DataFormatException

/**
 * Describes a ChangeForm.
 *
 * @author Mark Fairchild
 */
class ChangeForm(input: ByteBuffer, context: ESSContext) : Element, AnalyzableElement, Linkable {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        refID?.write(output)
        changeFlags.write(output)
        val RAWTYPE = TYPEFIELD and 0x3F
        when (dataLength) {
            LengthSize.INT8 -> {
                output!!.put(RAWTYPE.toByte())
                output.put(VERSION)
                output.put(length1.toByte())
                output.put(length2.toByte())
            }
            LengthSize.INT16 -> {
                output!!.put((RAWTYPE or 0x40).toByte())
                output.put(VERSION)
                output.putShort(length1.toShort())
                output.putShort(length2.toShort())
            }
            LengthSize.INT32 -> {
                output!!.put((RAWTYPE or 0x80).toByte())
                output.put(VERSION)
                output.putInt(length1)
                output.putInt(length2)
            }
            else -> throw IllegalStateException("Invalid type.")
        }
        output.put(RAWDATA)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 2
        sum += refID?.calculateSize() ?: 0
        sum += changeFlags.calculateSize()
        sum += when (dataLength) {
            LengthSize.INT8 -> 2
            LengthSize.INT16 -> 4
            LengthSize.INT32 -> 8
        }
        sum += length1
        return sum
    }

    /**
     * @return Returns the size of the data length, in bytes.
     */
    val dataLength: LengthSize
        get() = when (TYPEFIELD ushr 6) {
            0 -> LengthSize.INT8
            1 -> LengthSize.INT16
            2 -> LengthSize.INT32
            else -> throw IllegalArgumentException()
        }
    /**
     * Replaces the changeform's data, handling compression and decompression as
     * necessary.
     *
     * @param body The new body
     */
    /*private void updateRawData(ChangeFormData body) {
        Objects.requireNonNull(body);

        final int UNCOMPRESSED_SIZE = body.calculateSize();
        final ByteBuffer UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_SIZE);
        body.write(UNCOMPRESSED);
        ((Buffer) 1).flip()
        
        if (this.ISCOMPRESSED) {
            final Deflater DEFLATER = new Deflater();
            try {
                DEFLATER.setInput(UNCOMPRESSED);
                ByteBuffer COMPRESSED = ByteBuffer.allocate(11 * UNCOMPRESSED_SIZE / 10);
                DEFLATER.deflate(COMPRESSED);
                ((Buffer) 1).flip();
                this.length2 = DEFLATER.getTotalIn();
                this.length1 = DEFLATER.getTotalOut();

                this.RAWDATA = COMPRESSED;
                assert this.length2 == UNCOMPRESSED_SIZE;
                assert this.length1 == DEFLATER.getTotalOut();
            } finally {
                DEFLATER.end();
            }

        } else {
            this.length1 = UNCOMPRESSED_SIZE;
            assert this.length1 == body.calculateSize();
            assert this.length1 == this.RAWDATA.limit();
        }
    }*/
    /**
     * @return The version field.
     */
    val version: Int
        get() = VERSION.toInt()

    /**
     * Returns the raw data for the ChangeForm. It will be decompressed first,
     * if necessary.
     *
     * @return The raw form of the `ChangeFormData`.
     */
    val bodyData: ByteBuffer?
        get() = if (isCompressed) {
            decompress(RAWDATA, length2)
        } else {
            RAWDATA.let {
                ByteBuffer.allocate(it.size).put(
                    RAWDATA
                )
            }
        }

    /**
     * Parses the changeform's data and returns it, handling decompression as
     * necessary.
     *
     * If the changeform's data is compressed and cannot be successfully
     * decompressed, null will be returned.
     *
     * If the changeform's data cannot be parsed and `bestEffort` is
     * false, null will be returned.
     *
     * @param analysis
     * @param context The `ESSContext` info.
     * @param bestEffort A flag indicating whether or not to return a
     * ChangeFormDefault if there was a problem parsing the data.
     * @return The `ChangeFormData`.
     */
    fun getData(analysis: Analysis?, context: ESSContext?, bestEffort: Boolean): ChangeFormData? {
        if (parsedData != null) {
            return parsedData
        }
        val BODYDATA = bodyData ?: return null
        BODYDATA.order(ByteOrder.LITTLE_ENDIAN)
        (BODYDATA as Buffer).position(0)
        try {
            parsedData = when (type) {
                ChangeFormType.FLST -> context?.let { ChangeFormFLST(BODYDATA, changeFlags, it) }
                ChangeFormType.LVLN -> ChangeFormLVLN(BODYDATA, changeFlags, context)
                ChangeFormType.REFR -> changeFlags.let { refID?.let { it1 ->
                    ChangeFormRefr(BODYDATA, it,
                        it1, analysis, context)
                } }
                ChangeFormType.ACHR -> changeFlags.let { refID?.let { it1 ->
                    ChangeFormACHR(BODYDATA, it,
                        it1, context)
                } }
                ChangeFormType.NPC_ -> context?.let { ChangeFormNPC(BODYDATA, changeFlags, it) }
                else -> if (bestEffort) {
                    ChangeFormDefault(BODYDATA, length1)
                } else {
                    return null
                }
            }
        } catch (ex: ElementException) {
            LOG.warning(ex.message)
            if (bestEffort) {
                (BODYDATA as Buffer).position(0)
                parsedData = ChangeFormDefault(BODYDATA, length1)
            } else {
                return null
            }
        } catch (ex: BufferUnderflowException) {
            LOG.warning(ex.message)
            if (bestEffort) {
                (BODYDATA as Buffer).position(0)
                parsedData = ChangeFormDefault(BODYDATA, length1)
            } else {
                return null
            }
        }
        if (null == parsedData) {
            throw NullPointerException("This shouldn't happen!")
        }
        return parsedData
    }
    /**
     * Enables the modified flag, indicating that the changeform will have to
     * recalculate it's size and compressed data.
     */
    /*public void setModified() {
        this.modified = true;
    }*/
    /**
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return refID?.toHTML(target) ?: ""
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        BUF.append(type)
        if (null != refID?.PLUGIN) {
            BUF.append(" (").append(refID!!.PLUGIN).append(")")
        } else if (refID!!.type === RefID.Type.FORMIDX) {
            val k = 0
        }
        BUF.append(" refid=").append(refID)
        if (parsedData != null && parsedData is GeneralElement) {
            val gen = parsedData as GeneralElement?
            if (gen!!.hasVal("BASE_OBJECT")) {
                val base = gen.getVal("BASE_OBJECT") as RefID
                BUF.append(" base=").append(base.toString())
            } else if (gen.hasVal("INITIAL")) {
                val initial = gen.getGeneralElement("INITIAL")
                if (initial?.hasVal("BASE_OBJECT") == true) {
                    val base = initial.getVal("BASE_OBJECT") as RefID
                    BUF.append(" base=").append(base.toString())
                }
            }
        }
        return BUF.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        val HOLDERS = save!!.papyrus?.scriptInstances?.values
            ?.filter { i: ess.papyrus.ScriptInstance -> refID == i.refID }
            ?.toList()?.toSet()
        BUILDER.append("<html><h3>CHANGEFORM</h3>")
        BUILDER.append("<p>RefID: $refID</p>")
        BUILDER.append("<p style=\"display:inline-table;\">ChangeFlags: ${changeFlags.toHTML()}</p>")
        BUILDER.append("<p>")
        BUILDER.append("DataLength: $dataLength<br/>")
        BUILDER.append(type?.let { String.format("Type: %s (%d : %d)<br/>", type, it.SKYRIMCODE, type!!.FULL) })
        BUILDER.append(String.format("Version: %d<br/>", VERSION))
        if (length2 > 0) {
            BUILDER.append(String.format("Length: %d bytes (%d bytes uncompressed)<br/>", length1, length2))
        } else {
            BUILDER.append(String.format("Length: %d bytes<br/>", length1))
        }
        BUILDER.append("</p>")
        if (HOLDERS != null) {
            if (HOLDERS.isEmpty()) {
                BUILDER.append("<p>No attached instances.</p>")
            } else {
                BUILDER.append(String.format("<p>%d attached instances:</p><ul>", HOLDERS.size))
                HOLDERS.forEach { owner: ess.papyrus.ScriptInstance? ->
                    if (owner != null) {
                        BUILDER.append(
                            "<li>${owner.javaClass.simpleName} - ${(owner as Linkable).toHTML(this)}"
                        )
                    }
                }
                BUILDER.append("</ul>")
            }
        }
        BUILDER.append(refID?.let { "<h3>ANALYZE RAW DATA: ${it.toHTML(null)}</h3>" })
        val BODY = getData(analysis, save.context, true)
        when (BODY) {
            null -> {
                BUILDER.append("<p><b>The ChangeForm appears to contain invalid data.</b></p>")
            }
            is ChangeFormDefault -> {
                BUILDER.append("<p><b>The ChangeForm could not be parsed.</b></p>")
                BUILDER.append(BODY.getInfo(analysis, save))
            }
            else -> {
                BUILDER.append(BODY.getInfo(analysis, save))
            }
        }
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }
    /**
     * @return The `RefID` of the `CHangeForm`.
     */
    /**
     * The `RefID` of the `ChangeForm`.
     */
    var refID: RefID? = null
    /**
     * @return The changeflag field.
     */
    /**
     * ChangeFlags describe what parts of the form have changed.
     */
    var changeFlags: Flags.FlagsInt

    /**
     * The type of Form.
     */
    private var TYPEFIELD: Int = 0

    /**
     * @return The type field.
     */
    var type: ChangeFormType? = null
    private var VERSION: Byte = 0

    /**
     * For compressed changeForms, length1 represents the size of the compressed
     * data.
     */
    private var length1 = 0

    /**
     * For compressed changeForms, length2 represents the size of the
     * uncompressed data.
     */
    private var length2 = 0

    /**
     * @return Whether the data is compressed.
     */
    var isCompressed: Boolean = false
    private var RAWDATA: ByteArray
    private var parsedData: ChangeFormData? = null

    /**
     * Data sizes for the length fields.
     */
    enum class LengthSize {
        INT8, INT16, INT32
    }

    companion object {
        private val LOG = Logger.getLogger(ChangeForm::class.java.canonicalName)

        /**
         * Decompresses a buffer.
         *
         * @param buf
         * @param length
         * @return
         */
        private fun decompress(buf: ByteArray, length: Int): ByteBuffer? {
            return try {
                mf.BufferUtil.inflateZLIB(ByteBuffer.wrap(buf), length, buf.size)
            } catch (ex: DataFormatException) {
                null
            }
        }

        /**
         * Verifies that two instances of `ChangeForm` are identical.
         *
         * @param cf1 The first `ChangeForm`.
         * @param cf2 The second `ChangeForm`.
         * @throws IllegalStateException Thrown if the two instances of
         * `ChangeForm` are not equal.
         */
        @JvmStatic
        @Throws(IllegalStateException::class)
        fun verifyIdentical(cf1: ChangeForm, cf2: ChangeForm) {
            check(cf1.refID == cf2.refID) { "RefID mismatch: ${cf1.refID} vs ${cf2.refID}." }
            check(cf1.type == cf2.type) { "Type mismatch: ${cf1.type} vs ${cf2.type}." }
        }
    }

    /**
     * Creates a new `ChangeForm` by reading from a
     * `ByteBuffer`.
     *
     * @param input The input stream.
     * @param context The `ESSContext` info.
     */
    init {
        refID = context.readRefID(input)
        changeFlags = Flags.readIntFlags(input)
        TYPEFIELD = UtilityFunctions.toUnsignedInt(input.get())
        VERSION = input.get()
        val typeCode = TYPEFIELD and 0x3F
        val type = context.game.let { it?.let { it1 -> ChangeFormType.getType(it1, typeCode) } }
            ?: throw IllegalStateException("Invalid changeform type index: $typeCode")
        this.type = type
        when (dataLength) {
            LengthSize.INT8 -> {
                length1 = UtilityFunctions.toUnsignedInt(input.get())
                length2 = UtilityFunctions.toUnsignedInt(input.get())
            }
            LengthSize.INT16 -> {
                length1 = UtilityFunctions.toUnsignedInt(input.short)
                length2 = UtilityFunctions.toUnsignedInt(input.short)
            }
            LengthSize.INT32 -> {
                length1 = input.int
                length2 = input.int
            }
        }
        check(length1 >= 0) {
            String.format(
                "Invalid data size: l1 = %d, l2 = %d, %s, %s",
                length1,
                length2,
                this.type,
                refID
            )
        }
        check(length2 >= 0) {
            String.format(
                "Invalid data size: l1 = %d, l2 = %d, %s, %s",
                length1,
                length2,
                this.type,
                refID
            )
        }

        // Read the changeform's data.
        val BUF = ByteArray(length1)
        input[BUF]

        // If the length2 field is greater than 0, then the data is compressed.
        isCompressed = length2 > 0
        RAWDATA = BUF
        parsedData = null
        //this.modified = false;
    }
}