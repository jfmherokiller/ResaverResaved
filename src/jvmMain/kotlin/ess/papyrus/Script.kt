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
import resaver.IString
import resaver.ListException
import ess.Element
import ess.Linkable
import java.nio.ByteBuffer


/**
 * Describes a script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `Script` by reading from a
 * `ByteBuffer`. No error handling is performed.
 *
 * @param input The input stream.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusFormatException
 * @throws PapyrusElementException
 */
class Script(input: ByteBuffer, context: ess.papyrus.PapyrusContext) : Definition() {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        name.write(output)
        type.write(output)
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
        sum += type.calculateSize()
        val result = MEMBERS!!.sumOf { it.calculateSize() }
        sum += result
        return sum
    }

    /**
     * @return The list of `MemberDesc`.
     */
    override val members: List<MemberDesc>
        get() = MEMBERS!!

    /**
     * @return The list of `MemberDesc` prepended by the
     * `MemberDesc` objects of all superscripts.
     */
    val extendedMembers: MutableList<MemberDesc>
        get() = if (null != parent) {
            val EXTENDED = parent!!.extendedMembers
            EXTENDED.addAll(MEMBERS!!)
            EXTENDED
        } else {
            ArrayList(MEMBERS)
        }

    /**
     * @param scripts The ScriptMap.
     */
    fun resolveParent(scripts: ess.papyrus.ScriptMap) {
        parent = scripts[type]
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        if (target is MemberDesc) {
            val i = extendedMembers.indexOf(target)
            if (i >= 0) {
                return Linkable.makeLink("script", name, i, name.toString())
            }
        }
        return Linkable.makeLink("script", name, name.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (isUndefined) {
            "#$name ($instanceCount)"
        } else "$name ($instanceCount)"
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ess.ESS?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html>")
        when {
            type.isEmpty -> {
                BUILDER.append("<h3>SCRIPT $name</h3>")
            }
            null != parent -> {
                BUILDER.append("<h3>SCRIPT $name extends ${parent!!.toHTML(this)}</h3>")
            }
            else -> {
                BUILDER.append("<h3>SCRIPT $name extends $type</h3>")
            }
        }
        if (isUndefined) {
            BUILDER.append("<p>WARNING: SCRIPT MISSING!<br />Selecting \"Remove Undefined Instances\" will delete this.</p>")
        }
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
        var inheritCount = 0
        var p = parent
        while (p != null) {
            inheritCount += p.MEMBERS!!.size
            p = p.parent
        }
        BUILDER.append(
            String.format(
                "<p>Contains %d member variables, %d were inherited.</p>",
                MEMBERS!!.size + inheritCount,
                inheritCount
            )
        )
        val INSTANCES: MutableList<ess.papyrus.ScriptInstance> = ArrayList()
        for (instance in save!!.papyrus
            .scriptInstances
            .values) {
            if (instance.script == this) {
                INSTANCES.add(instance)
            }
        }
        val REFERENCES: MutableList<ess.papyrus.Reference> = ArrayList()
        for (ref in save.papyrus
            .references
            .values) {
            if (ref.script == this) {
                REFERENCES.add(ref)
            }
        }
        BUILDER.append(String.format("<p>There are %d instances of this script.</p>", INSTANCES.size))
        if (INSTANCES.size < 20) {
            BUILDER.append("<ul>")
            INSTANCES.forEach { i: ess.papyrus.ScriptInstance ->
                val s = "<li>${i.toHTML(null)}</a>"
                BUILDER.append(s)
            }
            BUILDER.append("</ul>")
        }
        BUILDER.append(String.format("<p>There are %d references of this script.</p>", REFERENCES.size))
        if (REFERENCES.size < 20) {
            BUILDER.append("<ul>")
            REFERENCES.forEach { i: ess.papyrus.Reference ->
                val s = "<li>${i.toHTML(null)}</a>"
                BUILDER.append(s)
            }
            BUILDER.append("</ul>")
        }

        /*if (null != analysis && analysis.SCRIPTS.containsKey(this.NAME.toIString())) {
            final Path PEXFILE = analysis.SCRIPTS.get(this.NAME.toIString());
            BUILDER.append("");
            BUILDER.append(String.format("<hr /><p>Disassembled source code:<br />(from %s)</p>", PEXFILE));

            if (Files.exists(PEXFILE) && Files.isReadable(PEXFILE)) {
                try {
                    final resaver.pex.PexFile SCRIPT = resaver.pex.PexFile.readScript(PEXFILE);
                    final List<String> CODE = new LinkedList<>();
                    try {
                        SCRIPT.disassemble(CODE, AssemblyLevel.STRIPPED);
                    } catch (Exception ex) {
                        BUILDER.append("Error disassembling script: ").append(ex.getMessage());
                    }

                    BUILDER.append("<p<code><pre>");
                    CODE.forEach(s -> BUILDER.append(s).append('\n'));
                    BUILDER.append("</pre></code></p>");

                } catch (RuntimeException ex) {
                    BUILDER.append("<p><em>Error: disassembly failed.</em></p>");
                } catch (java.io.IOException ex) {
                    BUILDER.append("<p><em>Error: couldn't read the script file.</em></p>");
                } catch (Error ex) {
                    BUILDER.append("<p><em>Error: unexpected error while reading script file.</em></p>");
                }

            } else {
                Path origin = PEXFILE.getParent();
                while (origin.getNameCount() > 0 && !Files.isReadable(origin)) {
                    origin = origin.getParent();
                }

                if (Configurator.validFile(origin)) {
                    try (final LittleEndianRAF INPUT = LittleEndianRAF.open(origin);
                            final ArchiveParser PARSER = ArchiveParser.createParser(origin, INPUT)) {
                        final PexFile SCRIPT = PARSER.getScript(PEXFILE);

                        final List<String> CODE = new LinkedList<>();
                        try {
                            SCRIPT.disassemble(CODE, AssemblyLevel.STRIPPED);
                        } catch (Exception ex) {
                            BUILDER.append("Error disassembling script: ").append(ex.getMessage());
                        }

                        BUILDER.append("<p<code><pre>");
                        CODE.forEach(s -> BUILDER.append(s).append('\n'));
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
        }*/BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        val OWNERS = analysis!!.SCRIPT_ORIGINS[name.toIString()]
        return null != OWNERS && OWNERS.contains(mod)
    }

    /**
     * @return A flag indicating if the `Script` is undefined.
     */
    override val isUndefined: Boolean
        get() = if (null != type && !type.isEmpty) {
            false
        } else !NATIVE_SCRIPTS.contains(name.toIString())

    /**
     * @return The name of the papyrus element.
     */
    override val name: TString

    /**
     * @return The type of the array.
     */
    val type: TString
    private var MEMBERS: List<MemberDesc>? = null
    private var parent: Script? = null

    companion object {
        /**
         * A list of scripts that only exist implicitly.
         */
        @JvmField
        val NATIVE_SCRIPTS = listOf(
            IString["ActiveMagicEffect"],
            IString["Alias"],
            IString["Debug"],
            IString["Form"],
            IString["Game"],
            IString["Input"],
            IString["Math"],
            IString["ModEvent"],
            IString["SKSE"],
            IString["StringUtil"],
            IString["UI"],
            IString["Utility"],
            IString["CommonArrayFunctions"],
            IString["ScriptObject"],
            IString["InputEnableLayer"]
        )
    }


    init {
        name = context.readTString(input)
        type = context.readTString(input)
        try {
            val count = input.int
            MEMBERS = MemberDesc.readList(input, count, context)
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read Script members.", ex, this)
        }
    }
}