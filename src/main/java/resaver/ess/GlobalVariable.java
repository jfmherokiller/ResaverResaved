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
import resaver.ess.papyrus.EID;

/**
 *
 * @author Mark
 */
public class GlobalVariable implements Element {

    /**
     * Creates a new <code>GlobalVariable</code>.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     */
    public GlobalVariable(ByteBuffer input, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        this.REFID = context.readRefID(input);
        this.value = input.getFloat();
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer) 
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        this.REFID.write(output);
        output.putFloat(this.value);
    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        return 4 + this.REFID.calculateSize();
    }

    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();

        if (this.REFID.getName() != null) {
            BUF.append(this.REFID.toString());
        } else {
            BUF.append("NAME MISSING");
        }

        BUF.append(" (");

        if (this.REFID.getPLUGIN() != null) {
            BUF.append(this.REFID.getPLUGIN().NAME);
            BUF.append(":");
        }

        if (this.REFID.getType() == RefID.Type.FORMIDX) {
            BUF.append(EID.Companion.pad8(this.REFID.getFORMID()));
        } else {
            BUF.append(this.REFID.toString());
        }

        BUF.append(") = ");
        BUF.append(this.value);
        return BUF.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.REFID);
        hash = 53 * hash + Float.floatToIntBits(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GlobalVariable other = (GlobalVariable) obj;
        return other.REFID.equals(this.REFID) && other.value == this.value;
    }

    /**
     *
     * @return The value of the <code>GlobalVariable</code>.
     */
    public float getValue() {
        return value;
    }

    /**
     * Sets the value of the <code>GlobalVariable</code>.
     * @param newVal The new value of the <code>GlobalVariable</code>.
     */
    public void setValue(float newVal) {
        this.value = newVal;
    }

    final private RefID REFID;
    private float value;

}
