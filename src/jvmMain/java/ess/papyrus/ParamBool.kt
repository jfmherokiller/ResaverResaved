package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores a boolean.
 */
final public class ParamBool extends Parameter {

    public ParamBool(byte val) {
        this.VALUE = val;
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.BOOLEAN;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        output.put(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 2;
    }

    @NotNull
    @Override
    public String toValueString() {
        return Boolean.toString(this.VALUE != 0);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.getType());
        hash = 41 * hash + Byte.hashCode(this.VALUE);
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
        final ParamBool other = (ParamBool) obj;
        return this.VALUE == other.VALUE;
    }

    final public byte VALUE;
}
