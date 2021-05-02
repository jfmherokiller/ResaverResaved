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
import resaver.ess.papyrus.Script.Companion.NATIVE_SCRIPTS
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer
import kotlin.experimental.and

/**
 * Describes a reference in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class Reference
/**
 * Creates a new `Reference` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param scripts The `ScriptMap` containing the definitions.
 * @param context The `PapyrusContext` info.
 */
internal constructor(input: ByteBuffer, scripts: ScriptMap, context: PapyrusContext) :
    GameElement(input, scripts, context), SeparateData, HasVariables {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        super.write(output)
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
        data = ReferenceData(input, context)
    }

    /**
     * @see SeparateData.writeData
     * @param input
     */
    override fun writeData(input: ByteBuffer) {
        data!!.write(input)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = super.calculateSize()
        sum += if (data == null) 0 else data!!.calculateSize()
        return sum
    }

    /**
     * @return The name of the corresponding `Script`.
     */
    val scriptName: TString
        get() = super.definitionName

    /**
     * @return The corresponding `Script`.
     */
    val script: Script?
        get() {
            assert(super.definition is Script)
            return super.definition as Script
        }

    /**
     * @return A flag indicating if the `Reference` is undefined.
     */
    override val isUndefined: Boolean
        get() = if (null != script) {
            script!!.isUndefined
        } else !NATIVE_SCRIPTS.contains(scriptName.toIString())


    /**
     * @return The type of the reference.
     */
    val type: TString?
        get() = if (null == data) null else data!!.TYPE

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override fun getVariables(): List<Variable> {
        return if (data == null) emptyList() else Collections.unmodifiableList(data!!.VARIABLES)
    }

    /**
     * @see HasVariables.getDescriptors
     * @return
     */
    override fun getDescriptors(): List<MemberDesc> {
        return script!!.extendedMembers
    }

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable) {
        if (data == null || data!!.VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require(!(index <= 0 || index >= data!!.VARIABLES!!.size)) { "Invalid variable index: $index" }
        data!!.VARIABLES!![index] = newVar
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (null != target) {
            if (target is Variable) {
                val i = this.variables.indexOf(target)
                if (i >= 0) {
                    return Linkable.makeLink("reference", this.iD, i, this.toString())
                }
            } else {
                val result = this.variables.stream()
                    .filter { obj: Variable -> obj.hasRef() }
                    .filter { v: Variable -> v.referent === target }
                    .findFirst()
                if (result.isPresent) {
                    val i = this.variables.indexOf(result.get())
                    if (i >= 0) {
                        return Linkable.makeLink("reference", this.iD, i, this.toString())
                    }
                }
            }
        }
        return Linkable.makeLink("reference", this.iD, this.toString())
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis, save: ESS): String {
        val BUILDER = StringBuilder()
        if (null != script) {
            BUILDER.append(String.format("<html><h3>REFERENCE of %s</h3>", script!!.toHTML(this)))
        } else {
            BUILDER.append(String.format("<html><h3>REFERENCE of %s</h3>", scriptName))
        }
        val providers = analysis.SCRIPT_ORIGINS[scriptName.toIString()]
        if (null != providers) {
            val probablyProvider = providers.last()
            BUILDER.append(String.format("<p>This script probably came from \"%s\".</p>", probablyProvider))
            if (providers.size > 1) {
                BUILDER.append("<p>Full list of providers:</p><ul>")
                providers.forEach(Consumer { mod: String? -> BUILDER.append(String.format("<li>%s", mod)) })
                BUILDER.append("</ul>")
            }
        }
        BUILDER.append(String.format("<p>ID: %s</p>", this.iD))
        BUILDER.append(String.format("<p>Type2: %s</p>", type))
        BUILDER.append(String.format("<p>Unknown1: %08x</p>", data!!.UNKNOWN1))
        BUILDER.append(String.format("<p>Unknown2: %08x</p>", data!!.UNKNOWN2))
        val UNKNOWN1 = save.papyrus.context.broadSpectrumSearch(data!!.UNKNOWN1)
        if (null != UNKNOWN1) {
            BUILDER.append("<p>Potential match for unknown1 found using general search:<br/>")
            BUILDER.append(UNKNOWN1.toHTML(this))
            BUILDER.append("</p>")
        }
        val UNKNOWN2 = save.papyrus.context.broadSpectrumSearch(data!!.UNKNOWN2)
        if (null != UNKNOWN2) {
            BUILDER.append("<p>Potential match for unknown2 found using general search:<br/>")
            BUILDER.append(UNKNOWN2.toHTML(this))
            BUILDER.append("</p>")
        }
        save.papyrus.printReferrents(this, BUILDER, "reference")
        BUILDER.append("</html>")
        return BUILDER.toString()
    }
    /**
     * @return The `ReferenceData` for the instance.
     */
    /**
     * Sets the data field.
     *
     * @param newData The new value for the data field.
     */
    var data: ReferenceData? = null

    inner class ReferenceData(input: ByteBuffer, context: PapyrusContext) : PapyrusDataFor<Reference?> {
        /**
         * @see resaver.ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer) {
            iD.write(output)
            output.put(FLAG)
            TYPE.write(output)
            output.putInt(UNKNOWN1)
            if ((FLAG and 0x04).toInt() != 0) {
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
            if ((FLAG and 0x04).toInt() != 0) {
                sum += 4
            }
            sum += TYPE.calculateSize()
            sum += VARIABLES!!.parallelStream().mapToInt { obj: Variable -> obj.calculateSize() }
                .sum()
            return sum
        }

        private val FLAG: Byte
        val TYPE: TString
        val UNKNOWN1: Int
        val UNKNOWN2: Int
        var VARIABLES: MutableList<Variable>? = null

        /**
         * Creates a new `ReferenceData` by reading from a
         * `LittleEndianDataOutput`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         * @throws PapyrusFormatException
         */
        init {
            FLAG = input.get()
            TYPE = context.readTString(input)
            UNKNOWN1 = input.int
            UNKNOWN2 = if ((FLAG and 0x04).toInt() != 0) input.int else 0
            try {
                val count = input.int
                VARIABLES = Variable.readList(input, count, context)
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read struct variables.", ex, this)
            }
        }
    }
}