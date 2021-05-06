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
 * Describes a script entry in a VMAD field.
 *
 * @author Mark Fairchild
 */
class Script(input: ByteBuffer, ctx: ESPContext) : Entry {
    /**
     * Writes the Script.
     *
     * @param output The ByteBuffer to write.
     */
    override fun write(output: ByteBuffer?) {
        if (NAME.isEmpty()) {
            output!!.put(NAME.uTF8)
            return
        }
        output!!.put(NAME.uTF8)
        output.put(STATUS)
        output.putShort(PROPERTIES!!.size.toShort())
        PROPERTIES!!.forEach { prop: Property -> prop.write(output) }
    }

    /**
     * @return The calculated size of the Script.
     */
    override fun calculateSize(): Int {
        if (NAME.isEmpty()) {
            return 2
        }
        var sum = 5 + NAME.length
        var result = 0
        for (PROPERTY in PROPERTIES!!) {
            val calculateSize = PROPERTY.calculateSize()
            result += calculateSize
        }
        sum += result
        return sum
    }

    @JvmField
    val NAME: IString = IString[BufferUtil.getWString(input)]
    private var STATUS: Byte
    var PROPERTIES: MutableList<Property>?

    /**
     * Creates a new Script by reading it from a LittleEndianInput.
     *
     * @param input The `ByteBuffer` to read.
     * @param ctx
     */
    init {
        if (NAME.isEmpty()) {
            PROPERTIES = null
            STATUS = 0
        }
        else {
            ctx.pushContext("script:$NAME")
            STATUS = input.get()
            val propertyCount = java.lang.Short.toUnsignedInt(input.short)
            PROPERTIES = mutableListOf()
            try {
                for (i in 0 until propertyCount) {
                    val prop = Property(input, ctx)
                    PROPERTIES!!.add(prop)
                }
            } finally {
                ctx.popContext()
            }
        }
    }
}