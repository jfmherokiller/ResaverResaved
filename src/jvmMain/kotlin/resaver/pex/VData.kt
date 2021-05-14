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
package resaver.pex

import PlatformByteBuffer
import java.io.IOException

/**
 * Describes the data stored by a variable, property, or parameter.
 *
 * @author Mark Fairchild
 */
abstract class VData {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @param strings The string table.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    abstract fun write(output: PlatformByteBuffer?)

    /**
     * Calculates the size of the VData, in bytes.
     *
     * @return The size of the VData.
     */
    abstract fun calculateSize(): Int

    /**
     * Collects all of the strings used by the VData and adds them to a set.
     *
     * @param strings The set of strings.
     */
    open fun collectStrings(strings: MutableSet<TString?>?) {}

    /**
     * The `VData` is a `Term`, returns it encloded in
     * brackets. Otherwise it is identical to `toString()`.
     */
    open fun paren(): String {
        return this.toString()
    }

    /**
     * @return Returns the type of the VData.
     */
    abstract val type: DataType?

    companion object {
        /**
         * Creates a `VData` by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The string table.
         * @return The new `VData`.
         * @throws IOException Exceptions aren't handled.
         */
        @Throws(IOException::class)
        fun readVariableData(input: PlatformByteBuffer, strings: StringTable): VData {
            val TYPE = DataType.read(input)
            return when (TYPE) {
                DataType.NONE -> VDataNone()
                DataType.IDENTIFIER -> {
                    val index = UtilityFunctions.toUnsignedInt(input.getShort())
                    if (index < 0 || index >= strings.size) {
                        throw IOException()
                    }
                    VDataID(strings[index])
                }
                DataType.STRING -> {
                    val index = UtilityFunctions.toUnsignedInt(input.getShort())
                    if (index < 0 || index >= strings.size) {
                        throw IOException()
                    }
                    VDataStr(strings[index])
                }
                DataType.INTEGER -> {
                    val `val` = input.getInt()
                    VDataInt(`val`)
                }
                DataType.FLOAT -> {
                    val `val` = input.getFloat()
                    VDataFlt(`val`)
                }
                DataType.BOOLEAN -> {
                    val `val` = input.getByte().toInt() != 0
                    VDataBool(`val`)
                }
            }
        }
    }
}