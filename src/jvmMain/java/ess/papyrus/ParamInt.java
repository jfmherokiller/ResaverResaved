package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores an integer.
 */
final public class ParamInt extends Parameter {

    public ParamInt(int val) {
        this.VALUE = val;
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.INTEGER;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        output.putInt(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @NotNull
    @Override
    public String toValueString() {
        return Integer.toString(this.VALUE);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.getType());
        hash = 41 * hash + Integer.hashCode(this.VALUE);
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
        final ParamInt other = (ParamInt) obj;
        return this.VALUE == other.VALUE;
    }

    final public int VALUE;
}
