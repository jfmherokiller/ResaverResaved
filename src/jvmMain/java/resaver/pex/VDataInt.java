package resaver.pex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static resaver.pex.DataType.INTEGER;

/**
 * VData that stores an integer.
 */
public class VDataInt extends VData {

    VDataInt(int val) {
        this.VALUE = val;
    }

    @Override
    public void write(@NotNull ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
        output.putInt(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @Override
    public DataType getType() {
        return INTEGER;
    }

    @Override
    public String toString() {
        return String.format("%d", this.VALUE);
    }

    public int getValue() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Integer.hashCode(this.VALUE);
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
        final VDataInt other = (VDataInt) obj;
        return this.VALUE == other.VALUE;
    }

    final private int VALUE;
}
