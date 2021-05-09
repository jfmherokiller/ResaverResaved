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
import resaver.IString;
import java.nio.ByteBuffer;
import resaver.Analysis;
import ess.ESS;

/**
 * Describes a function message data in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
public class FunctionMessageData implements PapyrusElement, AnalyzableElement, HasVariables {

    /**
     * Creates a new <code>FunctionMessageData</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param parent The parent of the message.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    public FunctionMessageData(@NotNull ByteBuffer input, PapyrusElement parent, @NotNull PapyrusContext context) throws PapyrusFormatException, PapyrusElementException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        this.UNKNOWN = input.get();
        this.SCRIPTNAME = context.readTString(input);
        this.SCRIPT = context.findScript(this.SCRIPTNAME);

        this.EVENT = context.readTString(input);
        this.UNKNOWNVAR = Variable.read(input, context);

        try {
            int count = input.getInt();
            this.VARIABLES = Variable.readList(input, count, context);
        } catch (ListException ex) {
            throw new PapyrusElementException("Failed to read FunctionMessage variables.", ex, this);
        }
    }

    /**
     * @see ess.Element#write(resaver.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(@NotNull ByteBuffer output) {
        output.put(this.UNKNOWN);
        this.SCRIPTNAME.write(output);
        this.EVENT.write(output);
        this.UNKNOWNVAR.write(output);
        output.putInt(this.VARIABLES.size());
        this.VARIABLES.forEach(var -> var.write(output));
    }

    /**
     * @see ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 1;
        sum += this.SCRIPTNAME.calculateSize();
        sum += this.EVENT.calculateSize();
        sum += this.UNKNOWNVAR.calculateSize();
        sum += 4;
        int result = 0;
        for (Variable VARIABLE : this.VARIABLES) {
            int calculateSize = VARIABLE.calculateSize();
            result += calculateSize;
        }
        sum += result;
        return sum;
    }

    /**
     * @return The script name field.
     */
    public TString getScriptName() {
        return this.SCRIPTNAME;
    }

    /**
     * @return The script field.
     */
    public Script getScript() {
        return this.SCRIPT;
    }

    /**
     * @return The event field.
     */
    public TString getEvent() {
        return this.EVENT;
    }

    /**
     * @see HasVariables#getVariables()
     * @return
     */
    @Override
    public List<Variable> getVariables() {
        return this.VARIABLES == null 
                ? Collections.emptyList() 
                : Collections.unmodifiableList(this.VARIABLES);
    }

    /**
     * @see HasVariables#getDescriptors() 
     * @return 
     */
    @Override
    public List<MemberDesc> getDescriptors() {
        return this.getScript().getExtendedMembers();
    }

    /**
     * @see HasVariables#setVariable(int, ess.papyrus.Variable)
     * @param index
     * @param newVar 
     */
    @Override
    public void setVariable(int index, Variable newVar) {
        if (this.VARIABLES == null) {
            throw new NullPointerException("The variable list is missing.");
        }
        if (index <= 0 || index >= this.VARIABLES.size()) {
            throw new IllegalArgumentException("Invalid variable index: " + index);
        }
        
        this.VARIABLES.set(index, newVar);
    }

    /**
     * @return The qualified name of the function being executed.
     */
    @NotNull
    public IString getFName() {
        IString fname = IString.format("%s.%s", this.SCRIPTNAME, this.EVENT);
        return fname;
    }

    /**
     * @return String representation.
     */
    @NotNull
    @Override
    public String toString() {
        if (this.isUndefined()) {
            return "#" + this.SCRIPTNAME + "#." + this.EVENT;
        } else {
            return this.SCRIPTNAME + "." + this.EVENT;
        }
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(@Nullable resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();
        if (null != this.SCRIPT) {
            BUILDER.append(String.format("<html><h3>FUNCTIONMESSAGEDATA of %s</h3>", this.SCRIPT.toHTML(null)));
        } else {
            BUILDER.append(String.format("<html><h3>FUNCTIONMESSAGEDATA of %s</h3>", this.SCRIPTNAME));
        }

        if (null != analysis) {
            SortedSet<String> providers = analysis.SCRIPT_ORIGINS.get(this.SCRIPTNAME.toIString());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>This message probably came from \"%s\".</p>", probablyProvider));
                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }

        BUILDER.append("<p>");

        if (null != this.SCRIPT) {
            BUILDER.append(String.format("Script: %s<br/>", this.SCRIPT.toHTML(null)));
        } else {
            BUILDER.append(String.format("Script: %s<br/>", this.SCRIPTNAME));
        }

        BUILDER.append(String.format("Event: %s<br/>", this.EVENT));
        BUILDER.append(String.format("Unknown: %02x<br/>", this.UNKNOWN));

        if (null != this.UNKNOWNVAR) {
            BUILDER.append(String.format("Unknown variable: %s<br/>", this.UNKNOWNVAR.toHTML(null)));
        } else {
            BUILDER.append("Unknown variable: null<br/>");
        }

        BUILDER.append(String.format("%d function variables.<br/>", this.VARIABLES.size()));
        BUILDER.append("</p>");
        BUILDER.append("</html>");
        return BUILDER.toString();

    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis, resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(@NotNull Analysis analysis, String mod) {
        Objects.requireNonNull(analysis);
        Objects.requireNonNull(mod);

        final SortedSet<String> OWNERS = analysis.SCRIPT_ORIGINS.get(this.SCRIPTNAME.toIString());
        if (null == OWNERS) {
            return false;
        }
        return OWNERS.contains(mod);
    }

    /**
     * @return A flag indicating if the <code>FunctionMessageData</code> is
     * undefined.
     *
     */
    public boolean isUndefined() {
        if (null != this.SCRIPT) {
            return this.SCRIPT.isUndefined();
        }

        return !Script.NATIVE_SCRIPTS.contains(this.SCRIPTNAME.toWString());
    }

    final private byte UNKNOWN;
    final private TString SCRIPTNAME;
    final private Script SCRIPT;
    final private TString EVENT;
    @NotNull
    final private Variable UNKNOWNVAR;
    @NotNull
    final private List<Variable> VARIABLES;
}
