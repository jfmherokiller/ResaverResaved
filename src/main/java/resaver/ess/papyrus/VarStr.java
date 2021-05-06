package resaver.ess.papyrus;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Variable that stores a string.
 */
final public class VarStr extends Variable {

    public VarStr(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        Objects.requireNonNull(input);
        this.VALUE = context.readTString(input);
    }

    public VarStr(String newValue, PapyrusContext context) {
        Objects.requireNonNull(newValue);
        this.VALUE = context.addTString(newValue);
    }

    public TString getValue() {
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

    @Override
    public VarType getType() {
        return VarType.STRING;
    }

    @Override
    public String toValueString() {
        //return String.format("\"%s\"", this.VALUE);
        return "\"" + this.VALUE + "\"";
    }

    @Override
    public String toString() {
        //return String.format("%s:\"%s\"", this.getType(), this.VALUE);
        return this.getType() + ":" + this.toValueString();
    }

    //final private StringTable STRINGS;
    final private TString VALUE;
}
