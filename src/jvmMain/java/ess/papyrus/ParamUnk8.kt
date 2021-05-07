package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores a variant.
 */
final public class ParamUnk8 extends Parameter {

    public ParamUnk8(TString val) {
        this.VALUE = Objects.requireNonNull(val);
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.UNKNOWN8;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
        this.VALUE.write(output);
    }

    @Override
    public int calculateSize() {
        return 1 + this.VALUE.calculateSize();
    }

    @NotNull
    @Override
    public String toValueString() {
        return this.VALUE.toString().replace("\n", "\\n");
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
        final ParamUnk8 other = (ParamUnk8) obj;
        return this.VALUE.equals(other.VALUE);
    }

    final public TString VALUE;
}
