package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;

import static resaver.pex.DataType.FLOAT;

/**
 * VData that stores a float.
 */
public class VDataFlt extends VData {

    VDataFlt(float val) {
        this.VALUE = val;
    }

    @Override
    public void write(ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
        output.putFloat(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @Override
    public DataType getType() {
        return FLOAT;
    }

    @Override
    public String toString() {
        return String.format("%g", this.VALUE);
    }

    public float getValue() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Float.hashCode(this.VALUE);
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
        final VDataFlt other = (VDataFlt) obj;
        return this.VALUE == other.VALUE;
    }

    final private float VALUE;
}
