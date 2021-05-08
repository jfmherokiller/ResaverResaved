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
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

/**
 * Describes script fragments for INFO records and PACK records.
 *
 * @author Mark
 */
class FragmentInfoPack(input: ByteBuffer, ctx: ESPContext) : FragmentBase() {
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        output?.put(FLAGS)
        if (null != SCRIPT) {
            SCRIPT?.write(output)
        }
        if (null != FILENAME) {
            output?.put(FILENAME!!.toByteArray(StandardCharsets.UTF_8))
        }
        FRAGMENTS.forEach(Consumer { fragment: Fragment -> fragment.write(output) })
    }

    override fun calculateSize(): Int {
        var sum = 2
        sum += (if (null != SCRIPT) SCRIPT?.calculateSize() else 0)!!
        sum += if (null != FILENAME) 2 + FILENAME?.length!! else 0
        var result = 0
        for (FRAGMENT in FRAGMENTS) {
            val calculateSize = FRAGMENT.calculateSize()
            result += calculateSize
        }
        sum += result
        return sum
    }

    override fun toString(): String {
        return when {
            null != SCRIPT -> {
                String.format(
                    "InfoPack: %s (%d, %d, %d frags)",
                    SCRIPT!!.NAME,
                    FLAGS,
                    UNKNOWN,
                    FRAGMENTS.size
                )
            }
            null != FILENAME -> {
                String.format(
                    "InfoPack: %s (%d, %d, %d frags)",
                    FILENAME,
                    FLAGS,
                    UNKNOWN,
                    FRAGMENTS.size
                )
            }
            else -> {
                String.format("InfoPack: (%d, %d, %d frags)", FLAGS, UNKNOWN, FRAGMENTS.size)
            }
        }
    }

    val UNKNOWN: Byte = input.get()
    val FLAGS: Byte = input.get()
    var SCRIPT: Script? = null
    var FILENAME: String? = null
    val FRAGMENTS: MutableList<Fragment>

    /**
     *
     */
    inner class Fragment(input: ByteBuffer) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.put(this.UNKNOWN)
            output?.put(SCRIPTNAME.uTF8)
            output?.put(FRAGMENTNAME.uTF8)
        }

        override fun calculateSize(): Int {
            return 5 + SCRIPTNAME.length + FRAGMENTNAME.length
        }

        private val UNKNOWN: Byte = input.get()
        private val SCRIPTNAME: IString = IString[BufferUtil.getUTF(input)!!]
        private val FRAGMENTNAME: IString = IString[BufferUtil.getUTF(input)!!]

    }

    init {
        if (ctx.GAME.isFO4) {
            ctx.pushContext("FragmentInfoPack")
            FILENAME = null
            SCRIPT = Script(input, ctx)
            ctx.PLUGIN_INFO.addScriptData(SCRIPT!!)
        } else {
            FILENAME = BufferUtil.getUTF(input)
            SCRIPT = null
            ctx.pushContext("FragmentInfoPack:$FILENAME")
        }
        FRAGMENTS = mutableListOf()
        val flagsCount = NumberOfSetBits(FLAGS.toInt())
        for (i in 0 until flagsCount) {
            val fragment = Fragment(input)
            FRAGMENTS.add(fragment)
        }
    }
}