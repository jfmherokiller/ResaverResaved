/*
 * Copyright 2018 Mark.
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
package resaver.pex

import PlatformByteBuffer
import resaver.IString.Companion.format
import kotlin.Throws
import java.io.IOException
import resaver.IString
import java.lang.StringBuilder

/**
 * Describes the debugging information for a function.
 *
 */
internal class DebugFunction(input: PlatformByteBuffer, strings: StringTable) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: PlatformByteBuffer) {
        OBJECTNAME.write(output)
        STATENAME.write(output)
        FUNCNAME.write(output)
        output.put(FUNCTYPE)
        output.putShort(INSTRUCTIONS.size.toShort())
        INSTRUCTIONS.forEach { instr: Int -> output.putShort(instr.toShort()) }
    }

    /**
     * Collects all of the strings used by the DebugFunction and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<TString?>) {
        strings.add(OBJECTNAME)
        strings.add(STATENAME)
        strings.add(FUNCNAME)
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     */
    val fullName: IString
        get() = format("%s.%s", OBJECTNAME, FUNCNAME)

    /**
     * @return The size of the `DebugFunction`, in bytes.
     */
    fun calculateSize(): Int {
        return 9 + 2 * INSTRUCTIONS.size
    }

    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("$OBJECTNAME $STATENAME.$FUNCNAME (type $FUNCTYPE): ")
        INSTRUCTIONS.forEach { instr: Int? -> buf.append(String.format("%04x ", instr)) }
        return buf.toString()
    }

    private val OBJECTNAME: TString = strings.read(input)
    private val STATENAME: TString = strings.read(input)
    private val FUNCNAME: TString = strings.read(input)
    private val FUNCTYPE: Byte = input.getByte()
    private val INSTRUCTIONS: MutableList<Int>

    /**
     * Creates a DebugFunction by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The `StringTable` for the `PexFile`.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        val instructionCount = UtilityFunctions.toUnsignedInt(input.getShort())
        INSTRUCTIONS = mutableListOf()
        for (i in 0 until instructionCount) {
            INSTRUCTIONS.add(UtilityFunctions.toUnsignedInt(input.getShort()))
        }
    }
}