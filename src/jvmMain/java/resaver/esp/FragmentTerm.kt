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
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        SCRIPT.write(output)
        output?.putShort(FRAGMENTS.size.toShort())
        FRAGMENTS.forEach(Consumer { fragment: Fragment -> fragment.write(output) })
    }

    override fun calculateSize(): Int {
        var sum = 3
        sum += SCRIPT.calculateSize()
        var result = 0
        for (FRAGMENT in FRAGMENTS) {
            val calculateSize = FRAGMENT.calculateSize()
            result += calculateSize
        }
        sum += result
        return sum
    }

    override fun toString(): String {
        return String.format("Term: %s (%d, %d fragments)", SCRIPT.NAME, UNKNOWN, FRAGMENTS.size)
    }

    private val UNKNOWN: Byte = input.get()
    private val SCRIPT: Script = Script(input, ctx)
    private val FRAGMENTS: MutableList<Fragment>

    /**
     *
     */
    inner class Fragment(input: ByteBuffer) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.put(INDEX.toByte())
            output?.put(this.UNKNOWN)
            output?.put(SCRIPTNAME.uTF8)
            output?.put(FRAGMENTNAME.uTF8)
        }

        override fun calculateSize(): Int {
            return 9 + SCRIPTNAME.length + FRAGMENTNAME.length
        }

        override fun toString(): String {
            return String.format("%d: %s [%s] (%d)", INDEX, SCRIPTNAME, FRAGMENTNAME, this.UNKNOWN)
        }

        private val INDEX: Int = input.int
        private val UNKNOWN: Byte = input.get()
        private val SCRIPTNAME: IString = IString[BufferUtil.getUTF(input)!!]
        private val FRAGMENTNAME: IString = IString[BufferUtil.getUTF(input)!!]

    }

    init {
        //input = <code>ByteBuffer</code>.debug(input);
        ctx.PLUGIN_INFO.addScriptData(SCRIPT)
        val fragCount = input.short.toInt()
        FRAGMENTS = ArrayList(fragCount)
        for (i in 0 until fragCount) {
            val fragment = Fragment(input)
            FRAGMENTS.add(fragment)
        }
    }
}