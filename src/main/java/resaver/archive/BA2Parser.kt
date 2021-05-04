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

import mf.BufferUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.*
import java.util.stream.Collectors

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
    override fun getFiles(dir: Path?, matcher: PathMatcher?): Map<Path?, Optional<ByteBuffer?>?>? {
        return FILES.stream()
            .filter { file: BA2FileRecord -> dir == null || file.path.startsWith(dir) }
            .filter { file: BA2FileRecord -> matcher!!.matches(file.path) }
            .collect(
                Collectors.toMap(
                    { file: BA2FileRecord -> super.PATH.resolve(file.path) },
                    { file: BA2FileRecord -> file.getData(CHANNEL) })
            )
    }

    /**
     * @see ArchiveParser.getFilenames
     */
    @Throws(IOException::class)
    override fun getFilenames(dir: Path?, matcher: PathMatcher?): Map<Path?, Path?>? {
        return FILES.stream()
            .filter { file: BA2FileRecord -> dir == null || file.path.startsWith(dir) }
            .filter { file: BA2FileRecord -> matcher!!.matches(file.path) }
            .collect(
                Collectors.toMap(
                    { record: BA2FileRecord -> super.PATH.fileName.resolve(record.path) },
                    { obj: BA2FileRecord -> obj.path })
            )
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
        val HEADERBLOCK = ByteBuffer.allocate(BA2Header.SIZE).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(HEADERBLOCK)
        HEADERBLOCK.flip()
        HEADER = BA2Header(HEADERBLOCK)
        FILES = ArrayList(HEADER.FILE_COUNT)
        val FILERECORDS = ByteBuffer.allocate(HEADER.FILE_COUNT * BA2FileRecord.SIZE).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(FILERECORDS)
        FILERECORDS.order(ByteOrder.LITTLE_ENDIAN).flip()

        // Read file records.
        for (i in 0 until HEADER.FILE_COUNT) {
            val file = BA2FileRecord(FILERECORDS, HEADER)
            FILES.add(file)
        }

        // Read the filename table.
        channel.position(HEADER.NAMETABLE_OFFSET)
        val NAMEBUFFER = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(NAMEBUFFER)
        for (i in 0 until HEADER.FILE_COUNT) {
            NAMEBUFFER.flip()
            val fileName = BufferUtil.getWString(NAMEBUFFER)
            FILES[i].name = fileName
            NAMEBUFFER.compact()
            channel.read(NAMEBUFFER)
        }
    }
}