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
import resaver.IString
import java.nio.ByteBuffer

/**
 * FieldSimple represents an EDID field.
 *
 * @author Mark Fairchild
 */
class FieldEDID(code: IString?, input: ByteBuffer?, size: Int, big: Boolean, ctx: ESPContext?) : FieldSimple(
    code!!, input!!, size, big, ctx
) {
    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        return this.code.toString() + "=" + value
    }

    /**
     * @return The string value of the EDID.
     */
    val value: String

    /**
     * Creates a new FieldSimple by reading it from a `ByteBuffer`.
     *
     * @param code The field code.
     * @param input The `ByteBuffer` to read.
     * @param size The amount of data.
     * @param big A flag indicating that this is a BIG field.
     * @param ctx The mod descriptor.
     */
    init {
        value = BufferUtil.getZString(super.byteBuffer)
    }
}