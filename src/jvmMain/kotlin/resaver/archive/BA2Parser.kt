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
class BA2Parser(path: Path?, channel: FileChannel) : ArchiveParser(path!!, channel) {
    /**
     * @see ArchiveParser.getFiles
     */
    @Throws(IOException::class)
    override fun getFiles(dir: Path?, matcher: PathMatcher?): MutableMap<Path?, PlatformByteBuffer?> {
        return FILES
            .filter { file: BA2FileRecord -> dir == null || file.path!!.startsWith(dir) }
            .filter { file: BA2FileRecord -> matcher!!.matches(file.path) }
            .associateBy({file: BA2FileRecord -> super.PATH.resolve(file.path!!)},{file: BA2FileRecord -> file.getData(CHANNEL)}).toMutableMap()
    }

    /**
     * @see ArchiveParser.getFilenames
     */
    @Throws(IOException::class)
    override fun getFilenames(dir: Path?, matcher: PathMatcher?): Map<Path, Path> {
        return FILES
            .filter { file: BA2FileRecord -> dir == null || file.path!!.startsWith(dir) }
            .filter { file: BA2FileRecord -> matcher!!.matches(file.path) }
            .associateBy({ k: BA2FileRecord -> super.PATH.fileName.resolve(k.path!!) }, { v: BA2FileRecord -> v.path!! })
    }
    val HEADER: BA2Header
    private val FILES: MutableList<BA2FileRecord>

    /**
     * Creates a new `BA2Parser`.
     *
     * @param path
     * @param channel
     * @throws IOException
     * @see ArchiveParser.ArchiveParser
     */
    init {

        // Read the header.
        val HEADERBLOCK = PlatformByteBuffer.allocate(BA2Header.SIZE)
        HEADERBLOCK.makeLe()
        HEADERBLOCK.readFileChannel(channel)
        HEADERBLOCK.flip()
        HEADER = BA2Header(HEADERBLOCK)
        FILES = ArrayList(HEADER.FILE_COUNT)
        val FILERECORDS = PlatformByteBuffer.allocate(HEADER.FILE_COUNT * BA2FileRecord.SIZE)
        FILERECORDS.makeLe()
        FILERECORDS.readFileChannel(channel)
        FILERECORDS.makeLe()
        FILERECORDS.flip()

        // Read file records.
        for (i in 0 until HEADER.FILE_COUNT) {
            val file = BA2FileRecord(FILERECORDS, HEADER)
            FILES.add(file)
        }

        // Read the filename table.
        channel.position(HEADER.NAMETABLE_OFFSET)
        val NAMEBUFFER = PlatformByteBuffer.allocate(2048)
        NAMEBUFFER.makeLe()
        NAMEBUFFER.readFileChannel(channel)
        for (i in 0 until HEADER.FILE_COUNT) {
            NAMEBUFFER.flip()
            val fileName = BufferUtil.getWString(NAMEBUFFER)
            FILES[i].name = fileName
            NAMEBUFFER.compact()
            NAMEBUFFER.readFileChannel(channel)
        }
    }
}