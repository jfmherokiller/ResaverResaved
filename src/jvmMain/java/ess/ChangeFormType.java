package ess;

import resaver.Game;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Types of ChangeForms.
 */
public enum ChangeFormType {
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

    private ChangeFormType(int skyrim, int fo4, int full, String name) {
        this.SKYRIMCODE = (byte) skyrim;
        this.FALLOUTCODE = (byte) fo4;
        this.FULL = (byte) full;
        this.NAME = name;
    }

    static ChangeFormType getType(Game game, int code) {
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
    static final private ChangeFormType[] SKYRIM_VALUES = values();
    static final private ChangeFormType[] FALLOUT4_VALUES = values();

    static {
        Arrays.sort(SKYRIM_VALUES, Comparator.comparingInt(a -> a.SKYRIMCODE));
        Arrays.sort(FALLOUT4_VALUES, Comparator.comparingInt(a -> a.FALLOUTCODE));
    }
}
