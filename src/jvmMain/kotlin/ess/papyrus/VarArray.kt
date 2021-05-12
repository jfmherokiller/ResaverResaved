package ess.papyrus

import ess.Element
import ess.Linkable.Companion.makeLink
import java.nio.ByteBuffer

/**
 * Variable that stores an ARRAY.
 */
class VarArray(varType: VarType, input: ByteBuffer, context: PapyrusContext) : Variable() {
    val elementType: VarType
        get() = VarType.values()[VarTYPE.ordinal - 7]

    override val type: VarType
        get() = VarTYPE


    override fun hasRef(): Boolean {
        return true
    }

    override fun hasRef(id: EID?): Boolean {
        return arrayID == id
    }

    override val ref: EID
        get() = arrayID


    override val referent: GameElement?
        get() = null


    override fun write(output: ByteBuffer?) {
        this.type.write(output)
        if (VarTYPE.isRefType) {
            REFTYPE!!.write(output)
        }
        arrayID.write(output)
    }

    override fun calculateSize(): Int {
        var sum = 1
        sum += if (VarTYPE.isRefType) REFTYPE!!.calculateSize() else 0
        sum += arrayID.calculateSize()
        return sum
    }

    override fun toTypeString(): String {
        if (null == array) {
            return if (VarTYPE.isRefType) {
                "$REFTYPE[ ]"
            } else {
                "$elementType[ ]"
            }
        }
        return if (VarTYPE.isRefType) {
            "$VarTYPE:$REFTYPE[${array.length}]"
        } else {
            "$VarTYPE:$elementType[${array.length}]"
        }
    }

    override fun toValueString(): String {
        return if (null != array) {
            "$arrayID: ${array.toValueString()}"
        } else {
            arrayID.toString()
        }
    }

    override fun toHTML(target: Element?): String {
        val LINK = makeLink("array", arrayID, arrayID.toString())
        return String.format("%s : %s", toTypeString(), LINK)
    }

    override fun toString(): String {
        return "${toTypeString()} $arrayID"
    }

    private val VarTYPE: VarType
    val arrayID: EID
    private val REFTYPE: TString?
    val array: ArrayInfo?

    init {
        VarTYPE = varType
        REFTYPE = if (VarTYPE.isRefType) context.readTString(input) else null
        arrayID = context.readEID(input)
        array = context.findArray(arrayID)
    }
}