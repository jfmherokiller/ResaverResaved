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

import PlatformByteBuffer
import resaver.Analysis
import ess.AnalyzableElement
import ess.Element
import ess.Linkable
import ess.papyrus.EID.Companion.pad8


/**
 * Describes a function message in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `FunctionMessage` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusElementException
 */
class FunctionMessage(input: PlatformByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable {
    /**
     * @see Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(UNKNOWN)
        iD?.write(output)
        output.put(FLAG)
        message?.write(output)
    }

    /**
     * @see Element.calculateSize
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
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (null != target && null != message) {
            var result: Variable? = null
            for (v in message!!.variables) {
                if (v.hasRef()) {
                    if (v.referent === target) {
                        result = v
                        break
                    }
                }
            }
            if (result != null) {
                val i: Int = message!!.variables.indexOf(result)
                if (i >= 0) {
                    return Linkable.makeLink("message", iD, i, this.toString())
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
            "MSG $FLAG,${pad8(UNKNOWN.toInt())} ($iD)"
        } else {
            "MSG $FLAG,${pad8(UNKNOWN.toInt())}"
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ess.ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>FUNCTIONMESSAGE</h3>")
        if (null != analysis && null != message) {
            val scriptName = message!!.scriptName.toIString()
            val mods = analysis.SCRIPT_ORIGINS[scriptName]
            if (null != mods) {
                val mod = mods.last()
                BUILDER.append("<p>Probably running code from mod $mod.</p>")
            }
        }
        BUILDER.append("<p>")
        if (message != null) {
            BUILDER.append("Function: ${message!!.fName}<br/>")
        }
        BUILDER.append("ID: $iD<br/>")
        if (THREAD != null) {
            BUILDER.append("ActiveScript: ${THREAD.toHTML(null)}<br/>")
        } else {
            BUILDER.append("ActiveScript: NONE<br/>")
        }
        BUILDER.append("Flag: $FLAG<br/>")
        BUILDER.append("Unknown: $UNKNOWN<br/>")
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
        return hasMessage() && message!!.matches(analysis, mod)
    }

    /**
     * @return A flag indicating if the `FunctionMessage` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = hasMessage() && message!!.isUndefined
    private val UNKNOWN: Byte = input.getByte()

    /**
     * @return The ID of the papyrus element.
     */
    val iD: EID? = if (UNKNOWN <= 2) context.readEID32(input) else null
    private val FLAG: Byte = input.getByte()

    /**
     * @return The message field.
     */
    var message: FunctionMessageData? = null
    private val THREAD: ActiveScript? = if (iD == null) null else context.findActiveScript(iD)
    //, HasID {

    init {
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