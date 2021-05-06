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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import resaver.Analysis;
import static resaver.ess.ChangeFlagConstantsRef.*;

/**
 * Describes a ChangeForm containing an NPC leveled list.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormLVLN implements ChangeFormData {

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param flags The change form flags.
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormLVLN(ByteBuffer input, Flags.Int flags, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        if (flags.getFlag(CHANGE_FORM_FLAGS)) {
            this.FLAGS = new ChangeFormFlags(input);
        } else {
            this.FLAGS = null;
        }
        if (flags.getFlag(31)) {
            int formCount = input.get();
            this.ENTRIES = new ArrayList<>(formCount);

            for (int i = 0; i < formCount; i++) {
                final LeveledEntry ENTRY = new LeveledEntry(input, context);
                this.ENTRIES.add(ENTRY);
            }
        } else {
            this.ENTRIES = null;
        }
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);

        if (null != this.FLAGS) {
            this.FLAGS.write(output);
        }

        if (null != this.ENTRIES) {
            output.putShort((short) this.ENTRIES.size());
            this.ENTRIES.forEach(entry -> entry.write(output));
        }
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 0;

        if (null != this.FLAGS) {
            sum += this.FLAGS.calculateSize();
        }

        if (null != this.ENTRIES) {
            sum += 2;
            int result = 0;
            for (LeveledEntry ENTRY : this.ENTRIES) {
                int calculateSize = ENTRY.calculateSize();
                result += calculateSize;
            }
            sum += result;
        }

        return sum;
    }

    /**
     * @return The <code>ChangeFormFlags</code> field.
     */
    public ChangeFormFlags getRefID() {
        return this.FLAGS;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        if (null == this.ENTRIES) {
            return "";

        } else {
            return "(" + this.ENTRIES.size() + " leveled entries)";
        }
    }

    /**
     * @see Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.FLAGS);
        hash = 41 * hash + Objects.hashCode(this.ENTRIES);
        return hash;
    }

    /**
     * @see Object#equals()
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!Objects.equals(this.getClass(), obj.getClass())) {
            return false;
        }

        final ChangeFormLVLN other = (ChangeFormLVLN) obj;
        return Objects.equals(this.FLAGS, other.FLAGS) && Objects.equals(this.ENTRIES, other.ENTRIES);
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

        BUILDER.append("<hr/><p>FORMLIST:</p>");

        if (null != this.FLAGS) {
            BUILDER.append(String.format("<p>ChangeFormFlags: %s</p>", this.FLAGS));
        }

        if (null != this.ENTRIES) {
            BUILDER.append(String.format("<p>List size: %d</p><ol start=0>", this.ENTRIES.size()));
            this.ENTRIES.forEach(entry -> {
                BUILDER.append("<li>").append(entry.toHTML(null)).append("</li>");
            });
            BUILDER.append("</ol>");
        }

        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        return false;
    }

    final private ChangeFormFlags FLAGS;
    final private List<LeveledEntry> ENTRIES;

}
