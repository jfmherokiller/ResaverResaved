/*
 * Copyright 2018 Mark.
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
package resaver.pex;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import resaver.IString;
import resaver.pex.StringTable.TString;

/**
 * Describes the debugging information for a property group.
 *
 */
final class StructOrder {

    /**
     * Creates a DebugFunction by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the <code>PexFile</code>.
     * @throws IOException Exceptions aren't handled.
     */
    StructOrder(ByteBuffer input, StringTable strings) throws IOException {
        this.OBJECTNAME = strings.read(input);
        this.ORDERNAME = strings.read(input);

        int nameCount = Short.toUnsignedInt(input.getShort());
        this.NAMES = new ArrayList<>(nameCount);
        for (int i = 0; i < nameCount; i++) {
            this.NAMES.add(strings.read(input));
        }
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    void write(ByteBuffer output) throws IOException {
        this.OBJECTNAME.write(output);
        this.ORDERNAME.write(output);
        output.putShort((short) this.NAMES.size());
        for (TString prop : this.NAMES) {
            prop.write(output);
        }
    }

    /**
     * Collects all of the strings used by the DebugFunction and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<StringTable.TString> strings) {
        strings.add(this.OBJECTNAME);
        strings.add(this.ORDERNAME);
        strings.addAll(this.NAMES);
    }

    /**
     * Generates a qualified name for the object of the form "OBJECT.FUNCTION".
     *
     * @return A qualified name.
     *
     */
    public IString getFullName() {
        return IString.format("%s.%s", this.OBJECTNAME, this.ORDERNAME);
    }

    /**
     * @return The size of the <code>StructOrder</code>, in bytes.
     *
     */
    public int calculateSize() {
        return 6 + 2 * this.NAMES.size();
    }
    
    /**
     * Pretty-prints the DebugFunction.
     *
     * @return A string representation of the DebugFunction.
     */
    @Override
    public String toString() {
        return String.format("%s.%s []", this.OBJECTNAME, this.ORDERNAME, this.NAMES.toString());
    }

    final private TString OBJECTNAME;
    final private TString ORDERNAME;
    private final List<TString> NAMES;

}
