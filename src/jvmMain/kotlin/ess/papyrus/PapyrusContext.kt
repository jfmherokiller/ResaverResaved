/*
 * Copyright 2020 Mark.
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
package ess.papyrus

import PlatformByteBuffer
import ess.ESS.ESSContext
import ess.Linkable
import ess.papyrus.EID.Companion.make4byte
import ess.papyrus.EID.Companion.make8Byte
import ess.papyrus.EID.Companion.read4byte
import ess.papyrus.EID.Companion.read8byte


/**
 *
 * @author Mark
 */
class PapyrusContext : ESSContext {
    /**
     * Creates a new `PapyrusContext` from an existing
     * `ESSContext` and an instance of `Papyrus`.
     *
     * @param context
     * @param papyrus
     */
    constructor(context: ESSContext?, papyrus: Papyrus?) : super(context) {
        this.papyrus = papyrus!!
    }

    /**
     * Creates a new `PapyrusContext` from an existing
     * `PapyrusContext`.
     *
     * @param context
     */
    constructor(context: PapyrusContext) : super(context) {
        papyrus = context.papyrus
    }

    /**
     * Reads an `EID` from a `ByteBuffer`. The size of the
     * `EID` is determined from the `ID64` flag of the
     * `Game` field of the relevant `ESS`.
     *
     * @param input The input stream.
     * @return The `EID`.
     */
    fun readEID(input: PlatformByteBuffer): EID {
        return if (game!!.isID64) readEID64(input) else readEID32(input)
    }

    /**
     * Makes an `EID` from a `long`. The actual size of
     * the `EID` is determined from the `ID64` flag of the
     * `Game` field of the relevant `ESS`.
     *
     * @param `val` The id value.
     * @return The `EID`.
     */
    fun makeEID(`val`: ULong): EID {
        return if (game!!.isID64) makeEID64(`val`.toLong()) else makeEID32(`val`.toInt())
    }

    /**
     * Reads a four-byte `EID` from a `ByteBuffer`.
     *
     * @param input The input stream.
     * @return The `EID`.
     */
    fun readEID32(input: PlatformByteBuffer): EID {
        return read4byte(input, papyrus)
    }

    /**
     * Reads an eight-byte `EID` from a `ByteBuffer`.
     *
     * @param input The input stream.
     * @return The `EID`.
     */
    fun readEID64(input: PlatformByteBuffer): EID {
        return read8byte(input, papyrus)
    }

    /**
     * Makes a four-byte `EID` from an int.
     *
     * @param val The id value.
     * @return The `EID`.
     */
    fun makeEID32(`val`: Int): EID {
        return make4byte(`val`, papyrus)
    }

    /**
     * Makes an eight-byte `EID` from a long.
     *
     * @param val The id value.
     * @return The `EID`.
     */
    fun makeEID64(`val`: Long): EID {
        return make8Byte(`val`, papyrus)
    }

    /**
     * Shortcut for getStringTable().readRefID(input)
     *
     * @param input The input stream.
     * @return The new `TString`.
     * @throws PapyrusFormatException
     */
    @Throws(PapyrusFormatException::class)
    fun readTString(input: PlatformByteBuffer): TString {
        return papyrus.stringTable.read(input)!!
    }

    /**
     * Shortcut for getStringTable().add(s)
     *
     * @param s The new `String`.
     * @return The new `TString`.
     */
    fun addTString(s: String?): TString {
        return papyrus.stringTable.addString(s)
    }

    /**
     * Shortcut for getStringTable().get(s)
     *
     * @param index The index of the `TString`.
     * @return The `TString`.
     */
    fun getTString(index: Int): TString? {
        return papyrus.stringTable[index]
    }

    /**
     * Does a very general search for an ID.
     *
     * @param number The data to search for.
     * @return Any match of any kind.
     */
    override fun broadSpectrumSearch(number: Number): Linkable? {
        val r1 = this.findAny(makeEID32(number.toInt()))
        if (r1 != null) {
            return r1
        }
        val r2 = this.findAny(makeEID64(number.toLong()))
        if (r2 != null) {
            return r2
        }
        val r3 = super.broadSpectrumSearch(number)
        if (r3 != null) {
            return r3
        }
        if (number.toInt() >= 0 && number.toInt() < papyrus.stringTable.size) {
            val s = papyrus.stringTable[number.toInt()]
            return this.findAny(s)
        }
        return null
    }

    fun findAny(name: TString?): Definition? {
        for (c in listOf(
            papyrus.scripts,
            papyrus.structs
        )) {
            if (c.containsKey(name)) {
                return c[name]
            }
        }
        return null
    }

    fun findAny(id: EID): HasID? {
        return when {
            papyrus.scriptInstances.containsKey(id) -> {
                papyrus.scriptInstances[id]
            }
            papyrus.structInstances.containsKey(id) -> {
                papyrus.structInstances[id]
            }
            papyrus.references.containsKey(id) -> {
                papyrus.references[id]
            }
            papyrus.arrays.containsKey(id) -> {
                papyrus.arrays[id]
            }
            papyrus.activeScripts.containsKey(id) -> {
                papyrus.activeScripts[id]
            }
            papyrus.suspendedStacks1.containsKey(id) -> {
                papyrus.suspendedStacks1[id]
            }
            papyrus.suspendedStacks2.containsKey(id) -> {
                papyrus.suspendedStacks2[id]
            }
            else -> papyrus.unbinds.getOrDefault(id, null)
        }
    }

    fun findAll(id: EID?): HasID? {
        for (c in listOf(
            papyrus.scriptInstances,
            papyrus.structInstances,
            papyrus.references,
            papyrus.arrays,
            papyrus.activeScripts,
            papyrus.suspendedStacks1,
            papyrus.suspendedStacks2,
            papyrus.unbinds
        )) {
            if (c.containsKey(id)) {
                return c[id]
            }
        }
        return null
    }

    fun findScript(name: TString?): Script? {
        return papyrus.scripts.getOrDefault(name, null)
    }

    fun findStruct(name: TString?): Struct? {
        return papyrus.structs.getOrDefault(name, null)
    }

    fun findScriptInstance(id: EID?): ScriptInstance? {
        return papyrus.scriptInstances.getOrDefault(id, null)
    }

    fun findStructInstance(id: EID?): StructInstance? {
        return papyrus.structInstances.getOrDefault(id, null)
    }

    fun findReference(id: EID?): Reference? {
        return papyrus.references.getOrDefault(id, null)
    }

    fun findArray(id: EID?): ArrayInfo? {
        return papyrus.arrays.getOrDefault(id, null)
    }

    fun findActiveScript(id: EID?): ActiveScript? {
        return papyrus.activeScripts.getOrDefault(id, null)
    }

    fun findReferrent(id: EID): GameElement? {
        return papyrus.findReferrent(id)
    }

    /**
     * @return The `Papyrus` itself. May not be full constructed.
     */
    protected val papyrus: Papyrus
}