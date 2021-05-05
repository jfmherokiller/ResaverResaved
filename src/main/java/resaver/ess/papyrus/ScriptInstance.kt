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
package resaver.ess.papyrus

import resaver.Analysis
import resaver.ListException
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Linkable
import resaver.ess.RefID
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
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
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        super.write(output)
        output!!.putShort(UNKNOWN2BITS)
        output.putShort(unknown)
        refID.write(output)
        output.put(UNKNOWN_BYTE)
        if (null != UNKNOWN_FO_BYTE) {
            output.put(UNKNOWN_FO_BYTE!!)
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
    override fun readData(input: ByteBuffer, context: PapyrusContext) {
        data = ScriptData(input, context)
    }

    /**
     * @see SeparateData.writeData
     * @param output
     */
    override fun writeData(output: ByteBuffer) {
        data!!.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
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
     * @return The name of the corresponding `Script`.
     */
    val scriptName: TString
        get() = super.definitionName

    /**
     * @return The corresponding `Script`.
     */
    val script: Script
        get() {
            assert(super.definition is Script)
            return super.definition as Script
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
        val DESCS = script.extendedMembers
        val VARS = this.variables
        return VARS.isEmpty() && DESCS.isNotEmpty()
    }

    /**
     * @return Checks for a definition error.
     */
    fun hasDefinitionError(): Boolean {
        val DESCS = script.extendedMembers
        val VARS = this.variables
        return DESCS.size != VARS.size && VARS.isNotEmpty()
    }

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override val variables: List<Variable>
        get()= if (data == null) emptyList() else Collections.unmodifiableList(data!!.VARIABLES)

    /**
     * @see HasVariables.getDescriptors
     * @return
     */
    override val descriptors: List<MemberDesc>
        get()= script.extendedMembers


    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        if (data == null || data!!.VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require((index < 0 || index >= data!!.VARIABLES!!.size).not()) { "Invalid variable index: $index" }
        if (newVar != null) {
            data!!.VARIABLES!![index] = newVar
        }
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return if (null == target || null == data) {
            Linkable.makeLink("scriptinstance", this.iD, this.toString())
        } else if (target is Variable) {
            val index = this.variables.indexOf(target)
            if (index >= 0) {
                Linkable.makeLink("scriptinstance", this.iD, index, this.toString())
            } else {
                Linkable.makeLink("scriptinstance", this.iD, this.toString())
            }
        } else {
            this.variables.stream()
                .filter { obj: Variable -> obj.hasRef() }
                .filter { `var`: Variable -> `var`.referent === target }
                .map { `var`: Variable -> this.variables.indexOf(`var`) }
                .filter { index: Int -> index >= 0 }
                .findFirst()
                .map { index: Int? -> Linkable.makeLink("scriptinstance", this.iD, index!!, this.toString()) }
                .orElse(Linkable.makeLink("scriptinstance", this.iD, this.toString()))
        }
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        if (this.isUndefined) {
            BUF.append("#").append(scriptName).append("#: ")
        } else {
            BUF.append(scriptName).append(": ")
        }
        if (isMysteryFlag) {
            BUF.append("*")
        }
        BUF.append(refID.toString())
        BUF.append(" (").append(this.iD).append(")")
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
            BUILDER.append(String.format("<html><h3>INSTANCE of %s</h3>", script.toHTML(this)))
        val PLUGIN = refID.PLUGIN
        if (PLUGIN != null) {
            BUILDER.append(String.format("<p>This instance is attached to an object from %s.</p>", PLUGIN.toHTML(this)))
        } else if (refID.type === RefID.Type.CREATED) {
            BUILDER.append("<p>This instance was created in-game.</p>")
        }
        if (this.isUndefined) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Remove Undefined Instances\" will delete this.</p>")
        }
        if (this.isUnattached) {
            BUILDER.append("<p><em>WARNING: OBJECT MISSING!</em><br/>Selecting \"Remove Unattached Instances\" will delete this.</p>")
        }
        if (refID.type === RefID.Type.CREATED && !save!!.changeForms.containsKey(refID)) {
            BUILDER.append("<p><em>REFID POINTS TO NONEXISTENT CREATED FORM.</em><br/>Remove non-existent form instances\" will delete this. However, some mods create these instances deliberately. </p>")
        }
        if (null != analysis) {
            val PROVIDERS = analysis.SCRIPT_ORIGINS[scriptName.toIString()]
            if (null != PROVIDERS) {
                val probableProvider = PROVIDERS.last()
                BUILDER.append(String.format("<p>The script probably came from mod \"%s\".</p>", probableProvider))
                if (PROVIDERS.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    PROVIDERS.forEach(Consumer { mod: String? -> BUILDER.append(String.format("<li>%s", mod)) })
                    BUILDER.append("</ul>")
                }
            }
        }
        BUILDER.append("<p>")
        BUILDER.append(String.format("ID: %s<br/>", this.iD))
        BUILDER.append(String.format("Type: %s<br/>", type))
        val mysteryFlag = unknown.toInt() == -1
        if (save!!.changeForms.containsKey(refID)) {
            BUILDER.append(String.format("RefID%s: %s<br/>", if (mysteryFlag) "@" else "", refID.toHTML(null)))
        } else {
            BUILDER.append(String.format("RefID%s: %s<br/>", if (mysteryFlag) "@" else "", refID.toString()))
        }
        BUILDER.append(String.format("Unknown2bits: %01X<br/>", UNKNOWN2BITS))
        BUILDER.append(String.format("UnknownShort: %04X<br/>", unknown))
        BUILDER.append(String.format("UnknownByte: %02x<br/>", UNKNOWN_BYTE))
        BUILDER.append("</p>")
        save.papyrus.printReferrents(this, BUILDER, "script instance")
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @return A flag indicating if the `ScriptInstance` is
     * unattached.
     */
    val isUnattached: Boolean
        get() = refID.isZero

    /**
     * @return A flag indicating that the `ScriptInstance` has a
     * canary variable.
     */
    fun hasCanary(): Boolean {
        val DESCS = script.extendedMembers
        return DESCS.stream().anyMatch(isCanary)
    }

    /**
     * @return A flag indicating that the `ScriptInstance` has a
     * canary variable.
     */
    val canary: Int
        get() {
            val MEMBERS = this.variables
            if (MEMBERS.isEmpty()) {
                return 0
            }
            val NAMES = script.extendedMembers
            val canary = NAMES.stream().filter(isCanary).findFirst()
            return if (canary.isPresent) {
                val `var` = MEMBERS[NAMES.indexOf(canary.get())]
                if (`var` is Variable.Int) {
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
        get()= script.isUndefined


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
         * @see resaver.ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer?) {
            iD.write(output)
            output!!.put(FLAG)
            this.type.write(output)
            output.putInt(UNKNOWN1)
            if ((FLAG and 0x04.toByte()).toInt() != 0) {
                output.putInt(UNKNOWN2)
            }
            output.putInt(VARIABLES!!.size)
            VARIABLES!!.forEach(Consumer { `var`: Variable -> `var`.write(output) })
        }

        /**
         * @see resaver.ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 9
            sum += iD.calculateSize()
            sum += if ((FLAG and 0x04.toByte()).toInt() != 0) 4 else 0
            sum += this.type.calculateSize()
            sum += VARIABLES!!.stream().mapToInt { obj: Variable -> obj.calculateSize() }.sum()
            return sum
        }

        /**
         * @see Object.toString
         * @return
         */
        override fun toString(): String {
            val BUILDER = StringBuilder()
            BUILDER.append("SCRIPTDATA\n")
            BUILDER.append(String.format("ID = %s\n", iD))
            BUILDER.append(String.format("flag= %d\n", FLAG))
            BUILDER.append(String.format("type = %s\n", this.type))
            BUILDER.append(String.format("unknown1 = %d\n", UNKNOWN1))
            BUILDER.append(String.format("unknown2 = %d\n\n", UNKNOWN2))
            VARIABLES!!.forEach(Consumer { `var`: Variable? -> BUILDER.append(String.format("%s\n", `var`)) })
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
        var VARIABLES: MutableList<Variable>? = null

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
            Objects.requireNonNull(input)
            Objects.requireNonNull(context)
            FLAG = input.get()
            this.type = context.readTString(input)
            UNKNOWN1 = input.int
            UNKNOWN2 = if ((FLAG and 0x04.toByte()).toInt() != 0) input.int else 0
            try {
                val count = input.int
                VARIABLES = Variable.readList(input, count, context)
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read struct variables.", ex, this)
            }
        }
    }

    companion object {
        private val isCanary = Predicate { desc: MemberDesc -> desc.name.equals("::iPapyrusDataVerification_var") }
        private val LOG = Logger.getLogger(ScriptInstance::class.java.canonicalName)
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
        UNKNOWN_FO_BYTE = if (context.game.isFO4) {
            if ((UNKNOWN2BITS and 0x3.toShort()).toInt() == 0x3) {
                input.get()
            } else {
                null
            }
        } else {
            null
        }
    }
}