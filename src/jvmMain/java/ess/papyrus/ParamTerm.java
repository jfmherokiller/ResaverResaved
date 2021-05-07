package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores a boolean.
 */
final public class ParamTerm extends Parameter {

    public ParamTerm(String val) {
        this.VALUE = Objects.requireNonNull(val);
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.TERM;
    }

    @Override
    public void write(ByteBuffer output) {
        throw new IllegalStateException("Terms can't be written.");
    }

    @Override
    public int calculateSize() {
        throw new IllegalStateException("Terms don't have a serialized size.");
    }

    @Override
    public String toValueString() {
        return this.VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.getType());
        hash = 41 * hash + Objects.hashCode(this.VALUE);
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
        final ParamTerm other = (ParamTerm) obj;
        return this.VALUE.equals(other.VALUE);
    }

    final public String VALUE;
}
