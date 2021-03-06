package ess.papyrus

import PlatformByteBuffer

/**
 * Variable that stores an UNKNOWN7.
 */
class VarStruct : VarAbstractRef {
    constructor(input: PlatformByteBuffer, context: PapyrusContext) : super(input!!, context) {}
    constructor(type: TString?, id: EID?, context: PapyrusContext) : super(type, id, context) {}

    fun derive(id: Long, context: PapyrusContext): VarStruct {
        return VarStruct(refType, ref.derive(id), context)
    }

    override val type: VarType
        get() = VarType.STRUCT
}