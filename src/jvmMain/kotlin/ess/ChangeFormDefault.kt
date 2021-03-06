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

/**
 * Describes a default ChangeForm.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `ChangeFormDefault` by storing a data buffer.
 *
 * @param input The data buffer.
 * @param size Buffer size
 */
class ChangeFormDefault(input: PlatformByteBuffer, size: Int) : ChangeFormData {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(DATA)
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return DATA.size
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: resaver.Analysis?, save: ESS?): String {
        val BUILDER = ParseIntoBytes().toString() + ParseIntoData().toString()
        return BUILDER
    }
    private fun ParseIntoData(): StringBuilder {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>RAW DATA TEXT:</p><code><pre>")
        val mstring = String(DATA)
        val printable = mstring.replace(Regex("\\P{Print}"), ".")
        for (i in printable.indices) {
            if (i > 0 && i % 32 == 0) {
                BUILDER.append('\n')
            }
            val B = printable[i]
            BUILDER.append(String.format("%s", B))
        }
        BUILDER.append("</pre></code>")
        return BUILDER
    }
    private fun ParseIntoBytes(): StringBuilder {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>RAW DATA:</p><code><pre>")
        for (i in DATA.indices) {
            if (i > 0 && i % 16 == 0) {
                BUILDER.append('\n')
            }
            val B = DATA[i]
            BUILDER.append(String.format("%02x ", B))
        }
        BUILDER.append("</pre></code>")
        return BUILDER
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return "" //(" + this.BUFFER.length + " bytes)";
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: resaver.Analysis?, mod: String?): Boolean {
        return false
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        return DATA.contentHashCode()
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as ChangeFormDefault
        return DATA.contentEquals(other2.DATA)
    }

    private val DATA: ByteArray = ByteArray(size)


    init {
        input[DATA]
    }
}