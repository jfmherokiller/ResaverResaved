/*
 * Copyright 2018 Mark.
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
package resaver.ess

import resaver.ess.ESS.ESSContext
import java.nio.ByteBuffer



/**
 *
 * @author Mark
 */
class AnimObjects : GlobalDataBlock {
    /**
     * Creates a new `AnimObjects` by reading from a
     * `LittleEndianInput`. No error handling is performed.
     *
     * @param input The input data.
     * @param context The `ESSContext` info.
     */
    constructor(input: ByteBuffer, context: ESSContext?) {
        val COUNT = input.int
        require(!(COUNT < 0 || COUNT > 1e6)) { "AnimObject count was an illegal value: $COUNT" }
        ANIMATIONS = mutableListOf()
        for (i in 0 until COUNT) {
            val `var` = AnimObject(input, context)
            ANIMATIONS.add(`var`)
        }
    }

    /**
     * Creates a new empty `AnimObjects`.
     */
    constructor() {
        ANIMATIONS = emptyList()
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.putInt(ANIMATIONS.size)
        ANIMATIONS.forEach { `var`: AnimObject -> `var`.write(output) }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 4
        var result = 0
        for (ANIMATION in ANIMATIONS) {
            val calculateSize = ANIMATION.calculateSize()
            result += calculateSize
        }
        sum += result
        return sum
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 17 * hash + ANIMATIONS.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as AnimObjects
        for (i in ANIMATIONS.indices) {
            val ao1 = ANIMATIONS[i]
            val ao2 = other2.ANIMATIONS[i]
            if (ao1 != ao2) {
                return ao1 == ao2
            }
        }
        return ANIMATIONS == other.ANIMATIONS
    }

    /**
     * @return The `AnimObject` list.
     */
    val animations: List<AnimObject>
        get() = ANIMATIONS
    private val ANIMATIONS: List<AnimObject>

    /**
     *
     */
    class AnimObject(input: ByteBuffer?, context: ESSContext?) : GeneralElement() {
        /**
         * Creates a new `AnimObject` by reading from a
         * `LittleEndianInput`. No error handling is performed.
         *
         * @param input The input data.
         * @param context The `ESSContext`.
         */
        init {
            super.readRefID(input, "ACHR", context)
            super.readRefID(input, "ANIM", context)
            super.readByte(input, "UNKNOWN")
        }
    }
}