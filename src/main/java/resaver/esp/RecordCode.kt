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
package resaver.esp

/**
 * Describes the prefix codes of skyrim mod file records.
 *
 * @author Mark Fairchild
 */
enum class RecordCode(val FULLNAME: String, val HAS_VMAD: Boolean) {
    AACT("Action", false), ACHR("Actor", true), ACTI("Activator", true), ADDN(
        "Addon Node",
        false
    ),
    AECH("Audio Effect Chain", false), ALCH("Potion", false), AMDL("Aim Model", false), AMMO(
        "Ammo",
        false
    ),
    ANIO("Animated Object", false), AORU("Attraction Rule", false), APPA("Apparatus", true), ARMA(
        "Armature",
        false
    ),
    ARMO("Armor", true), ARTO("Art Object", false), ASPC("Acoustic Space", false), ASTP(
        "Association Type",
        false
    ),
    AVIF("Actor Values / Perk Tree Graphics", false), BOOK("Book", true), BNDS(
        "Bendable Spline",
        false
    ),
    BPTD("Body Part Data", false), CAMS("Camera Shot", false), CELL("Cell", false), CLAS("Class", false), CLFM(
        "Color",
        false
    ),
    CLMT("Climate", false), CMPO("Component", false), COBJ("Recipes", false), COLL(
        "Collision Layer",
        false
    ),
    CONT("Container", true), CPTH("Camera Path", false), CSTY("Combat Style", false), DEBR(
        "Debris",
        false
    ),
    DFOB("Default Object", false), DIAL("Dialog Topic", false), DLBR("Dialog Branch", false), DLVW(
        "Dialog View",
        false
    ),
    DMGT("Damage Type", false), DOBJ("Default Object Manager", false), DOOR("Door", true), DUAL(
        "Dual Cast Data",
        false
    ),
    ECZN("Encounter Zone", false), EFSH("Effect Shader", false), ENCH("Enchantment", false), EQUP(
        "Equip Type",
        false
    ),
    EXPL("Explosion", false), EYES("Eyes", false), FACT("Faction", false), FLOR("Flora", true), FLST(
        "Form List",
        false
    ),
    FSTP("Footstep", false), FSTS("Footstep Set", false), FURN("Furniture", true), GDRY(
        "God Rays",
        false
    ),
    GLOB("Global Variable", false), GMST("Game Settings", false), GRAS("Grass", false), GRUP(
        "Group",
        false
    ),
    HAZD("Hazard", false), HDPT("Head Part", false), IDLE("Idle Animation", false), IDLM(
        "Idle Marker",
        false
    ),
    IMAD("Image Space Modifier", false), IMGS("Image Space", false), INFO("Dialog Topic Info", true), INGR(
        "Ingredient",
        true
    ),
    INNR("Instance Naming Rules", false), IPCT("Impact Data", false), IPDS("Impatact Data Set", false), KEYM(
        "Key",
        true
    ),
    KSSM("Sound Keyword Mapping", false), KYWD("Keyword", false), LAND("Landscape", false), LAYR(
        "Layer",
        false
    ),
    LCRT("Location Reference Type", false), LCTN("Location", false), LENS(
        "Lens Flare",
        false
    ),
    LGTM("Lighting Template", false), LIGH("Light", true), LSCR("Load Screen", false), LTEX(
        "Land Texture",
        false
    ),
    LVLI("Leveled Item", false), LVLN("Leveled Actor", false), LVSP("Leveled Spell", false), MATO(
        "Material Object",
        false
    ),
    MATT("Material Type", false), MESG("Message", false), MGEF("Magic Effect", true), MISC(
        "Miscellaneous Object",
        true
    ),
    MOVT("Movement Type", false), MSTT("Moveable Static", false), MSWP("Material Swap", false), MUSC(
        "Music Type",
        false
    ),
    MUST("Music Track", false), NAVI("Navigation", false), NAVM(
        "Vav Mesh",
        false
    ),
    NOCM("Navigation Mesh Obstacle Manager", false), NOTE("Note", true), NPC_(
        "Actor",
        true
    ),
    OMOD("Object Modification", false), OTFT("Outfit", false), OVIS(
        "Outfit Visibility Manager",
        false
    ),
    PACK("AI Package", true), PBEA("UNKNOWN PBEA", true), PKIN("Pack-In", false), PERK(
        "Perk",
        true
    ),
    PGRE("Placed Grenade", false), PHZD("Placed Hazard", true), PMIS("Placed Missile", true), PROJ(
        "Projectile",
        false
    ),
    QUST("Quest", true), RACE("Race", false), RFCT("Visual Effect", false), REFR(
        "Object Reference",
        true
    ),
    REGN("Region", false), RELA("Relationship", false), REVB("Reverb Parameters", false), RFGP(
        "Reference Group",
        false
    ),
    SCCO("Scene Collection", false), SCEN("Scene", true), SCOL("Static Collection", false), SCRL("Scroll", false), SCSN(
        "Audio Category Snapshot",
        false
    ),
    SHOU("Shout", false), SLGM("Soulgem", false), SMBN(
        "Story Manager Branch Node",
        false
    ),
    SMEN("Story Manager Event Node", false), SMQN("Story Manager Quest Node", false), SNCT(
        "Sound Category",
        false
    ),
    SNDR("Sound Reference", false), SOPM("Sound Output Model", false), SOUN("Sound", false), SPEL("Spell", false), SPGD(
        "Shader Particle Geometry",
        false
    ),
    STAG("Animation Sound Tag Set", false), STAT("Static", false), TACT("Talking Activator", true), TERM(
        "Terminal",
        true
    ),
    TES4("Plugin Info", false), TREE("Tree", false), TRNS("Transform", false), TXST(
        "Texture Set",
        false
    ),
    VTYP("Voice Type", false), VOLI("Volumetric Lighting", false), WATR("Water Type", false), WEAP(
        "Weapon",
        true
    ),
    WOOP("Word of Power", false), WRLD("World Space", false), WTHR("Weather", false), ZOOM("Zoom", false);
}