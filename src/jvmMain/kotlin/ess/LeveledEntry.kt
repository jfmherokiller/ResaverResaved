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
package ess

import PlatformByteBuffer
import ess.ESS.ESSContext

/**
 * Describes an entry in a leveled list.
 *
 * @author Mark Fairchild
 */
class LeveledEntry(input: PlatformByteBuffer, context: ESSContext) : Element, Linkable {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(LEVEL.toByte())
        REFID.write(output)
        output.putShort(COUNT.toShort())
        output.put(CHANCE.toByte())
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 3 + REFID.calculateSize()
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return String.format("%d (${REFID.toHTML(target)}) = %d", LEVEL, COUNT)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return String.format("%d ($REFID) = %d", LEVEL, COUNT)
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 29 * hash + REFID.hashCode()
        hash = 29 * hash + LEVEL.hashCode()
        hash = 29 * hash + COUNT.hashCode()
        hash = 29 * hash + CHANCE.hashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> {
                true
            }
            other == null -> {
                false
            }
            javaClass != other.javaClass -> {
                false
            }
            else -> {
                val other2 = other as LeveledEntry
                LEVEL == other2.LEVEL && COUNT == other2.COUNT && CHANCE == other2.CHANCE && REFID == other2.REFID
            }
        }
    }

    val REFID: RefID = context.readRefID(input)
    val LEVEL: Int = input.getByte().toInt()
    val COUNT: Int = input.getShort().toInt()
    val CHANCE: Int = input.getByte().toInt()

}