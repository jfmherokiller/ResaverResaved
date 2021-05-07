package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores Null.
 */
final public class ParamNull extends Parameter {

    public ParamNull() {
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.NULL;
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        this.getType().write(output);
    }

    @Override
    public int calculateSize() {
        return 1;
    }

    /**
     * @return String representation.
     */
    @NotNull
    @Override
    public String toValueString() {
        return "NULL";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.getType());
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
