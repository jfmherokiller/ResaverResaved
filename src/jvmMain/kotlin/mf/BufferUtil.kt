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
package mf

import net.jpountz.lz4.LZ4Factory
import org.jetbrains.annotations.Contract
import org.mozilla.universalchardet.UniversalDetector
import java.nio.Buffer
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 *
 * @author Mark
 */
object BufferUtil {
    /**
     * Reads an LString (4-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getLString(buffer: ByteBuffer): String? {
        val length = buffer.int
        return readSizedString(buffer, length, false)
    }

    /**
     * Reads a raw WString (4-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    fun getLStringRaw(buffer: ByteBuffer): ByteArray? {
        val length = buffer.int
        return readSizedStringRaw(buffer, length, false)
    }

    /**
     * Reads a WString (2-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getWString(buffer: ByteBuffer): String? {
        val length = UtilityFunctions.toUnsignedInt(buffer.short)
        return readSizedString(buffer, length, false)
    }

    /**
     * Reads a raw WString (2-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    fun getWStringRaw(buffer: ByteBuffer): ByteArray? {
        val length = UtilityFunctions.toUnsignedInt(buffer.short)
        return readSizedStringRaw(buffer, length, false)
    }

    /**
     * Reads a BString (1-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getBString(buffer: ByteBuffer): String? {
        val length = UtilityFunctions.toUnsignedInt(buffer.get())
        return readSizedString(buffer, length, false)
    }

    /**
     * Reads an LString (4-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getLZString(buffer: ByteBuffer): String? {
        val length = buffer.int
        return readSizedString(buffer, length, true)
    }

    /**
     * Reads a WString (2-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getWZString(buffer: ByteBuffer): String? {
        val length = UtilityFunctions.toUnsignedInt(buffer.short)
        return readSizedString(buffer, length, true)
    }

    /**
     * Reads a BString (1-byte length-prefixed).
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getBZString(buffer: ByteBuffer): String? {
        val length = java.lang.Byte.toUnsignedInt(buffer.get())
        return readSizedString(buffer, length, true)
    }

    /**
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getZString(buffer: ByteBuffer): String? {
        val BYTES = getZStringRaw(buffer)
        return if (BYTES == null) null else String(BYTES, StandardCharsets.UTF_8)
    }

    /**
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The raw byte data, excluding the terminus (if any).
     */
    fun getZStringRaw(buffer: ByteBuffer): ByteArray? {
        val start = buffer.position()

        //while (buffer.get() != 0);
        var b = buffer.get()
        while (b.toInt() != 0) {
            b = buffer.get()
        }
        val LENGTH = buffer.position() - start
        (buffer as Buffer).position(start)
        require(LENGTH > 0) { "Found invalid ZString length of $LENGTH" }
        return readSizedStringRaw(buffer, LENGTH, true)
    }

    /**
     * Reads a string with a known size.
     *
     * @param buffer The `ByteBuffer` to read.
     * @param zterminated A flag indicating whether the string is 0-terminated.
     * @param size The size of the string, including the terminus (if any).
     * @return The `String`.
     */
    fun readSizedString(buffer: ByteBuffer, size: Int, zterminated: Boolean): String? {
        val BYTES = readSizedStringRaw(buffer, size, zterminated)
        return if (BYTES == null) null else String(BYTES, StandardCharsets.UTF_8)
    }

    /**
     * Reads a string with a known size.
     *
     * @param buffer The `ByteBuffer` to read.
     * @param zterminated A flag indicating whether the string is 0-terminated.
     * @param size The size of the string, including the terminus (if any).
     * @return The raw byte data, excluding the terminus (if any).
     */
    fun readSizedStringRaw(buffer: ByteBuffer, size: Int, zterminated: Boolean): ByteArray? {
        return try {
            val length = if (zterminated) size - 1 else size
            when {
                length < 0 -> {
                    null
                }
                length == 0 -> {
                    ByteArray(0)
                }
                else -> {
                    val bytes = ByteArray(length)
                    buffer[bytes]
                    val terminus = if (zterminated) buffer.get().toInt() else 0
                    require(terminus == 0) { "Missing terminus" }
                    bytes
                }
            }
        } catch (ex: BufferUnderflowException) {
            throw ex
        }
    }

    /**
     * Reads a UTF8 string.
     *
     * @param buffer The `ByteBuffer` to read.
     * @return The `String`.
     */
    fun getUTF(buffer: ByteBuffer): String {
        val length = UtilityFunctions.toUnsignedInt(buffer.short)
        require((length < 0 || length >= 32768).not()) { "Invalid string length: $length" }
        return if (length == 0) {
            ""
        } else {
            val bytes = ByteArray(length)
            buffer[bytes]
            String(bytes, StandardCharsets.UTF_8)
        }
    }

    /**
     *
     * @param buffer The `ByteBuffer` to write.
     * @param string The `String`.
     * @return The `ByteBuffer` (allows chaining).
     */
    fun putZString(buffer: ByteBuffer, string: String): ByteBuffer {
        return buffer.put(string.toByteArray(StandardCharsets.UTF_8)).put(0.toByte())
    }

    /**
     *
     * @param buffer The `ByteBuffer` to write.
     * @param string The `String`.
     * @return The `ByteBuffer` (allows chaining).
     */
    fun putWString(buffer: ByteBuffer, string: String): ByteBuffer {
        val bytes = string.toByteArray(StandardCharsets.UTF_8)
        return buffer.putShort(bytes.size.toShort()).put(bytes)
    }

