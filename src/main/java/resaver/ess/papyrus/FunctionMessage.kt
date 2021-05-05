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
import resaver.ess.AnalyzableElement
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Linkable
import resaver.ess.papyrus.EID.Companion.pad8
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Predicate

/**
 * Describes a function message in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class FunctionMessage(input: ByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.put(UNKNOWN)
        if (iD != null) {
            iD.write(output)
        }
        output.put(FLAG)
        message?.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 2
        sum += iD?.calculateSize() ?: 0
        sum += message?.calculateSize() ?: 0
        return sum
    }

    /**
     * @return The message field.
     */
    fun hasMessage(): Boolean {
        return message != null
    }

    /**
     * @return The corresponding `Script`.
     */
    val script: Script?
        get() = if (hasMessage()) message!!.script else null

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element): String {
        if (null != message) {
            val result: Optional<Variable?>? = message!!.variables?.stream()
                ?.filter { obj: Variable? -> obj!!.hasRef() }
                ?.filter { v: Variable? -> v!!.referent === target }
                ?.findFirst()
            if (result != null) {
                if (result.isPresent) {
                    val i: Int? = message!!.variables?.indexOf(result.get())
                    if (i != null) {
                        if (i >= 0) {
                            return Linkable.makeLink("message", iD, i, this.toString())
                        }
                    }
                }
            }
        }
        return Linkable.makeLink("message", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (message != null) {
            val scriptName = message!!.scriptName
            if (isUndefined) {
                if (UNKNOWN <= 2) {
                    "MSG #$scriptName# ($iD)"
                } else {
                    "MSG #$scriptName"
                }
            } else if (UNKNOWN <= 2) {
                "MSG $scriptName ($iD)"
            } else {
                "MSG $scriptName"
            }
        } else if (iD != null) {
            "MSG " + FLAG + "," + pad8(UNKNOWN.toInt()) + " (" + iD + ")"
        } else {
            "MSG " + FLAG + "," + pad8(UNKNOWN.toInt())
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String? {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>FUNCTIONMESSAGE</h3>")
        if (null != analysis && null != message) {
            val scriptName = message!!.scriptName.toIString()
            val mods = analysis.SCRIPT_ORIGINS[scriptName]
            if (null != mods) {
                val mod = mods.last()
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mod))
            }
        }
        BUILDER.append("<p>")
        if (message != null) {
            BUILDER.append(String.format("Function: %s<br/>", message!!.fName))
        }
        BUILDER.append(String.format("ID: %s<br/>", iD))
        if (THREAD != null) {
            BUILDER.append(String.format("ActiveScript: %s<br/>", THREAD.toHTML(null)))
        } else {
            BUILDER.append("ActiveScript: NONE<br/>")
        }
        BUILDER.append(String.format("Flag: %s<br/>", FLAG))
        BUILDER.append(String.format("Unknown: %d<br/>", UNKNOWN))
        BUILDER.append("</p>")
        if (hasMessage()) {
            BUILDER.append("<hr/>")
            BUILDER.append(message!!.getInfo(analysis, save))
        }
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
        Objects.requireNonNull(analysis)
        Objects.requireNonNull(mod)
        return hasMessage() && message!!.matches(analysis, mod)
    }

    /**
     * @return A flag indicating if the `FunctionMessage` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = hasMessage() && message!!.isUndefined
    private val UNKNOWN: Byte

    /**
     * @return The ID of the papyrus element.
     */
    val iD: EID?
    private val FLAG: Byte

    /**
     * @return The message field.
     */
    var message: FunctionMessageData? = null
    private val THREAD: ActiveScript?
    //, HasID {
    /**
     * Creates a new `FunctionMessage` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusElementException
     */
    init {
        Objects.requireNonNull(input)
        Objects.requireNonNull(context)
        UNKNOWN = input.get()
        iD = if (UNKNOWN <= 2) context.readEID32(input) else null
        FLAG = input.get()
        THREAD = if (iD == null) null else context.findActiveScript(iD)
        assert(THREAD != null)
        if (FLAG.toInt() == 0) {
            message = null
        } else {
            var message: FunctionMessageData? = null
            try {
                message = FunctionMessageData(input, this, context)
            } catch (ex: PapyrusElementException) {
                message = ex.partial as FunctionMessageData
                throw PapyrusElementException("Failed to read message for FunctionMessage.", ex, this)
            } catch (ex: PapyrusFormatException) {
                throw PapyrusElementException("Failed to read message for FunctionMessage.", ex, this)
            } finally {
                this.message = message
            }
        }
    }
}