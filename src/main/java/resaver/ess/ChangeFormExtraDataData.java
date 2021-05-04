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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Manages the data in one element of a change form's extra data.
 *
 * @author Mark Fairchild
 */
public class ChangeFormExtraDataData extends GeneralElement {

    /**
     * Creates a new <code>ChangeFormInitialData</code>.
     *
     * @param input
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormExtraDataData(ByteBuffer input, ESS.ESSContext context) throws ElementException {
        Objects.requireNonNull(input);
        final int TYPE = Byte.toUnsignedInt(super.readByte(input, "TYPE"));
        if (TYPE < 0 || TYPE >= 256) {
            throw new IllegalArgumentException("Invalid extraData type: " + TYPE);
        }

        switch (TYPE) {
            case 22:
                this.NAME = "Worn";
                break;
            case 23:
                this.NAME = "WornLeft";
                break;
            case 24:
                this.NAME = "PackageStartLocation";
                super.readRefID(input, "UNK", context);
                super.readFloats(input, "POS", 3);
                super.readFloat(input, "UNK1");
                break;
            case 25:
                this.NAME = "Package";
                super.readRefID(input, "UNK1", context);
                super.readRefID(input, "UNK2", context);
                super.readInt(input, "UNK3");
                super.readBytes(input, "UNK4", 3);
                break;
            case 26:
                this.NAME = "TrespassPackage";
                RefID ref = super.readRefID(input, "UNK", context);
                if (ref.isZero()) {
                    assert false : "INCOMPLETE";
                }
                break;
            case 27:
                this.NAME = "RunOncePacks";
                super.readIntsVS(input, "PACKS");
                break;
            case 28:
                this.NAME = "ReferenceHandle";
                super.readRefID(input, "ID", context);
                break;
            case 29:
                this.NAME = "Unknown29";
                break;
            case 30:
                this.NAME = "LevCreaModifier";
                super.readInt(input, "MOD");
                break;
            case 31:
                this.NAME = "Ghost";
                super.readByte(input, "UNK");
                break;
            case 32:
                this.NAME = "UNKNOWN32";
                break;
            case 33:
                this.NAME = "Ownership";
                super.readRefID(input, "OWNER", context);
                break;
            case 34:
                this.NAME = "Global";
                super.readRefID(input, "UNK", context);
                break;
            case 35:
                this.NAME = "Rank";
                super.readRefID(input, "RANKID", context);
                break;
            case 36:
                this.NAME = "Count";
                super.readShort(input, "COUNT");
                break;
            case 37:
                this.NAME = "Health";
                super.readFloat(input, "HEALTH");
                break;
            case 39:
                this.NAME = "TimeLeft";
                super.readInt(input, "TIME");
                break;
            case 40:
                this.NAME = "Charge";
                super.readFloat(input, "CHARGE");
                break;
            case 42:
                this.NAME = "Lock";
                super.readBytes(input, "UNKS", 2);
                super.readRefID(input, "KEY", context);
                super.readInts(input, "UNKS2", 2);
                break;
            case 43:
                this.NAME = "Teleport";
                super.readFloats(input, "POS", 3);
                super.readFloats(input, "ROT", 3);
                super.readByte(input, "UNK");
                super.readRefID(input, "REF", context);
                break;
            case 44:
                this.NAME = "MapMarker";
                super.readByte(input, "UNK");
                break;
            case 45:
                this.NAME = "LeveledCreature";
                super.readRefID(input, "UNK1", context);
                super.readRefID(input, "UNK2", context);
                Flags.Int flags = super.readElement(input, "NPCChangeFlags", i -> Flags.readIntFlags(i));
                super.readElement(input, "NPC", in -> new ChangeFormNPC(in, flags, context));
                break;
            case 46:
                this.NAME = "LeveledItem";
                super.readInt(input, "UNK");
                super.readByte(input, "UNK2");
                break;
            case 47:
                this.NAME = "Scale";
                super.readFloat(input, "scale");
                break;
            case 49:
                this.NAME = "NonActorMagicCaster";
                super.readInt(input, "unk1");
                super.readRefID(input, "ref1", context);
                super.readInt(input, "unk2");
                super.readInt(input, "unk3");
                super.readRefID(input, "ref2", context);
                super.readFloat(input, "unk4");
                super.readRefID(input, "ref3", context);
                super.readRefID(input, "ref4", context);
                break;
            case 50:
                this.NAME = "NonActorMagicTarget";
                super.readRefID(input, "ref", context);
                super.readVSElemArray(input, "targets", in -> new MagicTarget(in, context));
                break;
            case 52:
                this.NAME = "PlayerCrimeList";
                super.readLongsVS(input, "list");
                break;
            //case 53:
            //    this.NAME= "SUPERUNKNOWN";
            //    break;                
            case 56:
                this.NAME = "ItemDropper";
                super.readRefID(input, "unk", context);
                break;
            case 61:
                this.NAME = "CannotWear";
                break;
            case 62:
                this.NAME = "ExtraPoison";
                super.readRefID(input, "ref", context);
                super.readInt(input, "unk");
                break;
            case 68:
                this.NAME = "FriendHits";
                super.readFloatsVS(input, "unk");
                break;
            case 69:
                this.NAME = "HeadingTarget";
                super.readRefID(input, "targetID", context);
                break;
            case 72:
                this.NAME = "StartingWorldOrCell";
                super.readRefID(input, "worldOrCellID", context);
                break;
            case 73:
                this.NAME = "HotKey";
                super.readByte(input, "unk");
                break;
            case 112:
                this.NAME = "EncounterZone";
                super.readRefID(input, "REF", context);
                break;
            case 136:
                this.NAME = "AliasInstanceArray";
                super.readVSElemArray(input, "ALIASES", in -> new AliasInstance(in, context));
                break;
            case 140:
                this.NAME = "PromotedRef";
                super.readVSElemArray(input, "REFS", in -> context.readRefID(input));
                break;
            default:
                throw new ElementException("Unknown ExtraData: type=" + TYPE, null, this);
        }

    }

    /**
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    @Override
    public String toString(int level) {
        return this.toString(this.NAME, level);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString(this.NAME, 0);
    }

    final public String NAME;

    static private class AliasInstance extends GeneralElement {

        AliasInstance(ByteBuffer input, ESS.ESSContext context) {
            super.readRefID(input, "REF", context);
            super.readInt(input, "FORMID");
        }
    }

    static private class MagicTarget extends GeneralElement {

        MagicTarget(ByteBuffer input, ESS.ESSContext context) {
            super.readRefID(input, "REF", context);
            super.readByte(input, "unk1");
            super.readVSVal(input, "unk2");
            super.readBytesVS(input, "data");
        }
    }

}
