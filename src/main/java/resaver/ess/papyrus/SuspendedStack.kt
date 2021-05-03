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
import java.nio.ByteBuffer
import java.util.*

/**
 * Describes an active script's stack in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class SuspendedStack(input: ByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable, HasID {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        ID.write(output)
        output.put(FLAG)
        assert(FLAG > 0 || message == null)
        if (message != null) {
            message!!.write(output)
        }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1
        sum += ID.calculateSize()
        sum += if (message == null) 0 else message!!.calculateSize()
        return sum
    }

    /**
     * @return The ID of the papyrus element.
     */
    override val iD: EID
        get() = ID


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
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (null != target && hasMessage()) {
            val result = message!!.variables.stream()
                .filter { obj: Variable -> obj.hasRef() }
                .filter { v: Variable -> v.referent === target }
                .findFirst()
            if (result.isPresent) {
                val i = message!!.variables.indexOf(result.get())
                if (i >= 0) {
                    return Linkable.makeLink("suspended", this.iD, i, this.toString())
                }
            }
        }
        return Linkable.makeLink("suspended", this.iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        if (hasMessage()) {
            return message.toString()
        } else if (THREAD != null) {
            return THREAD.toString()
        } else {
            BUF.append(ID)
        }
        return BUF.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis, save: ESS): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>SUSPENDEDSTACK</h3>")
        if (THREAD != null) {
            BUILDER.append(String.format("<p>ActiveScript: %s</p>", THREAD.toHTML(this)))
        }
        if (hasMessage()) {
            if (null != analysis) {
                val mods = analysis.SCRIPT_ORIGINS[message!!.scriptName.toIString()]
                if (null != mods) {
                    BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", mods.last()))
                }
            }
            BUILDER.append(String.format("<p>Function: %s</p>", message!!.fName))
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
    override fun matches(analysis: Analysis, mod: String): Boolean {
        Objects.requireNonNull(analysis)
        Objects.requireNonNull(mod)
        return hasMessage() && message!!.matches(analysis, mod)
    }

    /**
     * @return A flag indicating if the `SuspendedStack` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = hasMessage() && message!!.isUndefined
    private val ID: EID
    private val FLAG: Byte

    /**
     * @return The message field.
     */
    var message: FunctionMessageData? = null
    private val THREAD: ActiveScript?

    /**
     * Creates a new `SuspendedStack` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    init {
        Objects.requireNonNull(input)
        Objects.requireNonNull(context)
        ID = context.readEID32(input)
        FLAG = input.get()
        THREAD = context.findActiveScript(ID)
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