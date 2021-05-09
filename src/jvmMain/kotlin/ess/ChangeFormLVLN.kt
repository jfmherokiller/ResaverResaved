/*
 * Copyright 2018 Mark Fairchild.
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

import ess.ESS.ESSContext
import java.nio.ByteBuffer


/**
 * Describes a ChangeForm containing an NPC leveled list.
 *
 * @author Mark Fairchild
 */
class ChangeFormLVLN(input: ByteBuffer, flags: Flags.FlagsInt, context: ESSContext?) : ChangeFormData {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        refID?.write(output)
        if (null != ENTRIES) {
            output!!.putShort(ENTRIES!!.size.toShort())
            ENTRIES!!.forEach { entry: LeveledEntry -> entry.write(output) }
        }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 0
        if (null != refID) {
            sum += refID!!.calculateSize()
        }
        if (null != ENTRIES) {
            sum += 2
            var result = 0
            for (ENTRY in ENTRIES!!) {
                val calculateSize = ENTRY.calculateSize()
                result += calculateSize
            }
            sum += result
        }
        return sum
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (null == ENTRIES) {
            ""
        } else {
            "(${ENTRIES!!.size} leveled entries)"
        }
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + refID.hashCode()
        hash = 41 * hash + ENTRIES.hashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @param other
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
            this.javaClass != other.javaClass -> {
                false
            }
            else -> {
                val other2 = other as ChangeFormLVLN
                refID == other2.refID && ENTRIES == other2.ENTRIES
            }
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: resaver.Analysis?, save: ess.ESS?): String? {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>FORMLIST:</p>")
        if (null != refID) {
            BUILDER.append(String.format("<p>ChangeFormFlags: %s</p>", refID))
        }
        if (null != ENTRIES) {
            BUILDER.append(String.format("<p>List size: %d</p><ol start=0>", ENTRIES!!.size))
            ENTRIES!!.forEach { entry: LeveledEntry ->
                BUILDER.append("<li>").append(entry.toHTML(null)).append("</li>")
            }
            BUILDER.append("</ol>")
        }
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: resaver.Analysis?, mod: String?): Boolean {
        return false
    }

    /**
     * @return The `ChangeFormFlags` field.
     */
    var refID: ChangeFormFlags? = null
    private var ENTRIES: MutableList<LeveledEntry>? = null

    /**
     * Creates a new `ChangeForm` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The `ESSContext` info.
     */
    init {
        refID = if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS)) {
            ChangeFormFlags(input)
        } else {
            null
        }
        if (flags.getFlag(31)) {
            val formCount = input.get().toInt()
            ENTRIES = mutableListOf()
            for (i in 0 until formCount) {
                val ENTRY = context?.let { LeveledEntry(input, it) }
                ENTRY?.let { ENTRIES!!.add(it) }
            }
        } else {
            ENTRIES = null
        }
    }
}