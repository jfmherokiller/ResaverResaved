package ess.papyrus

import java.nio.ByteBuffer

/**
 * Variable that stores a string.
 */
class VarStr : Variable {
    constructor(input: ByteBuffer, context: PapyrusContext) {
        value = context.readTString(input)
    }

    constructor(newValue: String, context: PapyrusContext) {
        value = context.addTString(newValue)
    }

    override fun calculateSize(): Int {
        return 1 + value.calculateSize()
    }

    override fun write(output: ByteBuffer?) {
        type.write(output)
        value.write(output)
    }

    override val type: VarType
        get() = VarType.STRING

    override fun toValueString(): String {
        //return String.format("\"%s\"", this.VALUE);
        return "\"$value\""
    }

    override fun toString(): String {
        //return String.format("%s:\"%s\"", this.getType(), this.VALUE);
        return "$type:${toValueString()}"
    }

    //final private StringTable STRINGS;
    val value: TString
}