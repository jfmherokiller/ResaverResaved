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

/**
 * Describes a ChangeForm containing an NPC.
 *
 * @author Mark Fairchild
 */
class ChangeFormNPC(input: PlatformByteBuffer, flags: Flags.FlagsInt, context: ESSContext) : ess.GeneralElement(), ChangeFormData {
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
    override fun getInfo(analysis: resaver.Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<hr/><p>NPC:</p>")
        if (this.hasVal("FULLNAME")) {
            BUILDER.append("<p>FullName: ${this.getVal("FULLNAME")}\n")
        }
        if (null != changeFormFlags) {
            BUILDER.append("<p>ChangeFormFlags: $changeFormFlags\n")
        }
        if (this.hasVal("ACBS")) {
            BUILDER.append("<p>Base stats: ")
            for (b in super.getVal("ACBS") as ByteArray) {
                BUILDER.append(String.format("%02x", b))
            }
            BUILDER.append("\n")
        }
        if (this.hasVal("FACTIONRANKS")) {
            val UncastedFactions = super.getVal("FACTIONRANKS")
            var ranks: List<FactionRank> = listOf()
            if(UncastedFactions is Array<*>) {
                ranks = UncastedFactions.map { i -> i as FactionRank }
            }
            if(UncastedFactions is List<*>) {
                ranks = UncastedFactions.map { i -> i as FactionRank }
            }
            BUILDER.append("<p>${ranks.size} faction ranks.</p><ul>")
            ranks.forEach { v: FactionRank? -> BUILDER.append("<li>$v") }
            BUILDER.append("</ul>")
        }
        if (super.hasVal("SPELLS")) {
            val UncastedSpells = super.getVal("SPELLS")
            var spells:List<RefID> = listOf()
            if(UncastedSpells is Array<*>) {
                spells = UncastedSpells.map { i -> i as RefID }
            }
            if(UncastedSpells is List<*>) {
                spells = UncastedSpells.map { i -> i as RefID }
            }
            val uncastedLVLSpells = super.getVal("SPELLS_LEVELLED")
            var spells_levelled:List<RefID> = listOf()
            if(uncastedLVLSpells is Array<*>) {
                spells_levelled = uncastedLVLSpells.map { i -> i as RefID }
            }
            if(uncastedLVLSpells is List<*>) {
                spells_levelled = uncastedLVLSpells.map { i -> i as RefID }
            }
            val uncastedShouts = super.getVal("SHOUTS")
            var shouts:List<RefID> = listOf()
            if(uncastedShouts is Array<*>) {
                shouts = uncastedShouts.map { i -> i as RefID }
            }
            if(uncastedShouts is List<*>) {
                shouts = uncastedShouts.map { i -> i as RefID }
            }
            BUILDER.append("<p>${spells.size} spells.</p><ul>")
            spells.forEach { v: RefID? -> BUILDER.append("<li>$v") }
            BUILDER.append("</ul>")
            BUILDER.append("<p>${spells_levelled.size} levelled spells.</p><ul>")
            spells_levelled.forEach { v: RefID? -> BUILDER.append("<li>$v") }
            BUILDER.append("</ul>")
            BUILDER.append("<p>${shouts.size} shouts.</p><ul>")
            shouts.forEach { v: RefID? -> BUILDER.append("<li>$v") }
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
    override fun matches(analysis: resaver.Analysis?, mod: String?): Boolean {
        return false
    }

    /**
     * @return The `ChangeFormFlags` field.
     */
    var changeFormFlags: ChangeFormFlags? = null

    /**
     * Faction rank.
     */
    private class FactionRank(input: PlatformByteBuffer?, context: ESSContext?) : ess.GeneralElement() {
        override fun toString(): String {
            return "Rank ${this.getVal("RANK").toString()} with ${this.getVal("FACTION")}"
        }

        init {
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "FACTION", context)
                }
            }
            if (input != null) {
                super.readByte(input, "RANK")
            }
        }
    }

    /**
     * Face
     */
    private class FaceData(input: PlatformByteBuffer?, context: ESSContext) : ess.GeneralElement() {
        init {
            val facePresent = input?.let { super.readByte(it, "FACEPRESENT") }
            if (facePresent != null) {
                if (facePresent.toInt() != 0) {
                    super.readRefID(input, "HAIRCOLOR", context)
                    super.readInt(input, "SKINTONE")
                    super.readRefID(input, "SKIN", context)
                    super.readVSElemArray(input, "HEADPARTS") { input: PlatformByteBuffer? -> input?.let { context.readRefID(it) } }
                    val faceDataPrsent = input.let { super.readByte(it, "FACEDATAPRESENT") }
                    if (faceDataPrsent.toInt() != 0) {
                        input.let { super.readInt(it, "MORPHS_COUNT") }.let {
                            super.readFloats(input, "MORPHS", it)
                        }
                        input.let { super.readInt(it, "PRESETS_COUNT") }.let {
                            super.readInts(input, "PRESETS", it)
                        }
                    }
                }
            }
        }
    }

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
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
            super.readElement(input, ChangeFlagConstantsNPC.CHANGE_FORM_FLAGS) { input: PlatformByteBuffer? ->
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
                super.readVSElemArray(input, "FACTIONRANKS") { `in`: PlatformByteBuffer? -> FactionRank(`in`, context) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_SPELLLIST)) {
                super.readVSElemArray(input, "SPELLS") { `in`: PlatformByteBuffer? -> context.readRefID(input) }
                super.readVSElemArray(input, "SPELLS_LEVELLED") { `in`: PlatformByteBuffer? -> context.readRefID(input) }
                super.readVSElemArray(input, "SHOUTS") { `in`: PlatformByteBuffer? -> context.readRefID(input) }
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_AIDATA)) {
                super.readBytes(input, "AIDT", 20)
            }
            if (flags.getFlag(ChangeFlagConstantsNPC.CHANGE_ACTOR_BASE_FULLNAME)) {
                super.readElement(input, "FULLNAME") { obj: PlatformByteBuffer? -> WStringElement.read(obj) }
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
                super.readElement(input, "FACEDATA") { `in`: PlatformByteBuffer? -> FaceData(input, context) }
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