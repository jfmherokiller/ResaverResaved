/*
 * Copyright 2016 Mark.
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
import ess.Flags
import ess.Flags.Companion.readByteFlags
import ess.Linkable.Companion.makeLink
import ess.papyrus.Variable.Companion.readList
import resaver.Analysis
import resaver.ListException
import java.nio.ByteBuffer
import java.util.*


/**
 *
 * @author Mark Fairchild
 */
class StructInstance
/**
 * Creates a new `Struct` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param structs The `StructMap` containing the definitions.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusFormatException
 */
    (input: ByteBuffer, structs: StructMap, context: PapyrusContext, override val descriptors: List<MemberDesc?>?) : GameElement(input, structs, context),
    SeparateData, HasVariables {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
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
    override fun readData(input: ByteBuffer?, context: PapyrusContext?) {
        data = StructData(input!!, context!!)
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
        sum += if (data == null) 0 else data!!.calculateSize()
        return sum
    }

    /**
     * @return The corresponding `Struct`.
     */
    val struct: Struct?
        get() {
            assert(super.definition is Struct)
            return super.definition as Struct?
        }

    /**
     * @return A flag indicating if the `StructInstance` is
     * undefined.
     */
    override val isUndefined: Boolean
        get() = if (null != struct) {
            struct!!.isUndefined
        } else false

    /**
     * @return The flag field.
     */
    val flag: Flags.FlagsByte?
        get() = if (null == data) null else data!!.FLAG

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override val variables: List<Variable>
        get() = if (data == null) emptyList() else data!!.VARIABLES.filterNotNull()

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        if (data == null || data!!.VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require(!(index <= 0 || index >= data!!.VARIABLES.size)) { "Invalid variable index: $index" }
        data!!.VARIABLES.set(index, newVar)
    }

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String? {
        return if (null == target || null == data) {
            makeLink("structinstance", iD, this.toString())
        } else if (target is Variable) {
            val index = variables.indexOf(target)
            if (index >= 0) {
                makeLink("structinstance", iD, index, this.toString())
            } else {
                makeLink("structinstance", iD, this.toString())
            }
        } else {
            for (`var` in variables) {
                if (`var`.hasRef()) {
                    if (`var`.referent === target) {
                        val indexOf = variables.indexOf(`var`)
                        if (indexOf >= 0) {
                            return Optional.of(indexOf)
                                .map { index: Int? -> makeLink("structinstance", iD, index!!, this.toString()) }
                                .orElse(makeLink("structinstance", iD, this.toString()))
                        }
                    }
                }
            }
            Optional.empty<Int>()
                .map { index: Int? -> makeLink("structinstance", iD, index!!, this.toString()) }
                .orElse(makeLink("structinstance", iD, this.toString()))
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        if (null != struct) {
            BUILDER.append(String.format("<html><h3>STRUCTURE of %s</h3>", struct!!.toHTML(this)))
        } else {
            BUILDER.append(String.format("<html><h3>STRUCTURE of %s</h3>", StructName))
        }

        /*if (null != analysis) {
            SortedSet<String> providers = analysis.SCRIPT_ORIGINS.get(this.getScriptName());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>This struct probably came from \"%s\".</p>", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }*/BUILDER.append(String.format("<p>ID: %s</p>", iD))
        if (null == data) {
            BUILDER.append("<h3>DATA MISSING</h3>")
        } else {
            BUILDER.append(String.format("<p>Flag: %s</p>", flag))
        }
        save?.papyrus!!.printReferrents(this, BUILDER, "struct")
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    private var data: StructData? = null

    /**
     * Describes struct data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    private inner class StructData(input: ByteBuffer, context: PapyrusContext) : PapyrusDataFor<StructInstance?> {
        /**
         * @see ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer?) {
            iD.write(output)
            FLAG.write(output)
            output?.putInt(VARIABLES.size)
            VARIABLES.forEach { `var`: Variable? -> `var`!!.write(output) }
        }

        /**
         * @see ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 4
            sum += FLAG.calculateSize()
            sum += iD.calculateSize()
            var result = 0
            for (VARIABLE in VARIABLES) {
                val calculateSize = VARIABLE!!.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        /**
         * @return String representation.
         */
        override fun toString(): String {
            return iD.toString() + VARIABLES
        }

        //final private EID ID;
        val FLAG: Flags.FlagsByte
        var VARIABLES: MutableList<Variable?> = mutableListOf()

        /**
         * Creates a new `StructData` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         */
        init {
            FLAG = readByteFlags(input)
            try {
                val count = input.int
                VARIABLES = readList(input, count, context).toMutableList()
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read struct variables.", ex, this)
            }
        }
    }
}