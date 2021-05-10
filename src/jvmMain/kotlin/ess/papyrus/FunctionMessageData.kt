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

import ess.AnalyzableElement
import ess.ESS
import ess.papyrus.Variable.Companion.read
import ess.papyrus.Variable.Companion.readList
import resaver.Analysis
import resaver.IString
import resaver.IString.Companion.format
import resaver.ListException
import java.nio.ByteBuffer



/**
 * Describes a function message data in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class FunctionMessageData(input: ByteBuffer, parent: PapyrusElement?, context: PapyrusContext) : PapyrusElement,
    AnalyzableElement, HasVariables {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output?.put(UNKNOWN)
        scriptName.write(output)
        event.write(output)
        UNKNOWNVAR.write(output)
        output?.putInt(VARIABLES.size)
        VARIABLES.forEach { `var`: Variable? -> `var`!!.write(output) }
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1
        sum += scriptName.calculateSize()
        sum += event.calculateSize()
        sum += UNKNOWNVAR.calculateSize()
        sum += 4
        val result = VARIABLES.sumOf { it!!.calculateSize() }
        sum += result
        return sum
    }

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override val variables: List<Variable>
        get() = VARIABLES.filterNotNull()

    /**
     * @see HasVariables.getDescriptors
     * @return
     */
    override val descriptors: MutableList<MemberDesc>?
        get() = script?.extendedMembers

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        if (VARIABLES == null) {
            throw NullPointerException("The variable list is missing.")
        }
        require(!(index <= 0 || index >= VARIABLES.size)) { "Invalid variable index: $index" }
        VARIABLES[index] = newVar
    }

    /**
     * @return The qualified name of the function being executed.
     */
    val fName: IString
        get() = format("%s.%s", scriptName, event)

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (isUndefined) {
            "#$scriptName#.$event"
        } else {
            "$scriptName.$event"
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
        if (null != script) {
            BUILDER.append("<html><h3>FUNCTIONMESSAGEDATA of ${script.toHTML(null)}</h3>")
        } else {
            BUILDER.append("<html><h3>FUNCTIONMESSAGEDATA of $scriptName</h3>")
        }
        if (null != analysis) {
            val providers = analysis.SCRIPT_ORIGINS[scriptName.toIString()]
            if (null != providers) {
                val probablyProvider = providers.last()
                BUILDER.append("<p>This message probably came from \"$probablyProvider\".</p>")
                if (providers.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    providers.forEach { mod: String? -> BUILDER.append("<li>$mod") }
                    BUILDER.append("</ul>")
                }
            }
        }
        BUILDER.append("<p>")
        if (null != script) {
            BUILDER.append("Script: ${script.toHTML(null)}<br/>")
        } else {
            BUILDER.append("Script: $scriptName<br/>")
        }
        BUILDER.append("Event: $event<br/>")
        BUILDER.append(String.format("Unknown: %02x<br/>", UNKNOWN))
        if (null != UNKNOWNVAR) {
            BUILDER.append("Unknown variable: ${UNKNOWNVAR.toHTML(null)}<br/>")
        } else {
            BUILDER.append("Unknown variable: null<br/>")
        }
        BUILDER.append(String.format("%d function variables.<br/>", VARIABLES.size))
        BUILDER.append("</p>")
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
        val OWNERS = analysis?.SCRIPT_ORIGINS?.get(scriptName.toIString()) ?: return false
        return OWNERS.contains(mod)
    }

    /**
     * @return A flag indicating if the `FunctionMessageData` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = script?.isUndefined ?: !Script.NATIVE_SCRIPTS.contains(scriptName.toWString())
    private val UNKNOWN: Byte = input.get()

    /**
     * @return The script name field.
     */
    val scriptName: TString = context.readTString(input)

    /**
     * @return The script field.
     */
    val script: Script? = context.findScript(scriptName)

    /**
     * @return The event field.
     */
    val event: TString = context.readTString(input)
    private val UNKNOWNVAR: Variable = read(input, context)
    private var VARIABLES: MutableList<Variable?> = mutableListOf()

    /**
     * Creates a new `FunctionMessageData` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param parent The parent of the message.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    init {
        try {
            val count = input.int
            VARIABLES = readList(input, count, context).toMutableList()
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read FunctionMessage variables.", ex, this)
        }
    }
}