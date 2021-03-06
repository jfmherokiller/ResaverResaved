package ess.papyrus

import PlatformByteBuffer
import ess.Element
import ess.Linkable.Companion.makeLink

/**
 * ABT for a variable that stores some type of ref.
 */
abstract class VarAbstractRef : Variable {
    constructor(input: PlatformByteBuffer, context: PapyrusContext) {
        refType = context.readTString(input)
        ref = context.readEID(input)
        referent = context.findReferrent(ref)
    }

    constructor(type: TString?, id: EID?, context: PapyrusContext) {
        ref = id!!
        refType = type!!
        referent = context.findReferrent(ref)
    }

    override fun hasRef(): Boolean {
        return true
    }

    override fun hasRef(id: EID?): Boolean {
        return ref == id
    }

    override fun calculateSize(): Int {
        var sum = 1
        sum += refType.calculateSize()
        sum += ref.calculateSize()
        return sum
    }

    override fun write(output: PlatformByteBuffer?) {
        type.write(output)
        refType.write(output)
        ref.write(output)
    }

    /**
     * @return
     * @see Variable.toTypeString
     */
    override fun toTypeString(): String {
        return refType.toString()
    }

    override fun toValueString(): String {
        return referent?.toString() ?: "$ref ($refType)"
    }

    override fun toHTML(target: Element?): String? {
        return if (null != referent) {
            val REFLINK = referent.toHTML(this)
            "$type : $REFLINK"
        } else {
            val DEFLINK = makeLink("script", refType, refType.toString())
            "$type : $ref ($DEFLINK)"
        }
    }
    val isNull: Boolean
        get() =  this.ref.isZero

    override fun toString(): String {
        return "$type : ${toValueString()}"
    }

    val refType: TString
    final override val ref: EID
    final override val referent: GameElement?
}