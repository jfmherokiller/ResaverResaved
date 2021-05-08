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
import java.util.*

/**
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
class FragmentScen(input: ByteBuffer, ctx: ESPContext) : FragmentBase() {
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        output?.put(FLAGS)
        if (null != SCRIPT) {
            SCRIPT?.write(output)
        }
        if (null != FILENAME) {
            output?.put(FILENAME?.toByteArray(StandardCharsets.UTF_8))
        }
        FRAGMENTS.forEach { fragment: Fragment -> fragment.write(output) }
        output?.putShort(PHASES.size.toShort())
        PHASES.forEach { phase: Phase -> phase.write(output) }
    }

    override fun calculateSize(): Int {
        var sum = 4
        sum += SCRIPT?.calculateSize() ?: 0
        sum += if (null != FILENAME) 2 + FILENAME?.length!! else 0
        var result = 0
        for (FRAGMENT in FRAGMENTS) {
            val calculateSize = FRAGMENT.calculateSize()
            result += calculateSize
        }
        sum += result
        var sum1 = 0
        for (PHASE in PHASES) {
            val calculateSize = PHASE.calculateSize()
            sum1 += calculateSize
        }
        sum += sum1
        return sum
    }

    override fun toString(): String {
        return when {
            null != SCRIPT -> {
                String.format(
                    "Scene: %s (%d, %d, %d frags, %d phases)",
                    SCRIPT?.NAME,
                    FLAGS,
                    UNKNOWN,
                    FRAGMENTS.size,
                    PHASES.size
                )
            }
            null != FILENAME -> {
                String.format(
                    "Scene: %s (%d, %d, %d frags, %d phases)",
                    FILENAME,
                    FLAGS,
                    UNKNOWN,
                    FRAGMENTS.size,
                    PHASES.size
                )
            }
            else -> {
                String.format(
                    "Scene: (%d, %d, %d frags, %d phases)",
                    FLAGS,
                    UNKNOWN,
                    FRAGMENTS.size,
                    PHASES.size
                )
            }
        }
    }

    private val UNKNOWN: Byte
    private val FLAGS: Byte
    private var SCRIPT: Script? = null
    private var FILENAME: String? = null
    private val FRAGMENTS: MutableList<Fragment>
    private val PHASES: MutableList<Phase>

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

        override fun toString(): String {
            return String.format("Frag %d %s[%s]", this.UNKNOWN, SCRIPTNAME, FRAGMENTNAME)
        }

        private val UNKNOWN: Byte
        private val SCRIPTNAME: IString
        private val FRAGMENTNAME: IString

        init {
            this.UNKNOWN = input.get()
            SCRIPTNAME = IString[BufferUtil.getUTF(input)!!]
            FRAGMENTNAME = IString[BufferUtil.getUTF(input)!!]
        }
    }

    /**
     *
     */
    inner class Phase(input: ByteBuffer) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.put(UNKNOWN1)
            output?.putInt(PHASE)
            output?.put(UNKNOWN2)
            output?.put(SCRIPTNAME.uTF8)
            output?.put(FRAGMENTNAME.uTF8)
        }

        override fun calculateSize(): Int {
            return 10 + SCRIPTNAME.length + FRAGMENTNAME.length
        }

        override fun toString(): String {
            return String.format("Phase %d.%d.%d %s[%s]", PHASE, UNKNOWN1, UNKNOWN2, SCRIPTNAME, FRAGMENTNAME)
        }

        private val UNKNOWN1: Byte
        private val PHASE: Int
        private val UNKNOWN2: Byte
        private val SCRIPTNAME: IString
        private val FRAGMENTNAME: IString

        init {
            UNKNOWN1 = input.get()
            PHASE = input.int
            UNKNOWN2 = input.get()
            SCRIPTNAME = IString[BufferUtil.getUTF(input)!!]
            FRAGMENTNAME = IString[BufferUtil.getUTF(input)!!]
        }
    }

    init {
        UNKNOWN = input.get()
        FLAGS = input.get()
        if (ctx.GAME.isFO4) {
            ctx.pushContext("FragmentScene")
            FILENAME = null
            SCRIPT = Script(input, ctx)
            ctx.PLUGIN_INFO.addScriptData(SCRIPT!!)
        } else {
            FILENAME = BufferUtil.getUTF(input)
            SCRIPT = null
            ctx.pushContext("FragmentScene:$FILENAME")
        }
        FRAGMENTS = LinkedList()
        PHASES = LinkedList()
        val flagCount = NumberOfSetBits(FLAGS.toInt())
        for (i in 0 until flagCount) {
            val fragment = Fragment(input)
            FRAGMENTS.add(fragment)
        }
        val phaseCount = UtilityFunctions.toUnsignedInt(input.short)
        for (i in 0 until phaseCount) {
            val phase = Phase(input)
            PHASES.add(phase)
        }
    }
}