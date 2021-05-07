package resaver.pex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static resaver.pex.DataType.BOOLEAN;

/**
 * VData that stores a boolean.
 */
public class VDataBool extends VData {

    VDataBool(boolean val) {
        this.VALUE = val;
    }

    @Override
    public void write(@NotNull ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
        output.put(this.VALUE ? (byte) 1 : (byte) 0);
    }

    @Override
    public int calculateSize() {
        return 2;
    }

    @Override
    public DataType getType() {
        return BOOLEAN;
    }

    @Override
    public String toString() {
        return String.format("%b", this.VALUE);
    }

    public boolean getValue() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Boolean.hashCode(this.VALUE);
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
        final VDataBool other = (VDataBool) obj;
        return this.VALUE == other.VALUE;
    }

    final private boolean VALUE;
}
