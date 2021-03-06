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
package ess

import PlatformByteBuffer
import ess.CompressionType.Companion.read
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.*
import javax.swing.ImageIcon
import kotlin.experimental.and

/**
 * Describes header of Skyrim savegames.
 *
 * @author Mark Fairchild
 */
class Header(input: PlatformByteBuffer, path: Path?) : Element {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(MAGIC)
        output.putInt(partialSize())
        output.putInt(VERSION)
        output.putInt(SAVENUMBER)
        NAME.write(output)
        output.putInt(LEVEL)
        LOCATION.write(output)
        GAMEDATE.write(output)
        RACEID.write(output)
        output.putShort(SEX)
        output.putFloat(CURRENT_XP)
        output.putFloat(NEEDED_XP)
        output.putLong(FILETIME)
        output.putInt(SCREENSHOT_WIDTH)
        output.putInt(SCREENSHOT_HEIGHT)
        if (compression != null) {
            compression!!.write(output)
        }
        output.put(SCREENSHOT)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 4
        sum += partialSize()
        sum += MAGIC.size
        sum += SCREENSHOT.size
        return sum
    }

    /**
     * The size of the header, not including the magic string, the size itself,
     * or the screenshot.
     *
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    private fun partialSize(): Int {
        var sum = 0
        sum += 4 // version
        sum += 4 // savenumber
        sum += NAME.calculateSize()
        sum += 4 // level
        sum += LOCATION.calculateSize()
        sum += GAMEDATE.calculateSize()
        sum += RACEID.calculateSize()
        sum += 2 // sex
        sum += 4 // current xp
        sum += 4 // needed xp
        sum += 8 // filtime
        sum += 8 // screenshot size
        sum += if (compression == null) 0 else compression!!.calculateSize()
        return sum
    }

    /**
     * @param width The width for scaling.
     * @return A `ImageIcon` that can be used to display the
     * screenshot.
     */
    fun getImage(width: Int): ImageIcon? {
        if (IMAGE == null) {
            return null
        }
        val scale = width.toDouble() / SCREENSHOT_WIDTH.toDouble()
        val newWidth = (scale * SCREENSHOT_WIDTH).toInt()
        val newHeight = (scale * SCREENSHOT_HEIGHT).toInt()
        val IMG = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val XFORM = AffineTransform.getScaleInstance(scale, scale)
        val G = IMG.createGraphics()
        G.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        G.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        G.drawRenderedImage(IMAGE, XFORM)
        return ImageIcon(IMG)
    }

    /**
     * @return The `CompressionType` of the savefile.
     */
    fun getCompression(): CompressionType {
        return if (compression == null) CompressionType.UNCOMPRESSED else compression!!
    }

    /**
     * @param newCompressionType The new `CompressionType` for the
     * `Header`.
     *
     * @throws IllegalArgumentException Thrown if the
     * `CompressionType` is `null` for a savefile that supports
     * compression, or if this method is called for any savefile that does not support compression.
     */
    fun setCompression(newCompressionType: CompressionType?) {
        when (GAME) {
            resaver.Game.SKYRIM_SE, resaver.Game.SKYRIM_SW, resaver.Game.SKYRIM_VR -> if (newCompressionType != null) {
                compression = newCompressionType
                return
            } else {
                throw IllegalArgumentException("The compression type must not be null.")
            }
            resaver.Game.SKYRIM_LE, resaver.Game.FALLOUT4, resaver.Game.FALLOUT_VR -> throw IllegalArgumentException("Compression not supported.")
            else -> throw IllegalArgumentException("Compression not supported.")
        }
    }

    val MAGIC: ByteArray = when (val PREFIX = mf.BufferUtil.readSizedString(input.slice(), 4, false)) {
        "TES4", "FO4_" -> {
            ByteArray(12)
        }
        "TESV" -> {
            ByteArray(13)
        }
        else -> throw IllegalArgumentException("Unrecognized header: $PREFIX")
    }
    val VERSION: Int
    val SAVENUMBER: Int
    val NAME: WStringElement
    val LEVEL: Int
    val LOCATION: WStringElement
    val GAMEDATE: WStringElement
    val RACEID: WStringElement
    val SEX: Short
    val CURRENT_XP: Float
    val NEEDED_XP: Float
    val FILETIME: Long
    val SCREENSHOT_WIDTH: Int
    val SCREENSHOT_HEIGHT: Int
    var BYPP = 0
    private var compression: CompressionType?
    var GAME: resaver.Game? = null
    var SCREENSHOT: ByteArray
    private var IMAGE: BufferedImage? = null

    companion object {
        /**
         * Verifies that two instances of `Header` are identical.
         *
         * @param h1 The first `Header`.
         * @param h2 The second `Header`.
         * @throws IllegalStateException Thrown if the two instances of
         * `Header` are not equal.
         */

        @Throws(IllegalStateException::class)
        fun verifyIdentical(h1: Header, h2: Header) {
            check(h1.MAGIC.contentEquals(h2.MAGIC)) {
                "Magic mismatch: ${h1.MAGIC.contentToString()} vs ${h2.MAGIC.contentToString()}."
            }
            check(h1.NAME.equals(h2.NAME)) { "Name mismatch: ${h1.NAME} vs ${h2.NAME}." }
        }
    }

    /**
     * Creates a new `Header` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param path The path to the file.
     * @throws IOException
     */
    init {
        input[MAGIC]

        // Read the header size.
        val HEADERSIZE = input.getInt()
        require(HEADERSIZE < 256) { "Invalid header size $HEADERSIZE" }

        // Read the version number.
        VERSION = input.getInt()

        // Identify which game produced the savefile.
        // Bit of a business, really.
        when (val MAGICSTRING = String(MAGIC).uppercase(Locale.getDefault())) {
            "TESV_SAVEGAME" -> GAME = if (VERSION <= 9 && path?.let { resaver.Game.SKYRIM_LE.testFilename(it) } == true) {
                resaver.Game.SKYRIM_LE
            } else if (VERSION >= 12 && path?.let { resaver.Game.SKYRIM_SE.testFilename(it) } == true) {
                resaver.Game.SKYRIM_SE
            } else if (VERSION >= 12 && path?.let { resaver.Game.SKYRIM_SW.testFilename(it) } == true) {
                resaver.Game.SKYRIM_SW
            } else {
                throw IllegalArgumentException("Unknown version of Skyrim: $VERSION")
            }
            "FO4_SAVEGAME" -> if (11 <= VERSION && path?.let { resaver.Game.FALLOUT4.testFilename(it) } == true) {
                GAME = resaver.Game.FALLOUT4
            } else {
                throw IllegalArgumentException("Unknown version of Fallout4: $VERSION")
            }
            else -> throw IllegalArgumentException("Unknown game: $MAGICSTRING")
        }
        SAVENUMBER = input.getInt()
        NAME = WStringElement.read(input)
        LEVEL = input.getInt()
        LOCATION = WStringElement.read(input)
        GAMEDATE = WStringElement.read(input)
        RACEID = WStringElement.read(input)
        SEX = input.getShort()
        CURRENT_XP = input.getFloat()
        NEEDED_XP = input.getFloat()
        FILETIME = input.getLong()
        SCREENSHOT_WIDTH = input.getInt()
        SCREENSHOT_HEIGHT = input.getInt()
        compression = if (GAME == resaver.Game.SKYRIM_SE) read(input) else null
        require(HEADERSIZE == partialSize()) {
            "Header size should be $HEADERSIZE, found ${partialSize()}."
        }
        BYPP = when (GAME) {
            resaver.Game.SKYRIM_LE -> 3
            resaver.Game.FALLOUT4, resaver.Game.SKYRIM_SE -> 4
            else -> throw IllegalArgumentException("Invalid game: $GAME")
        }
        //todo figure out how i broke this
        SCREENSHOT = ByteArray(BYPP * SCREENSHOT_WIDTH * SCREENSHOT_HEIGHT)
        input[SCREENSHOT]
        if (SCREENSHOT.size < 10) {
            IMAGE = null
        } else {
            IMAGE = BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB)
            var x = 0
            var y = 0
            var i = 0
            while (i < SCREENSHOT.size) {
                var rgb = 0

                val r = SCREENSHOT[i + 2]
                val g = SCREENSHOT[i + 1]
                val b = SCREENSHOT[i + 0]
                rgb = rgb or (( r and 0xFF.toByte()).toInt())
                rgb = rgb or ((g  and 0xFF.toByte()).toInt() shl 8)
                rgb = rgb or ((b  and 0xFF.toByte()).toInt() shl 16)
                if (BYPP == 4) {
                    val a = SCREENSHOT[i + 3]
                    rgb = rgb or ((a and 0xFF.toByte()).toInt() shl 24)
                }
                IMAGE!!.setRGB(x, y, rgb)
                x++
                if (x >= SCREENSHOT_WIDTH) {
                    x = 0
                    y++
                }
                i += BYPP
            }
        }
    }
}