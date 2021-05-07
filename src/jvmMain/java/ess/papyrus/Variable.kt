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
import java.nio.ByteBuffer;
import java.util.Objects;
import ess.Element;
import ess.Linkable;

/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
abstract public class Variable implements PapyrusElement, Linkable {

    /**
     * Creates a new <code>List</code> of <code>Variable</code> by reading from
     * a <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param count The number of variables.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>List</code> of <code>Variable</code>.
     * @throws ListException
     */
    @NotNull
    static public java.util.List<Variable> readList(@NotNull ByteBuffer input, int count, @NotNull PapyrusContext context) throws ListException {
        final java.util.List<Variable> VARIABLES = new java.util.ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            try {
                Variable var = Variable.read(input, context);
                VARIABLES.add(var);
            } catch (PapyrusFormatException ex) {
                throw new ListException(i, count, ex);
            }
        }

        return VARIABLES;
    }

    /**
     * Creates a new <code>Variable</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @return The new <code>Variable</code>.
     * @throws PapyrusFormatException
     */
    @NotNull
    static public Variable read(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);

        final VarType VarTYPE = VarType.read(input);

        switch (VarTYPE) {
            case NULL:
                return new VarNull(input);
            case REF:
                return new VarRef(input, context);
            case STRING:
                return new VarStr(input, context);
            case INTEGER:
                return new VarInt(input);
            case FLOAT:
                return new VarFlt(input);
            case BOOLEAN:
                return new VarBool(input);
            case VARIANT:
                return new VarVariant(input, context);
            case STRUCT:
                return new VarStruct(input, context);
            case REF_ARRAY:
            case STRING_ARRAY:
            case INTEGER_ARRAY:
            case FLOAT_ARRAY:
            case BOOLEAN_ARRAY:
            case VARIANT_ARRAY:
            case STRUCT_ARRAY:
                return new VarArray(VarTYPE, input, context);
            default:
                throw new PapyrusException("Illegal typecode for variable", null, null);
        }
    }

    /**
     * @return The EID of the papyrus element.
     */
    abstract public VarType getType();

    /**
     * @see ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Nullable
    @Override
    public String toHTML(Element target) {
        return this.toString();
    }

    /**
     * @return A string representation that only includes the type field.
     */
    public String toTypeString() {
        return this.getType().toString();
    }

    /**
     * Checks if the variable stores a reference to something.
     *
     * @return
     */
    public boolean hasRef() {
        return false;
    }

    /**
     * Checks if the variable stores a reference to a particular something.
     *
     * @param id
     * @return
     */
    public boolean hasRef(EID id) {
        return false;
    }

    /**
     * Returns the variable's refid or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    @Nullable
    public EID getRef() {
        return null;
    }

    /**
     * Returns the variable's REFERENT or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    @Nullable
    public GameElement getReferent() {
        return null;
    }

    /**
     * @return A string representation that doesn't include the type field.
     */
    abstract public String toValueString();


    /*static final public class Array6 extends Variable {

        public Array6(ByteBuffer input, StringTable strtab) throws IOException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(ctx);
            this.VALUE = new byte[8];
            int read = input.read(this.VALUE);
            assert read == 8;
        }

        public byte[] getValue() {
            return this.VALUE;
        }

        @Override
        public int calculateSize() {
            return 1 + VALUE.length;
        }

        @Override
        public void write(ByteBuffer output) throws IOException {
            this.getType().write(output);
            output.write(this.VALUE);
        }

        @Override
        public Type getType() {
            return Type.UNKNOWN6_ARRAY;
        }

        @Override
        public String toValueString() {
            //return String.format("%d", this.VALUE);
            return Arrays.toString(this.VALUE);
        }

        @Override
        public String toString() {
            //return String.format("%s:%d", this.getType(), this.VALUE);
            return this.getType() + ":" + this.toValueString();
        }

        final private byte[] VALUE;
    }*/
}
