package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

/**
 * Describe the debugging info section of a PEX file.
 */
final public class DebugInfo {

    private final PexFile pexFile;

    /**
     * Creates a DebugInfo by reading from a DataInput.
     *
     * @param input   A datainput for a Skyrim PEX file.
     * @param strings The <code>StringTable</code> for the
     *                <code>PexFile</code>.
     * @throws IOException Exceptions aren't handled.
     */
    DebugInfo(PexFile pexFile, ByteBuffer input, StringTable strings) throws IOException {
        this.pexFile = pexFile;
        this.hasDebugInfo = input.get();
        this.DEBUGFUNCTIONS = new ArrayList<>(0);
        this.PROPERTYGROUPS = new ArrayList<>(0);
        this.STRUCTORDERS = new ArrayList<>(0);

        if (this.hasDebugInfo == 0) {

        } else {
            this.modificationTime = input.getLong();

            int functionCount = Short.toUnsignedInt(input.getShort());
            this.DEBUGFUNCTIONS.ensureCapacity(functionCount);
            for (int i = 0; i < functionCount; i++) {
                this.DEBUGFUNCTIONS.add(new DebugFunction(input, strings));
            }

            if (pexFile.GAME.isFO4()) {
                int propertyCount = Short.toUnsignedInt(input.getShort());
                this.PROPERTYGROUPS.ensureCapacity(propertyCount);
                for (int i = 0; i < propertyCount; i++) {
                    this.PROPERTYGROUPS.add(new PropertyGroup(input, strings));
                }

                int orderCount = Short.toUnsignedInt(input.getShort());
                this.STRUCTORDERS.ensureCapacity(orderCount);
                for (int i = 0; i < orderCount; i++) {
                    this.STRUCTORDERS.add(new StructOrder(input, strings));
                }

            }
        }
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     *                     passed on.
     */
    public void write(ByteBuffer output) throws IOException {
        output.put(this.hasDebugInfo);

        if (this.hasDebugInfo != 0) {
            output.putLong(this.modificationTime);

            output.putShort((short) this.DEBUGFUNCTIONS.size());
            for (DebugFunction function : this.DEBUGFUNCTIONS) {
                function.write(output);
            }

            if (pexFile.GAME.isFO4()) {
                output.putShort((short) this.PROPERTYGROUPS.size());
                for (PropertyGroup function : this.PROPERTYGROUPS) {
                    function.write(output);
                }

                output.putShort((short) this.STRUCTORDERS.size());
                for (StructOrder function : this.STRUCTORDERS) {
                    function.write(output);
                }
            }
        }
    }

    /**
     * Removes all debug info.
     */
    public void clear() {
        this.hasDebugInfo = 0;
        this.DEBUGFUNCTIONS.clear();
        this.PROPERTYGROUPS.clear();
        this.STRUCTORDERS.clear();
    }

    /**
     * Collects all of the strings used by the DebugInfo and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    public void collectStrings(Set<TString> strings) {
        for (DebugFunction DEBUGFUNCTION : this.DEBUGFUNCTIONS) {
            DEBUGFUNCTION.collectStrings(strings);
        }
        for (PropertyGroup PROPERTYGROUP : this.PROPERTYGROUPS) {
            PROPERTYGROUP.collectStrings(strings);
        }
        for (StructOrder f : this.STRUCTORDERS) {
            f.collectStrings(strings);
        }
    }

    /**
     * @return The size of the <code>DebugInfo</code>, in bytes.
     */
    public int calculateSize() {
        int sum = 1;
        if (this.hasDebugInfo != 0) {
            sum += 8;
            int result1 = 0;
            for (DebugFunction DEBUGFUNCTION : this.DEBUGFUNCTIONS) {
                int i = DEBUGFUNCTION.calculateSize();
                result1 += i;
            }
            sum += 2 + result1;

            if (pexFile.GAME.isFO4()) {
                int sum1 = 0;
                for (PropertyGroup PROPERTYGROUP : this.PROPERTYGROUPS) {
                    int size = PROPERTYGROUP.calculateSize();
                    sum1 += size;
                }
                sum += 2 + sum1;
                int result = 0;
                for (StructOrder STRUCTORDER : this.STRUCTORDERS) {
                    int calculateSize = STRUCTORDER.calculateSize();
                    result += calculateSize;
                }
                sum += 2 + result;
            }
        }

        return sum;
    }

    /**
     * Pretty-prints the DebugInfo.
     *
     * @return A string representation of the DebugInfo.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("DEBUGINFO\n");
        for (DebugFunction function : this.DEBUGFUNCTIONS) {
            buf.append('\t').append(function).append('\n');
        }
        buf.append('\n');
        return buf.toString();
    }

    private byte hasDebugInfo;
    private long modificationTime;
    final private ArrayList<DebugFunction> DEBUGFUNCTIONS;
    final private ArrayList<PropertyGroup> PROPERTYGROUPS;
    final private ArrayList<StructOrder> STRUCTORDERS;

}
