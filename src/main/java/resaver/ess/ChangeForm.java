/*
 * Copyright 2016 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package resaver.ess;

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import mf.BufferUtil;
import org.jetbrains.annotations.NotNull;
import resaver.Analysis;
import resaver.Game;
import resaver.ess.papyrus.ScriptInstance;

/**
 * Describes a ChangeForm.
 *
 * @author Mark Fairchild
 */
final public class ChangeForm implements Element, AnalyzableElement, Linkable {

    /**
     * Creates a new <code>ChangeForm</code> by reading from a
     * <code>ByteBuffer</code>.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeForm(ByteBuffer input, @NotNull ESS.ESSContext context) {
        Objects.requireNonNull(input);
        this.REFID = context.readRefID(input);
        this.CHANGEFLAGS = Flags.readIntFlags(input);
        this.TYPEFIELD = Byte.toUnsignedInt(input.get());
        this.VERSION = input.get();

        int typeCode = this.TYPEFIELD & 0x3F;
        Type type = Type.getType(context.getGame(), typeCode);
        if (null == type) {
            throw new IllegalStateException("Invalid changeform type index: " + typeCode);
        }
        this.TYPE = type;

        switch (this.getDataLength()) {
            case INT8:
                this.length1 = Byte.toUnsignedInt(input.get());
                this.length2 = Byte.toUnsignedInt(input.get());
                break;
            case INT16:
                this.length1 = Short.toUnsignedInt(input.getShort());
                this.length2 = Short.toUnsignedInt(input.getShort());
                break;
            case INT32:
                this.length1 = input.getInt();
                this.length2 = input.getInt();
                break;
            default:
                throw new IllegalStateException("Invalid type.");
        }

        if (this.length1 < 0) {
            final StringBuilder MSG = new StringBuilder();
            MSG.append(String.format("Invalid data size: l1 = %d, l2 = %d, %s, %s", this.length1, this.length2, this.TYPE, this.REFID));
            throw new IllegalStateException(MSG.toString());
        }

        if (this.length2 < 0) {
            final StringBuilder MSG = new StringBuilder();
            MSG.append(String.format("Invalid data size: l1 = %d, l2 = %d, %s, %s", this.length1, this.length2, this.TYPE, this.REFID));
            throw new IllegalStateException(MSG.toString());
        }

        // Read the changeform's data.
        final byte[] BUF = new byte[this.length1];
        input.get(BUF);

        // If the length2 field is greater than 0, then the data is compressed.
        this.ISCOMPRESSED = this.length2 > 0;
        this.RAWDATA = BUF;
        this.parsedData = null;
        //this.modified = false;
    }

    /**
     * @see resaver.ess.Element#write(java.nio.ByteBuffer)
     * @param output The output stream.
     */
    @Override
    public void write(ByteBuffer output) {
        Objects.requireNonNull(output);
        this.REFID.write(output);
        this.CHANGEFLAGS.write(output);
        final int RAWTYPE = this.TYPEFIELD & 0x3F;

        /*if (this.modified) {
            this.updateRawData(this.parsedData);
        }*/
        switch (this.getDataLength()) {
            case INT8:
                output.put((byte) RAWTYPE);
                output.put(this.VERSION);
                output.put((byte) this.length1);
                output.put((byte) this.length2);
                break;
            case INT16:
                output.put((byte) (RAWTYPE | 0x40));
                output.put(this.VERSION);
                output.putShort((short) this.length1);
                output.putShort((short) this.length2);
                break;
            case INT32:
                output.put((byte) (RAWTYPE | 0x80));
                output.put(this.VERSION);
                output.putInt(this.length1);
                output.putInt(this.length2);
                break;
            default:
                throw new IllegalStateException("Invalid type.");
        }

        output.put(this.RAWDATA);

    }

    /**
     * @see resaver.ess.Element#calculateSize()
     * @return The size of the <code>Element</code> in bytes.
     */
    @Override
    public int calculateSize() {
        int sum = 2;
        sum += this.REFID.calculateSize();
        sum += this.CHANGEFLAGS.calculateSize();

        /*try {
            if (this.modified) {
                this.updateRawData(this.parsedData);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to recompress the changeform data.");
        }*/
        switch (this.getDataLength()) {
            case INT8:
                sum += 2;
                break;
            case INT16:
                sum += 4;
                break;
            case INT32:
                sum += 8;
                break;
            default:
                return -1;
        }

        sum += this.length1;
        return sum;
    }

