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
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Linkable
import java.nio.ByteBuffer



/**
 * Describes a structure in a Fallout 4 savegame.
 *
 * @author Mark Fairchild
 */
class Struct(input: ByteBuffer, context: PapyrusContext) : Definition() {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        assert(null != output)
        name.write(output)
        output!!.putInt(MEMBERS!!.size)
        MEMBERS!!.forEach { member: MemberDesc -> member.write(output) }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 4
        sum += name.calculateSize()
        var result = 0
        for (MEMBER in MEMBERS!!) {
            val calculateSize = MEMBER.calculateSize()
            result += calculateSize
        }
        sum += result
        return sum
    }

    /**
     * @return The list of `MemberDesc`.
     */
    override val members: List<MemberDesc>
        get() = MEMBERS!!

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (target is MemberDesc) {
            val i = members.indexOf(target)
            if (i >= 0) {
                return Linkable.makeLink("struct", name, i, name.toString())
            }
        }
        return Linkable.makeLink("struct", name, name.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return name.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html>")
        BUILDER.append("<h3>STRUCTURE DEFINITION ${name}s</h3>")
        if (null != analysis) {
            val mods = analysis.SCRIPT_ORIGINS[name.toIString()]
            if (null != mods) {
                if (mods.size > 1) {
                    BUILDER.append("<p>WARNING: MORE THAN ONE MOD PROVIDES THIS SCRIPT!<br />Exercise caution when editing or deleting this script!</p>")
                }
                val probablyProvider = mods.last()
                BUILDER.append("<p>This script probably came from \"$probablyProvider\".</p>")
                BUILDER.append("<p>Full list of providers:</p>")
                BUILDER.append("<ul>")
                mods.forEach { mod: String? -> BUILDER.append("<li>$mod") }
                BUILDER.append("</ul>")
            }
        }
        BUILDER.append(String.format("<p>Contains %d member variables.</p>", MEMBERS!!.size))
        val STRUCTS: MutableList<StructInstance> = ArrayList()
        for (instance in save!!.papyrus
            .structInstances
            .values) {
            if (instance.struct == this) {
                STRUCTS.add(instance)
            }
        }
        BUILDER.append(String.format("<p>There are %d instances of this structure definition.</p>", STRUCTS.size))
        if (STRUCTS.size < 20) {
            BUILDER.append("<ul>")
            STRUCTS.forEach { i: StructInstance ->
                val s = "<li>${i.toHTML(this)}</a>"
                BUILDER.append(s)
            }
            BUILDER.append("</ul>")
        }

        /*if (null != analysis && analysis.STRUCT_ORIGINS.containsKey(this.NAME)) {
            final java.io.File PEXFILE = analysis.SCRIPTS.get(this.NAME);
            final java.io.File PARENT = PEXFILE.getParentFile();

            BUILDER.append("");
            BUILDER.append(String.format("<hr /><p>Disassembled source code:<br />(from %s)</p>", PEXFILE.getPath()));

            if (PEXFILE.exists() && PEXFILE.canRead()) {
                try {
                    final resaver.pex.Pex SCRIPT = resaver.pex.Pex.readScript(PEXFILE);

                    java.io.StringWriter code = new java.io.StringWriter();
                    SCRIPT.disassemble(code, resaver.pex.AssemblyLevel.STRIPPED);

                    BUILDER.append("<p<code><pre>");
                    BUILDER.append(code.getBuffer());
                    BUILDER.append("</pre></code></p>");

                } catch (RuntimeException ex) {
                    BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                } catch (java.io.IOException ex) {
                    BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                } catch (Error ex) {
                    BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                }

            } else if (PARENT.exists() && PARENT.isFile()) {
                try (resaver.LittleEndianRAF input = new resaver.LittleEndianRAF(PARENT, "r")) {
                    resaver.bsa.BSAParser BSA = new resaver.bsa.BSAParser(PARENT.getName(), input);
                    final resaver.pex.Pex SCRIPT = BSA.getScript(PEXFILE.getName());

                    java.io.StringWriter code = new java.io.StringWriter();
                    SCRIPT.disassemble(code, resaver.pex.AssemblyLevel.STRIPPED);

                    BUILDER.append("<p<code><pre>");
                    BUILDER.append(code.getBuffer());
                    BUILDER.append("</pre></code></p>");

                } catch (RuntimeException ex) {
                    BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                } catch (java.io.IOException ex) {
                    BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                } catch (Error ex) {
                    BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                }
            }
        }
         */BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {


        //final SortedSet<String> OWNERS = analysis.SCRIPT_ORIGINS.get(this.NAME);
        //return null != OWNERS && OWNERS.contains(mod);
        return false
    }

    /**
     * @return A flag indicating if the `Script` is undefined.
     */
    override val isUndefined: Boolean
        get() = false

    /**
     * @return The ID of the papyrus element.
     */
    override val name: TString
    private var MEMBERS: List<MemberDesc>? = null

    /**
     * Creates a new `Structure` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    init {
        name = context.readTString(input)
        try {
            val count = input.int
            MEMBERS = MemberDesc.readList(input, count, context)
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read Struct members.", ex, this)
        }
    }
}