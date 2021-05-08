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
import java.util.function.Consumer

/**
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
class FragmentQust(input: ByteBuffer, ctx: ESPContext) : FragmentBase() {
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        output?.putShort(FRAGMENTS.size.toShort())
        if (null != FILENAME) {
            output?.put(FILENAME?.toByteArray(StandardCharsets.UTF_8))
        }
        if (null != SCRIPT) {
            SCRIPT?.write(output)
        }
        FRAGMENTS.forEach(Consumer { fragment: Fragment -> fragment.write(output) })
        output?.putShort(ALIASES.size.toShort())
        ALIASES.forEach(Consumer { alias: Alias -> alias.write(output) })
    }

    override fun calculateSize(): Int {
        var sum = 5
        sum += if (null != FILENAME) 2 + FILENAME!!.length else 0
        sum += SCRIPT?.calculateSize() ?: 0
        var result = 0
        for (FRAGMENT in FRAGMENTS) {
            val calculateSize = FRAGMENT.calculateSize()
            result += calculateSize
        }
        sum += result
        var sum1 = 0
        for (ALIAS in ALIASES) {
            val calculateSize = ALIAS.calculateSize()
            sum1 += calculateSize
        }
        sum += sum1
        return sum
    }

    override fun toString(): String {
        return when {
            null != SCRIPT -> {
                String.format(
                    "Quest: %s (%d, %d frags, %d aliases)",
                    SCRIPT?.NAME,
                    UNKNOWN,
                    FRAGMENTS.size,
                    ALIASES.size
                )
            }
            null != FILENAME -> {
                String.format(
                    "Quest: %s (%d, %d frags, %d aliases)",
                    FILENAME,
                    UNKNOWN,
                    FRAGMENTS.size,
                    ALIASES.size
                )
            }
            else -> {
                String.format("Quest: (%d, %d frags, %d aliases)", UNKNOWN, FRAGMENTS.size, ALIASES.size)
            }
        }
    }

    private var UNKNOWN: Byte = 0
    private var FILENAME: String? = null
    private var SCRIPT: Script? = null
    private var FRAGMENTS: MutableList<Fragment> = mutableListOf()
    private var ALIASES: MutableList<Alias> = mutableListOf()

    /**
     *
     */
    inner class Fragment(input: ByteBuffer, ctx: ESPContext?) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.putShort(STAGE.toShort())
            output?.putShort(UNKNOWN1)
            output?.putInt(LOGENTRY)
            output?.put(UNKNOWN2)
            output?.put(SCRIPTNAME.uTF8)
            output?.put(FRAGMENTNAME.uTF8)
        }

        override fun calculateSize(): Int {
            var sum = 13
            sum += SCRIPTNAME.length
            sum += FRAGMENTNAME.length
            return sum
        }

        private val STAGE: Int = UtilityFunctions.toUnsignedInt(input.short)
        private val UNKNOWN1: Short = input.short
        private val LOGENTRY: Int = input.int
        private val UNKNOWN2: Byte = input.get()
        private val SCRIPTNAME: IString = IString[BufferUtil.getUTF(input)!!]
        private val FRAGMENTNAME: IString = IString[BufferUtil.getUTF(input)!!]

    }

    /**
     *
     */
    inner class Alias(input: ByteBuffer, ctx: ESPContext) : Entry {
        override fun write(output: ByteBuffer?) {
            output?.putLong(OBJECT)
            output?.putShort(VERSION)
            output?.putShort(OBJFORMAT)
            output?.putShort(SCRIPTS.size.toShort())
            SCRIPTS.forEach { script: Script -> script.write(output) }
        }

        override fun calculateSize(): Int {
            var sum = 14
            var result = 0
            for (script in SCRIPTS) {
                val calculateSize = script.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        private val OBJECT: Long = input.long
        private val VERSION: Short = input.short
        private val OBJFORMAT: Short = input.short
        private val SCRIPTS: MutableList<Script>

        init {
            SCRIPTS = LinkedList()
            val scriptCount = UtilityFunctions.toUnsignedInt(input.short)
            for (i in 0 until scriptCount) {
                val script = Script(input, ctx)
                SCRIPTS.add(script)
                ctx.PLUGIN_INFO.addScriptData(script)
            }
        }
    }

    init {
        try {
            UNKNOWN = input.get()
            val fragmentCount = UtilityFunctions.toUnsignedInt(input.short)
            if (ctx.GAME.isFO4) {
                ctx.pushContext("FragmentQust")
                FILENAME = null
                SCRIPT = Script(input, ctx)
                ctx.PLUGIN_INFO.addScriptData(SCRIPT!!)
            } else {
                FILENAME = BufferUtil.getUTF(input)
                SCRIPT = null
                ctx.pushContext("FragmentQust:$FILENAME")
            }
            FRAGMENTS = LinkedList()
            ALIASES = LinkedList()
            for (i in 0 until fragmentCount) {
                val fragment: Fragment = Fragment(input, ctx)
                FRAGMENTS.add(fragment)
            }
            val aliasCount = UtilityFunctions.toUnsignedInt(input.short)
            for (i in 0 until aliasCount) {
                val alias = Alias(input, ctx)
                ALIASES.add(alias)
            }
        } finally {
            ctx.popContext()
        }
    }
}