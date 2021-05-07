package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static resaver.pex.DataType.IDENTIFIER;

/**
 * VData that stores an identifier.
 */
public class VDataID extends VData {

    VDataID(TString val) {
        this.value = Objects.requireNonNull(val);
    }

    @Override
    public void write(ByteBuffer output) throws IOException {
        output.put((byte) this.getType().ordinal());
        this.value.write(output);
    }

    @Override
    public int calculateSize() {
        return 3;
    }

    @Override
    public void collectStrings(Set<TString> strings) {
        strings.add(this.value);
    }

    @Override
    public DataType getType() {
        return IDENTIFIER;
    }

    @Override
    public String toString() {
        //return String.format("ID[%s]", this.VALUE);
        return this.value.toString();
    }

    public TString getValue() {
        return this.value;
    }

    void setValue(TString val) {
        this.value = Objects.requireNonNull(val);
    }

    public boolean isTemp() {
        return TEMP_PATTERN.test(this.value.toString())
                && !AUTOVAR_PATTERN.test(this.value.toString())
                && !NONE_PATTERN.test(this.value.toString());
    }

    public boolean isAutovar() {
        return AUTOVAR_PATTERN.test(this.value.toString());
    }

    public boolean isNonevar() {
        return NONE_PATTERN.test(this.value.toString());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.value.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final VDataID other = (VDataID) obj;
        return Objects.equals(this.value, other.value);
    }

    private TString value;
    static final Predicate<String> TEMP_PATTERN = Pattern.compile("^::.+$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> NONE_PATTERN = Pattern.compile("^::NoneVar$", Pattern.CASE_INSENSITIVE).asPredicate();
    static final Predicate<String> AUTOVAR_PATTERN = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE).asPredicate();
}
