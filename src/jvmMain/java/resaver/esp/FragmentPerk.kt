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

/**
 * Describes script fragments for PERK records.
 *
 * @author Mark Fairchild
 */
class FragmentPerk(input: ByteBuffer, ctx: ESPContext) : FragmentBase() {
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        if (null != SCRIPT) {
            SCRIPT?.write(output)
        }
        if (null != FILENAME) {
            output?.put(FILENAME?.toByteArray(StandardCharsets.UTF_8))
        }
        output?.putShort(FRAGMENTS.size.toShort())
        FRAGMENTS.forEach { fragment: Fragment -> fragment.write(output) }
    }

    override fun calculateSize(): Int {
        var sum = 3
        sum += SCRIPT?.calculateSize() ?: 0
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
                String.format("Perk: %s (%d, %d frags)", SCRIPT!!.NAME, UNKNOWN, FRAGMENTS.size)
            }
            null != FILENAME -> {
                String.format("Perk: %s (%d, %d frags)", FILENAME, UNKNOWN, FRAGMENTS.size)
            }
            else -> {
                String.format("Perk: (%d, %d frags)", UNKNOWN, FRAGMENTS.size)
            }
        }
    }

    private var UNKNOWN: Byte = 0
    private var FILENAME: String? = null
    private var SCRIPT: Script? = null
    private var FRAGMENTS: MutableList<Fragment> = mutableListOf()

    /**
     *
     */
    inner class Fragment(input: ByteBuffer) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.putShort(INDEX.toShort())
            output?.putShort(UNKNOWN1)
            output?.put(UNKNOWN2)
            output?.put(SCRIPTNAME.uTF8)
            output?.put(FRAGMENTNAME.uTF8)
        }

        override fun calculateSize(): Int {
            return 9 + SCRIPTNAME.length + FRAGMENTNAME.length
        }

        private val INDEX: Int = UtilityFunctions.toUnsignedInt(input.short)
        private val UNKNOWN1: Short = input.short
        private val UNKNOWN2: Byte = input.get()
        private val SCRIPTNAME: IString = IString[BufferUtil.getUTF(input)!!]
        private val FRAGMENTNAME: IString = IString[BufferUtil.getUTF(input)!!]

    }

    init {
        try {
            UNKNOWN = input.get()
            if (ctx.GAME.isFO4) {
                ctx.pushContext("FragmentPerk")
                FILENAME = null
                SCRIPT = Script(input, ctx)
                ctx.PLUGIN_INFO.addScriptData(SCRIPT!!)
            } else {
                FILENAME = BufferUtil.getUTF(input)
                SCRIPT = null
                ctx.pushContext("FragmentPerk:$FILENAME")
            }
            val fragmentCount = UtilityFunctions.toUnsignedInt(input.short)
            FRAGMENTS = ArrayList(fragmentCount)
            for (i in 0 until fragmentCount) {
                val fragment = Fragment(input)
                FRAGMENTS.add(fragment)
            }
        } finally {
            ctx.popContext()
        }
    }
}