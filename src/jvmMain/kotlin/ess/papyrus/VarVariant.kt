package ess.papyrus

import PlatformByteBuffer
import ess.Element

/**
 * Variable that stores a Variant.
 */
class VarVariant(input: PlatformByteBuffer, context: PapyrusContext) : Variable() {
    override fun calculateSize(): Int {
        return 1 + value.calculateSize()
    }

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        value.write(output)
    }

    override val type: VarType
        get() = VarType.VARIANT

    override fun hasRef(): Boolean {
        return value.hasRef()
    }

    override fun hasRef(id: EID?): Boolean {
        return value.hasRef(id)
    }

    override val ref: EID?
        get() = value.ref
    override val referent: GameElement?
        get() = value.referent

    override fun toValueString(): String? {
        return value.toValueString()
    }

    override fun toHTML(target: Element?): String {
        return "$type[${value.toHTML(target)}]"
    }

    override fun toString(): String {
        return "$type:$value"
    }

    val value: Variable = read(input, context)

}