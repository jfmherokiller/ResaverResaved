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

import mf.BufferUtil
import kotlin.Throws
import java.io.IOException
import java.util.Objects
import resaver.IString
import java.util.function.ToIntFunction
import java.util.Optional
import resaver.WString
import java.lang.RuntimeException
import resaver.ListException
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.function.Predicate

/**
 * An abstraction describing a string table.
 *
 * @author Mark Fairchild
 */
class StringTable(input: ByteBuffer) : ArrayList<StringTable.TString>() {
    /**
     * Creates a new `TString` by reading from a
     * `DataInput`. No error handling is performed.
     *
     * @param input The input stream.
     * @return The new `TString`.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun read(input: ByteBuffer): TString {
        Objects.requireNonNull(input)
        val index = java.lang.Short.toUnsignedInt(input.short)
        if (index < 0 || index >= this.size) {
            throw IOException(String.format("Invalid TString index: %d (size %d)", index, this.size))
        }
        return this[index]
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
    fun write(output: ByteBuffer) {
        output.putShort(this.size.toShort())
        for (tstr in this) {
            try {
                tstr.writeFull(output)
            } catch (ex: IOException) {
                throw IOException("Error writing string #" + tstr.INDEX, ex)
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
        sum += stream().mapToInt(ToIntFunction { obj: TString -> obj.calculateSize() }).sum()
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
    fun addString(`val`: IString): TString {
        val match: Optional<TString> = stream().filter(Predicate { v: TString -> v.equals(`val`) }).findFirst()
        if (match.isPresent) {
            return match.get()
        }
        val tstr = TString(`val`, this.size)
        this.add(tstr)
        return tstr
    }

    /**
     * A case-insensitive string with value semantics that reads and writes as a
     * two-byte index into a string table.
     *
     * @author Mark Fairchild
     */
    class TString
    /**
     * Creates a new `TString` from a character sequence and an
     * index.
     *
     * @param cs The `CharSequence`.
     * @param index The index of the `TString`.
     */(cs: CharSequence, val INDEX: Int) : WString(cs) {
        /**
         * @see WString.write
         * @param output The output stream.
         * @throws IOException
         */
        @Throws(IOException::class)
        fun writeFull(output: ByteBuffer?) {
            BufferUtil.putWString(output, super.toString())
        }

        /**
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer) {
            output.putShort(INDEX.toShort())
        }
    }

    /**
     * Creates a new `DataInput` by reading from a
     * `LittleEndianByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     */
    init {
        val strCount = java.lang.Short.toUnsignedInt(input.short)
        ensureCapacity(strCount)
        for (i in 0 until strCount) {
            try {
                val STR = BufferUtil.getUTF(input)
                val TSTR = TString(STR, i)
                this.add(TSTR)
            } catch (ex: RuntimeException) {
                throw ListException("Error reading string", i, strCount, ex)
            }
        }
    }
}