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
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.DataFormatException

/**
 * Describes a BA2 file record.
 *
 * @author Mark Fairchild
 */
internal class BA2FileRecord(input: PlatformByteBuffer, header: BA2Header?) {
    override fun toString(): String {
        return name ?: String.format("%d bytes at offset %d", FILESIZE.coerceAtLeast(REALSIZE), OFFSET)
    }

    fun getData(channel: FileChannel): Optional<PlatformByteBuffer> {
        return try {
            when (FILESIZE) {
                0 -> {
                    val DATA = PlatformByteBuffer.allocate(REALSIZE)
                    DATA.readFileChannel(channel,OFFSET)
                    Optional.of(DATA)
                }
                REALSIZE -> {
                    val DATA = PlatformByteBuffer.allocate(FILESIZE)
                    DATA.readFileChannel(channel,OFFSET)
                    Optional.of(DATA)
                }
                else -> {
                    val COMPRESSED = PlatformByteBuffer.allocate(FILESIZE)
                    COMPRESSED.readFileChannel(channel,OFFSET)
                    COMPRESSED.readFileChannel(channel,OFFSET)
                    COMPRESSED.flip()
                    val DATA = BufferUtil.inflateZLIB(COMPRESSED, REALSIZE, FILESIZE)
                    Optional.of(DATA)
                }
            }
        } catch (ex: IOException) {
            Optional.empty()
        } catch (ex: DataFormatException) {
            Optional.empty()
        }
    }

    val path: Path?
        get() = try {
            Paths.get(name!!)
        } catch (ex: InvalidPathException) {
            null
        }
    val NAMEHASH: Int
    val EXT: ByteArray
    val DIRHASH: Int
    val FLAGS: Int
    val OFFSET: Long
    val FILESIZE: Int
    val REALSIZE: Int
    val ALIGN: Int
    var name: String?

    companion object {
        const val SIZE = 36
    }

    /**
     * Creates a new `FileRecord` by reading from a
     * `ByteBuffer`. The name field will be set to null.
     *
     * @param input The file from which to read..
     * @param header
     * @throws IOException
     */
    init {
        NAMEHASH = input.getInt()
        EXT = ByteArray(4)
        input[EXT]
        DIRHASH = input.getInt()
        FLAGS = input.getInt()
        OFFSET = input.getLong()
        FILESIZE = input.getInt()
        REALSIZE = input.getInt()
        ALIGN = input.getInt()
        name = null
    }
}