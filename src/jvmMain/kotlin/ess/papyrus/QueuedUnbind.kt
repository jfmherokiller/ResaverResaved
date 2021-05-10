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

import resaver.Analysis
import ess.AnalyzableElement
import ess.Element
import ess.Linkable
import ess.papyrus.EID.Companion.pad8
import java.nio.ByteBuffer



/**
 * Describes a function parameter in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class QueuedUnbind(input: ByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable, HasID {
    /**
     * @see Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        iD.write(output)
        output!!.putInt(unknown)
    }

    /**
     * @see Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 4 + iD.calculateSize()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ess.ESS?): String {
        val BUILDER = StringBuilder()
        when {
            scriptInstance?.script != null -> {
                BUILDER.append("<html><h3>QUEUED UNBIND of ${scriptInstance.script!!.toHTML(this)}</h3>")
            }
            null != scriptInstance -> {
                BUILDER.append("<html><h3>QUEUED UNBIND of ${scriptInstance.scriptName}</h3>")
            }
            else -> {
                BUILDER.append("<html><h3>QUEUED UNBIND of $iD</h3>")
            }
        }
        if (null == scriptInstance) {
            BUILDER.append("<p>Instance: $iD</p>")
        } else {
            BUILDER.append("<p>Instance: ${scriptInstance.toHTML(this)}</p>")
        }
        BUILDER.append("<p>Unknown: ${pad8(unknown)}</p>")
        val UNK = save!!.papyrus?.context?.broadSpectrumSearch(unknown)
        if (null != UNK) {
            BUILDER.append("<p>Potential match for UNKNOWN found using general search:<br/>")
            BUILDER.append(UNK.toHTML(this))
            BUILDER.append("</p>")
        }
        if (null != analysis) {
            val providers = analysis.SCRIPT_ORIGINS[scriptInstance!!.scriptName.toIString()]
            if (null != providers) {
                val probablyProvider = providers.last()
                BUILDER.append(
                    "The queued unbinding probably came from mod \"$probablyProvider\".\n\n"
                )
                if (providers.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    providers.forEach { mod: String? -> BUILDER.append("<li>$mod") }
                    BUILDER.append("</ul>")
                }
            }
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
        return scriptInstance!!.matches(analysis, mod)
    }

    /**
     * @return A flag indicating if the `ScriptInstance` is
     * undefined.
     */
    val isUndefined: Boolean
        get() = scriptInstance!!.isUndefined

    /**
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return Linkable.makeLink("unbind", iD, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (null == scriptInstance) {
            "$iD: ${pad8(unknown)} (MISSING INSTANCE)"
        } else {
            "$iD: ${pad8(unknown)} (${scriptInstance.scriptName})"
        }
    }

    /**
     * @return The ID of the papyrus element.
     */
    override val iD: EID = context.readEID(input)

    /**
     * @return The unknown field.
     */
    val unknown: Int = input.int

    /**
     * @return The corresponding `ScriptInstance`.
     */
    val scriptInstance: ScriptInstance? = context.findScriptInstance(iD)

    /**
     * Creates a new `MemberData` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     */
    init {
        assert(null != scriptInstance) { "QueuedUnbind could not be associated with a script instance!" }
    }
}