package ess.papyrus;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Variable that stores an UNKNOWN7.
 */
final public class VarStruct extends VarAbstractRef {

    public VarStruct(ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        super(input, context);
    }

    public VarStruct(TString type, EID id, @NotNull PapyrusContext context) {
        super(type, id, context);
    }

    @NotNull
    public VarStruct derive(long id, @NotNull PapyrusContext context) {
        return new VarStruct(this.getRefType(), this.getRef().derive(id), context);
    }

    @NotNull
    @Override
    public VarType getType() {
        return VarType.STRUCT;
    }

}