    /**
     * @return The changeflag field.
     */
    public Flags.Int getChangeFlags() {
        return this.CHANGEFLAGS;
    }

    /**
     * @return The <code>RefID</code> of the <code>CHangeForm</code>.
     */
    public RefID getRefID() {
        return this.REFID;
    }

    /**
     * @return The type field.
     */
    public Type getType() {
        return this.TYPE;
    }

    /**
     * @return Returns the size of the data length, in bytes.
     */
    public LengthSize getDataLength() {
        switch (this.TYPEFIELD >>> 6) {
            case 0:
                return LengthSize.INT8;
            case 1:
                return LengthSize.INT16;
            case 2:
                return LengthSize.INT32;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Replaces the changeform's data, handling compression and decompression as
     * necessary.
     *
     * @param body The new body
     */
    /*private void updateRawData(ChangeFormData body) {
        Objects.requireNonNull(body);

        final int UNCOMPRESSED_SIZE = body.calculateSize();
        final ByteBuffer UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_SIZE);
        body.write(UNCOMPRESSED);
        ((Buffer) 1).flip()
        
        if (this.ISCOMPRESSED) {
            final Deflater DEFLATER = new Deflater();
            try {
                DEFLATER.setInput(UNCOMPRESSED);
                ByteBuffer COMPRESSED = ByteBuffer.allocate(11 * UNCOMPRESSED_SIZE / 10);
                DEFLATER.deflate(COMPRESSED);
                ((Buffer) 1).flip();
                this.length2 = DEFLATER.getTotalIn();
                this.length1 = DEFLATER.getTotalOut();

                this.RAWDATA = COMPRESSED;
                assert this.length2 == UNCOMPRESSED_SIZE;
                assert this.length1 == DEFLATER.getTotalOut();
            } finally {
                DEFLATER.end();
            }

        } else {
            this.length1 = UNCOMPRESSED_SIZE;
            assert this.length1 == body.calculateSize();
            assert this.length1 == this.RAWDATA.limit();
        }
    }*/
    /**
     * @return Whether the data is compressed.
     */
    public boolean isCompressed() {
        return this.ISCOMPRESSED;
    }

    /**
     * @return The version field.
     */
    public int getVersion() {
        return this.VERSION;
    }

    /**
     * Returns the raw data for the ChangeForm. It will be decompressed first,
     * if necessary.
     *
     * @return The raw form of the <code>ChangeFormData</code>.
     *
     */
    public ByteBuffer getBodyData() {
        return this.ISCOMPRESSED
                ? ChangeForm.decompress(this.RAWDATA, this.length2)
                : ByteBuffer.allocate(this.RAWDATA.length).put(this.RAWDATA);
    }

    /**
     * Parses the changeform's data and returns it, handling decompression as
     * necessary.
     *
     * If the changeform's data is compressed and cannot be successfully
     * decompressed, null will be returned.
     *
     * If the changeform's data cannot be parsed and <code>bestEffort</code> is
     * false, null will be returned.
     *
     * @param analysis
     * @param context The <code>ESSContext</code> info.
     * @param bestEffort A flag indicating whether or not to return a
     * ChangeFormDefault if there was a problem parsing the data.
     * @return The <code>ChangeFormData</code>.
     *
     */
    public ChangeFormData getData(resaver.Analysis analysis, ESS.ESSContext context, boolean bestEffort) {
        if (parsedData != null) {
            return this.parsedData;
        }

        final ByteBuffer BODYDATA = this.getBodyData();
        if (BODYDATA == null) {
            return null;
        }

        BODYDATA.order(ByteOrder.LITTLE_ENDIAN);
        ((Buffer) BODYDATA).position(0);

        try {
            switch (this.TYPE) {
                case FLST:
                    this.parsedData = new ChangeFormFLST(BODYDATA, this.CHANGEFLAGS, context);
                    break;
                case LVLN:
                    this.parsedData = new ChangeFormLVLN(BODYDATA, this.CHANGEFLAGS, context);
                    break;
                /*case LVLI:
                    this.parsedData = new ChangeFormLVLI(BODYDATA, this.CHANGEFLAGS);
                    break;*/
                case REFR:
                    this.parsedData = new ChangeFormRefr(BODYDATA, this.CHANGEFLAGS, this.REFID, analysis, context);
                    break;
                case ACHR:
                    this.parsedData = new ChangeFormACHR(BODYDATA, this.CHANGEFLAGS, this.REFID, analysis, context);
                    break;
                case NPC_:
                    this.parsedData = new ChangeFormNPC(BODYDATA, this.CHANGEFLAGS, context);
                    break;
                default:
                    if (bestEffort) {
                        this.parsedData = new ChangeFormDefault(BODYDATA, this.length1);
                    } else {
                        return null;
                    }
                    break;
            }
        } catch (ElementException | BufferUnderflowException ex) {
            LOG.warning(ex.getMessage());

            if (bestEffort) {
                ((Buffer) BODYDATA).position(0);
                this.parsedData = new ChangeFormDefault(BODYDATA, this.length1);
            } else {
                return null;
            }
        }

        if (null == this.parsedData) {
            throw new NullPointerException("This shouldn't happen!");
        }

        return this.parsedData;
    }

    /**
     * Enables the modified flag, indicating that the changeform will have to
     * recalculate it's size and compressed data.
     */
    /*public void setModified() {
        this.modified = true;
    }*/
    /**
     * @see resaver.ess.Linkable#toHTML(Element)
     * @param target A target within the <code>Linkable</code>.
     * @return
     */
    @Override
    public String toHTML(Element target) {
        return this.REFID.toHTML(target);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        BUF.append(this.TYPE);

        if (null != this.REFID.getPLUGIN()) {
            BUF.append(" (").append(this.REFID.getPLUGIN()).append(")");
        } else if (this.REFID.getType() == RefID.Type.FORMIDX) {
            int k = 0;
        }
        BUF.append(" refid=").append(this.REFID.toString());

        if (parsedData != null && this.parsedData instanceof GeneralElement) {
            GeneralElement gen = (GeneralElement) this.parsedData;
            if (gen.hasVal("BASE_OBJECT")) {
                RefID base = (RefID) gen.getVal("BASE_OBJECT");
                BUF.append(" base=").append(base.toString());
            } else if (gen.hasVal("INITIAL")) {
                GeneralElement initial = gen.getGeneralElement("INITIAL");
                if (initial.hasVal("BASE_OBJECT")) {
                    RefID base = (RefID) initial.getVal("BASE_OBJECT");
                    BUF.append(" base=").append(base.toString());
                }
            }
        }

        return BUF.toString();
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    @Override
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUILDER = new StringBuilder();

        Set<ScriptInstance> HOLDERS = save.getPapyrus().getScriptInstances().values().stream()
                .filter(i -> this.getRefID().equals(i.getRefID()))
                .collect(Collectors.toSet());

        BUILDER.append("<html><h3>CHANGEFORM</h3>");
        BUILDER.append(String.format("<p>RefID: %s</p>", this.REFID));
        BUILDER.append(String.format("<p style=\"display:inline-table;\">ChangeFlags: %s</p>", this.CHANGEFLAGS.toHTML()));
        BUILDER.append("<p>");
        BUILDER.append(String.format("DataLength: %s<br/>", this.getDataLength()));
        BUILDER.append(String.format("Type: %s (%d : %d)<br/>", this.getType(), this.getType().SKYRIMCODE, this.getType().FULL));
        BUILDER.append(String.format("Version: %d<br/>", this.VERSION));

        if (this.length2 > 0) {
            BUILDER.append(String.format("Length: %d bytes (%d bytes uncompressed)<br/>", this.length1, this.length2));
        } else {
            BUILDER.append(String.format("Length: %d bytes<br/>", this.length1));
        }
        BUILDER.append("</p>");

        if (HOLDERS.isEmpty()) {
            BUILDER.append("<p>No attached instances.</p>");
        } else {
            BUILDER.append(String.format("<p>%d attached instances:</p><ul>", HOLDERS.size()));
            HOLDERS.forEach(owner -> {
                if (owner instanceof Linkable) {
                    BUILDER.append(String.format("<li>%s - %s", owner.getClass().getSimpleName(), ((Linkable) owner).toHTML(this)));
                } else if (owner != null) {
                    BUILDER.append(String.format("<li>%s - %s", owner.getClass().getSimpleName(), owner));
                }
            });
            BUILDER.append("</ul>");
        }

        BUILDER.append(String.format("<h3>ANALYZE RAW DATA: %s</h3>", this.REFID.toHTML(null)));

        final ChangeFormData BODY = this.getData(analysis, save.getContext(), true);
        if (null == BODY) {
            BUILDER.append("<p><b>The ChangeForm appears to contain invalid data.</b></p>");
        } else if (BODY instanceof ChangeFormDefault) {
            BUILDER.append("<p><b>The ChangeForm could not be parsed.</b></p>");
            BUILDER.append(BODY.getInfo(analysis, save));
        } else {
            BUILDER.append(BODY.getInfo(analysis, save));
        }
        BUILDER.append("</html>");
        return BUILDER.toString();
    }

    /**
     * @see AnalyzableElement#matches(resaver.Analysis,resaver.Mod)
     * @param analysis
     * @param mod
     * @return
     */
    @Override
    public boolean matches(Analysis analysis, String mod) {
        return false;
    }

    /**
     * The <code>RefID</code> of the <code>ChangeForm</code>.
     */
    final private RefID REFID;

    /**
     * ChangeFlags describe what parts of the form have changed.
     */
    final private Flags.Int CHANGEFLAGS;

    /**
     * The type of Form.
     */
    final private int TYPEFIELD;
    final private Type TYPE;
    final private byte VERSION;

    /**
     * For compressed changeForms, length1 represents the size of the compressed
     * data.
     */
    private int length1;

    /**
     * For compressed changeForms, length2 represents the size of the
     * uncompressed data.
     */
    final private int length2;

    final private boolean ISCOMPRESSED;
    final private byte[] RAWDATA;
    private ChangeFormData parsedData;
    static final private Logger LOG = Logger.getLogger(ChangeForm.class.getCanonicalName());

    /**
     * Data sizes for the length fields.
     */
    static public enum LengthSize {
        INT8, INT16, INT32
    }

    /**
     * Types of ChangeForms.
     */
    static public enum Type {
        REFR(0, 0, 63, "Object Reference"),
        ACHR(1, 1, 64, "NPC Reference"),
        PMIS(2, 2, 65, ""),
        PGRE(3, 3, 67, ""),
        PBEA(4, 4, 68, ""),
        PFLA(5, 5, 69, ""),
        CELL(6, 6, 62, "Cell"),
        INFO(7, 7, 78, "Dialogue Info"),
        QUST(8, 8, 79, "Quest"),
        NPC_(9, 9, 45, "NPC Template"),
        ACTI(10, 10, 25, "Activator"),
        TACT(11, 11, 26, "Talking Activator"),
        ARMO(12, 12, 27, "Armor"),
        BOOK(13, 13, 28, "Book"),
        CONT(14, 14, 29, "Container"),
        DOOR(15, 15, 30, "Door"),
        INGR(16, 16, 31, "Ingredient"),
        LIGH(17, 17, 32, "Light"),
        MISC(18, 18, 33, "Miscellaneous"),
        APPA(19, 19, 34, ""),
        STAT(20, 20, 35, "Static"),
        MSTT(21, 21, 37, "Moveable Static"),
        FURN(22, 22, 42, "Furniture"),
        WEAP(23, 23, 43, "Weapon"),
        AMMO(24, 24, 44, "Ammunition"),
        KEYM(25, 25, 47, "Key"),
        ALCH(26, 26, 48, "Ingestible"),
        IDLM(27, 27, 49, "Idle Marker"),
        NOTE(28, 28, 50, "Note"),
        ECZN(29, 29, 105, "Encounter Zone"),
        CLAS(30, 30, 10, "Class"),
        FACT(31, 31, 11, "Faction"),
        PACK(32, 32, 81, "Package"),
        NAVM(33, 33, 75, "Navigation Mesh"),
        WOOP(34, 34, 120, ""),
        MGEF(35, 35, 19, "Magical Effect"),
        SMQN(36, 36, 115, "Story Manager Quest Node"),
        SCEN(37, 37, 124, "Scene"),
        LCTN(38, 38, 106, "Location"),
        RELA(39, 39, 123, "Relationship"),
        PHZD(40, 40, 72, "Physical Hazard"),
        PBAR(41, 41, 71, ""),
        PCON(42, 42, 70, ""),
        FLST(43, 43, 93, "Form List"),
        LVLN(44, 44, 46, "Leveled NPC"),
        LVLI(45, 45, 55, "Leveled Item"),
        LVSP(46, 46, 84, "Leveled Spell"),
        PARW(47, 47, 66, ""),
        ENCH(48, 48, 22, "Enchantment"),
        UNKNOWN49(49, 49, -1, ""),
        UNKNOWN50(50, 50, -1, ""),
        UNKNOWN51(51, 51, -1, ""),
        UNKNOWN52(52, 52, -1, ""),
        UNKNOWN53(53, 53, -1, ""),
        UNKNOWN54(54, 54, -1, ""),
        UNKNOWN55(55, 55, -1, ""),
        UNKNOWN56(56, 56, -1, ""),
        UNKNOWN57(57, 57, -1, ""),
        UNKNOWN58(58, 58, -1, ""),
        UNKNOWN59(59, 59, -1, ""),
        UNKNOWN60(60, 60, -1, ""),
        UNKNOWN61(61, 61, -1, ""),
        UNKNOWN62(62, 62, -1, ""),
        LAND(63, 63, -1, "");

        private Type(int skyrim, int fo4, int full, String name) {
            this.SKYRIMCODE = (byte) skyrim;
            this.FALLOUTCODE = (byte) fo4;
            this.FULL = (byte) full;
            this.NAME = name;
        }

        static Type getType(Game game, int code) {
            if (code >= SKYRIM_VALUES.length || code < 0) {
                return null;
            }

            if (game.isSkyrim()) {
                if (SKYRIM_VALUES[code].SKYRIMCODE != code) {
                    throw new IllegalStateException();
                }
                return SKYRIM_VALUES[code];
            } else if (game.isFO4()) {
                if (FALLOUT4_VALUES[code].FALLOUTCODE != code) {
                    throw new IllegalStateException();
                }
                return FALLOUT4_VALUES[code];
            }
            return null;
        }

        final public byte SKYRIMCODE;
        final public byte FALLOUTCODE;
        final public byte FULL;
        final public String NAME;
        static final private Type[] SKYRIM_VALUES = values();
        static final private Type[] FALLOUT4_VALUES = values();

        static {
            Arrays.sort(SKYRIM_VALUES, (a, b) -> Integer.compare(a.SKYRIMCODE, b.SKYRIMCODE));
            Arrays.sort(FALLOUT4_VALUES, (a, b) -> Integer.compare(a.FALLOUTCODE, b.FALLOUTCODE));
        }
    }

    /**
     * Decompresses a buffer.
     *
     * @param buf
     * @param length
     * @return
     */
    static private ByteBuffer decompress(byte[] buf, int length) {
        try {
            final ByteBuffer DECOMPRESSED = BufferUtil.inflateZLIB(ByteBuffer.wrap(buf), length, buf.length);
            return DECOMPRESSED;
        } catch (DataFormatException ex) {
            return null;
        }
    }

    /**
     * Verifies that two instances of <code>ChangeForm</code> are identical.
     *
     * @param cf1 The first <code>ChangeForm</code>.
     * @param cf2 The second <code>ChangeForm</code>.
     * @throws IllegalStateException Thrown if the two instances of
     * <code>ChangeForm</code> are not equal.
     */
    static public void verifyIdentical(ChangeForm cf1, ChangeForm cf2) throws IllegalStateException {
        if (!Objects.equals(cf1.getRefID(), cf2.getRefID())) {
            throw new IllegalStateException(String.format("RefID mismatch: %s vs %s.", cf1.getRefID(), cf2.getRefID()));
        } else if (cf1.getType() != cf2.getType()) {
            throw new IllegalStateException(String.format("Type mismatch: %s vs %s.", cf1.getType(), cf2.getType()));
        } else if (!Objects.equals(cf1.getChangeFlags(), cf2.getChangeFlags())) {
            throw new IllegalStateException(String.format("ChangeFlags mismatch: %s vs %s.", cf1.getChangeFlags(), cf2.getChangeFlags()));
        } else if (cf1.getVersion() != cf2.getVersion()) {
            throw new IllegalStateException(String.format("Version mismatch: %d vs %d.", cf1.getVersion(), cf2.getVersion()));
        } else if (cf1.isCompressed() != cf2.isCompressed()) {
            throw new IllegalStateException(String.format("RefID mismatch: %s vs %s.", cf1.isCompressed(), cf2.isCompressed()));
        } else if (cf1.getDataLength() != cf2.getDataLength()) {
            throw new IllegalStateException(String.format("Data length mismatch: %d vs %d.", cf1.getDataLength(), cf2.getDataLength()));
        } else if (!Objects.equals(cf1.getBodyData(), cf2.getBodyData())) {
            throw new IllegalStateException(String.format("RefID mismatch: %s vs %s.", cf1.getBodyData(), cf2.getBodyData()));
        }

    }
}
