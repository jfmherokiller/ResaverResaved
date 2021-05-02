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
package resaver.ess.papyrus

import resaver.Analysis
import resaver.ListException
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Flags
import resaver.ess.Linkable
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer

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
    (input: ByteBuffer, structs: StructMap, context: PapyrusContext) : GameElement(input, structs, context),
    SeparateData, HasVariables {
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
        data = StructData(input, context)
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
     * @return The name of the corresponding `Struct`.
     */
    val structName: TString
        get() = super.definitionName

    /**
     * @return The corresponding `Struct`.
     */
    val struct: Struct
        get() {
            assert(super.definition is Struct)
            return super.definition as Struct
        }

    /**
     * @return A flag indicating if the `StructInstance` is
     * undefined.
     */
    override val isUndefined: Boolean
        get() = struct.isUndefined

    /**
     * @return The flag field.
     */
    val flag: Flags.Byte?
        get() = if (null == data) null else data!!.FLAG

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
    override fun getDescriptors(): List<MemberDesc?>? {
        return struct.members
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
    override fun toHTML(target: Element): String {
        return if (null == data) {
            Linkable.makeLink("structinstance", this.iD, this.toString())
        } else if (target is Variable) {
            val index = this.variables.indexOf(target)
            if (index >= 0) {
                Linkable.makeLink("structinstance", this.iD, index, this.toString())
            } else {
                Linkable.makeLink("structinstance", this.iD, this.toString())
            }
        } else {
            this.variables.stream()
                .filter { obj: Variable -> obj.hasRef() }
                .filter { `var`: Variable -> `var`.referent === target }
                .map { `var`: Variable -> this.variables.indexOf(`var`) }
                .filter { index: Int -> index >= 0 }
                .findFirst()
                .map { index: Int? -> Linkable.makeLink("structinstance", this.iD, index!!, this.toString()) }
                .orElse(Linkable.makeLink("structinstance", this.iD, this.toString()))
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis, save: ESS): String {
        val BUILDER = StringBuilder()
        BUILDER.append(String.format("<html><h3>STRUCTURE of %s</h3>", struct.toHTML(this)))

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
        }*/BUILDER.append(String.format("<p>ID: %s</p>", this.iD))
        if (null == data) {
            BUILDER.append("<h3>DATA MISSING</h3>")
        } else {
            BUILDER.append(String.format("<p>Flag: %s</p>", flag))
        }
        save.papyrus.printReferrents(this, BUILDER, "struct")
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    private var data: StructData? = null

    /**
     * Describes struct data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    private inner class StructData(input: ByteBuffer, context: PapyrusContext?) : PapyrusDataFor<StructInstance?> {
        /**
         * @see resaver.ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer) {
            Objects.requireNonNull(output)
            iD.write(output)
            FLAG.write(output)
            output.putInt(VARIABLES!!.size)
            VARIABLES!!.forEach(Consumer { `var`: Variable -> `var`.write(output) })
        }

        /**
         * @see resaver.ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = 4
            sum += FLAG.calculateSize()
            sum += iD.calculateSize()
            sum += VARIABLES!!.stream().mapToInt { obj: Variable -> obj.calculateSize() }.sum()
            return sum
        }

        /**
         * @return String representation.
         */
        override fun toString(): String {
            return iD.toString() + VARIABLES
        }

        //final private EID ID;
        val FLAG: Flags.Byte
        var VARIABLES: MutableList<Variable>? = null

        /**
         * Creates a new `StructData` by reading from a
         * `ByteBuffer`. No error handling is performed.
         *
         * @param input The input stream.
         * @param context The `PapyrusContext` info.
         * @throws PapyrusElementException
         */
        init {
            Objects.requireNonNull(input)
            Objects.requireNonNull(context)
            FLAG = Flags.readByteFlags(input)
            try {
                val count = input.int
                VARIABLES = Variable.readList(input, count, context)
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read struct variables.", ex, this)
            }
        }
    }
}