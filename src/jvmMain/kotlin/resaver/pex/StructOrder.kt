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

import resaver.IString
import resaver.IString.Companion.format
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Describes the debugging information for a property group.
 *
 */
internal class StructOrder(input: ByteBuffer, strings: StringTable) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer) {
        OBJECTNAME.write(output)
        ORDERNAME.write(output)
        output.putShort(NAMES.size.toShort())
        for (prop in NAMES) {
            prop.write(output)
        }
    }

    /**
     * Collects all of the strings used by the DebugFunction and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<TString>) {
        strings.add(OBJECTNAME)
        strings.add(ORDERNAME)
        strings.addAll(NAMES)
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     */
    val fullName: IString
        get() = format("%s.%s", OBJECTNAME, ORDERNAME)

    /**
     * @return The size of the `StructOrder`, in bytes.
     */
    fun calculateSize(): Int {
        return 6 + 2 * NAMES.size
    }

    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    override fun toString(): String {
        return String.format("%s.%s [%s]", OBJECTNAME, ORDERNAME, NAMES.toString())
    }

    private val OBJECTNAME: TString = strings.read(input)
    private val ORDERNAME: TString = strings.read(input)
    private val NAMES: MutableList<TString> = mutableListOf()

    /**
     * Creates a DebugFunction by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The `StringTable` for the `PexFile`.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        val nameCount = input.short.toUInt()
        for (i in 0 until nameCount.toInt()) {
            NAMES.add(strings.read(input))
        }
    }
}