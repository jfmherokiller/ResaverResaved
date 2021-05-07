package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores a float.
 */
final public class ParamFlt extends Parameter {

    public ParamFlt(float val) {
        this.VALUE = val;
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.FLOAT;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        output.putFloat(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 5;
    }

    @Override
    public String toValueString() {
        return Float.toString(this.VALUE);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.getType());
        hash = 41 * hash + Float.hashCode(this.VALUE);
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
        final ParamFlt other = (ParamFlt) obj;
        return this.VALUE == other.VALUE;
    }

    final public float VALUE;
}
