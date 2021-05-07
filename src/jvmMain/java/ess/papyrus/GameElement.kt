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

import resaver.Analysis
import ess.AnalyzableElement
import ess.Linkable
import java.nio.ByteBuffer
import java.util.*

/**
 * `GameElement` is a superclass of `ScriptInstance`,
 * `Reference`, and `Struct`, for situations in which they
 * are interchangeable.
 *
 * @author Mark Fairchild
 */
abstract class GameElement(input: ByteBuffer, defs: Map<TString?, Definition?>, context: ess.papyrus.PapyrusContext) :
    AnalyzableElement, Linkable, PapyrusElement, HasID {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        Objects.requireNonNull(output)
        iD.write(output)
        definitionName.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 0
        sum += iD.calculateSize()
        sum += definitionName.calculateSize()
        return sum
    }

    /**
     * @return A flag indicating if the `GameElement` is undefined.
     */
    abstract val isUndefined: Boolean

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        Objects.requireNonNull(analysis)
        Objects.requireNonNull(mod)
        val OWNERS = analysis!!.SCRIPT_ORIGINS[definitionName.toIString()] ?: return false
        return OWNERS.contains(mod)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        if (isUndefined) {
            BUF.append("#").append(definitionName).append("# ")
        } else {
            BUF.append(definitionName).append(": ")
        }
        BUF.append(" (").append(iD).append(")")
        return BUF.toString()
    }

    /**
     * @return The ID of the papyrus element.
     */
    final override val iD: EID

    /**
     * @return The name of the corresponding `Definition`.
     */
    val definitionName: TString

    /**
     * @return The corresponding `Definition`.
     */
    val definition: Definition?

    /**
     * Creates a new `GameElement` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param defs The list of definitions.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     */
    init {
        Objects.requireNonNull(input)
        Objects.requireNonNull(defs)
        Objects.requireNonNull(context)
        iD = context.readEID(input)
        definitionName = context.readTString(input)
        definition = defs.getOrDefault(definitionName, null)
        definition!!.incrementInstanceCount()
    }
}