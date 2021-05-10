/*
 * Copyright 2020 Mark.
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

import java.nio.ByteBuffer


/**
 * Describes the table of file locations.
 *
 * @author Mark Fairchild
 */
class FileLocationTable : Element {
    /**
     * Creates a new `FileLocationTable` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param game Specifies which format to use.
     * @throws IOException
     */
    constructor(input: ByteBuffer, game: resaver.Game?) {
        GAME = game
        formIDArrayCountOffset = input.int
        unknownTable3Offset = input.int
        table1Offset = input.int
        table2Offset = input.int
        changeFormsOffset = input.int
        table3Offset = input.int
        TABLE1COUNT = input.int
        TABLE2COUNT = input.int
        val count = input.int
        TABLE3COUNT = if (GAME!!.isSkyrim) count + 1 else count
        changeFormCount = input.int
        UNUSED = IntArray(15)
        for (i in 0..14) {
            UNUSED[i] = input.int
        }
        t1size = table2Offset - table1Offset
        t2size = changeFormsOffset - table2Offset
        t3size = table3Offset - changeFormsOffset
    }

    /**
     * Creates a new `FileLocationTable` by analyzing an
     * `ESS`.
     *
     * @param ess The `ESS` to rebuild for.
     */
    constructor(ess: ESS) {
        GAME = ess.header.GAME
        TABLE1COUNT = ess.table1.size
        TABLE2COUNT = ess.table2.size
        TABLE3COUNT = ess.table3.size
        UNUSED = IntArray(15)
        var table1Size = 0
        for (globalData in ess.table1) {
            val calculateSize = globalData.calculateSize()
            table1Size += calculateSize
        }
        var table2Size = 0
        for (globalData in ess.table2) {
            val calculateSize = globalData.calculateSize()
            table2Size += calculateSize
        }
        var table3Size = 0
        for (globalData in ess.table3) {
            val calculateSize = globalData.calculateSize()
            table3Size += calculateSize
        }
        var changeFormsSize = 0
        for (changeForm in ess.changeForms.values) {
            val calculateSize = changeForm?.calculateSize()
            changeFormsSize += calculateSize!!
        }
        table1Offset = 0
        table1Offset += ess.header.calculateSize()
        table1Offset += 1
        table1Offset += ess.pluginInfo.calculateSize()
        table1Offset += calculateSize()
        if (null != ess.versionString) {
            table1Offset += ess.versionString!!.length + 2
        }
        table2Offset = table1Offset + table1Size
        changeFormCount = ess.changeForms.size
        changeFormsOffset = table2Offset + table2Size
        table3Offset = changeFormsOffset + changeFormsSize
        formIDArrayCountOffset = table3Offset + table3Size
        unknownTable3Offset = 0
        unknownTable3Offset += formIDArrayCountOffset
        unknownTable3Offset += 4 + 4 * ess.formIDs.size
        unknownTable3Offset += 4 + 4 * (ess.visitedWorldspaceArray?.size ?: 0)
    }

    /**
     * Rebuilds the file location table for an `ESS`.
     *
     * @param ess The `ESS` to rebuild for.
     */
    fun rebuild(ess: ESS) {
        var table1Size = 0
        for (globalData in ess.table1) {
            val calculateSize = globalData.calculateSize()
            table1Size += calculateSize
        }
        var table2Size = 0
        for (globalData in ess.table2) {
            val calculateSize = globalData.calculateSize()
            table2Size += calculateSize
        }
        var table3Size = 0
        for (globalData in ess.table3) {
            val calculateSize = globalData.calculateSize()
            table3Size += calculateSize
        }
        var changeFormsSize = 0
        for (changeForm in ess.changeForms.values) {
            val calculateSize = changeForm?.calculateSize()
            changeFormsSize += calculateSize!!
        }
        table1Offset = 0
        table1Offset += ess.header.calculateSize()
        table1Offset += 1
        table1Offset += ess.pluginInfo.calculateSize()
        table1Offset += calculateSize()
        if (null != ess.versionString) {
            table1Offset += ess.versionString!!.length + 2
        }
        table2Offset = table1Offset + table1Size
        changeFormCount = ess.changeForms.size
        changeFormsOffset = table2Offset + table2Size
        table3Offset = changeFormsOffset + changeFormsSize
        formIDArrayCountOffset = table3Offset + table3Size
        unknownTable3Offset = 0
        unknownTable3Offset += formIDArrayCountOffset
        unknownTable3Offset += 4 + 4 * ess.formIDs.size
        unknownTable3Offset += 4 + 4 * (ess.visitedWorldspaceArray?.size ?: 0)
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.putInt(formIDArrayCountOffset)
        output.putInt(unknownTable3Offset)
        output.putInt(table1Offset)
        output.putInt(table2Offset)
        output.putInt(changeFormsOffset)
        output.putInt(table3Offset)
        output.putInt(TABLE1COUNT)
        output.putInt(TABLE2COUNT)
        output.putInt(if (GAME!!.isSkyrim) TABLE3COUNT - 1 else TABLE3COUNT)
        output.putInt(changeFormCount)
        for (j in UNUSED) {
            output.putInt(j)
        }
    }

    /**
     * @see Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 100
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 73 * hash + formIDArrayCountOffset
        hash = 73 * hash + unknownTable3Offset
        hash = 73 * hash + table1Offset
        hash = 73 * hash + table2Offset
        hash = 73 * hash + changeFormsOffset
        hash = 73 * hash + table3Offset
        hash = 73 * hash + TABLE1COUNT
        hash = 73 * hash + TABLE2COUNT
        hash = 73 * hash + TABLE3COUNT
        hash = 73 * hash + changeFormCount
        hash = 73 * hash + UNUSED.contentHashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null || javaClass != other.javaClass) {
            return false
        }
        val other2 = other as FileLocationTable
        return when {
            formIDArrayCountOffset != other2.formIDArrayCountOffset -> {
                false
            }
            unknownTable3Offset != other2.unknownTable3Offset -> {
                false
            }
            table1Offset != other2.table1Offset -> {
                false
            }
            table2Offset != other2.table2Offset -> {
                false
            }
            changeFormsOffset != other2.changeFormsOffset -> {
                false
            }
            table3Offset != other2.table3Offset -> {
                false
            }
            TABLE1COUNT != other2.TABLE1COUNT -> {
                false
            }
            TABLE2COUNT != other2.TABLE2COUNT -> {
                false
            }
            TABLE3COUNT != other2.TABLE3COUNT -> {
                false
            }
            changeFormCount != other2.changeFormCount -> {
                false
            }
            else -> {
                UNUSED.contentEquals(other2.UNUSED)
            }
        }
    }

    var t1size = 0
    var t2size = 0
    var t3size = 0
    var formIDArrayCountOffset: Int
    var unknownTable3Offset: Int
    var table1Offset: Int
    var table2Offset: Int
    var changeFormsOffset: Int
    var table3Offset: Int
    val TABLE1COUNT: Int
    val TABLE2COUNT: Int
    val TABLE3COUNT: Int
    var changeFormCount: Int
    val UNUSED: IntArray
    val GAME: resaver.Game?
}