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

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Describes the ChangeForm flags for a ChangeForm.
 *
 * @author Mark Fairchild
 */
final public class ChangeFormFlags implements Element {

    /**
     * Creates a new <code>ChangeFormFlags</code>.
     *
     * @param input The input stream.
     */
    public ChangeFormFlags(ByteBuffer input) {
        this.FLAG = input.getInt();
        this.UNKNOWN = input.getShort();
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer) 
     * @param output The output stream.
     */
    @Override
    public void write(@NotNull ByteBuffer output) {
        Objects.requireNonNull(output);
        output.putInt(this.FLAG);
        output.putShort(this.UNKNOWN);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 6;
    }

    /**
     * @return The flag field.
     */
    public int getFlags() {
        return this.FLAG;
    }

    /**
     * @return The unknown field.
     */
    public short getUnknown() {
        return this.UNKNOWN;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        final String S = Integer.toUnsignedString(this.FLAG, 2);

        int idx = 0;

        while (idx < 32 - S.length()) {
            if (idx > 0 && idx % 4 == 0) {
                BUF.append(' ');
            }

            BUF.append('0');
            idx++;
        }

        while (idx < 32) {
            if (idx > 0 && idx % 4 == 0) {
                BUF.append(' ');
            }
            
            BUF.append(S.charAt(idx - 32 + S.length()));
            idx++;
        }

        return BUF.toString();
    }

    final private int FLAG;
    final private short UNKNOWN;

}
