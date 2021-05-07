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

import ess.Plugin
import ess.PluginInfo
import ess.PluginInfo.Companion.makeFormID
import mf.BufferUtil
import resaver.IString
import resaver.esp.Entry.Companion.advancingSlice
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.*

/**
 * RecordTes4 is the first record. It handles its own data and is not read using
 * Record.readRecord().
 *
 * @author Mark Fairchild
 */
class RecordTes4(input: ByteBuffer, plugin: Plugin?, plugins: PluginInfo?, ctx: ESPContext) : Record() {
    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: ByteBuffer?) {
        output?.put(this.code.toString().toByteArray())
        output?.putInt(calculateSize() - 24)
        header.write(output)
        FIELDS.forEach { field: Field? -> field?.write(output) }
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        var sum = 24
        val result = FIELDS
            .mapNotNull { it?.calculateSize() }
            .sum()
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

    /**
     * Remaps formIDs. If the formID's master is not available, the plugin field
     * of the formid will be set to 255.
     *
     * @param id The ID to remap.
     * @param ctx The mod descriptor.
     * @return
     */
    fun remapFormID(id: Int, ctx: ESPContext?): Int {
        val headerIndex = id ushr 24
        assert(headerIndex in 0..255)
        return if (headerIndex == MASTERS.size) {
            makeFormID(PLUGIN, id)
        } else if (headerIndex < MASTERS.size) {
            val originPluginName = MASTERS[headerIndex]
            val origin = PLUGINS.paths[Paths.get(originPluginName)]
            if (origin == null) id or -0x1000000 else makeFormID(origin, id)
        } else {
            id or -0x1000000
        }
    }

    val PLUGIN: Plugin
    val MASTERS: List<String?>
    private val PLUGINS: PluginInfo

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    override val code: RecordCode

    /**
     * @return The record header.
     */
    val header: RecordHeader
    private val FIELDS: FieldList
    private var VERSION = 0f
    private var RECORD_COUNT = 0
    private var NEXT_RECORD = 0

    /**
     * Creates a new RecordTes4 by reading it from a LittleEndianInput.
     *
     * @param input The `ByteBuffer` to read.
     * @param plugin The `Plugin` corresponding to the
     * `ESP`.
     * @param plugins The list of plugins, for correcting FormIDs.
     * @param ctx The mod descriptor.
     */
    init {
        this.code = RecordCode.TES4
        PLUGIN = Objects.requireNonNull(plugin)!!
        PLUGINS = Objects.requireNonNull(plugins)!!
        val CODEBYTES = ByteArray(4)
        input[CODEBYTES]
        val CODESTRING = String(CODEBYTES)
        assert(CODESTRING == "TES4")
        ctx.pushContext(CODESTRING)
        val DATASIZE = input.int
        header = RecordHeader(input, ctx)

        // Read the record data.
        val FIELDINPUT = advancingSlice(input, DATASIZE)
        FIELDS = FieldList()
        while (FIELDINPUT.hasRemaining()) {
            val newFields = readField(RecordCode.TES4, FIELDINPUT, ctx)
            FIELDS.addAll(newFields)
        }
        val masters: MutableList<String?> = ArrayList()
        for (FIELD in FIELDS) {
            if (FIELD?.code!!.equals(IString["MAST"])) {
                if (FIELD is FieldSimple) {
                    val byteBuffer = FIELD.byteBuffer
                    val zString = BufferUtil.getZString(byteBuffer)
                    masters.add(zString)
                }
            }
        }
        MASTERS = Collections.unmodifiableList(ArrayList(masters))
        var HEDR = Optional.empty<ByteBuffer>()
        for (f in FIELDS) {
            if (f?.code!!.equals(IString["HEDR"])) {
                if (f is FieldSimple) {
                    val byteBuffer = f.byteBuffer
                    HEDR = Optional.of(byteBuffer)
                    break
                }
            }
        }
        if (HEDR.isPresent) {
            VERSION = HEDR.get().float
            RECORD_COUNT = HEDR.get().int
            NEXT_RECORD = HEDR.get().int
        } else {
            VERSION = Float.NaN
            RECORD_COUNT = 0
            NEXT_RECORD = 0
        }

        /*Map<String, Integer> esps = new java.util.HashMap<>(espList.size());
        for (int i = 0; i < espList.size(); i++) {
            esps.put(espList.get(i), i);
        }

        this.ESPs = java.util.Collections.unmodifiableMap(esps);*/
    }
}