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
package resaver.esp

import mf.BufferUtil
import java.nio.file.PathMatcher
import resaver.esp.StringsFile
import kotlin.Throws
import java.io.IOException
import java.util.Objects
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.HashSet
import java.nio.file.FileSystems
import org.mozilla.universalchardet.UniversalDetector
import resaver.ess.Plugin
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.HashMap
import java.util.logging.Logger

/**
 * A StringsFile stores reads and stores strings from the mod stringtables;
 * mostly just applies to Skyrim.esm and the DLCs.
 *
 * @author Mark Fairchild
 */
class StringsFile private constructor(path: Path, plugin: Plugin?, input: ByteBuffer, type: Type?) {
    /**
     * Retrieves a string using its string ID.
     *
     * @param stringID
     * @return
     */
    operator fun get(stringID: Int): String? {
        return TABLE[stringID]
    }

    /**
     * @see Object.toString
     * @return
     */
    override fun toString(): String {
        return PATH.fileName.toString()
    }

    /**
     * The reference for accessing the stringtable.
     */
    val TABLE: MutableMap<Int, String>

    /**
     * The `Plugin` that the `StringsFile` supplies.
     */
    val PLUGIN: Plugin

    /**
     * The name of the stringtable.
     */
    val PATH: Path

    /**
     * The three different types of Strings file.
     */
    enum class Type(glob: String) {
        STRINGS("glob:**.Strings"), ILSTRINGS("glob:**.ILStrings"), DLSTRINGS("glob:**.DLStrings");

        private val GLOB: PathMatcher

        companion object {
            fun match(file: Path?): Type? {
                if (STRINGS.GLOB.matches(file)) {
                    return STRINGS
                } else if (ILSTRINGS.GLOB.matches(file)) {
                    return ILSTRINGS
                } else if (DLSTRINGS.GLOB.matches(file)) {
                    return DLSTRINGS
                }
                return null
            }
        }

        init {
            GLOB = FS.getPathMatcher(glob)
        }
    }

    companion object {
        /**
         * Reads a `StringsFile` from a file.
         *
         * @param file The path to the file.
         * @param plugin The `Plugin` that the `StringsFile`
         * supplies.
         * @return The `StringsFile`.
         * @throws IOException
         */
        @Throws(IOException::class)
        fun readStringsFile(file: Path, plugin: Plugin?): StringsFile {
            Objects.requireNonNull(file)
            Objects.requireNonNull(plugin)
            FileChannel.open(file).use { channel ->
                val SIZE = channel.size().toInt()
                val BUFFER = ByteBuffer.allocate(SIZE)
                val bytesRead = channel.read(BUFFER)
                assert(bytesRead == SIZE)
                (BUFFER as Buffer).flip()
                return readStringsFile(file, plugin, BUFFER)
            }
        }

        /**
         * Reads a `StringsFile` from a `LittleEndianInput`.
         *
         * @param file The filename.
         * @param plugin The `Plugin` that the `StringsFile`
         * supplies.
         * @param input The input stream.
         * @return The `StringsFile`.
         */
        fun readStringsFile(file: Path, plugin: Plugin?, input: ByteBuffer): StringsFile {
            Objects.requireNonNull(file)
            Objects.requireNonNull(plugin)
            Objects.requireNonNull(input)
            val type = Type.match(file)
            return StringsFile(file, plugin, input, type)
        }

        private val CHARSET_LOG: MutableSet<Charset> = HashSet()
        private val LOG = Logger.getLogger(StringsFile::class.java.name)
        private val FS = FileSystems.getDefault()

        /**
         * Makes a string from a byte array in a region-friendly way. Thank you
         * Mozilla!
         *
         * @param bytes
         * @return
         */
        fun makeString(bytes: ByteArray): String {
            val DETECTOR = UniversalDetector(null)
            DETECTOR.handleData(bytes, 0, bytes.size)
            DETECTOR.dataEnd()
            val ENCODING = DETECTOR.detectedCharset
            DETECTOR.reset()
            val CHARSET = if (null == ENCODING) StandardCharsets.UTF_8 else Charset.forName(ENCODING)
            if (CHARSET_LOG.add(CHARSET)) {
                LOG.info(String.format("Detected a new character encoding: %s.", CHARSET))
            }
            return String(bytes, CHARSET)
        }
    }

    /**
     * Reads a `StringsFile` from a `LittleEndianInput`.
     *
     * @param name The name of the stringtable.
     * @param plugin The `Plugin` that the `StringsFile`
     * supplies.
     * @param input The input stream.
     * @param type The type of stringtable.
     */
    init {
        Objects.requireNonNull(input)
        Objects.requireNonNull(type)
        input.order(ByteOrder.LITTLE_ENDIAN)
        PATH = Objects.requireNonNull(path)
        PLUGIN = Objects.requireNonNull(plugin)!!
        val COUNT = input.int
        val SIZE = input.int
        val DATASTART = 8 + COUNT * 8
        TABLE = HashMap(COUNT)
        val DIRECTORY = input.slice().order(ByteOrder.LITTLE_ENDIAN)
        (DIRECTORY as Buffer).limit(COUNT * 8)
        for (i in 0 until COUNT) {
            val STRINGID = DIRECTORY.int
            val OFFSET = DIRECTORY.int
            (input as Buffer).position(DATASTART + OFFSET)
            if (type == Type.STRINGS) {
                val bytes = BufferUtil.getZStringRaw(input)
                val string = BufferUtil.mozillaString(bytes)
                TABLE[STRINGID] = string
            } else {
                val length = input.int
                val bytes = ByteArray(length)
                input[bytes]
                val string = BufferUtil.mozillaString(bytes)
                TABLE[STRINGID] = string
            }
        }
    }
}