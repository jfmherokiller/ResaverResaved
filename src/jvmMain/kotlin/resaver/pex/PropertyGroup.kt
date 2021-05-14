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

/**
 * Describes the debugging information for a property group.
 *
 */
/**
 * Creates a DebugFunction by reading from a DataInput.
 *
 * @param input A datainput for a Skyrim PEX file.
 * @param strings The `StringTable` for the `PexFile`.
 * @throws IOException Exceptions aren't handled.
 */
internal class PropertyGroup(input: PlatformByteBuffer, strings: StringTable) {
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
        GROUPNAME.write(output)
        DOCSTRING.write(output)
        output.putInt(USERFLAGS)
        output.putShort(PROPERTIES.size.toShort())
        for (prop in PROPERTIES) {
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
        strings.add(GROUPNAME)
        strings.add(DOCSTRING)
        strings.addAll(PROPERTIES)
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     */
    val fullName: IString
        get() = format("%s.%s", OBJECTNAME, GROUPNAME)

    /**
     * @return The size of the `PropertyGroup`, in bytes.
     */
    fun calculateSize(): Int {
        return 12 + 2 * PROPERTIES.size
    }

    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    override fun toString(): String {
        return "$OBJECTNAME.$GROUPNAME [$PROPERTIES]"
    }

    private val OBJECTNAME: TString = strings.read(input)
    private val GROUPNAME: TString = strings.read(input)
    private val DOCSTRING: TString = strings.read(input)
    private val USERFLAGS: Int = input.getInt()
    private val PROPERTIES: MutableList<TString> = mutableListOf()


    init {
        val propertyCount = UtilityFunctions.toUnsignedInt(input.getShort())
        for (i in 0 until propertyCount) {
            PROPERTIES.add(strings.read(input))
        }
    }
}