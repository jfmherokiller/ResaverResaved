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
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Describes a ChangeForm containing an NPC.
 *
 * @author Mark Fairchild
 */
class ChangeFormNPC(input: ByteBuffer, flags: Flags.Int, context: ESSContext) : GeneralElement(), ChangeFormData {
    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (super.hasVal("FULLNAME")) super.getVal("FULLNAME").toString() else ""
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>NPC:</p>")
        if (this.hasVal("FULLNAME")) {
            BUILDER.append(String.format("<p>FullName: %s\n", this.getVal("FULLNAME")))
        }
        if (null != changeFormFlags) {
            BUILDER.append(String.format("<p>ChangeFormFlags: %s\n", changeFormFlags))
        }
        if (this.hasVal("ACBS")) {
            BUILDER.append("<p>Base stats: ")
            for (b in super.getVal("ACBS") as ByteArray) {
                BUILDER.append(String.format("%02x", b))
            }
            BUILDER.append("\n")
        }
        if (this.hasVal("FACTIONRANKS")) {
            val UncastedFactions = super.getVal("FACTIONRANKS") as Array<*>
            val ranks: List<FactionRank> = UncastedFactions.map { i -> i as FactionRank }
            BUILDER.append(String.format("<p>%s faction ranks.</p><ul>", ranks.size))
            ranks.forEach { v: FactionRank? -> BUILDER.append(String.format("<li>%s", v)) }
            BUILDER.append("</ul>")
        }
        if (super.hasVal("SPELLS")) {
            val UncastedSpells = super.getVal("SPELLS")
            val spells = UncastedSpells as Array<RefID>
            val spells_levelled = super.getVal("SPELLS_LEVELLED") as Array<RefID>
            val shouts = super.getVal("SHOUTS") as Array<RefID>
            BUILDER.append(String.format("<p>%s spells.</p><ul>", spells.size))
            listOf(*spells).forEach { v: RefID? -> BUILDER.append(String.format("<li>%s", v)) }
            BUILDER.append("</ul>")
            BUILDER.append(String.format("<p>%s levelled spells.</p><ul>", spells_levelled.size))
            listOf(*spells_levelled)
                .forEach { v: RefID? -> BUILDER.append(String.format("<li>%s", v)) }
            BUILDER.append("</ul>")
            BUILDER.append(String.format("<p>%s shouts.</p><ul>", shouts.size))
            listOf(*shouts).forEach { v: RefID? -> BUILDER.append(String.format("<li>%s", v)) }
            BUILDER.append("</ul>")
        }
        if (super.hasVal("AIDT")) {
            BUILDER.append("<p>AI:</p><code>")
            for (b in super.getVal("AIDT") as ByteArray) {
                BUILDER.append(String.format("%02x", b))
            }
            BUILDER.append("</code>")
        }
        if (super.hasVal("DNAM")) {
            BUILDER.append("<p>Skills:</p><code>")
            for (b in super.getVal("DNAM") as ByteArray) {
                BUILDER.append(String.format("%02x", b))
            }
            BUILDER.append("</code>")
        }
        return BUILDER.toString()
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
     * @return The `ChangeFormFlags` field.
     */
    var changeFormFlags: ChangeFormFlags? = null

    /**
     * Faction rank.
     */
    private class FactionRank(input: ByteBuffer?, context: ESSContext?) : GeneralElement() {
        override fun toString(): String {
            return String.format("Rank %s with %s", this.getVal("RANK").toString(), this.getVal("FACTION"))
        }

        init {
            super.readRefID(input, "FACTION", context)
            super.readByte(input, "RANK")
        }
    }

    /**
     * Face
     */
    private class FaceData(input: ByteBuffer?, context: ESSContext) : GeneralElement() {
        init {
            val facePresent = super.readByte(input, "FACEPRESENT")
            if (facePresent.toInt() != 0) {
                super.readRefID(input, "HAIRCOLOR", context)
                super.readInt(input, "SKINTONE")
                super.readRefID(input, "SKIN", context)
                super.readVSElemArray(input, "HEADPARTS") { input: ByteBuffer? -> context.readRefID(input) }
                val faceDataPrsent = super.readByte(input, "FACEDATAPRESENT")
                if (faceDataPrsent.toInt() != 0) {
                    super.readFloats(input, "MORPHS", super.readInt(input, "MORPHS_COUNT"))
                    super.readInts(input, "PRESETS", super.readInt(input, "PRESETS_COUNT"))
                }
            }
        }
    }

    companion object {
        val LOG: Logger = Logger.getLogger(ChangeFormNPC::class.java.canonicalName)
    }

    /**
     * Creates a new `ChangeForm`.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The `ESSContext` info.
     */
    init {
        changeFormFlags = if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_FORM_FLAGS)) {
            super.readElement(input, ChangeFlagConstantsNPC.CHANGE_FORM_FLAGS) { input: ByteBuffer? ->
                ChangeFormFlags(
                    input!!
                )
            }
        } else {
            null
        }
        try {
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_DATA)) {
                super.readBytes(input, "ACBS", 24)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_FACTIONS)) {
                super.readVSElemArray(input, "FACTIONRANKS") { `in`: ByteBuffer? -> FactionRank(`in`, context) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_SPELLLIST)) {
                super.readVSElemArray(input, "SPELLS") { `in`: ByteBuffer? -> context.readRefID(input) }
                super.readVSElemArray(input, "SPELLS_LEVELLED") { `in`: ByteBuffer? -> context.readRefID(input) }
                super.readVSElemArray(input, "SHOUTS") { `in`: ByteBuffer? -> context.readRefID(input) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_AIDATA)) {
                super.readBytes(input, "AIDT", 20)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_FULLNAME)) {
                super.readElement(input, "FULLNAME") { obj: ByteBuffer? -> WStringElement.read(obj) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_NPC_SKILLS)) {
                super.readBytes(input, "DNAM", 52)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_NPC_CLASS)) {
                super.readRefID(input, "CCLASS", context)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_NPC_RACE)) {
                super.readRefID(input, "RACE", context)
                super.readRefID(input, "OLDRACE", context)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_NPC_FACE)) {
                super.readElement(input, "FACEDATA") { `in`: ByteBuffer? -> FaceData(input, context) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_NPC_GENDER)) {
                super.readByte(input, "GENDER")
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_DEFAULT_OUTFIT)) {
                super.readRefID(input, "DEFAULT_OUTFIT", context)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_SLEEP_OUTFIT)) {
                super.readRefID(input, "SLEEP_OUTFIT", context)
            }
        } catch (ex: RuntimeException) {
            LOG.log(Level.WARNING, "Error parsing NPC_ ChangeForm.", ex)
            var count = 0
            while (input.hasRemaining()) {
                val size = 256.coerceAtMost(input.capacity() - input.position())
                super.readBytes(input, "UNPARSED_DATA_$count", size)
                count++
            }
        }
    }
}