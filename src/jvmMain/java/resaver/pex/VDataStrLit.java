package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import static resaver.pex.DataType.STRING;

/**
 * VData that stores a string literal, for disassembly purposes.
 */
class VDataStrLit extends VData {

    public VDataStrLit(String val) {
        this.VALUE = Objects.requireNonNull(val);
    }

    @Override
    public void write(ByteBuffer output) throws IOException {
        throw new IllegalStateException("Not valid for Terms.");
    }

    @Override
    public int calculateSize() {
        throw new IllegalStateException("Not valid for Terms.");
    }

    @Override
    public void collectStrings(Set<TString> strings) {
        throw new IllegalStateException("Not valid for Terms.");
    }

    @Override
    public DataType getType() {
        return STRING;
    }

    @Override
    public String toString() {
        return ("\"" + this.VALUE + "\"").replace("\n", "\\n");
    }

    public String getValue() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.VALUE);
        return hash;
    }

    public boolean equals(VDataTerm obj) {
        if (obj == null) {
            return false;
        }
        return Objects.equals(this.VALUE, obj.VALUE);
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
        final VDataStrLit other =  (VDataStrLit) obj;
        return Objects.equals(this.VALUE, other.VALUE);
    }

    final private String VALUE;
}
