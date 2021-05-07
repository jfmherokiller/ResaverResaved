package ess.papyrus;

import ess.Element;
import ess.Linkable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * ABT for a variable that stores some type of ref.
 */
abstract class VarAbstractRef extends Variable {

    public VarAbstractRef(@NotNull ByteBuffer input, @NotNull PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        this.REFTYPE = context.readTString(input);
        this.REF = context.readEID(input);
        this.REFERENT = context.findReferrent(this.REF);
    }

    public VarAbstractRef(TString type, EID id, @NotNull PapyrusContext context) {
        this.REF = Objects.requireNonNull(id);
        this.REFTYPE = Objects.requireNonNull(type);
        this.REFERENT = context.findReferrent(this.REF);
    }

    public boolean isNull() {
        return this.REF.isZero();
    }

    public TString getRefType() {
        return this.REFTYPE;
    }

    @Override
    public boolean hasRef() {
        return true;
    }

    @Override
    public boolean hasRef(EID id) {
        return Objects.equals(this.REF, id);
    }

    @Override
    public EID getRef() {
        return this.REF;
    }

    @Nullable
    @Override
    public GameElement getReferent() {
        return this.REFERENT;
    }

    @Override
    public int calculateSize() {
        int sum = 1;
        sum += this.REFTYPE.calculateSize();
        sum += this.REF.calculateSize();
        return sum;
    }

    @Override
    public void write(ByteBuffer output) {
        this.getType().write(output);
        this.REFTYPE.write(output);
        this.REF.write(output);
    }

    /**
     * @return
     * @see Variable#toTypeString()
     */
    @NotNull
    @Override
    public String toTypeString() {
        return this.REFTYPE.toString();
    }

    @NotNull
    @Override
    public String toValueString() {
        return this.getReferent() != null
                ? this.REFERENT.toString()
                : this.REF.toString() + " (" + this.REFTYPE + ")";
    }

    @Override
    public String toHTML(Element target) {
        if (null != this.REFERENT) {
            final String REFLINK = this.REFERENT.toHTML(this);
            return String.format("%s : %s", this.getType(), REFLINK);
        } else {
            final String DEFLINK = Linkable.makeLink("script", this.REFTYPE, this.REFTYPE.toString());
            return String.format("%s : %s (%s)", this.getType(), this.REF, DEFLINK);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return this.getType() + " : " + this.toValueString();
    }

    final private TString REFTYPE;
    final private EID REF;
    @Nullable
    final private GameElement REFERENT;
}
