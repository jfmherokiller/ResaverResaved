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
package resaver.archive


import GenericSupplier
import java.nio.channels.FileChannel
import java.io.IOException
import java.nio.Buffer
import java.nio.ByteOrder
import java.nio.file.Paths
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * Describes a BSA folder record.
 *
 * @author Mark Fairchild
 */
internal class BSAFolderRecord(input: ByteBuffer, header: BSAHeader, channel: FileChannel, names: GenericSupplier<String?>) {
    override fun toString(): String {
        return NAME!!
    }

    /**
     * A 64bit hash of the folder NAME.
     */
    val NAMEHASH: Long

    /**
     * The number of files in the folder.
     */
    val COUNT: Int
    var PADDING = 0
    var OFFSET: Long = 0
    var NAME: String? = null
    val PATH: Path
    val FILERECORDS: MutableList<BSAFileRecord> //final public BSAFileRecordBlock FILERECORDBLOCK;

    companion object {
        const val SIZE = 24
    }

    /**
     * Creates a new `BSAFolder` by reading from a
     * `LittleEndianDataInput`.
     *
     * @param input The file from which to read.
     * @param header
     * @param channel
     * @param names
     */
    init {
        NAMEHASH = input.long
        COUNT = input.int
        when (header.VERSION) {
            103, 104 -> {
                PADDING = 0
                OFFSET = input.int.toLong()
            }
            105 -> {
                PADDING = input.int
                OFFSET = input.long
            }
            else -> throw IOException("Unknown header version " + header.VERSION)
        }
        val BLOCK = ByteBuffer.allocate(1024 + COUNT * BSAFileRecord.SIZE)
        channel.read(BLOCK, OFFSET - header.TOTAL_FILENAME_LENGTH)
        BLOCK.order(ByteOrder.LITTLE_ENDIAN)
        (BLOCK as Buffer).flip()
        if (header.INCLUDE_DIRECTORYNAMES) {
            val NAMELEN = UtilityFunctions.toUnsignedInt(BLOCK.get())
            NAME = mf.BufferUtil.getZString(BLOCK)
        } else {
            NAME = null
        }
        PATH = Paths.get(NAME!!)
        FILERECORDS = mutableListOf()
        for (i in 0 until COUNT) {
            try {
                val file = BSAFileRecord(BLOCK, header, names)
                FILERECORDS.add(file)
            } catch (ex: BufferUnderflowException) {
                throw IOException(String.format("Buffer underflow while reading file %d/%d in %s.", i, COUNT, NAME))
            }
        }
    }
}