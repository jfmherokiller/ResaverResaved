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

import PlatformByteBuffer
import ess.ESS.ESSContext
import mu.KLoggable
import mu.KLogger
import resaver.Analysis

/**
 * Describes a ChangeForm containing an NPC Reference.
 *
 * @author Mark Fairchild
 */
class ChangeFormACHR(
    input: PlatformByteBuffer,
    flags: Flags.FlagsInt,
    refid: RefID,
    context: ESSContext?
) :
    ess.GeneralElement(), ChangeFormData {

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
        return "<hr/><pre><code>${super.toString("REFR", 0)}</code></pre>"
    }

    // The change flags.
    private val FLAGS: Flags.FlagsInt = flags

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }

    init {
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
            super.readElement(input, "INITIAL") { `in`: PlatformByteBuffer? ->
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
                ) { input: PlatformByteBuffer? -> input?.let { ChangeFormFlags(it) } }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_BASEOBJECT)) {
                if (context != null) {
                    super.readRefID(input, "BASE_OBJECT", context)
                }
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
                super.readElement(input, "EXTRADATA") { `in`: PlatformByteBuffer? -> ChangeFormExtraData(`in`!!, context!!) }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_INVENTORY)
                || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_LEVELED_INVENTORY)
            ) {
                super.readVSElemArray(input, "INVENTORY") { `in`: PlatformByteBuffer? ->
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
            logger.warn(ex) { "Error parsing NPC_ ChangeForm." }
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