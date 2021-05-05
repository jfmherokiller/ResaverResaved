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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import resaver.Analysis;
import static resaver.ess.ChangeFlagConstantsRef.*;

/**
 * Describes a ChangeForm containing a placed Reference.
 *
 * @author Mark Fairchild
 */
public class ChangeFormRefr extends GeneralElement implements ChangeFormData {

    /**
     * Creates a new <code>ChangeFormRefr</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The ChangeForm flags.
     * @param refid The ChangeForm refid.
     * @param analysis
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormRefr(ByteBuffer input, Flags.Int flags, RefID refid, resaver.Analysis analysis, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        this.FLAGS = Objects.requireNonNull(flags);

        int initialType;

        if (refid.getType() == RefID.Type.CREATED) {
            initialType = 5;
        } else if (flags.getFlag(CHANGE_REFR_PROMOTED) || flags.getFlag(CHANGE_REFR_CELL_CHANGED)) {
            initialType = 6;
        } else if (flags.getFlag(CHANGE_REFR_HAVOK_MOVE) || flags.getFlag(CHANGE_REFR_MOVE)) {
            initialType = 4;
        } else {
            initialType = 0;
        }

        try {
            super.readElement(input, "INITIAL", in -> new ChangeFormInitialData(in, initialType, context));

            if (flags.getFlag(CHANGE_REFR_HAVOK_MOVE)) {
                super.readBytesVS(input, "HAVOK");
            }

            if (flags.getFlag(CHANGE_FORM_FLAGS)) {
                super.readElement(input, CHANGE_FORM_FLAGS, ChangeFormFlags::new);
            }

            if (flags.getFlag(CHANGE_REFR_BASEOBJECT)) {
                super.readRefID(input, "BASE_OBJECT", context);
            }

            if (flags.getFlag(CHANGE_REFR_SCALE)) {
                super.readFloat(input, "SCALE");
            }

            if (flags.getFlag(CHANGE_REFR_MOVE)) {
                super.readRefID(input, "MOVE_CELL", context);
                super.readFloats(input, "MOVE_POS", 3);
                super.readFloats(input, "MOVE_ROT", 3);
            }

            if (flags.getFlag(CHANGE_REFR_EXTRA_OWNERSHIP)
                    || flags.getFlag(CHANGE_OBJECT_EXTRA_LOCK)
                    || flags.getFlag(CHANGE_REFR_EXTRA_ENCOUNTER_ZONE)
                    || flags.getFlag(CHANGE_REFR_EXTRA_GAME_ONLY)
                    || flags.getFlag(CHANGE_OBJECT_EXTRA_AMMO)
                    || flags.getFlag(CHANGE_DOOR_EXTRA_TELEPORT)
                    || flags.getFlag(CHANGE_REFR_PROMOTED)
                    || flags.getFlag(CHANGE_REFR_EXTRA_ACTIVATING_CHILDREN)
                    || flags.getFlag(CHANGE_OBJECT_EXTRA_ITEM_DATA)) {
                super.readElement(input, "EXTRADATA", in -> new ChangeFormExtraData(in, context));
            }

            if (flags.getFlag(CHANGE_REFR_INVENTORY) || flags.getFlag(CHANGE_REFR_LEVELED_INVENTORY)) {
                super.readVSElemArray(input, "INVENTORY", in -> new ChangeFormInventoryItem(in, context));
            }

            if (flags.getFlag(CHANGE_REFR_ANIMATION)) {
                super.readBytesVS(input, "ANIMATIONS");
            }

        } catch (Throwable ex) {
            LOG.log(Level.WARNING, "Error parsing NPC_ ChangeForm.", ex);

            int count = 0;
            while (input.hasRemaining()) {
                int size = Math.min(256, input.capacity() - input.position());
                super.readBytes(input, "UNPARSED_DATA_" + count, size);
                count++;
            }
        }
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();
        if (this.FLAGS.FLAGS != 0) {
            BUILDER.append("<hr/>");
            BUILDER.append("<h3>ChangeFlags</h3><ul>");
            for (ChangeFlagConstants flag : ChangeFlagConstantsRef.values()) {
                if (this.FLAGS.getFlag(flag)) {
                    BUILDER.append("<li>");
                    BUILDER.append(flag.getPosition());
                    BUILDER.append(": ");
                    BUILDER.append(flag);
                    BUILDER.append("</li><br/>");
                }
            }
            BUILDER.append("</ul>");
        }

        BUILDER.append("<hr/>");
        BUILDER.append("<pre><code>");
        BUILDER.append(super.toString("REFR", 0));
        BUILDER.append("</code></pre>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, java.lang.String) 
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        return false;
    }

    // The change flags.
    final private Flags.Int FLAGS;
    static final Logger LOG = Logger.getLogger(ChangeFormRefr.class.getCanonicalName());

}
