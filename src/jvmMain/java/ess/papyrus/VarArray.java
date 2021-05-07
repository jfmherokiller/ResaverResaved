package ess.papyrus;

import ess.Element;
import ess.Linkable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Variable that stores an ARRAY.
 */
final public class VarArray extends Variable {

    protected VarArray(VarType varType, ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(varType);
        Objects.requireNonNull(input);
        this.VarTYPE = varType;
        this.REFTYPE = this.VarTYPE.isRefType() ? context.readTString(input) : null;
        this.ARRAYID = context.readEID(input);
        this.ARRAY = context.findArray(this.ARRAYID);
    }

    public EID getArrayID() {
        return this.ARRAYID;
    }

    public ArrayInfo getArray() {
        return this.ARRAY;
    }

    public VarType getElementType() {
        return VarType.values()[this.VarTYPE.ordinal() - 7];
    }

    @Override
    public VarType getType() {
        return this.VarTYPE;
    }

    @Override
    public boolean hasRef() {
        return true;
    }

    @Override
    public boolean hasRef(EID id) {
        return Objects.equals(this.ARRAYID, id);
    }

    @Override
    public EID getRef() {
        return this.getArrayID();
    }

    @Nullable
    @Override
    public GameElement getReferent() {
        return null;
    }

    @Override
    public void write(ByteBuffer output) {
        this.getType().write(output);

        if (this.VarTYPE.isRefType()) {
            this.REFTYPE.write(output);
        }

        this.ARRAYID.write(output);
    }

    @Override
    public int calculateSize() {
        int sum = 1;
        sum += (this.VarTYPE.isRefType() ? this.REFTYPE.calculateSize() : 0);
        sum += this.ARRAYID.calculateSize();
        return sum;
    }

    @NotNull
    @Override
    public String toTypeString() {
        if (null == this.ARRAY) {
            if (this.VarTYPE.isRefType()) {
                return "" + this.REFTYPE + "[ ]";
            } else {
                return this.getElementType() + "[ ]";
            }
        }

        if (this.VarTYPE.isRefType()) {
            return this.VarTYPE + ":" + "" + this.REFTYPE + "[" + this.ARRAY.getLength() + "]";
        } else {
            return this.VarTYPE + ":" + this.getElementType() + "[" + this.ARRAY.getLength() + "]";
        }
    }

    @Override
    public String toValueString() {
        if (null != this.getArray()) {
            return "" + this.ARRAYID + ": " + this.getArray().toValueString();
        } else {
            return this.ARRAYID.toString();
        }
    }

    @Override
    public String toHTML(Element target) {
        final String LINK = Linkable.makeLink("array", this.ARRAYID, this.ARRAYID.toString());
        return String.format("%s : %s", this.toTypeString(), LINK);
    }

    @NotNull
    @Override
    public String toString() {
        return this.toTypeString() + " " + this.ARRAYID;
    }

    final private VarType VarTYPE;
    final private EID ARRAYID;
    @Nullable
    final private TString REFTYPE;
    final private ArrayInfo ARRAY;
}
