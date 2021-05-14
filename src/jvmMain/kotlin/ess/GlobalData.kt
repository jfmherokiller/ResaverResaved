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

import PlatformByteBuffer
import ess.ESS.ESSContext

/**
 * Describes 3-byte formIDs from Skyrim savegames.
 *
 * @author Mark Fairchild
 */
class GlobalData(input: PlatformByteBuffer, context: ESSContext?, model: ModelBuilder?) : Element {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.putInt(type)
        output.putInt(dataBlock!!.calculateSize())
        dataBlock!!.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 8 + dataBlock!!.calculateSize()
    }/*if (!(this.TYPE == 1001 && this.BLOCK instanceof Papyrus)) {
            throw new IllegalStateException("Not a papyrus block.");
        }*/

    /**
     *
     * @return
     */
    val papyrus: ess.papyrus.Papyrus?
        get() =/*if (!(this.TYPE == 1001 && this.BLOCK instanceof Papyrus)) {
            throw new IllegalStateException("Not a papyrus block.");
        }*/
            dataBlock as ess.papyrus.Papyrus?

    /**
     * @see Object.toString
     * @return
     */
    override fun toString(): String {
        return super.toString() + ": type " + type.toString()
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + dataBlock.hashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as GlobalData
        return dataBlock == other2.dataBlock
    }

    /**
     * @return The value of the type field.
     */
    val type: Int

    /**
     * @return The data block.
     */
    var dataBlock: GlobalDataBlock? = null

    /**
     * Creates a new `GlobalData` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `ESSContext` info.
     * @param model A `ModelBuilder`.
     * @throws PapyrusException
     */
    init {
        type = input.getInt()
        val blockSize = input.getInt()
        val subSection = input.slice()
        subSection.makeLe()
        subSection.limit(blockSize)
        input.position(input.position() + blockSize)
        dataBlock = when (type) {
            3 -> GlobalVariableTable(subSection, context)
            1001 -> context?.let { model?.let { it1 -> ess.papyrus.Papyrus(subSection, it, it1) } }
            else -> {
                val DATA = ByteArray(blockSize)
                subSection[DATA]
                DefaultGlobalDataBlock(DATA)
            }
        }
        val calculatedSize = (calculateSize() - 8).toLong()
        check(calculatedSize == blockSize.toLong()) {
            String.format(
                "Missing data for table %d, calculated size is %d but block size is %d.",
                type,
                calculatedSize,
                blockSize
            )
        }
    }
}