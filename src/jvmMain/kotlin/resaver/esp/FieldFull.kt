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

import resaver.IString
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * FieldSimple represents a FULL (fullname) field.
 *
 * @author Mark Fairchild
 */
class FieldFull(code: IString?, input: ByteBuffer?, size: Int, big: Boolean, ctx: ESPContext) : FieldSimple(
    code!!, input!!, size, big, ctx
) {
    /**
     * @return A flag indicating whether the field stores a string.
     */
    fun hasString(): Boolean {
        return null != STR
    }

    /**
     * @return A flag indicating whether the field stores a stringtable index.
     */
    fun hasIndex(): Boolean {
        return IDX != -1
    }

    /**
     * @return The string value of the FULL.
     */
    val string: String?
        get() {
            assert(hasString())
            return STR
        }

    /**
     * @return The stringtable index of the FULL.
     */
    val index: Int
        get() {
            assert(hasIndex())
            return IDX
        }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        when {
            hasIndex() -> {
                BUF.append(this.code).append("=").append(String.format("%08x", IDX))
            }
            hasString() -> {
                BUF.append(this.code).append("=").append(STR)
            }
            else -> {
                BUF.append(this.code).append("=")
            }
        }
        return BUF.toString()
    }

    private var STR: String? = null
    private var IDX = 0

    /**
     * Creates a new FieldFULL by reading it from a `ByteBuffer`.
     *
     * @param code The field code.
     * @param input The `ByteBuffer` to read.
     * @param size The amount of data.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx The mod descriptor.
     */
    init {
        when {
            ctx.TES4!!.header.isLocalized -> {
                assert(super.data.size == 4)
                val `val` = super.byteBuffer.int
                IDX = `val` //(ctx.TES4.PLUGIN.INDEX << 24) | val;
                STR = null
            }
            super.data.isEmpty() -> {
                STR = null
                IDX = -1
            }
            else -> {
                STR = String(super.data, StandardCharsets.UTF_8)
                IDX = -1
            }
        }
    }
}