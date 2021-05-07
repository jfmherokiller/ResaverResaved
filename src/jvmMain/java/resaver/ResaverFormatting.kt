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
package resaver


import java.util.function.Function

/**
 *
 * @author Mark
 */
object ResaverFormatting {
    @JvmStatic
    fun <T> makeHTMLList(msg: String?, items: List<T>, limit: Int, namer: Function<T, CharSequence?>): CharSequence {
        val BUF = StringBuilder()
        BUF.append("<p>")
            .append(String.format(msg!!, items.size))
            .append("<ol>")
        var limit1 = limit.toLong()
        for (item in items) {
            if (limit1-- == 0L) break
            val charSequence = namer.apply(item)
            BUF.append("<li>").append(charSequence).append("</li>")
        }
        BUF.append("</ol>")
        val excess = items.size - limit
        if (excess > 0) {
            BUF.append(String.format("(+ %d more)", excess))
        }
        BUF.append("</p>")
        return BUF
    }

    fun <T> makeTextList(msg: String?, items: List<T>, limit: Int, namer: Function<T, CharSequence>): CharSequence {
        val BUF = StringBuilder()
        BUF.append(String.format(msg!!, items.size))
        var limit1 = limit.toLong()
        for (item in items) {
            if (limit1-- == 0L) break
            val charSequence = namer.apply(item)
            BUF.append(NLDOT).append(charSequence)
        }
        val excess = items.size - limit
        if (excess > 0) {
            BUF.append(String.format("\n(+ %d more", excess))
        }
        return BUF
    }

    /**
     * Zero-pads the hexadecimal representation of an integer so that it is a
     * full 4 bytes long.
     *
     * @param val The value to convert to hexadecimal and pad.
     * @return The zero-padded string.
     */
    fun zeroPad8(`val`: Int): String {
        val hex = Integer.toHexString(`val`)
        val length = hex.length
        return ZEROES[8 - length].toString() + hex
    }

    /**
     * Zero-pads the hexadecimal representation of an integer so that it is a
     * full 3 bytes long.
     *
     * @param val The value to convert to hexadecimal and pad.
     * @return The zero-padded string.
     */
    fun zeroPad6(`val`: Int): String {
        val hex = java.lang.Long.toHexString(`val`.toLong())
        val length = hex.length
        return ZEROES[6 - length].toString() + hex
    }

    /**
     *
     * @return
     */
    private fun makeZeroes(): Array<String?> {
        val zeroes = arrayOfNulls<String>(16)
        zeroes[0] = ""
        for (i in 1 until zeroes.size) {
            zeroes[i] = zeroes[i - 1].toString() + "0"
        }
        return zeroes
    }

    /**
     * An array of strings of zeroes with the length matching the index.
     */
    private val ZEROES = makeZeroes()
    private const val NLDOT = "\n\u00b7 "
}