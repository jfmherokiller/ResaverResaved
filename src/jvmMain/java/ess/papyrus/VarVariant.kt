package ess.papyrus;

import ess.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Variable that stores a Variant.
 */
final public class VarVariant extends Variable {

    public VarVariant(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        this.VALUE = read(input, context);
    }

    @NotNull
    public Variable getValue() {
        return this.VALUE;
    }

    @Override
    public int calculateSize() {
        return 1 + this.VALUE.calculateSize();
    }

    @Override
    public void write(ByteBuffer output) {
        this.getType().write(output);
        this.VALUE.write(output);
    }

    @NotNull
    @Override
    public VarType getType() {
        return VarType.VARIANT;
    }

    @Override
    public boolean hasRef() {
        return this.VALUE.hasRef();
    }

    @Override
    public boolean hasRef(EID id) {
        return this.VALUE.hasRef(id);
    }

    @Nullable
    @Override
    public EID getRef() {
        return this.VALUE.getRef();
    }

    @Nullable
    @Override
    public GameElement getReferent() {
        return this.VALUE.getReferent();
    }

    @Override
    public String toValueString() {
        return this.VALUE.toValueString();
    }

    @Override
    public String toHTML(Element target) {
        return String.format("%s[%s]", this.getType(), this.VALUE.toHTML(target));
    }

    @NotNull
    @Override
    public String toString() {
        return this.getType() + ":" + this.VALUE;
    }

    @NotNull
    final private Variable VALUE;
}
