package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Variable that stores a ref. Note to self: a ref is a pointer to a papyrus
 * element, unlike a RefID which points to a form or changeform.
 */
final public class VarRef extends VarAbstractRef {

    public VarRef(ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        super(input, context);
    }

    public VarRef(TString type, EID id, @NotNull PapyrusContext context) {
        super(type, id, context);
    }

    @NotNull
    public VarRef derive(long id, @NotNull PapyrusContext context) {
        VarRef derivative = new VarRef(this.getRefType(), this.getRef().derive(id), context);
        return derivative;
    }

    @NotNull
    @Override
    public VarType getType() {
        return VarType.REF;
    }

}
