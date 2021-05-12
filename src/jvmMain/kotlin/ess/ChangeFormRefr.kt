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
import mu.KLoggable
import mu.KLogger
import java.nio.ByteBuffer

/**
 * Describes a ChangeForm containing a placed Reference.
 *
 * @author Mark Fairchild
 */
class ChangeFormRefr(input: ByteBuffer, flags: Flags.FlagsInt, refid: RefID, analysis: resaver.Analysis?, context: ESSContext?) :
    ess.GeneralElement(), ChangeFormData {
    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: resaver.Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        if (FLAGS.FLAGS != 0) {
            BUILDER.append("<hr/>")
            BUILDER.append("<h3>ChangeFlags</h3><ul>")
            for (flag in ChangeFlagConstantsRef.values()) {
                if (FLAGS.getFlag(flag)) {
                    BUILDER.append("<li>")
                    BUILDER.append(flag.position)
                    BUILDER.append(": ")
                    BUILDER.append(flag)
                    BUILDER.append("</li><br/>")
                }
            }
            BUILDER.append("</ul>")
        }
        BUILDER.append("<hr/>")
        BUILDER.append("<pre><code>")
        BUILDER.append(super.toString("REFR", 0))
        BUILDER.append("</code></pre>")
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

    // The change flags.
    private val FLAGS: Flags.FlagsInt

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }

    /**
     * Creates a new `ChangeFormRefr` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The ChangeForm flags.
     * @param refid The ChangeForm refid.
     * @param analysis
     * @param context The `ESSContext` info.
     */
    init {
        FLAGS = flags
        val initialType: Int
        initialType = if (refid.type === RefID.Type.CREATED) {
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
                ChangeFormInitialData(
                    `in`!!, initialType, context
                )
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_HAVOK_MOVE)) {
                super.readBytesVS(input, "HAVOK")
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_FORM_FLAGS)) {
                super.readElement(input, ChangeFlagConstantsRef.CHANGE_FORM_FLAGS) { input: ByteBuffer? ->
                    ChangeFormFlags(
                        input!!
                    )
                }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_BASEOBJECT)) {
                if (context != null) {
                    super.readRefID(input, "BASE_OBJECT", context)
                }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_SCALE)) {
                super.readFloat(input, "SCALE")
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_MOVE)) {
                if (context != null) {
                    super.readRefID(input, "MOVE_CELL", context)
                }
                super.readFloats(input, "MOVE_POS", 3)
                super.readFloats(input, "MOVE_ROT", 3)
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
                super.readElement(input, "EXTRADATA") { `in`: ByteBuffer? ->
                    ChangeFormExtraData(
                        `in`!!, context!!
                    )
                }
            }
            if (flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_INVENTORY) || flags.getFlag(ChangeFlagConstantsRef.CHANGE_REFR_LEVELED_INVENTORY)) {
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
            logger.warn(ex) {"Error parsing NPC_ ChangeForm."}
            var count = 0
            while (input.hasRemaining()) {
                val size = 256.coerceAtMost(input.capacity() - input.position())
                super.readBytes(input, "UNPARSED_DATA_$count", size)
                count++
            }
        }
    }
}