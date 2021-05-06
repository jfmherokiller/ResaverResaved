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
import java.util.*

/**
 * Describes an entry in a leveled list.
 *
 * @author Mark Fairchild
 */
class LeveledEntry(input: ByteBuffer, context: ESSContext) : Element, Linkable {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        Objects.requireNonNull(output)
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
        return String.format("%d (%s) = %d", LEVEL, REFID.toHTML(target), COUNT)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return String.format("%d (%s) = %d", LEVEL, REFID, COUNT)
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 29 * hash + Objects.hashCode(REFID)
        hash = 29 * hash + Integer.hashCode(LEVEL)
        hash = 29 * hash + Integer.hashCode(COUNT)
        hash = 29 * hash + Integer.hashCode(CHANCE)
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null) {
            false
        } else if (javaClass != obj.javaClass) {
            false
        } else {
            val other = obj as LeveledEntry
            LEVEL == other.LEVEL && COUNT == other.COUNT && CHANCE == other.CHANCE && REFID == other.REFID
        }
    }

    val REFID: RefID
    val LEVEL: Int
    val COUNT: Int
    val CHANCE: Int

    /**
     * Creates a new `Plugin` by reading from an input stream.
     *
     * @param input The input stream.
     * @param context The `ESSContext` info.
     */
    init {
        Objects.requireNonNull(input)
        REFID = context.readRefID(input)
        LEVEL = input.get().toInt()
        COUNT = input.short.toInt()
        CHANCE = input.get().toInt()
    }
}