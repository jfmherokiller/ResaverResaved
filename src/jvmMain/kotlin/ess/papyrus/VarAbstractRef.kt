package ess.papyrus

import ess.Element
import ess.Linkable.Companion.makeLink
import java.nio.ByteBuffer
import java.util.*

/**
 * ABT for a variable that stores some type of ref.
 */
abstract class VarAbstractRef : Variable {
    constructor(input: ByteBuffer, context: PapyrusContext) {
        Objects.requireNonNull(input)
        refType = context.readTString(input)
        ref = context.readEID(input)
        referent = context.findReferrent(ref)
    }

    constructor(type: TString?, id: EID?, context: PapyrusContext) {
        ref = Objects.requireNonNull(id)!!
        refType = Objects.requireNonNull(type)!!
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

    override fun write(output: ByteBuffer?) {
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
        return if (referent != null) referent.toString() else "$ref ($refType)"
    }

    override fun toHTML(target: Element?): String? {
        return if (null != referent) {
            val REFLINK = referent!!.toHTML(this)
            String.format("%s : %s", type, REFLINK)
        } else {
            val DEFLINK = makeLink("script", refType, refType.toString())
            String.format("%s : %s (%s)", type, ref, DEFLINK)
        }
    }
    val isNull: Boolean
        get() =  this.ref.isZero

    override fun toString(): String {
        return "$type : ${toValueString()}"
    }

    val refType: TString
    override val ref: EID
    override val referent: GameElement?
}