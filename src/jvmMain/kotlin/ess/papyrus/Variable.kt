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
package ess.papyrus

import ess.Element
import ess.Linkable
import ess.papyrus.VarType.Companion.read
import resaver.ListException
import java.nio.ByteBuffer


/**
 * Describes a variable in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
abstract class Variable : PapyrusElement, Linkable {
    /**
     * @return The EID of the papyrus element.
     */
    abstract val type: VarType

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String? {
        return this.toString()
    }

    /**
     * @return A string representation that only includes the type field.
     */
    open fun toTypeString(): String? {
        return type.toString()
    }

    /**
     * Checks if the variable stores a reference to something.
     *
     * @return
     */
    open fun hasRef(): Boolean {
        return false
    }

    /**
     * Checks if the variable stores a reference to a particular something.
     *
     * @param id
     * @return
     */
    open fun hasRef(id: EID?): Boolean {
        return false
    }

    /**
     * Returns the variable's refid or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    open val ref: EID?
        get() = null

    /**
     * Returns the variable's REFERENT or null if the variable isn't a reference
     * type.
     *
     * @return
     */
    open val referent: GameElement?
        get() = null

    /**
     * @return A string representation that doesn't include the type field.
     */
    abstract fun toValueString(): String? /*static final public class Array6 extends Variable {

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

    companion object {
        /**
         * Creates a new `List` of `Variable` by reading from
         * a `ByteBuffer`.
         *
         * @param input The input stream.
         * @param count The number of variables.
         * @param context The `PapyrusContext` info.
         * @return The new `List` of `Variable`.
         * @throws ListException
         */
        @JvmStatic
        @Throws(ListException::class)
        fun readList(input: ByteBuffer, count: Int, context: PapyrusContext): List<Variable> {
            val VARIABLES: MutableList<Variable> = ArrayList(count)
            for (i in 0 until count) {
                try {
                    val `var` = read(input, context)
                    VARIABLES.add(`var`)
                } catch (ex: PapyrusFormatException) {
                    throw ListException(i, count, ex)
                }
            }
            return VARIABLES
        }

        /**
         * Creates a new `Variable` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @return The new `Variable`.
         * @throws PapyrusFormatException
         */
        @JvmStatic
        @Throws(PapyrusFormatException::class)
        fun read(input: ByteBuffer, context: PapyrusContext): Variable {
            val VarTYPE = read(input)
            return when (VarTYPE) {
                VarType.NULL -> VarNull(input)
                VarType.REF -> VarRef(input, context)
                VarType.STRING -> VarStr(input, context)
                VarType.INTEGER -> VarInt(input)
                VarType.FLOAT -> VarFlt(input)
                VarType.BOOLEAN -> VarBool(input)
                VarType.VARIANT -> VarVariant(input, context)
                VarType.STRUCT -> VarStruct(input, context)
                VarType.REF_ARRAY, VarType.STRING_ARRAY, VarType.INTEGER_ARRAY, VarType.FLOAT_ARRAY, VarType.BOOLEAN_ARRAY, VarType.VARIANT_ARRAY, VarType.STRUCT_ARRAY -> VarArray(
                    VarTYPE,
                    input,
                    context
                )
                else -> throw PapyrusException("Illegal typecode for variable", null, null)
            }
        }
    }
}