    /**
     * Converts a byte array to a string using the fancy Mozilla region
     * detection.
     *
     * @param bytes
     * @return
     */
    fun mozillaString(bytes: ByteArray): String {
        DETECTOR.handleData(bytes, 0, bytes.size)
        DETECTOR.dataEnd()
        val ENCODING = DETECTOR.detectedCharset
        DETECTOR.reset()
        val CHARSET =
            (if (null == ENCODING) StandardCharsets.UTF_8 else Charset.forName(ENCODING))
                ?: throw IllegalStateException("Missing character set.")
        return String(bytes, CHARSET)
    }

    /**
     * Simple wrapper for inflating a small `ByteBuffer` using ZLIB.
     *
     * @param compressed
     * @param uncompressedSize
     * @return
     * @throws java.util.zip.DataFormatException
     */
    @Throws(DataFormatException::class)
    fun inflateZLIB(compressed: ByteBuffer, uncompressedSize: Int): ByteBuffer {
        val compressedSize = compressed.limit() - compressed.position()
        return inflateZLIB(compressed, uncompressedSize, compressedSize)
    }

    /**
     * Simple wrapper for inflating a small `ByteBuffer` using ZLIB.
     *
     * @param compressed
     * @param uncompressedSize
     * @param compressedSize
     * @return
     * @throws java.util.zip.DataFormatException
     */
    @Throws(DataFormatException::class)
    fun inflateZLIB(compressed: ByteBuffer, uncompressedSize: Int, compressedSize: Int): ByteBuffer {
        val UNCOMPRESSED_BYTES = ByteArray(uncompressedSize)
        val COMPRESSED_BYTES = ByteArray(compressedSize)
        compressed[COMPRESSED_BYTES]
        val INFLATER = Inflater()
        return try {
            INFLATER.setInput(COMPRESSED_BYTES)
            val bytesInflated = INFLATER.inflate(UNCOMPRESSED_BYTES)
            check(bytesInflated == uncompressedSize) {
                String.format(
                    "Inflated %d bytes but expecting %d bytes.",
                    bytesInflated,
                    uncompressedSize
                )
            }
            ByteBuffer.wrap(UNCOMPRESSED_BYTES)
        } finally {
            INFLATER.end()
        }
    }

    /**
     * Simple wrapper for inflating a small `ByteBuffer` using LZ4.
     *
     * @param compressed
     * @param uncompressedSize
     * @return
     */
    fun inflateLZ4(compressed: ByteBuffer, uncompressedSize: Int): ByteBuffer {
        val uncompressed = ByteBuffer.allocate(uncompressedSize)
        val LZ4FACTORY = LZ4Factory.fastestInstance()
        val LZ4DECOMP = LZ4FACTORY.fastDecompressor()
        LZ4DECOMP.decompress(compressed, uncompressed)
        (uncompressed as Buffer).flip()
        return uncompressed
    }

    /**
     * Simple wrapper for deflating a small `ByteBuffer` using ZLIB.
     *
     * @param uncompressed
     * @param uncompressedSize
     * @return
     */
    fun deflateZLIB(uncompressed: ByteBuffer, uncompressedSize: Int): ByteBuffer {
        val SIZE = uncompressedSize.coerceAtMost(uncompressed.limit())
        val UNCOMPRESSED_BYTES = ByteArray(SIZE)
        val COMPRESSED_BYTES = ByteArray(11 * SIZE / 10)
        uncompressed[UNCOMPRESSED_BYTES]
        val DEFLATER = Deflater(Deflater.BEST_COMPRESSION)
        return try {
            DEFLATER.setInput(UNCOMPRESSED_BYTES)
            DEFLATER.deflate(COMPRESSED_BYTES)
            check(DEFLATER.bytesRead == SIZE.toLong()) {
                String.format(
                    "Inflated %d bytes but expecting %d bytes.",
                    DEFLATER.bytesRead,
                    SIZE
                )
            }
            ByteBuffer.wrap(COMPRESSED_BYTES, 0, DEFLATER.bytesWritten.toInt())
        } finally {
            DEFLATER.end()
        }
    }

    /**
     * Simple wrapper for deflating a small `ByteBuffer` using LZ4.
     *
     * @param uncompressed
     * @param uncompressedSize
     * @return
     */
    fun deflateLZ4(uncompressed: ByteBuffer, uncompressedSize: Int): ByteBuffer {
        val LZ4FACTORY = LZ4Factory.fastestInstance()
        val LZ4COMP = LZ4FACTORY.fastCompressor()
        val COMPRESSED = ByteBuffer.allocate(LZ4COMP.maxCompressedLength(uncompressedSize))
        LZ4COMP.compress(uncompressed, COMPRESSED)
        (COMPRESSED as Buffer).flip()
        return COMPRESSED
    }

    /**
     * Used to decode strings intelligently.
     */
    private val DETECTOR: UniversalDetector = UniversalDetector(null)

    /**
     * @return A log of all character sets that have been detected.
     */
    @get:Contract(pure = true)
    val charsetLog: Set<Charset>
        get() = CHARSET_LOG
    private val CHARSET_LOG: Set<Charset> = Charset.availableCharsets().values.toSet()
}