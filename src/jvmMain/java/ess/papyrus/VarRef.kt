package ess.papyrus

import java.nio.ByteBuffer

/**
 * Variable that stores a ref. Note to self: a ref is a pointer to a papyrus
 * element, unlike a RefID which points to a form or changeform.
 */
class VarRef : VarAbstractRef {
    constructor(input: ByteBuffer?, context: PapyrusContext) : super(input!!, context) {}
    constructor(type: TString?, id: EID?, context: PapyrusContext) : super(type, id, context) {}

    fun derive(id: Long, context: PapyrusContext): VarRef {
        return VarRef(refType, ref?.derive(id), context)
    }

    override val type: VarType
        get() = VarType.REF
}