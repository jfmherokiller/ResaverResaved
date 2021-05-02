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
import resaver.IString
import resaver.ListException
import resaver.ess.ESS
import resaver.ess.Element
import resaver.ess.Linkable
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Describes a script in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class Script(input: ByteBuffer, context: PapyrusContext) : Definition() {
    /**
     * @return The name of the papyrus element.
     */
    override val name: TString
        get() = NAME
    /**
     * @return The list of `MemberDesc`.
     */
    override val members: List<MemberDesc?>?
        get() = Collections.unmodifiableList(MEMBERS)

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        Objects.requireNonNull(output)
        NAME.write(output)
        type.write(output)
        output.putInt(MEMBERS!!.size)
        MEMBERS!!.forEach(Consumer { member: MemberDesc -> member.write(output) })
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 4
        sum += NAME.calculateSize()
        sum += type.calculateSize()
        sum += MEMBERS!!.stream().mapToInt { obj: MemberDesc -> obj.calculateSize() }.sum()
        return sum
    }

    /**
     * @return The list of `MemberDesc` prepended by the
     * `MemberDesc` objects of all superscripts.
     */
    val extendedMembers: MutableList<MemberDesc>
        get() {
            val EXTENDED: MutableList<MemberDesc>
            if (null != parent) {
                EXTENDED = parent!!.extendedMembers
                EXTENDED.addAll(MEMBERS!!)
            } else {
                EXTENDED = ArrayList(MEMBERS)
            }
            return EXTENDED
        }

    /**
     * @param scripts The ScriptMap.
     */
    fun resolveParent(scripts: ScriptMap) {
        parent = scripts[type]
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element): String {
        if (target is MemberDesc) {
            val i = extendedMembers.indexOf(target)
            if (i >= 0) {
                return Linkable.makeLink("script", NAME, i, NAME.toString())
            }
        }
        return Linkable.makeLink("script", NAME, NAME.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return if (this.isUndefined) {
            "#$NAME ($instanceCount)"
        } else "$NAME ($instanceCount)"
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis, save: ESS): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<html>")
        when {
            type.isEmpty -> {
                BUILDER.append(String.format("<h3>SCRIPT %s</h3>", NAME))
            }
            null != parent -> {
                BUILDER.append(String.format("<h3>SCRIPT %s extends %s</h3>", NAME, parent!!.toHTML(this)))
            }
            else -> {
                BUILDER.append(String.format("<h3>SCRIPT %s extends %s</h3>", NAME, type))
            }
        }
        if (this.isUndefined) {
            BUILDER.append("<p>WARNING: SCRIPT MISSING!<br />Selecting \"Remove Undefined Instances\" will delete this.</p>")
        }
        val mods = analysis.SCRIPT_ORIGINS[NAME.toIString()]
        if (null != mods) {
            if (mods.size > 1) {
                BUILDER.append("<p>WARNING: MORE THAN ONE MOD PROVIDES THIS SCRIPT!<br />Exercise caution when editing or deleting this script!</p>")
            }
            val probablyProvider = mods.last()
            BUILDER.append(String.format("<p>This script probably came from \"%s\".</p>", probablyProvider))
            BUILDER.append("<p>Full list of providers:</p>")
            BUILDER.append("<ul>")
            mods.forEach(Consumer { mod: String? -> BUILDER.append(String.format("<li>%s", mod)) })
            BUILDER.append("</ul>")
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
        val INSTANCES = save.papyrus
            .scriptInstances
            .values
            .stream()
            .filter { instance: ScriptInstance -> instance.script == this }
            .collect(Collectors.toList())
        val REFERENCES = save.papyrus
            .references
            .values
            .stream()
            .filter { ref: Reference -> ref.script == this }
            .collect(Collectors.toList())
        BUILDER.append(String.format("<p>There are %d instances of this script.</p>", INSTANCES.size))
        if (INSTANCES.size < 20) {
            BUILDER.append("<ul>")
            INSTANCES.forEach(Consumer { i: ScriptInstance ->
                val s = String.format("<li>%s</a>", i.toHTML(null))
                BUILDER.append(s)
            })
            BUILDER.append("</ul>")
        }
        BUILDER.append(String.format("<p>There are %d references of this script.</p>", REFERENCES.size))
        if (REFERENCES.size < 20) {
            BUILDER.append("<ul>")
            REFERENCES.forEach(Consumer { i: Reference ->
                val s = String.format("<li>%s</a>", i.toHTML(null))
                BUILDER.append(s)
            })
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
    override fun matches(analysis: Analysis, mod: String): Boolean {
        Objects.requireNonNull(analysis)
        Objects.requireNonNull(mod)
        val OWNERS = analysis.SCRIPT_ORIGINS[NAME.toIString()]
        return null != OWNERS && OWNERS.contains(mod)
    }

    /**
     * @return A flag indicating if the `Script` is undefined.
     */
    override val isUndefined: Boolean
        get() {
            return if (!type.isEmpty) {
                false
            } else !NATIVE_SCRIPTS.contains(NAME.toIString())
        }

    private val NAME: TString

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
        val NATIVE_SCRIPTS = listOf(
            IString.get("ActiveMagicEffect"),
            IString.get("Alias"),
            IString.get("Debug"),
            IString.get("Form"),
            IString.get("Game"),
            IString.get("Input"),
            IString.get("Math"),
            IString.get("ModEvent"),
            IString.get("SKSE"),
            IString.get("StringUtil"),
            IString.get("UI"),
            IString.get("Utility"),
            IString.get("CommonArrayFunctions"),
            IString.get("ScriptObject"),
            IString.get("InputEnableLayer")
        )
    }

    /**
     * Creates a new `Script` by reading from a
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
        NAME = context.readTString(input)
        type = context.readTString(input)
        try {
            val count = input.int
            MEMBERS = MemberDesc.readList(input, count, context)
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read Script members.", ex, this)
        }
    }
}