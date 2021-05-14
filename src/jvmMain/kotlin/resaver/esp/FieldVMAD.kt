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
import resaver.IString

/**
 * FieldVMAD represents all a VMAD field.
 *
 * @author Mark Fairchild
 */
class FieldVMAD(recordCode: RecordCode, fieldCode: IString, input: PlatformByteBuffer, big: Boolean, ctx: ESPContext) : Field {
    /**
     * @see Entry.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(this.code.uTF8)
        output.putShort((if (BIG) 0 else calculateSize() - 6).toShort())
        output.putShort(VERSION)
        output.putShort(OBJFORMAT)
        output.putShort(SCRIPTS.size.toShort())
        SCRIPTS.forEach { script: Script -> script.write(output) }
        FRAGMENTS.forEach { fragment: FragmentBase -> fragment.write(output) }
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        var sum = 12
        var result = 0
        for (SCRIPT in SCRIPTS) {
            val calculateSize = SCRIPT.calculateSize()
            result += calculateSize
        }
        sum += result
        var sum1 = 0
        for (FRAGMENT in FRAGMENTS) {
            val calculateSize = FRAGMENT.calculateSize()
            sum1 += calculateSize
        }
        sum += sum1
        return sum
    }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        return this.code.toString()
    }

    private val RECORDCODE: RecordCode

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    override val code: IString
    private val VERSION: Short
    private val OBJFORMAT: Short
    private val SCRIPTS: List<Script>
    private val FRAGMENTS: MutableList<FragmentBase>
    val BIG: Boolean

    /**
     * Creates a new FieldVMAD by reading it from a LittleEndianInput.
     *
     * @param recordCode The record code.
     * @param fieldCode The field code, which must be "VMAD".
     * @param input The `ByteBuffer` to read.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx
     */
    init {
        assert(input.hasRemaining())
        assert(fieldCode.equals(IString["VMAD"]))
        RECORDCODE = recordCode
        this.code = fieldCode
        VERSION = input.getShort()
        OBJFORMAT = input.getShort()
        SCRIPTS = mutableListOf()
        FRAGMENTS = mutableListOf()
        BIG = big
        val scriptCount = UtilityFunctions.toUnsignedInt(input.getShort())
        for (i in 0 until scriptCount) {
            val script = Script(input, ctx)
            ctx.PLUGIN_INFO.addScriptData(script)
        }
        var i = 0
        while (input.hasRemaining()) {
            when (recordCode) {
                RecordCode.INFO, RecordCode.PACK -> FRAGMENTS.add(FragmentInfoPack(input, ctx))
                RecordCode.PERK -> FRAGMENTS.add(FragmentPerk(input, ctx))
                RecordCode.QUST -> FRAGMENTS.add(FragmentQust(input, ctx))
                RecordCode.SCEN -> FRAGMENTS.add(FragmentScen(input, ctx))
                RecordCode.TERM -> FRAGMENTS.add(FragmentTerm(input, ctx))
                else -> throw IllegalStateException("Unexpected fragment type: $recordCode")
            }
            i++
        }
    }
}