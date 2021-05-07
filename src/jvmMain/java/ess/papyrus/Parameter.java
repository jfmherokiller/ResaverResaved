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

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.nio.ByteBuffer;

/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
public abstract class Parameter implements PapyrusElement {

    /**
     * Creates a new <code>Parameter</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>Parameter</code>.
     * @throws PapyrusFormatException
     */
    @NotNull
    static public Parameter read(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        ParamType TYPE = ParamType.read(input);
        switch (TYPE) {
            case NULL:
                return new ParamNull();
            case IDENTIFIER:
                TString id = context.readTString(input);
                return new ParamID(id);
            case STRING:
                TString str = context.readTString(input);
                return new ParamStr(str);
            case INTEGER:
                int i = input.getInt();
                return new ParamInt(i);
            case FLOAT:
                float f = input.getFloat();
                return new ParamFlt(f);
            case BOOLEAN:
                byte b = input.get();
                return new ParamBool(b);
            case TERM:
                throw new IllegalStateException("Terms cannot be read.");
            case UNKNOWN8:
                TString u8 = context.readTString(input);
                return new ParamUnk8(u8);
            default:
                throw new PapyrusFormatException("Illegal Parameter type: " + TYPE);
        }

    }

    /**
     * Creates a term, a label for doing substitutions.
     *
     * @param value
     * @return
     */
    @NotNull
    static public Parameter createTerm(String value) {
        return new ParamTerm(value);
    }

    /**
     * @return The type of the parameter.
     */
    @NotNull
    abstract public ParamType getType();

    /**
     * @return A flag indicating if the parameter is an identifier to a temp
     * variable.
     */
    public boolean isTemp() {
        return false;
    }

    /**
     * @return A flag indicating if the parameter is an Autovariable.
     */
    public boolean isAutovar() {
        return false;
    }

    /**
     * @return A flag indicating if the parameter is an None variable.
     */
    public boolean isNonevar() {
        return false;
    }

    /**
     * @return Returns the identifier value of the <code>Parameter</code>, if
     * possible.
     */
    public TString getIDValue() {
        if (this instanceof ParamID) {
            return ((ParamID) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Returns the string value of the <code>Parameter</code>, if
     * possible.
     */
    public TString getTStrValue() {
        if (this instanceof ParamStr) {
            return ((ParamStr) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Returns the integer value of the <code>Parameter</code>, if
     * possible.
     */
    public int getIntValue() {
        if (this instanceof ParamInt) {
            return ((ParamInt) this).VALUE;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Short string representation.
     */
    abstract public String toValueString();

    /**
     * An appropriately parenthesized string form of the parameter.
     *
     * @return
     */
    public String paren() {
        if (this.getType() == ParamType.TERM) {
            return "(" + this.toValueString() + ")";
        } else {
            return this.toValueString();
        }
    }

    /**
     * @return String representation.
     */
    @NotNull
    @Override
    public String toString() {
        return this.getType() + ":" + this.toValueString();
    }

    static final Predicate<String> TEMP_PATTERN = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> NONE_PATTERN = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> AUTOVAR_PATTERN = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate();

}
