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
package ess.papyrus

import ess.ESS
import ess.Element
import ess.Linkable.Companion.makeLink
import ess.RefID
import ess.papyrus.Variable.Companion.readList
import resaver.Analysis
import resaver.ListException
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Logger
import kotlin.experimental.and

/**
 * Describes a script instance in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class ScriptInstance internal constructor(input: ByteBuffer, scripts: ScriptMap, context: PapyrusContext) :
    GameElement(input, scripts, context), SeparateData, HasVariables {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        super.write(output)
        output?.putShort(UNKNOWN2BITS)
        output?.putShort(unknown)
        refID.write(output)
        output?.put(UNKNOWN_BYTE)
        if (null != UNKNOWN_FO_BYTE) {
            output?.put(UNKNOWN_FO_BYTE!!)
        }
    }

    /**
     * @see SeparateData.readData
     * @param input
     * @param context
     * @throws PapyrusElementException
     * @throws PapyrusFormatException
     */
    @Throws(PapyrusElementException::class, PapyrusFormatException::class)
    override fun readData(input: ByteBuffer?, context: PapyrusContext?) {
        data = ScriptData(input!!, context!!)
    }

    /**
     * @see SeparateData.writeData
     * @param input
     */
    override fun writeData(input: ByteBuffer?) {
        data!!.write(input)
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = super.calculateSize()
        sum += 5
        sum += refID.calculateSize()
        if (null != UNKNOWN_FO_BYTE) {
            sum++
        }
        sum += if (data == null) 0 else data!!.calculateSize()
        return sum
    }

    /**
     * @return The mystery flag; equivalent to `this.getUnknown==-1`.
     */
    val isMysteryFlag: Boolean
        get() = unknown.toInt() == -1

    /**
     * @return The corresponding `Script`.
     */
    val script: Script?
        get() {
            assert(super.definition is Script)
            return super.definition as Script?
        }

    /**
     * Shortcut for getData().getType().
     *
     * @return The type of the script.
     */
    val type: TString?
        get() = if (null != data) data!!.type else null

    /**
     * @return Checks for a memberless error.
     */
    fun hasMemberlessError(): Boolean {
        val DESCS = if (null != script) script!!.extendedMembers else emptyList()
        val VARS = variables
        return VARS.isEmpty() && DESCS.isNotEmpty()
    }

    /**
     * @return Checks for a definition error.
     */
    fun hasDefinitionError(): Boolean {
        val DESCS = if (null != script) script!!.extendedMembers else emptyList()
        val VARS = variables
        return DESCS.size != VARS.size && VARS.isNotEmpty()
    }

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override val variables: List<Variable>
        get() = if (data == null) emptyList() else data!!.VARIABLES.filterNotNull()

    /**
     * @see HasVariables.getDescriptors
     * @return
     */
    override val descriptors: List<MemberDesc>
        get() = script!!.extendedMembers

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        if (data == null || data!!.VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require(!(index < 0 || index >= data!!.VARIABLES.size)) { "Invalid variable index: $index" }
        data!!.VARIABLES[index] = newVar
    }

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String? {
        return if (null == target || null == data) {
            makeLink("scriptinstance", iD, this.toString())
        } else if (target is Variable) {
            val index = variables.indexOf(target)
            if (index >= 0) {
                makeLink("scriptinstance", iD, index, this.toString())
            } else {
                makeLink("scriptinstance", iD, this.toString())
            }
        } else {
            for (`var` in variables) {
                if (`var`.hasRef()) {
                    if (`var`.referent === target) {
                        val indexOf = variables.indexOf(`var`)
                        if (indexOf >= 0) {
                            return Optional.of(indexOf)
                                .map { index: Int? -> makeLink("scriptinstance", iD, index!!, this.toString()) }
                                .orElse(makeLink("scriptinstance", iD, this.toString()))
                        }
                    }
                }
            }
            Optional.empty<Int>()
                .map { index: Int? -> makeLink("scriptinstance", iD, index!!, this.toString()) }
                .orElse(makeLink("scriptinstance", iD, this.toString()))
        }
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        if (isUndefined) {
            BUF.append("#").append(this.scriptName).append("#: ")
        } else {
            BUF.append(this.scriptName).append(": ")
        }
        if (isMysteryFlag) {
            BUF.append("*")
        }
        BUF.append(refID.toString())
        BUF.append(" (").append(iD).append(")")
        return BUF.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        if (null != script) {
            BUILDER.append("<html><h3>INSTANCE of ${script!!.toHTML(this)}</h3>")
        } else {
            BUILDER.append("<html><h3>INSTANCE of ${this.scriptName}</h3>")
        }
        val PLUGIN = refID.PLUGIN
        if (PLUGIN != null) {
            BUILDER.append("<p>This instance is attached to an object from ${PLUGIN.toHTML(this)}.</p>")
        } else if (refID.type === RefID.Type.CREATED) {
            BUILDER.append("<p>This instance was created in-game.</p>")
        }
        if (isUndefined) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Remove Undefined Instances\" will delete this.</p>")
        }
        if (this.isUnattached) {
            BUILDER.append("<p><em>WARNING: OBJECT MISSING!</em><br/>Selecting \"Remove Unattached Instances\" will delete this.</p>")
        }
        if (refID.type === RefID.Type.CREATED && save?.changeForms?.containsKey(refID)?.not() == true) {
            BUILDER.append("<p><em>REFID POINTS TO NONEXISTENT CREATED FORM.</em><br/>Remove non-existent form instances\" will delete this. However, some mods create these instances deliberately. </p>")
        }
        if (null != analysis) {
            val PROVIDERS = analysis.SCRIPT_ORIGINS[this.scriptName.toIString()]
            if (null != PROVIDERS) {
                val probableProvider = PROVIDERS.last()
                BUILDER.append("<p>The script probably came from mod \"$probableProvider\".</p>")
                if (PROVIDERS.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    PROVIDERS.forEach { mod: String? -> BUILDER.append(String.format("<li>%s", mod)) }
                    BUILDER.append("</ul>")
                }
            }
        }
        BUILDER.append("<p>")
        BUILDER.append(String.format("ID: %s<br/>", iD))
        BUILDER.append(String.format("Type: %s<br/>", type))
        val mysteryFlag = unknown.toInt() == -1
        if (save?.changeForms?.containsKey(refID) == true) {
            BUILDER.append("RefID${if (mysteryFlag) "@" else ""}: ${refID.toHTML(null)}<br/>")
        } else {
            BUILDER.append("RefID${if (mysteryFlag) "@" else ""}: $refID<br/>")
        }
        BUILDER.append(String.format("Unknown2bits: %01X<br/>", UNKNOWN2BITS))
        BUILDER.append(String.format("UnknownShort: %04X<br/>", unknown))
        BUILDER.append(String.format("UnknownByte: %02x<br/>", UNKNOWN_BYTE))
        BUILDER.append("</p>")
        save?.papyrus!!.printReferrents(this, BUILDER, "script instance")
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @return A flag indicating if the `ScriptInstance` is
     * unattached.
     */
    val isUnattached: Boolean
        get() = this.refID.isZero
    /**
     * @return A flag indicating that the `ScriptInstance` has a
     * canary variable.
     */
    fun hasCanary(): Boolean {
        if (null == script) {
            return false
        }
        val DESCS: List<MemberDesc> = script!!.extendedMembers
        for (DESC in DESCS) {
            if (isCanary.invoke(DESC)) {
                return true
            }
        }
        return false
    }

    /**
     * @return A flag indicating that the `ScriptInstance` has a
     * canary variable.
     */
    val canary: Int
        get() {
            val MEMBERS = variables
            if (null == script || MEMBERS.isEmpty()) {
                return 0
            }
            val NAMES: List<MemberDesc> = script!!.extendedMembers
            var canary: MemberDesc? = null
            for (NAME in NAMES) {
                if (isCanary.invoke(NAME)) {
                    canary = NAME
                    break
                }
            }
            return if (canary != null) {
                val `var` = MEMBERS[NAMES.indexOf(canary)]
                if (`var` is VarInt) {
                    `var`.value
                } else {
                    0
                }
            } else {
                0
            }
        }

    /**
     * @return A flag indicating if the `ScriptInstance` is
     * undefined.
     */
    override val isUndefined: Boolean
        get() = if (null != script) {
            script!!.isUndefined
        } else {
            !Script.NATIVE_SCRIPTS.contains(this.scriptName.toIString())
        }
    private val UNKNOWN2BITS: Short

    /**
     * @return The unknown short field; if it's -1, the RefID field may not be
     * valid.
     */
    val unknown: Short

    /**
     * @return The RefID of the papyrus element.
     */
    val refID: RefID
    private val UNKNOWN_BYTE: Byte
    private var UNKNOWN_FO_BYTE: Byte? = null
    /**
     * @return The `ScriptData` for the instance.
     */
    /**
     * Sets the data field.
     *
     * @param newData The new value for the data field.
     */
    var data: ScriptData? = null

    /**
     * Describes a script instance's data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    inner class ScriptData(input: ByteBuffer, context: PapyrusContext) : PapyrusDataFor<ScriptInstance?> {
        /**
         * @see ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer?) {
            iD.write(output)
            output?.put(FLAG)
            this.type.write(output)
            output?.putInt(UNKNOWN1)
            if ((FLAG and 0x04.toByte()).toInt() != 0) {
                output?.putInt(UNKNOWN2)
            }
            output?.putInt(VARIABLES.size)
            VARIABLES.forEach { `var`: Variable? -> `var`!!.write(output) }
        }

        /**
         * @see ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 9
            sum += iD.calculateSize()
            sum += if ((FLAG and 0x04.toByte()).toInt() != 0) 4 else 0
            sum += this.type.calculateSize()
            var result = 0
            for (`var` in VARIABLES) {
                val i = `var`!!.calculateSize()
                result += i
            }
            sum += result
            return sum
        }

        /**
         * @see Object.toString
         * @return
         */
        override fun toString(): String {
            val BUILDER = StringBuilder()
            BUILDER.append("SCRIPTDATA\n")
            BUILDER.append("ID = $iD\n")
            BUILDER.append(String.format("flag= %d\n", FLAG))
            BUILDER.append("type = ${this.type}\n")
            BUILDER.append(String.format("unknown1 = %d\n", UNKNOWN1))
            BUILDER.append(String.format("unknown2 = %d\n\n", UNKNOWN2))
            VARIABLES.forEach { `var`: Variable? -> BUILDER.append("$`var`\n") }
            return BUILDER.toString()
        }

        //final private EID ID;
        private val FLAG: Byte

        /**
         * @return The type of the script.
         */
        val type: TString
        private val UNKNOWN1: Int
        private val UNKNOWN2: Int
        var VARIABLES: MutableList<Variable?> = mutableListOf()

        /**
         * Creates a new `ScriptData` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        init {
            FLAG = input.get()
            this.type = context.readTString(input)
            UNKNOWN1 = input.int
            UNKNOWN2 = if ((FLAG and 0x04.toByte()).toInt() != 0) input.int else 0
            try {
                val count = input.int
                VARIABLES = readList(input, count, context).toMutableList()
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read struct variables.", ex, this)
            }
        }
    }

    companion object {
        private val isCanary = { desc: MemberDesc -> desc.name.equals("::iPapyrusDataVerification_var") }
    }

    /**
     * Creates a new `ScriptInstances` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param scripts The `ScriptMap` containing the definitions.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     */
    init {
        UNKNOWN2BITS = input.short
        unknown = input.short
        refID = context.readRefID(input)
        UNKNOWN_BYTE = input.get()
        UNKNOWN_FO_BYTE = if (context.game!!.isFO4) {
            if ((UNKNOWN2BITS and 0x3.toShort()) == 0x3.toShort()) {
                input.get()
            } else {
                null
            }
        } else {
            null
        }
    }
}