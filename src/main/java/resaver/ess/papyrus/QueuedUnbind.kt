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



/**
 * Describes a function parameter in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class QueuedUnbind(input: ByteBuffer, context: PapyrusContext) : PapyrusElement, AnalyzableElement, Linkable, HasID {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        iD.write(output)
        output!!.putInt(unknown)
    }

    /**
     * @see resaver.ess.Element.calculateSize
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
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        if (null != scriptInstance && null != scriptInstance.script) {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", scriptInstance.script.toHTML(this)))
        } else if (null != scriptInstance) {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", scriptInstance.scriptName))
        } else {
            BUILDER.append(String.format("<html><h3>QUEUED UNBIND of %s</h3>", iD))
        }
        if (null == scriptInstance) {
            BUILDER.append(String.format("<p>Instance: %s</p>", iD))
        } else {
            BUILDER.append(String.format("<p>Instance: %s</p>", scriptInstance.toHTML(this)))
        }
        BUILDER.append(String.format("<p>Unknown: %s</p>", pad8(unknown)))
        val UNK = save!!.papyrus.context.broadSpectrumSearch(unknown)
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
                    String.format(
                        "The queued unbinding probably came from mod \"%s\".\n\n",
                        probablyProvider
                    )
                )
                if (providers.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    providers.forEach { mod: String? -> BUILDER.append(String.format("<li>%s", mod)) }
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
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element): String {
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