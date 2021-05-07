package resaver.pex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static resaver.pex.DataType.NONE;

/**
 * VData that stores nothing.
 */
public class VDataNone extends VData {

    VDataNone() {
    }

    @Override
    public void write(@NotNull ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
    }

    @Override
    public int calculateSize() {
        return 1;
    }

    @Override
    public DataType getType() {
        return NONE;
    }

    @NotNull
    @Override
    public String toString() {
        return "NONE";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + VDataNone.class.hashCode();
        return hash;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else return getClass() == obj.getClass();
    }

}
