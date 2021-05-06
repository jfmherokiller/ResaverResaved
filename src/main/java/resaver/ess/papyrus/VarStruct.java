package resaver.ess.papyrus;

import java.nio.ByteBuffer;

/**
 * Variable that stores an UNKNOWN7.
 */
final public class VarStruct extends VarAbstractRef {

    public VarStruct(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        super(input, context);
    }

    public VarStruct(TString type, EID id, PapyrusContext context) {
        super(type, id, context);
    }

    public VarStruct derive(long id, PapyrusContext context) {
        return new VarStruct(this.getRefType(), this.getRef().derive(id), context);
    }

    @Override
    public VarType getType() {
        return VarType.STRUCT;
    }

}
