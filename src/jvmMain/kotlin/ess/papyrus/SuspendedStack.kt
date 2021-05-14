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
import ess.AnalyzableElement
import ess.Element
import ess.Linkable
import resaver.Analysis


/**
 * Describes an active script's stack in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `SuspendedStack` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusFormatException
 * @throws PapyrusElementException
 */
class SuspendedStack constructor(input: PlatformByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable, HasID {

    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        iD.write(output)
        output!!.put(FLAG)
        assert(FLAG > 0 || message == null)
        message?.write(output)
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1
        sum += iD.calculateSize()
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
        get() = if (hasMessage()) {
            message!!.script
        } else {
            null
        }

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (null != target && hasMessage()) {
            var result: Variable? = null
            for (v in message?.variables!!) {
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
                    return Linkable.makeLink("suspended", iD, i, this.toString())
                }
            }
        }
        return Linkable.makeLink("suspended", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        when {
            hasMessage() -> {
                return message.toString()
            }
            THREAD != null -> {
                return THREAD.toString()
            }
            else -> {
                BUF.append(iD)
            }
        }
        return BUF.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ess.ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>SUSPENDEDSTACK</h3>")
        if (THREAD != null) {
            BUILDER.append("<p>ActiveScript: ${THREAD.toHTML(this)}</p>")
        }
        if (hasMessage()) {
            if (null != analysis) {
                val mods = analysis.SCRIPT_ORIGINS[message!!.scriptName.toIString()]
                if (null != mods) {
                    BUILDER.append("<p>Probably running code from mod ${mods.last()}.</p>")
                }
            }
            BUILDER.append("<p>Function: ${message!!.fName}</p>")
        }
        BUILDER.append(String.format("<p>Flag: %02x</p>", FLAG))
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
     * @return A flag indicating if the `SuspendedStack` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = hasMessage() && message!!.isUndefined

    /**
     * @return The ID of the papyrus element.
     */
    override val iD: EID
    private val FLAG: Byte

    /**
     * @return The message field.
     */
    var message: FunctionMessageData? = null
    private val THREAD: ActiveScript?

    init {
        iD = context.readEID32(input)
        FLAG = input.getByte()
        THREAD = context.findActiveScript(iD)
        if (FLAG.toInt() == 0) {
            message = null
        } else {
            var message: FunctionMessageData? = null
            try {
                message = FunctionMessageData(input, this, context)
            } catch (ex: PapyrusElementException) {
                message = ex.partial as FunctionMessageData
                throw PapyrusElementException("Failed to read message for SuspendedStack.", ex, this)
            } catch (ex: PapyrusFormatException) {
                throw PapyrusElementException("Failed to read message for SuspendedStack.", ex, this)
            } finally {
                this.message = message
            }
        }
    }


}