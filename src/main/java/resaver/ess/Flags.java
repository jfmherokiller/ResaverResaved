/*
 * Copyright 2016 Mark.
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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Stores binary flag fields.
 *
 * @author Mark
 */
abstract public class Flags implements Element {

    /**
     * @return An HTML representation of the <code>Flags</code>.
     */
    public String toHTML() {
        final char[] ONES = new char[]{'\u2080', '\u2081', '\u2082', '\u2083', '\u2084', '\u2085', '\u2086', '\u2087', '\u2088', '\u2089'};
        //final char[] TENS = new char[]{'\u0000', '\u2081', '\u2082', '\u2083', '\u2084', '\u2085', '\u2086', '\u2087', '\u2088', '\u2089'};
        final int BITS = 8 * this.calculateSize();
        final StringBuilder BUF = new StringBuilder();

        //BUF.append("<p style=\"display:inline-table;\">");
        //BUF.append("</p>");
        BUF.append("<code><table cellspacing=0 cellpadding=1 border=0.5 style=\"display:inline-table;\">");
        BUF.append("<tr align=center>");

        for (int i = BITS - 1; i >= 0; i--) {
            BUF.append("<td>");
            BUF.append(ONES[i / 10]);
            BUF.append(ONES[i % 10]);
            BUF.append("</td>");
        }

        BUF.append("</tr><tr align=center>");

        for (int i = BITS - 1; i >= 0; i--) {
            boolean flag = this.getFlag(i);
            BUF.append("<td><code>");
            BUF.append(flag ? '1' : '0');
            BUF.append("</code></td>");
        }

        BUF.append("</tr></table></code>");
        return BUF.toString();
    }

    /**
     * Creates a new <code>Byte</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @return The <code>Byte</code> .
     */
    @NotNull
    @Contract("_ -> new")
    static public Byte readByteFlags(ByteBuffer input) {
        return new Byte(input);
    }

    /**
     * Creates a new <code>Short</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @return The <code>Short</code> .
     */
    @NotNull
    @Contract("_ -> new")
    static public Short readShortFlags(ByteBuffer input) {
        return new Short(input);
    }

    /**
     * Creates a new <code>Int</code> by reading from a
     * <code>ByteBuffer</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @return The <code>Int</code> .
     */
    @NotNull
    @Contract("_ -> new")
    static public Int readIntFlags(ByteBuffer input) {
        return new Int(input);
    }

    /**
     * Accesses the flag at a particular index in the field.
     *
     * @param index The index of the flag.
     * @return A boolean value representing the flag.
     */
    abstract public boolean getFlag(int index);

    /**
     * Accesses the flag corresponding to a ChangeFlagConstants.
     *
     * @param flag The ChangeFlagConstants.
     * @return A boolean value representing the flag.
     */
    public boolean getFlag(@NotNull ChangeFlagConstants flag) {
        return this.getFlag(flag.getPosition());
    }

    /**
     * 8-bit array of flags.
     */
    static public class Byte extends Flags {

        public Byte(@NotNull ByteBuffer input) {
            this.FLAGS = input.get();
        }

        public Byte(byte val) {
            this.FLAGS = val;
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.put(this.FLAGS);
        }

        @Override
        public int calculateSize() {
            return 1;
        }

        @Override
        public boolean getFlag(int index) {
            if (index < 0 || index >= 8) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            return (0x1 & (this.FLAGS >>> index)) != 0;
        }

        public boolean checkMask(byte mask) {
            int i1 = (int) mask & 0xFF;
            int i2 = (int) this.FLAGS & 0xFF;
            int result = i1 & i2;
            return result != 0;
        }

        @Override
        public String toString() {
            final int BITS = 8;
            String binary = Integer.toBinaryString(this.FLAGS);
            int len = binary.length();
            return ZEROS[BITS - len] + binary;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(this.FLAGS);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Byte && ((Byte) obj).FLAGS == this.FLAGS;
        }

        final public byte FLAGS;

    }

    /**
     * 16-bit array of flags.
     */
    static public class Short extends Flags {

        public Short(ByteBuffer input) {
            Objects.requireNonNull(input);
            this.FLAGS = input.getShort();
        }

        public Short(short val) {
            this.FLAGS = val;
        }

        public boolean checkMask(short mask) {
            int result = this.FLAGS & mask;
            return result != 0;
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.putShort(this.FLAGS);
        }

        @Override
        public int calculateSize() {
            return 2;
        }

        @Override
        public boolean getFlag(int index) {
            if (index < 0 || index >= 16) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            return (0x1 & (this.FLAGS >>> index)) != 0;
        }

        @Override
        public String toString() {
            final int BITS = 16;
            final int VAL = this.FLAGS & 0xFFFF;
            String binary = Integer.toBinaryString(VAL);
            int len = binary.length();
            return ZEROS[BITS - len] + binary;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(this.FLAGS);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Short && ((Short) obj).FLAGS == this.FLAGS;
        }

        final public short FLAGS;

    }

    /**
     * 32-bit array of flags.
     */
    static public class Int extends Flags {

        public Int(ByteBuffer input) {
            Objects.requireNonNull(input);
            this.FLAGS = input.getInt();
        }

        public Int(int val) {
            this.FLAGS = val;
        }

        public boolean checkMask(short mask) {
            int result = this.FLAGS & mask;
            return result != 0;
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.putInt(this.FLAGS);
        }

        @Override
        public int calculateSize() {
            return 4;
        }

        @Override
        public boolean getFlag(int index) {
            if (index < 0 || index >= 32) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            return (0x1 & (this.FLAGS >>> index)) != 0;
        }

        @Override
        public String toString() {
            final int BITS = 32;
            String binary = Integer.toBinaryString(this.FLAGS);
            int len = binary.length();
            return ZEROS[BITS - len] + binary;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(this.FLAGS);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Int && ((Int) obj).FLAGS == this.FLAGS;
        }

        final public int FLAGS;

    }

    static final private String[] ZEROS = makeZeros();

    @NotNull
    @Contract(pure = true)
    static private String[] makeZeros() {
        String[] zeros = new String[32];
        zeros[0] = "";

        for (int i = 1; i < 32; i++) {
            zeros[i] = zeros[i - 1] + "0";
        }
        return zeros;
    }
}
