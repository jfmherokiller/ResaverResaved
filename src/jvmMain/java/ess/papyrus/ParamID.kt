package ess.papyrus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An opcode parameter that stores an identifier.
 */
final public class ParamID extends Parameter {

    public ParamID(TString val) {
        this.VALUE = Objects.requireNonNull(val);
    }

    @NotNull
    @Override
    public ParamType getType() {
        return ParamType.IDENTIFIER;
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

    /**
     * @return String representation.
     */
    @NotNull
    @Override
    public String toValueString() {
        return this.VALUE.toString();
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
        final ParamID other = (ParamID) obj;
        return this.VALUE.equals(other.VALUE);
    }

    @Override
    public boolean isTemp() {
        return TEMP_PATTERN.test(this.VALUE.toString())
                && !AUTOVAR_PATTERN.test(this.VALUE.toString())
                && !NONE_PATTERN.test(this.VALUE.toString());
    }

    /**
     * @return A flag indicating if the parameter is an Autovariable.
     */
    @Override
    public boolean isAutovar() {
        return AUTOVAR_PATTERN.test(this.VALUE.toString());
    }

    /**
     * @return A flag indicating if the parameter is an None variable.
     */
    @Override
    public boolean isNonevar() {
        return NONE_PATTERN.test(this.VALUE.toString());
    }

    final public TString VALUE;
}
