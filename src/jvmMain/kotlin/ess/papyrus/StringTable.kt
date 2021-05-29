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
package ess.papyrus

import PlatformByteBuffer
import ess.ESS.ESSContext
import ess.WStringElement
import java.nio.BufferUnderflowException

/**
 * An abstraction describing a string table.
 *
 * @author Mark Fairchild
 */
class StringTable : ArrayList<TString?>, PapyrusElement {
    /**
     * Creates a new `TString` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @return The new `TString`.
     * @throws PapyrusFormatException
     */
    @Throws(PapyrusFormatException::class)
    fun read(input: PlatformByteBuffer): TString? {
        var index: Int
        if (STR32) {
            // SkyrimSE, Fallout4, and SkyrimLE with CrashFixes uses 32bit string indices.            
            index = input.getInt()
        } else {
            index = UtilityFunctions.toUnsignedInt(input.getShort())
            // SkyrimLegendary and Fallout4 use 16bit string indices.
            // Various corrections are possible though.
            if (index == 0xFFFF && !STBCORRECTION) {
                index = input.getInt()
            }
        }
        if (index < 0 || index >= this.size) {
            throw PapyrusFormatException(String.format("Invalid TString index: %d / %d", index, this.size))
        }
        return get(index)
    }

    /**
     * Creates a new `StringTable` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `ESSContext` info.
     * @throws PapyrusElementException
     */
    constructor(input: PlatformByteBuffer, context: ESSContext) {
        STR32 = context.isStr32
        var strCount: Int
        if (STR32) {
            // SkyrimSE uses 32bit string indices.            
            strCount = input.getInt()
            STBCORRECTION = false
        } else {
            // Skyrim Legendary (without CrashFixes) and old versions of 
            // Fallout4 use 16bit string indices.
            // Various corrections are possible though.           
            strCount = UtilityFunctions.toUnsignedInt(input.getShort())

            // Large string table version.
            if (strCount == 0xFFFF) {
                strCount = input.getInt()
            }

            // Fallback for catching the stringtable bug.
            if (context.game!!.isFO4 && strCount < 7000 || context.game!!.isSkyrim && strCount < 20000) {
                strCount = strCount or 0x10000
                STBCORRECTION = true
            } else {
                STBCORRECTION = false
            }
        }

        // Store the string count.
        STRCOUNT = strCount

        // Read the actual strings.
        try {
            ensureCapacity(strCount)
            for (i in 0 until strCount) {
                try {
                    val WSTR = WStringElement.read(input)
                    val TSTR = if (STR32) TString32(WSTR, i) else TString16(this, WSTR, i)
                    this.add(TSTR)
                } catch (ex: BufferUnderflowException) {
                    throw PapyrusException("Error reading string #$i", ex, null)
                }
            }
        } catch (ex: BufferUnderflowException) {
            isTruncated = true
            val msg = String.format("Error; read %d/%d strings.", this.size, strCount)
            throw PapyrusElementException(msg, ex, this)
        }
        isTruncated = false
    }

    /**
     * Creates an empty `StringTable` with the truncated flag.
     */
    constructor() {
        STBCORRECTION = false
        STR32 = false
        isTruncated = true
        STRCOUNT = 0
    }

    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        check(!STBCORRECTION) { "String-Table-Bug correction in effect. Cannot write." }
        check(!isTruncated) { "StringTable is truncated. Cannot write." }
        if (STR32) {
            // SkyrimSE uses 32bit string indices.
            output?.putInt(this.size)
        } else  // SkyrimLegendary and Fallout4 use 16bit string indices.
        // Various corrections are possible though.           
        // Large string table version.
        {
            if (this.size > 0xFFF0 && !STBCORRECTION) {
                output?.putShort(0xFFFF.toShort())
                output?.putInt(this.size)
            } else {
                output?.putShort(this.size.toShort())
            }
        }

        // Write the actual strings.
        this.forEach { tstr: TString? -> tstr?.writeFull(output) }
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 0
        sum += if (STR32) {
            4
        } else if (this.size > 0xFFF0 && !STBCORRECTION) {
            6
        } else {
            2
        }
        var result = 0
        for (tString in this) {
            val calculateFullSize = tString?.calculateFullSize()
            if (calculateFullSize != null) {
                result += calculateFullSize
            }
        }
        sum += result
        return sum
    }

    /**
     *
     * @param str
     * @return
     */
    fun resolve(str: String?): TString? {
        for (tstr in this) {
            if (tstr?.equals(str) == true) {
                return tstr
            }
        }
        return null
    }

    /**
     * Checks if the `StringTable` contains a `TString`
     * that matches a specified string value.
     *
     * @param val The value to match against.
     * @return True if the `StringTable` contains a matching
     * `TString`, false otherwise.
     */
    fun containsMatching(`val`: String?): Boolean {
        for (v in this) {
            if (v != null) {
                if (v.equals(`val`)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Adds a new string to the `StringTable` and returns the
     * corresponding `TString`.
     *
     * @param val The value of the new string.
     * @return The new `TString`, or the existing one if the
     * `StringTable` already contained a match.
     */
    fun addString(`val`: String?): TString {
        var match: TString? = null
        for (v in this) {
            if (v?.equals(`val`) == true) {
                match = v
                break
            }
        }
        if (match != null) {
            return match
        }
        val tstr = if (STR32) TString32(`val`, this.size) else TString16(this, `val`, this.size)
        this.add(tstr)
        return tstr
    }

    /**
     * A flag indicating that the string-table-bug correction is in effect. This
     * means that the table is NOT SAVABLE.
     *
     * @return
     */
    fun hasSTB(): Boolean {
        return STBCORRECTION
    }

    /**
     * @return For a truncated `StringTable` returns the number of
     * missing strings. Otherwise returns 0.
     */
    val missingCount: Int
        get() = STRCOUNT - this.size

    /**
     * A flag indicating that the string-table-bug correction is in effect.
     */
    val STBCORRECTION: Boolean
    /**
     * @return A flag indicating that the string table is truncated.
     */
    /**
     * Stores the truncated condition.
     */
    val isTruncated: Boolean

    /**
     * Stores the parsing context information.
     */
    private val STR32: Boolean

    /**
     * Stores the declared string table size. If the `StringTable` is
     * truncated, this will not actually match the size of the list.
     */
    private val STRCOUNT: Int
}