/*
 * Copyright 2017 Mark.
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

import PlatformByteBuffer
import mf.BufferUtil
import resaver.Entry
import resaver.IString
import resaver.esp.PropertyData.Companion.readPropertyData

/**
 * Describes a property entry in a VMAD's scripts.
 *
 * @author Mark Fairchild
 */
class Property(input: PlatformByteBuffer, ctx: ESPContext) : Entry {
    /**
     * @see Entry.write
     * @param output The ByteBuffer.
     */
    override fun write(output: PlatformByteBuffer?) {
        output?.put(NAME.uTF8)
        output?.put(TYPE)
        output?.put(STATUS)
        DATA?.write(output)
    }

    /**
     * @return The calculated size of the Script.
     */
    override fun calculateSize(): Int {
        return 4 + NAME.length + (DATA?.calculateSize() ?: 0)
    }

    override fun toString(): String {
        return String.format("%s: %d (%02x): %s", NAME, TYPE, STATUS, DATA)
    }

    private val NAME: IString
    private val TYPE: Byte
    private val STATUS: Byte
    private var DATA: PropertyData? = null

    /**
     * Creates a new Property by reading it from a LittleEndianInput.
     *
     * @param input The `ByteBuffer` to read.
     * @param ctx
     */
    init {
        NAME = IString[BufferUtil.getWString(input)!!]
        ctx.pushContext("prop:$NAME")
        TYPE = input.getByte()
        STATUS = input.getByte()
        try {
            DATA = readPropertyData(TYPE, input, ctx)
        } finally {
            ctx.popContext()
        }
    }
}