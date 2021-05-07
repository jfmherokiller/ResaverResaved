/*
 * Copyright 2017 Mark Fairchild.
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
import java.util.*

/**
 * Manages the data in one element of a change form's extra data.
 *
 * @author Mark Fairchild
 */
class ChangeFormExtraDataData(input: ByteBuffer, context: ESSContext) : ess.GeneralElement() {
    /**
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    override fun toString(level: Int): String {
        return this.toString(NAME, level)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return super.toString(NAME, 0)
    }

    var NAME: String? = null

    private class AliasInstance internal constructor(input: ByteBuffer?, context: ESSContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "REF", context)
                }
            }
            if (input != null) {
                super.readInt(input, "FORMID")
            }
        }
    }

    private class MagicTarget internal constructor(input: ByteBuffer?, context: ESSContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "REF", context)
                }
            }
            if (input != null) {
                super.readByte(input, "unk1")
            }
            if (input != null) {
                super.readVSVal(input, "unk2")
            }
            if (input != null) {
                super.readBytesVS(input, "data")
            }
        }
    }

    /**
     * Creates a new `ChangeFormInitialData`.
     *
     * @param input
     * @param context The `ESSContext` info.
     */
    init {
        Objects.requireNonNull(input)
        val TYPE = super.readByte(input, "TYPE")?.let { java.lang.Byte.toUnsignedInt(it) }!!
        require(!(TYPE < 0 || TYPE >= 256)) { "Invalid extraData type: $TYPE" }
        when (TYPE) {
            22 -> NAME = "Worn"
            23 -> NAME = "WornLeft"
            24 -> {
                NAME = "PackageStartLocation"
                super.readRefID(input, "UNK", context)
                super.readFloats(input, "POS", 3)
                super.readFloat(input, "UNK1")
            }
            25 -> {
                NAME = "Package"
                super.readRefID(input, "UNK1", context)
                super.readRefID(input, "UNK2", context)
                super.readInt(input, "UNK3")
                super.readBytes(input, "UNK4", 3)
            }
            26 -> {
                NAME = "TrespassPackage"
                val ref = super.readRefID(input, "UNK", context)!!
                if (ref.isZero) {
                    assert(false) { "INCOMPLETE" }
                }
            }
            27 -> {
                NAME = "RunOncePacks"
                super.readIntsVS(input, "PACKS")
            }
            28 -> {
                NAME = "ReferenceHandle"
                super.readRefID(input, "ID", context)
            }
            29 -> NAME = "Unknown29"
            30 -> {
                NAME = "LevCreaModifier"
                super.readInt(input, "MOD")
            }
            31 -> {
                NAME = "Ghost"
                super.readByte(input, "UNK")
            }
            32 -> NAME = "UNKNOWN32"
            33 -> {
                NAME = "Ownership"
                super.readRefID(input, "OWNER", context)
            }
            34 -> {
                NAME = "Global"
                super.readRefID(input, "UNK", context)
            }
            35 -> {
                NAME = "Rank"
                super.readRefID(input, "RANKID", context)
            }
            36 -> {
                NAME = "Count"
                super.readShort(input, "COUNT")
            }
            37 -> {
                NAME = "Health"
                super.readFloat(input, "HEALTH")
            }
            39 -> {
                NAME = "TimeLeft"
                super.readInt(input, "TIME")
            }
            40 -> {
                NAME = "Charge"
                super.readFloat(input, "CHARGE")
            }
            42 -> {
                NAME = "Lock"
                super.readBytes(input, "UNKS", 2)
                super.readRefID(input, "KEY", context)
                super.readInts(input, "UNKS2", 2)
            }
            43 -> {
                NAME = "Teleport"
                super.readFloats(input, "POS", 3)
                super.readFloats(input, "ROT", 3)
                super.readByte(input, "UNK")
                super.readRefID(input, "REF", context)
            }
            44 -> {
                NAME = "MapMarker"
                super.readByte(input, "UNK")
            }
            45 -> {
                NAME = "LeveledCreature"
                super.readRefID(input, "UNK1", context)
                super.readRefID(input, "UNK2", context)
                val flags = super.readElement(input, "NPCChangeFlags") { input: ByteBuffer? -> input?.let { Flags.readIntFlags(it) } }
                super.readElement(input, "NPC") { `in`: ByteBuffer? -> `in`?.let { flags?.let { it1 ->
                    ChangeFormNPC(it,
                        it1, context)
                } } }
            }
            46 -> {
                NAME = "LeveledItem"
                super.readInt(input, "UNK")
                super.readByte(input, "UNK2")
            }
            47 -> {
                NAME = "Scale"
                super.readFloat(input, "scale")
            }
            49 -> {
                NAME = "NonActorMagicCaster"
                super.readInt(input, "unk1")
                super.readRefID(input, "ref1", context)
                super.readInt(input, "unk2")
                super.readInt(input, "unk3")
                super.readRefID(input, "ref2", context)
                super.readFloat(input, "unk4")
                super.readRefID(input, "ref3", context)
                super.readRefID(input, "ref4", context)
            }
            50 -> {
                NAME = "NonActorMagicTarget"
                super.readRefID(input, "ref", context)
                super.readVSElemArray(input, "targets") { `in`: ByteBuffer? -> MagicTarget(`in`, context) }
            }
            52 -> {
                NAME = "PlayerCrimeList"
                super.readLongsVS(input, "list")
            }
            56 -> {
                NAME = "ItemDropper"
                super.readRefID(input, "unk", context)
            }
            61 -> NAME = "CannotWear"
            62 -> {
                NAME = "ExtraPoison"
                super.readRefID(input, "ref", context)
                super.readInt(input, "unk")
            }
            68 -> {
                NAME = "FriendHits"
                super.readFloatsVS(input, "unk")
            }
            69 -> {
                NAME = "HeadingTarget"
                super.readRefID(input, "targetID", context)
            }
            72 -> {
                NAME = "StartingWorldOrCell"
                super.readRefID(input, "worldOrCellID", context)
            }
            73 -> {
                NAME = "HotKey"
                super.readByte(input, "unk")
            }
            112 -> {
                NAME = "EncounterZone"
                super.readRefID(input, "REF", context)
            }
            136 -> {
                NAME = "AliasInstanceArray"
                super.readVSElemArray(input, "ALIASES") { `in`: ByteBuffer? -> AliasInstance(`in`, context) }
            }
            140 -> {
                NAME = "PromotedRef"
                super.readVSElemArray(input, "REFS") { `in`: ByteBuffer? -> context.readRefID(input) }
            }
            else -> throw ElementException("Unknown ExtraData: type=$TYPE", null, this)
        }
    }
}