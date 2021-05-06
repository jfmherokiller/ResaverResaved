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
import resaver.ess.AnalyzableElement
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Linkable
import resaver.ess.papyrus.Type.Companion.read
import java.nio.ByteBuffer



/**
 * Describes an array in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `ArrayData` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusElementException
 */
class ArrayInfo(input: ByteBuffer, context: PapyrusContext) : AnalyzableElement, Linkable, HasID, SeparateData,
    HasVariables {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        iD.write(output)
        type.write(output)
        refType?.write(output)
        output!!.putInt(length)
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
        data = input?.let { ArrayData(it, context) }
    }

    /**
     * @see SeparateData.writeData
     * @param input
     */
    override fun writeData(input: ByteBuffer?) {
        data!!.write(input)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 5
        sum += iD.calculateSize()
        if (null != refType) {
            sum += refType.calculateSize()
        }
        sum += if (data == null) 0 else data!!.calculateSize()
        return sum
    }

    /**
     * @return Short string representation.
     */
    fun toValueString(): String {
        return if (type.isRefType) {
            refType.toString() + "[" + length + "]"
        } else if (type === Type.NULL && variables.isNotEmpty()) {
            val t = variables[0]?.type
            "$t[$length]"
        } else {
            "$type[$length]"
        }
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (null != target && null != data) {
            var result: Variable? = null
            for (v in variables) {
                if (v?.hasRef() == true) {
                    if (v.referent === target) {
                        result = v
                        break
                    }
                }
            }
            if (result != null) {
                val i = variables.indexOf(result)
                if (i >= 0) {
                    return Linkable.makeLink("array", iD, i, this.toString())
                }
            }
        }
        return Linkable.makeLink("array", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return "${toValueString()} $iD"
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>ARRAY</h3>")
        if (HOLDERS.isEmpty()) {
            BUILDER.append("<p><em>WARNING: THIS ARRAY HAS NO OWNER.</em></p>")
        } else {
            BUILDER.append("<p>Owners:</p><ul>")
            HOLDERS.forEach { owner: PapyrusElement? ->
                if (owner is Linkable) {
                    BUILDER.append(
                        String.format(
                            "<li>%s %s",
                            owner.javaClass.simpleName,
                            (owner as Linkable).toHTML(this)
                        )
                    )
                } else if (owner != null) {
                    BUILDER.append(String.format("<li>%s %s", owner.javaClass.simpleName, owner))
                }
            }
            BUILDER.append("</ul>")
        }
        if (null != analysis) {
            HOLDERS.forEach { owner: PapyrusElement? ->
                if (owner is ScriptInstance) {
                    val mods = analysis.SCRIPT_ORIGINS[owner.scriptName.toIString()]
                    if (null != mods) {
                        val mod = mods.last()
                        val type = owner.scriptName
                        BUILDER.append(
                            "<p>Probably created by script <a href=\"script://$type\">$type</a> which came from mod \"$mod\".</p>"
                        )
                    }
                }
            }
        }
        BUILDER.append("<p/>")
        BUILDER.append(String.format("<p>ID: %s</p>", iD))
        BUILDER.append(String.format("<p>Content type: %s</p>", type))
        if (type.isRefType) {
            val SCRIPT = save!!.papyrus.scripts[refType]
            if (null != SCRIPT) {
                BUILDER.append(String.format("<p>Reference type: %s</p>", SCRIPT.toHTML(this)))
            } else {
                BUILDER.append(String.format("<p>Reference type: %s</p>", refType))
            }
        }
        BUILDER.append(String.format("<p>Length: %d</p>", length))
        //BUILDER.append("</p>");
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        return false
    }

    /**
     * @return The holder of the array, if there is exactly one. Null otherwise.
     */
    val holder: PapyrusElement?
        get() = if (HOLDERS.size == 1) {
            HOLDERS.iterator().next()
        } else {
            null
        }

    /**
     * Adds an element as a reference holder.
     *
     * @param newHolder The new reference holder.
     */
    fun addRefHolder(newHolder: PapyrusElement) {
        HOLDERS.add(newHolder)
    }

    /**
     * @see HasVariables.variables
     * @return
     */
    override val variables: List<Variable?>
        get() = if (data == null) emptyList() else data!!.VARIABLES!!

    /**
     * @see HasVariables.descriptors
     * @return An empty `List`.
     */
    override val descriptors: List<MemberDesc>
        get() = emptyList()

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        if (data == null || data!!.VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require(!(index <= 0 || index >= data!!.VARIABLES!!.size)) { "Invalid variable index: $index" }
        data!!.VARIABLES!![index] = newVar
    }

    /**
     * @return The ID of the papyrus element.
     */
    override val iD: EID = context.readEID(input)

    /**
     * @return The type of the array.
     */
    val type: Type

    /**
     * @return The reference type of the array.
     */
    val refType: TString?

    /**
     * @return the length of the array.
     */
    val length: Int
    private val HOLDERS: MutableCollection<PapyrusElement> = ArrayList(1)
    private var data: ArrayData? = null

    /**
     * Describes array data in a Skyrim savegame.
     *
     * @author Mark Fairchild
     */
    private inner class ArrayData(input: ByteBuffer, context: PapyrusContext?) : PapyrusDataFor<ArrayInfo?> {
        /**
         * @see resaver.ess.Element.write
         * @param output The output stream.
         */
        override fun write(output: ByteBuffer?) {
            iD.write(output)
            VARIABLES!!.forEach { `var`: Variable? -> `var`!!.write(output) }
        }

        /**
         * @see resaver.ess.Element.calculateSize
         * @return The size of the `Element` in bytes.
         */
        override fun calculateSize(): Int {
            var sum = iD.calculateSize()
            var result = 0
            for (VARIABLE in VARIABLES!!) {
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
        var VARIABLES: MutableList<Variable?>? = null


        init {
            try {
                val count = length
                VARIABLES = Variable.readList(input, count, context)
            } catch (ex: ListException) {
                throw PapyrusElementException("Couldn't read Array variables.", ex, this)
            }
        }
    }

    companion object {
        private val VALID_TYPES = listOf(
            Type.NULL,
            Type.REF,
            Type.STRING,
            Type.INTEGER,
            Type.FLOAT,
            Type.BOOLEAN,
            Type.VARIANT,
            Type.STRUCT
        )
    }

    /**
     * Creates a new `ArrayInfo` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     */
    init {
        val t = read(input)
        if (!VALID_TYPES.contains(t)) {
            throw PapyrusFormatException("Invalid ArrayInfo type: $t")
        }
        type = t
        refType = if (type.isRefType) context.readTString(input) else null
        length = input.int
    }
}