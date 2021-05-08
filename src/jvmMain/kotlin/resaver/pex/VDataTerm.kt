package resaver.pex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import static resaver.pex.DataType.IDENTIFIER;

/**
 * VData that stores a "term", for disassembly purposes.
 */
public class VDataTerm extends VData {

    public VDataTerm(String val) {
        this.VALUE = Objects.requireNonNull(val);
        this.PVALUE = "(" + this.VALUE + ")";
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
        return IDENTIFIER;
    }

    @Override
    public String toString() {
        return this.VALUE;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final VDataTerm other = (VDataTerm) obj;
        return Objects.equals(this.VALUE, other.VALUE);
    }

    @Override
    public String paren() {
        return this.PVALUE;
    }

    final public String VALUE;
    @NotNull
    final private String PVALUE;
}
