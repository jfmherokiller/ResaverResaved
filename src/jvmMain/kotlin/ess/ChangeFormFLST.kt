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
package ess

import ess.ESS.ESSContext
import java.nio.ByteBuffer


/**
 * Describes a ChangeForm containing a formlist.
 *
 * @author Mark Fairchild
 */
class ChangeFormFLST(input: ByteBuffer, flags: Flags.FlagsInt, context: ESSContext) : ChangeFormData {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        refID?.write(output)
        if (null != FORMS) {
            output!!.putInt(FORMS!!.size)
            FORMS!!.forEach { ref: RefID -> ref.write(output) }
        }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 0
        if (null != refID) {
            sum += refID.calculateSize()
        }
        if (null != FORMS) {
            sum += 4
            val result = FORMS!!.sumOf { it.calculateSize() }
            sum += result
        }
        return sum
    }

    /**
     * Removes null entries.
     *
     * @return The number of entries removed.
     */
    fun cleanse(): Int {
        if (null == FORMS) {
            return 0
        }
        val size = FORMS!!.size
        FORMS!!.removeIf(RefID::isZero)
        return size - FORMS!!.size
    }

    /**
     * @return A flag indicating that the formlist has nullref entries.
     */
    fun containsNullrefs(): Boolean {
        for (FORM in FORMS!!) {
            if (FORM.isZero) {
                return true
            }
        }
        return false
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return when {
            null == FORMS -> {
                ""
            }
            containsNullrefs() -> {
                "(${FORMS!!.size} refs, contains nullrefs)"
            }
            else -> {
                "(${FORMS!!.size} refs)"
            }
        }
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 41 * hash + refID.hashCode()
        hash = 41 * hash + FORMS.hashCode()
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
                val other2 = other as ChangeFormFLST
                refID == other2.refID && FORMS == other2.FORMS
            }
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: resaver.Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>FORMLIST:</p>")
        if (null != refID) {
            BUILDER.append("<p>ChangeFormFlags: $refID</p>")
        }
        if (null != FORMS) {
            BUILDER.append(String.format("<p>List size: %d</p><ol start=0>", FORMS!!.size))
            FORMS!!.forEach { refid: RefID ->
                if (save!!.changeForms.containsKey(refid)) {
                    BUILDER.append("<li>${refid.toHTML(null)}")
                } else {
                    BUILDER.append("<li>$refid")
                }
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
    val refID: ChangeFormFlags? = if (flags.getFlag(1)) ChangeFormFlags(input) else null
    private var FORMS: MutableList<RefID>? = null

    /**
     * Creates a new `ChangeForm` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The change flags.
     * @param context The `ESSContext` info.
     */
    init {
        if (flags.getFlag(31)) {
            val formCount = input.int
            require(formCount <= 0x3FFF) { "Invalid data: found $formCount formCount in FLST." }
            FORMS = mutableListOf()
            (0 until formCount).forEach { i ->
                val REF = context.readRefID(input)
                FORMS!!.add(REF)
            }
        } else {
            FORMS = null
        }
    }
}