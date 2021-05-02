/*
 * Copyright 2017 Mark Fairchild.
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
import java.util.function.Consumer

/**
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
class FragmentTerm(input: ByteBuffer, ctx: ESPContext) : FragmentBase() {
    override fun write(output: ByteBuffer) {
        output.put(UNKNOWN)
        SCRIPT.write(output)
        output.putShort(FRAGMENTS.size.toShort())
        FRAGMENTS.forEach(Consumer { fragment: Fragment -> fragment.write(output) })
    }

    override fun calculateSize(): Int {
        var sum = 3
        sum += SCRIPT.calculateSize()
        sum += FRAGMENTS.stream().mapToInt { v: Fragment -> v.calculateSize() }.sum()
        return sum
    }

    override fun toString(): String {
        return String.format("Term: %s (%d, %d fragments)", SCRIPT.NAME, UNKNOWN, FRAGMENTS.size)
    }

    private val UNKNOWN: Byte
    private val SCRIPT: Script
    private val FRAGMENTS: MutableList<Fragment>

    /**
     *
     */
    inner class Fragment(input: ByteBuffer) : Entry {
        override fun write(output: ByteBuffer) {
            output.put(INDEX.toByte())
            output.put(this.UNKNOWN)
            output.put(SCRIPTNAME.utF8)
            output.put(FRAGMENTNAME.utF8)
        }

        override fun calculateSize(): Int {
            return 9 + SCRIPTNAME.length + FRAGMENTNAME.length
        }

        override fun toString(): String {
            return String.format("%d: %s [%s] (%d)", INDEX, SCRIPTNAME, FRAGMENTNAME, this.UNKNOWN)
        }

        private val INDEX: Int
        private val UNKNOWN: Byte
        private val SCRIPTNAME: IString
        private val FRAGMENTNAME: IString

        init {
            INDEX = input.int
            this.UNKNOWN = input.get()
            SCRIPTNAME = IString.get(BufferUtil.getUTF(input))
            FRAGMENTNAME = IString.get(BufferUtil.getUTF(input))
        }
    }

    init {
        //input = <code>ByteBuffer</code>.debug(input);
        UNKNOWN = input.get()
        SCRIPT = Script(input, ctx)
        ctx.PLUGIN_INFO.addScriptData(SCRIPT)
        val fragCount = input.short.toInt()
        FRAGMENTS = ArrayList(fragCount)
        for (i in 0 until fragCount) {
            val fragment: Fragment = Fragment(input)
            FRAGMENTS.add(fragment)
        }
    }
}