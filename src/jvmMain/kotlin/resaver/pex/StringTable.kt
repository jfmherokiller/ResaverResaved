/*
 * Copyright 2016 Mark.
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
package resaver.pex

import PlatformByteBuffer
import UtilityFunctions
import resaver.IString
import resaver.ListException
import java.io.IOException
import java.util.*

/**
 * An abstraction describing a string table.
 *
 * @author Mark Fairchild
 */
class StringTable(input: PlatformByteBuffer) : ArrayList<TString?>() {
    /**
     * Creates a new `TString` by reading from a
     * `DataInput`. No error handling is performed.
     *
     * @param input The input stream.
     * @return The new `TString`.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun read(input: PlatformByteBuffer): TString {
        Objects.requireNonNull(input)
        val index = UtilityFunctions.toUnsignedInt(input.getShort())
        if (index < 0 || index >= this.size) {
            throw IOException(String.format("Invalid TString index: %d (size %d)", index, this.size))
        }
        return this[index]!!
    }

    /**
     * @return Returns a reusable instance of a blank `TString`.
     */
    fun blank(): TString {
        return addString(IString.BLANK)
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(output: PlatformByteBuffer) {
        output.putShort(this.size.toShort())
        for (tstr in this) {
            try {
                tstr?.writeFull(output)
            } catch (ex: IOException) {
                throw IOException("Error writing string #${tstr?.INDEX ?: 0}", ex)
            }
        }
    }

    /**
     * Rebuilds the string table. This is necessary if ANY strings in ANY of the
     * Pex's members has changed at all. Otherwise, writing the Pex will produce
     * an invalid file.
     *
     * @param inUse The `Set` of strings that are still in use.
     */
    fun rebuildStringTable(inUse: Set<TString?>?) {
        this.retainAll(this)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    fun calculateSize(): Int {
        var sum = 0
        sum += if (this.size > 0xFFF0) {
            6
        } else {
            2
        }
        var result = 0
        for (tString in this) {
            val calculateSize = tString?.calculateSize()?: 0
            result += calculateSize
        }
        sum += result
        return sum
    }

    /**
     * Adds a new string to the `StringTable` and returns the
     * corresponding `TString`.
     *
     * @param val The value of the new string.
     * @return The new `TString`, or the existing one if the
     * `StringTable` already contained a match.
     */
    fun addString(`val`: IString?): TString {
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
        val tstr = TString(`val`, this.size)
        this.add(tstr)
        return tstr
    }

    /**
     * Creates a new `DataInput` by reading from a
     * `LittleEndianByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     */
    init {
        val strCount = UtilityFunctions.toUnsignedInt(input.getShort())
        ensureCapacity(strCount)
        for (i in 0 until strCount) {
            try {
                val STR = mf.BufferUtil.getUTF(input)
                val TSTR = TString(STR, i)
                this.add(TSTR)
            } catch (ex: RuntimeException) {
                throw ListException("Error reading string", i, strCount, ex)
            }
        }
    }
}