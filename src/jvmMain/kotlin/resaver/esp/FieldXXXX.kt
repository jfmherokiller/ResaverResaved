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

/**
 * FieldBasic represents all fields that aren't a VMAD section.
 *
 * @author Mark Fairchild
 */
class FieldXXXX(code: IString, input: ByteBuffer) : Field {
    /**
     * @see Entry.write
     */
    override fun write(output: ByteBuffer?) {
        output!!.put(this.code.uTF8)
        output.putShort(4.toShort())
        output.putInt(data)
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        return 10
    }

    /**
     * Returns a String representation of the Field, which will just be the code
     * string.
     *
     * @return A string representation.
     */
    override fun toString(): String {
        return this.code.toString()
    }

    /**
     * Returns the field code.
     *
     * @return The field code.
     */
    override val code: IString

    /**
     * Returns a copy of the data section.
     *
     * @return A copy of the data array.
     */
    val data: Int

    /**
     * Creates a new FieldBasic by reading it from a LittleEndianInput.
     *
     * @param code The record code.
     * @param input The `ByteBuffer` to read.
     */
    init {
        assert(input.hasRemaining())
        assert(code.equals(IString["XXXX"]))
        this.code = code
        data = input.int
    }
}