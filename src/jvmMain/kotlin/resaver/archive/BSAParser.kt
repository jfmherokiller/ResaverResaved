/*
 * Copyright 2016 Mark Fairchild
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
import PlatformByteBuffer
import mf.BufferUtil
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.*

/**
 * Handles the job of reading scripts out of BSA files.
 *
 * @author Mark Fairchild
 */
class BSAParser(path: Path?, channel: FileChannel) : ArchiveParser(path, channel) {
    @Throws(IOException::class)
    private fun getNames(channel: FileChannel): GenericSupplier<String?> {
        val FILENAMES_OFFSET = (HEADER!!.FOLDER_OFFSET
                + HEADER!!.FOLDER_COUNT.toLong() * BSAFolderRecord.SIZE + HEADER!!.TOTAL_FOLDERNAME_LENGTH + HEADER!!.FOLDER_COUNT
                + HEADER!!.FILE_COUNT.toLong() * BSAFileRecord.SIZE)
        val FILENAMESBLOCK = PlatformByteBuffer.allocate(HEADER!!.TOTAL_FILENAME_LENGTH)
        FILENAMESBLOCK.readFileChannel(channel,FILENAMES_OFFSET)
        FILENAMESBLOCK.makeLe()
        FILENAMESBLOCK.flip()
        return { BufferUtil.getZString(FILENAMESBLOCK) }
    }

    /**
     * @see ArchiveParser.getFiles
     */
    @Throws(IOException::class)
    override fun getFiles(dir: Path?, matcher: PathMatcher?): Map<Path?, PlatformByteBuffer?> {
        return FOLDERRECORDS!!
            .filter { block: BSAFolderRecord -> dir == null || dir == block.PATH }
            .flatMap { block: BSAFolderRecord -> block.FILERECORDS }
            .filter { rec: BSAFileRecord -> matcher!!.matches(rec.path) }
            .associateBy(
                    { record: BSAFileRecord -> super.PATH.fileName.resolve(record.path!!) },
                    { record: BSAFileRecord? -> BSAFileData.getData(super.CHANNEL, record!!, HEADER!!) }).mapValues { it.value!! }
    }

    /**
     * @see ArchiveParser.getFilenames
     */
    @Throws(IOException::class)
    override fun getFilenames(dir: Path?, matcher: PathMatcher?): Map<Path, Path> {
        return FOLDERRECORDS!!
            .filter { block: BSAFolderRecord -> dir == null || dir == block.PATH }
            .flatMap { block: BSAFolderRecord -> block.FILERECORDS }
            .filter { rec: BSAFileRecord -> matcher!!.matches(rec.path) }
            .associateBy(
                    { record: BSAFileRecord -> super.PATH.fileName.resolve(record.path!!) },
                    { obj: BSAFileRecord -> obj.path!! })
    }

    override fun toString(): String {
        return NAME
    }

    var HEADER: BSAHeader? = null
    private var FOLDERRECORDS: MutableList<BSAFolderRecord>? = null

    /**
     * Creates a new `BSAParser`.
     *
     * @param path
     * @param channel
     * @throws IOException
     * @see ArchiveParser.ArchiveParser
     */
    init {
        try {
            // Read the header.
            val HEADERBLOCK = PlatformByteBuffer.allocate(BSAHeader.SIZE)
            HEADERBLOCK.readFileChannel(channel)
            HEADERBLOCK.makeLe()
            (HEADERBLOCK).flip()
            HEADER = BSAHeader(HEADERBLOCK, path?.fileName.toString())

            // Read the filename table indirectly.
            val NAMES: GenericSupplier<String?> = if (HEADER!!.INCLUDE_FILENAMES) getNames(channel) else { { null.toString() } }

            // Allocate storage for the folder records and file records.
            FOLDERRECORDS = mutableListOf()

            // Read folder records.
            val FOLDERBLOCK = PlatformByteBuffer.allocate(HEADER!!.FOLDER_COUNT * BSAFolderRecord.SIZE)
            FOLDERBLOCK.readFileChannel(channel,HEADER!!.FOLDER_OFFSET.toLong())
            FOLDERBLOCK.makeLe()
            FOLDERBLOCK.flip()
            for (i in 0 until HEADER!!.FOLDER_COUNT) {
                val folder = BSAFolderRecord(FOLDERBLOCK, HEADER!!, channel, NAMES)
                FOLDERRECORDS!!.add(folder)
            }
        } catch (ex: IOException) {
            throw IOException("Failed to parse $path", ex)
        }
    }
}