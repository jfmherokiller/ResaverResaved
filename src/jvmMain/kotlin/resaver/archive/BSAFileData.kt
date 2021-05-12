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
import java.nio.channels.FileChannel
import java.nio.ByteOrder
import java.io.IOException
import net.jpountz.lz4.LZ4Exception
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.zip.DataFormatException

/**
 * Describes a block of BSA file data.
 *
 * @author Mark Fairchild
 */
object BSAFileData {
    fun getData(channel: FileChannel, record: BSAFileRecord, header: BSAHeader): ByteBuffer? {
        return try {
            channel.position(record.OFFSET.toLong())
            val buffer = ByteBuffer.allocate(record.FILESIZE)
            val bytesRead = channel.read(buffer)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            (buffer as Buffer).flip()
            check(bytesRead == record.FILESIZE) {
                String.format(
                    "Read %d bytes but expected %d bytes.",
                    bytesRead,
                    record.FILESIZE
                )
            }

            // If the filename is embedded, readFully it from the data block.
            // Otherwise retrieve it from the file record.
            if (header.EMBED_FILENAME) {
                val b = buffer.get()
                buffer.position(b + 2)
            }

            // If the file is compressed, inflateZLIB it. Otherwise just readFully it in.
            if (!record.ISCOMPRESSED) {
                buffer
            } else {
                val uncompressedSize = buffer.int
                val uncompressedData: ByteBuffer = when (header.VERSION) {
                    104 -> BufferUtil.inflateZLIB(buffer, uncompressedSize, record.FILESIZE)
                    105 -> BufferUtil.inflateLZ4(buffer, uncompressedSize)
                    else -> throw IOException("Unknown version ${header.VERSION}")
                } // = ByteBuffer.allocate(uncompressedSize);
                uncompressedData.order(ByteOrder.LITTLE_ENDIAN)
                uncompressedData
            }
        } catch (ex: LZ4Exception) {
            null
        } catch (ex: DataFormatException) {
            null
        } catch (ex: IOException) {
            null
        }
    }
}