package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import static resaver.pex.DataType.STRING;

/**
 * VData that stores a string.
 */
public class VDataStr extends VData {

    VDataStr(TString val) {
        this.VALUE = Objects.requireNonNull(val);
    }

    @Override
    public void write(ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
        this.VALUE.write(output);
    }

    @Override
    public int calculateSize() {
        return 3;
    }

    @Override
    public void collectStrings(Set<TString> strings) {
        strings.add(this.VALUE);
    }

    @Override
    public DataType getType() {
        return STRING;
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", this.VALUE);
    }

    public TString getString() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.VALUE.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final VDataStr other = (VDataStr) obj;
        return Objects.equals(this.VALUE, other.VALUE);
    }

    final private TString VALUE;
}
