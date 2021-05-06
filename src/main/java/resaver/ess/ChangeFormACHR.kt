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
package resaver.ess

import resaver.Analysis
import resaver.ess.ESS.ESSContext
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Describes a ChangeForm containing an NPC Reference.
 *
 * @author Mark Fairchild
 */
class ChangeFormACHR(input: ByteBuffer, flags: Flags.Int, refid: RefID, analysis: Analysis?, context: ESSContext?) :
    GeneralElement(), ChangeFormData {
    /**
     * @return String representation.
     */
    override fun toString(): String {
        return super.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        return "<hr/>" +
                "<pre><code>" +
                super.toString("REFR", 0) +
                "</code></pre>"
    }

    // The change flags.
    private val FLAGS: Flags.Int

    companion object {
        val LOG = Logger.getLogger(ChangeFormACHR::class.java.canonicalName)
    }

    /**
     * Creates a new `ChangeFormACHR` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The ChangeForm flags.
     * @param refid The ChangeForm refid.
     * @param analysis
     * @param context The `ESSContext` info.
     * @throws ElementException
     */
    init {
        Objects.requireNonNull(input)
        FLAGS = Objects.requireNonNull(flags)
        val initialType: Int = if (refid.type === RefID.Type.CREATED) {
            5
        } else if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_PROMOTED) || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_CELL_CHANGED)) {
            6
        } else if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_HAVOK_MOVE) || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_MOVE)) {
            4
        } else {
            0
        }
        try {
            super.readElement(input, "INITIAL") { `in`: ByteBuffer? ->
                `in`?.let {
                    ChangeFormInitialData(
                        it,
                        initialType,
                        context
                    )
                }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_HAVOK_MOVE)) {
                readBytesVS(input, "HAVOK")
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS)) {
                super.readElement(
                    input,
                    ChangeFlagConstantsRef.CHANGE_FORM_FLAGS
                ) { input: ByteBuffer? -> input?.let { ChangeFormFlags(it) } }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_BASEOBJECT)) {
                super.readRefID(input, "BASE_OBJECT", context)
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_SCALE)) {
                super.readFloat(input, "SCALE")
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_EXTRA_OWNERSHIP)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_OBJECT_EXTRA_LOCK)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_EXTRA_ENCOUNTER_ZONE)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_EXTRA_GAME_ONLY)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_OBJECT_EXTRA_AMMO)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_DOOR_EXTRA_TELEPORT)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_PROMOTED)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_EXTRA_ACTIVATING_CHILDREN)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_OBJECT_EXTRA_ITEM_DATA)
            ) {
                super.readElement(input, "EXTRADATA") { `in`: ByteBuffer? -> ChangeFormExtraData(`in`!!, context!!) }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_INVENTORY)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_LEVELED_INVENTORY)
            ) {
                super.readVSElemArray(input, "INVENTORY") { `in`: ByteBuffer? ->
                    ChangeFormInventoryItem(
                        `in`,
                        context
                    )
                }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_ANIMATION)) {
                super.readBytesVS(input, "ANIMATIONS")
            }
        } catch (ex: Throwable) {
            LOG.log(Level.WARNING, "Error parsing NPC_ ChangeForm.", ex)
            var count = 0
            while (input.hasRemaining()) {
                val size = 256.coerceAtMost(input.capacity() - input.position())
                super.readBytes(input, "UNPARSED_DATA_$count", size)
                count++
            }
            throw ElementException("Failed to read ACHR", ex, this)
        }
    }
}