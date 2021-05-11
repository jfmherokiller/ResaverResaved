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
package resaver.archive

import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.*

/**
 *
 * @author Mark
 */
abstract class ArchiveParser protected constructor(path: Path?, channel: FileChannel) : Closeable {
    @Throws(IOException::class)
    override fun close() {
        CHANNEL.close()
    }

    protected val CHANNEL: FileChannel
    protected val NAME: String
    protected var PATH: Path = Path.of("")

    /**
     * Creates a `Map` pairing `Path` and `ByteBuffer`.
     *
     * @param dir A base directory to search in.
     * @param matcher A `PathMatcher` to determine files of interest.
     * @return The `Map`.
     * @throws IOException
     */
    @Throws(IOException::class)
    abstract fun getFiles(dir: Path?, matcher: PathMatcher?): Map<Path?, Optional<ByteBuffer>>?

    /**
     * Creates a `Map` pairing full `Path` to `
     *
     * @param dir A base directory to search in.
     * @param matcher A `PathMatcher` to determine files of interest.
     * @return The `Map`.
     * @throws IOException
    ` */
    @Throws(IOException::class)
    abstract fun getFilenames(dir: Path?, matcher: PathMatcher?): Map<Path, Path>?

    companion object {
        /**
         * Creates a new `ArchiveParser` by reading from a
         * `LittleEndianRAF`. A reference to the
         * `LittleEndianRAF` will be kept.
         *
         * @param path The path of the archive.
         * @param channel The input file.
         * @return The `ArchiveParser`.
         * @throws IOException
         */
        @JvmStatic
        @Throws(IOException::class)
        fun createParser(path: Path?, channel: FileChannel): ArchiveParser? {
            val magic = ByteBuffer.allocate(4)
            channel.read(magic, 0)
            if (Arrays.equals(magic.array(), BSA_MAGIC)) {
                return BSAParser(path, channel)
            } else if (Arrays.equals(magic.array(), BA2GEN_MAGIC)) {
                return BA2Parser(path, channel)
            }
            return null
        }

        private val BSA_MAGIC = byteArrayOf('B'.code.toByte(), 'S'.code.toByte(), 'A'.code.toByte(), 0)
        private val BA2GEN_MAGIC = byteArrayOf('B'.code.toByte(), 'T'.code.toByte(), 'D'.code.toByte(), 'X'.code.toByte())
    }

    /**
     * Creates a new `ArchiveParser` with a name and a
     * `LittleEndianRAF`. A reference to the
     * `LittleEndianRAF` will be kept.
     *
     * @param path The path of the archive.
     * @param channel The file from which to read.
     * @throws IOException
     */
    init {
        if (path != null) {
            PATH = path
        }
        NAME = path?.fileName.toString()
        CHANNEL = channel
    }
}