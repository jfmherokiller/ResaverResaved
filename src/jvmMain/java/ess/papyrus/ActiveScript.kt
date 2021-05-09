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
package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resaver.ListException;
import ess.AnalyzableElement;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import resaver.Analysis;
import ess.ESS;
import ess.Element;
import ess.Linkable;
import ess.Plugin;
import ess.RefID;

/**
 * Describes an active script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class ActiveScript implements AnalyzableElement, HasID, SeparateData {

    /**
     * Creates a new <code>ActiveScript</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     */
    public ActiveScript(@NotNull ByteBuffer input, @NotNull PapyrusContext context) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.ID = context.readEID32(input);
        this.TYPE = input.get();
        this.owner = null;
        this.suspendedStack = null;
    }

    /**
     * @see ess.Element#write(ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(@NotNull ByteBuffer output) {
        this.ID.write(output);
        output.put(this.TYPE);
    }

    /**
     * @see SeparateData#readData(java.nio.ByteBuffer,
     * ess.papyrus.PapyrusContext)
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Override
    public void readData(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
        this.data = new ActiveScriptData(input, context);
    }

    /**
     * @see SeparateData#writeData(java.nio.ByteBuffer)
     * @param output
     */
    @Override
    public void writeData(@NotNull ByteBuffer output) {
        this.data.write(output);
    }

    /**
     * @see ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 1 + this.ID.calculateSize();
        sum += this.data == null ? 0 : this.data.calculateSize();
        return sum;
    }

    /**
     * Replaces the opcodes of each <code>StackFrame</code> with NOPs.
     */
    public void zero() {
        this.getStackFrames().forEach(StackFrame::zero);
    }

    /**
     * @return The ID of the papyrus element.
     */
    @Override
    public EID getID() {
        return this.ID;
    }

    /**
     * @return The type of the script.
     */
    public byte getType() {
        return this.TYPE;
    }

    /**
     * @return The instance field.
     */
    @Nullable
    public AnalyzableElement getInstance() {
        return this.owner;
    }

    /**
     * Shortcut for getData().getStackFrames().
     *
     * @return
     */
    @NotNull
    public List<StackFrame> getStackFrames() {
        return null != this.data ? this.data.STACKFRAMES : Collections.emptyList();
    }

    /**
     * Shortcut for getData().getAttached().
     *
     * @return The attached field.
     */
    @Nullable
    public EID getAttached() {
        return null != this.data ? this.data.ATTACHED : null;
    }

    /**
     * Shortcut for getData().getAttachedElement().
     *
     * @return
     */
    @Nullable
    public HasID getAttachedElement() {
        return this.data == null ? null : this.data.ATTACHED_ELEMENT;
    }

    /**
     * Tests if the activescript has any stackframes.
     *
     * @return
     */
    public boolean hasStack() {
        return !this.getStackFrames().isEmpty();
    }

    /**
     * @return A flag indicating if the <code>ActiveScript</code> is terminated.
     *
     */
    public boolean isTerminated() {
        // Suspended stacks aren't terminated.
        if (this.isSuspended() || !this.hasStack()) {
            return false;
        }

        final StackFrame FIRST = this.getStackFrames().get(0);
        if (FIRST.isNative() || !FIRST.isZeroed()) return false;
        for (StackFrame f : this.getStackFrames()) {
            if (!f.isZeroed() && !f.isNative()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return A flag indicating if the <code>ActiveScript</code> is suspended.
     *
     */
    public boolean isSuspended() {
        return this.suspendedStack != null;
    }

    /**
     * @see ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @NotNull
    @Override
    public String toHTML(Element target) {
        return Linkable.makeLink("thread", this.ID, this.toString());
    }

    /**
     * @return String representation.
     */
    @NotNull
    @Override
    public String toString() {
        final StringBuilder BUILDER = new StringBuilder();
        if (null == this.data) {
            return this.ID + "-" + String.format("%02x", this.TYPE);
        }

        if (this.isUndefined()) {
            BUILDER.append("#");
        }

        if (this.hasStack()) {
            StackFrame topFrame = this.getStackFrames().get(0);
            TString scriptName = topFrame.getScriptName();
            BUILDER.append(scriptName).append(" ");

        } else if (this.suspendedStack != null && this.suspendedStack.getMessage() != null) {
            BUILDER.append(this.suspendedStack.getMessage());

        } else {
        }

        BUILDER.append("(").append(this.ID).append(") ");
        BUILDER.append(this.getStackFrames().size()).append(" frames");

        EID attached = this.getAttached();
        if (null != attached && attached.isZero()) {
            BUILDER.append(" (zero attached)");
        } else if (null != attached) {
            BUILDER.append(" (valid attached ").append(this.getAttached()).append(")");
        }

        if (this.isTerminated()) {
            BUILDER.append(" (TERMINATED)");
        } else if (this.isSuspended()) {
            BUILDER.append(" (SUSPENDED)");
        }

        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @NotNull
    @Override
    public String getInfo(@Nullable resaver.Analysis analysis, @NotNull ESS save) {
        final StringBuilder BUILDER = new StringBuilder();

        if (this.isTerminated()) {
            BUILDER.append("<html><h3>ACTIVESCRIPT (TERMINATED)</h3>");
            BUILDER.append("<p><em>WARNING: SCRIPT TERMINATED!</em><br/>This thread has been terminated and all of its instructions erased.</p>");
        } else if (this.isSuspended()) {
            BUILDER.append("<html><h3>ACTIVESCRIPT (SUSPENDED)</h3>");
            BUILDER.append("<p><em>WARNING: SCRIPT SUSPENDED!</em><br/>This script has been suspended. Terminating it may have unpredictable results.</p>");
            BUILDER.append("<p>Suspended stack: ").append(this.suspendedStack.toHTML(null)).append("</p>");
        } else {
            BUILDER.append("<html><h3>ACTIVESCRIPT</h3>");
        }

        if (this.isUndefined()) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Remove Undefined Instances\" will terminate this thread.</p>");
        }

        BUILDER.append("<p>Attachment ID: ").append(this.getAttached()).append("</p>");
        if (null != this.getAttachedElement()) {
            BUILDER.append("<p>Attachment element: ").append(this.getAttachedElement().toHTML(this)).append("</p>");
        } else if (this.getAttached() != null && !this.getAttached().isZero()) {
            BUILDER.append("<p>Attachment element: <em>not found</em></p>");
        } else {
            BUILDER.append("<p>Attachment element: <em>None</em></p>");
        }

        if (null != analysis && this.hasStack()) {
            StackFrame topFrame = this.getStackFrames().get(0);
            SortedSet<String> mods = analysis.SCRIPT_ORIGINS.get(topFrame.getScriptName().toIString());
            if (null != mods) {
                String mod = mods.last();
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mod));
            }

        }

        if (null == this.owner) {
            BUILDER.append("<p>This thread doesn't seem to be attached to an instance.</p>");

        } else if (this.owner instanceof ScriptInstance) {
            ScriptInstance instance = (ScriptInstance) this.owner;
            final RefID REF = instance.getRefID();
            final Plugin PLUGIN = REF.PLUGIN;

            if (PLUGIN != null) {
                BUILDER.append(String.format("<p>This thread is attached to an instance from %s.</p>", PLUGIN.toHTML(this)));
            } else if (instance.getRefID().getType() == RefID.Type.CREATED) {
                BUILDER.append("<p>This thread is attach to instance that was created in-game.</p>");
            }
        }

        if (null == this.owner) {
            BUILDER.append("<p>No owner was identified.</p>");

        } else if (this.owner instanceof Linkable) {
            Linkable l = (Linkable) this.owner;
            String type = this.owner.getClass().getSimpleName();
            BUILDER.append(String.format("<p>%s: %s</p>", type, l.toHTML(this)));
        } else {
            String type = this.owner.getClass().getSimpleName();
            BUILDER.append(String.format("<p>%s: %s</p>", type, this.owner));
        }

        BUILDER.append("<p>");
        BUILDER.append(String.format("ID: %s<br/>", this.getID()));
        BUILDER.append(String.format("Type: %02x<br/>", this.TYPE));

        if (null == this.data) {
            BUILDER.append("<h3>DATA MISSING</h3>");

        } else {
            BUILDER.append(String.format("Version: %d.%d<br/>", this.getMajorVersion(), this.getMinorVersion()));
            BUILDER.append(String.format("Unknown (var): %s<br/>", this.getUnknownVar().toHTML(this)));
            BUILDER.append(String.format("Flag: %08x<br/>", this.getFlag()));
            BUILDER.append(String.format("Unknown1 (byte): %02x<br/>", this.getUnknownByte()));
            BUILDER.append(String.format("Unknown2 (int): %08x<br/>", this.getUnknown2()));
            BUILDER.append(String.format("Unknown3 (byte): %02x<br/>", this.getUnknown3()));
            if (null != this.getUnknown4()) {
                BUILDER.append(String.format("FragmentData (struct): %s<br/>", this.getUnknown4().toHTML(this)));
            } else {
                BUILDER.append(String.format("FragmentData (struct): %s<br/>", this.getUnknown4()));
            }
            if (null != this.getUnknown5()) {
                BUILDER.append(String.format("Unknown5 (byte): %02x<br/>", this.getUnknown5()));
            } else {
                BUILDER.append("Unknown5 (byte): <em>absent</em><br/>");
            }

            Linkable UNKNOWN2 = save.getPapyrus().getContext().broadSpectrumSearch(this.getUnknown2());
            if (null != UNKNOWN2) {
                BUILDER.append("<p>Potential match for unknown2 found using general search:<br/>");
                BUILDER.append((UNKNOWN2).toHTML(this));
                BUILDER.append("</p>");
            }
        }

        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(Analysis, String)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(@NotNull Analysis analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);
        for (StackFrame v : this.getStackFrames()) {
            if (v.matches(analysis, mod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param stacks The SuspendedStacks.
     */
    public void resolveStack(@NotNull java.util.Map<EID, SuspendedStack> stacks) {
        if (this.hasStack()) {
            Variable ref = this.getStackFrames().get(0).getOwner();
            if (ref instanceof VarRef) {
                this.owner = ref.getReferent();
            } else if (ref instanceof VarNull) {
                this.owner = null;
            }
        }

        this.suspendedStack = stacks.getOrDefault(this.ID, null);
    }

    /**
     * Checks if any of the stackframes in the <code>ActiveScript</code> is
     * running code from the specified <code>Script</code>.
     *
     * @param script The <code>Script</code> the check.
     * @return A flag indicating if any of the stackframes in the script matches
     * the specified <code>Script</code>.
     */
    public boolean hasScript(Script script) {
        if (null == this.data) {
            return false;
        }
        for (StackFrame frame : this.getStackFrames()) {
            if (frame.getScript() == script) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The major version field.
     */
    public byte getMajorVersion() {
        return this.data.MAJORVERSION;
    }

    /**
     * @return The minor version field.
     */
    public int getMinorVersion() {
        return this.data.MINORVERSION;
    }

    /**
     * @return The flag field.
     */
    public int getFlag() {
        return this.data.FLAG;
    }

    /**
     * @return The unknown variable field.
     */
    @NotNull
    public Variable getUnknownVar() {
        return this.data.UNKNOWN;
    }

    /**
     * @return The FragmentTask field.
     */
    @Nullable
    public FragmentTask getUnknown4() {
        return this.data.UNKNOWN4;
    }

    /**
     * @return The Unknown byte field.
     */
    public byte getUnknownByte() {
        return this.data.UNKNOWNBYTE;
    }

    /**
     * @return The Unknown2 field.
     */
    public int getUnknown2() {
        return this.data.UNKNOWN2;
    }

    /**
     * @return The Unknown3 field.
     */
    public byte getUnknown3() {
        return this.data.UNKNOWN3;
    }

    /**
     * @return The Unknown5 field.
     */
    @Nullable
    public Byte getUnknown5() {
        return this.data.UNKNOWN5;
    }

    /**
     * @return A flag indicating if the <code>ActiveScript</code> is undefined.
     * An <code>ActiveScript</code> is undefined if any of its stack frames are
     * undefined.
     *
     */
    public boolean isUndefined() {
        if (null == this.data) {
            return false;
        } else if (this.hasStack()) {
            for (StackFrame stackFrame : this.getStackFrames()) {
                if (stackFrame.isUndefined()) {
                    return true;
                }
            }
            return false;
        } else if (this.suspendedStack != null) {
            return this.suspendedStack.isUndefined();
        } else {
            return false;
        }
    }

    final private EID ID;
    final private byte TYPE;
    private ActiveScriptData data;
    @Nullable
    private AnalyzableElement owner;
    @Nullable
    private SuspendedStack suspendedStack;
    static final private Logger LOG = Logger.getLogger(ActiveScript.class.getCanonicalName());

    final public class ActiveScriptData implements PapyrusDataFor<ActiveScript> {

        /**
         * Creates a new <code>ActiveScriptData</code>.
         *
         * @param input The input stream.
         * @param context The <code>PapyrusContext</code> info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        ActiveScriptData(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusElementException, PapyrusFormatException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(context);

            this.MAJORVERSION = input.get();
            this.MINORVERSION = input.get();
            if (this.MINORVERSION < 1 || this.MINORVERSION > 2) {
                throw new IllegalArgumentException("Wrong minor version = " + this.MINORVERSION);
            }

            this.UNKNOWN = Variable.read(input, context);

            this.FLAG = input.get();
            this.UNKNOWNBYTE = input.get();
            this.UNKNOWN2 = ((this.FLAG & 0x01) == 0x01 ? input.getInt() : 0);
            this.UNKNOWN3 = input.get();

            switch (this.UNKNOWN3) {
                case 1:
                case 2:
                case 3:
                    this.UNKNOWN4 = new FragmentTask(input, this.UNKNOWN3, context);
                    break;
                default:
                    this.UNKNOWN4 = null;
            }

            if (context.getGame().isFO4()) {
                if (null == this.UNKNOWN4) {
                    this.ATTACHED = context.readEID64(input);
                } else if (null != this.UNKNOWN4.TYPE && this.UNKNOWN4.TYPE != FragmentTask.FragmentType.TerminalRunResults) {
                    this.ATTACHED = context.readEID64(input);
                } else {
                    this.ATTACHED = null;
                }
            } else {
                this.ATTACHED = null;
            }

            if (null != this.ATTACHED && !this.ATTACHED.isZero()) {
                this.ATTACHED_ELEMENT = context.findAny(this.ATTACHED);
                if (this.ATTACHED_ELEMENT == null) {
                    LOG.log(Level.WARNING, String.format("Attachment ID did not match anything: %s\n", this.ATTACHED));
                }
            } else {
                this.ATTACHED_ELEMENT = null;
            }

            try {
                int count = input.getInt();
                if (count < 0) {
                    throw new IllegalArgumentException("Invalid stackFrame count: " + count);
                }

                this.STACKFRAMES = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    StackFrame frame = new StackFrame(input, ActiveScript.this, context);
                    this.STACKFRAMES.add(frame);
                }
            } catch (ListException ex) {
                throw new PapyrusElementException("Failed to read with StackFrame data.", ex, this);
            }

            this.UNKNOWN5 = (this.FLAG != 0 ? input.get() : null);
        }

        /**
         * @see ess.Element#write(resaver.ByteBuffer)
         * @param output The output stream.
         */
        @Override
        public void write(@NotNull ByteBuffer output) {
            ID.write(output);
            output.put(this.MAJORVERSION);
            output.put(this.MINORVERSION);
            this.UNKNOWN.write(output);
            output.put(this.FLAG);
            output.put(this.UNKNOWNBYTE);

            if ((this.FLAG & 0x01) == 0x01) {
                output.putInt(this.UNKNOWN2);
            }

            output.put(this.UNKNOWN3);

            if (null != this.UNKNOWN4) {
                this.UNKNOWN4.write(output);
            }

            if (null != this.ATTACHED) {
                this.ATTACHED.write(output);
            }

            output.putInt(this.STACKFRAMES.size());
            this.STACKFRAMES.forEach(frame -> frame.write(output));

            if (null != this.UNKNOWN5) {
                output.put(this.UNKNOWN5);
            }
        }

        /**
         * @see ess.Element#calculateSize()
         * @return The size of the <code>Element</code> in bytes.
         */
        @Override
        public int calculateSize() {
            int sum = 2;
            sum += ID.calculateSize();
            sum += this.UNKNOWN.calculateSize();
            sum += 2;
            sum += ((this.FLAG & 0x01) == 0x01 ? 4 : 0);
            sum += 5;
            sum += (null == this.ATTACHED ? 0 : this.ATTACHED.calculateSize());
            sum += (null == this.UNKNOWN4 ? 0 : this.UNKNOWN4.calculateSize());
            sum += (null != this.UNKNOWN5 ? 1 : 0);
            int result = 0;
            for (StackFrame STACKFRAME : this.STACKFRAMES) {
                int calculateSize = STACKFRAME.calculateSize();
                result += calculateSize;
            }
            sum += result;
            return sum;
        }

        final private byte MAJORVERSION;
        final private byte MINORVERSION;
        @NotNull
        final private Variable UNKNOWN;
        final private byte FLAG;
        final private byte UNKNOWNBYTE;
        final private int UNKNOWN2;
        final private byte UNKNOWN3;
        @Nullable
        final private FragmentTask UNKNOWN4;
        @Nullable
        final private EID ATTACHED;
        @Nullable
        final private HasID ATTACHED_ELEMENT;
        @NotNull
        final private List<StackFrame> STACKFRAMES;
        @Nullable
        final private Byte UNKNOWN5;
    }
}
