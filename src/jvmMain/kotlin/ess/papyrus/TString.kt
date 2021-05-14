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
import ess.*
import ess.Linkable.Companion.makeLink
import resaver.Analysis
import resaver.IString


/**
 * A case-insensitive string with value semantics that reads and writes as an
 * index into a string table.
 *
 * @author Mark Fairchild
 */
abstract class TString : PapyrusElement, AnalyzableElement, Linkable {
    /**
     * Creates a new `TString` from a `WStringElement` and an
     * index.
     *
     * @param wstr The `WStringElement`.
     * @param index The index of the `TString`.
     */
    protected constructor(wstr: WStringElement?, index: Int) {
        require(index >= 0) { "Illegal index: $index" }
        WSTR = wstr!!
        INDEX = index
    }

    /**
     * Creates a new `TString` from a character sequence and an
     * index.
     *
     * @param cs The `CharSequence`.
     * @param index The index of the `TString`.
     */
    protected constructor(cs: CharSequence?, index: Int) : this(WStringElement(cs), index) {}

    /**
     * Creates a new unindexed `TString` from a character sequence.
     *
     * @param cs The `CharSequence`.
     */
    protected constructor(cs: CharSequence?) {
        WSTR = WStringElement(cs)
        INDEX = -1
    }

    /**
     * @see WStringElement.write
     * @param output The output stream.
     */
    fun writeFull(output: PlatformByteBuffer?) {
        WSTR.write(output)
    }

    /**
     * @see WStringElement.calculateSize
     *
     * @return The size of the `Element` in bytes.
     */
    fun calculateFullSize(): Int {
        return WSTR.calculateSize()
    }

    /**
     * @see IString.hashCode
     * @return
     */
    override fun hashCode(): Int {
        return WSTR.hashCode()
    }

    /**
     * Tests for case-insensitive value-equality with another
     * `TString`, `IString`, or `String`.
     *
     * @param other The object to which to compare.
     * @see java.lang.String.equalsIgnoreCase
     */
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> {
                true
            }
            other is TString -> {
                INDEX == other.INDEX
            }
            else -> {
                WSTR == other
            }
        }
    }

    /**
     * Tests for case-insensitive value-equality with a `String`.
     *
     * @param obj The object to which to compare.
     * @see java.lang.String.equalsIgnoreCase
     * @return
     */
    fun equals(obj: String?): Boolean {
        return WSTR.equals(obj)
    }

    /**
     * Tests for case-insensitive value-equality with another
     * `TString`.
     *
     * @param other The `TString` to which to compare.
     * @return True if the strings have the same index, false otherwise.
     * @see java.lang.String.equalsIgnoreCase
     */
    fun equals(other: TString): Boolean {
        return if (INDEX < 0 || other.INDEX < 0) {
            WSTR.equals(other.WSTR)
        } else {
            INDEX == other.INDEX
        }
    }

    /**
     * Getter for the index field.
     *
     * @return
     */
    val index: Int
        get() {
            assert(INDEX >= 0)
            return INDEX
        }

    /**
     * @see java.lang.String.isEmpty
     * @return
     */
    val isEmpty: Boolean
        get() = WSTR.isEmpty()

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String? {
        val BUILDER = StringBuilder()
        BUILDER.append("<html><h3>STRING</h3>")
        BUILDER.append("<p>Value: \"$this\".</p>")
        BUILDER.append("<p>Length: ${WSTR.length}</p>")

        /*if (null != analysis) {
            final Map<String, Integer> OWNERS = analysis.STRING_ORIGINS.get(this.toIString());

            if (null != OWNERS) {
                int total = OWNERS.values().stream().mapToInt(k -> k).sum();

                BUILDER.append(String.format("<p>This string appears %d times in the script files of %d mods.</p><ul>", total, OWNERS.size()));
                OWNERS.forEach((mod, count) -> BUILDER.append(String.format("<li>String appears %d times in the scripts of mod \"%s\".", count, mod)));
                BUILDER.append("</ul>");

            } else {
                BUILDER.append("<p>String origin could not be determined.</p>");
            }
        }*/
        val PAPYRUS = save!!.papyrus
        val LINKS: MutableList<String?> = mutableListOf()

        // Check definitions (Scripts and Structs).
        if (PAPYRUS != null) {
                val vals = (PAPYRUS.scripts.values + PAPYRUS.structs.values)
                vals.forEach { def: Definition ->
                    if (this === def.name) {
                        LINKS.add(def.toHTML(null))
                    }
                    for (member in def.members!!) {
                        if (this.equals(member!!.name)) {
                            LINKS.add(def.toHTML(member))
                        }
                    }
                }
        }

        /*
        // Check function messages.
        PAPYRUS.getSuspendedStacks().values().stream().parallel()
                .map(s -> s.getMessage())
                .forEach(stack -> {
                    stack.getMembers().stream().filter(var -> var.)
                });
        
        Stream.of(
                PAPYRUS.getFunctionMessages().stream().filter(m -> m.getMessage() != null).map(m -> Pair.make(m, m.getMessage())),
                PAPYRUS.getSuspendedStacks().values().stream().filter(s -> s.getMessage() != null).map(s -> Pair.make(s, s.getMessage())))
                .flatMap(i -> i);

        final Stream<Pair<? extends PapyrusElement, List<Variable>>> ELEMENTS = Stream.of(
                PAPYRUS.getScriptInstances().values().stream()
                        .map(i -> Pair.make(i, i.getData().getVariables())),
                PAPYRUS.getStructInstances().values().stream()
                        .map(s -> Pair.make(s, s.getMembers())),
                PAPYRUS.getReferences().values().stream()
                        .map(r -> Pair.make(r, r.getMembers())),
                PAPYRUS.getActiveScripts().values().stream()
                        .flatMap(t -> t.getStackFrames().stream())
                        .map(f -> Pair.make(f, f.getVariables())),
                PAPYRUS.getFunctionMessages().stream()
                        .filter(m -> m.getMessage() != null)
                        .map(m -> Pair.make(m, m.getMessage().getMembers())),
                PAPYRUS.getSuspendedStacks1().stream()
                        .filter(s -> s.getMessage() != null)
                        .map(s -> Pair.make(s, s.getMessage().getMembers())),
                PAPYRUS.getSuspendedStacks2().stream()
                        .filter(s -> s.getMessage() != null)
                        .map(s -> Pair.make(s, s.getMessage().getMembers())))
                .flatMap(i -> i);

        ELEMENTS.parallel().forEach(e -> {
            final PapyrusElement ELEMENT = e.A;
            final List<Variable> VARS = e.B;

            VARS.stream()
                    .filter(var -> var instanceof Variable.Str)
                    .map(var -> (Variable.Str) var)
                    .filter(var -> var.getValue().equals(this))
                    .forEach(var -> HOLDERS.add(Pair.make(var, ELEMENT)));
            VARS.stream()
                    .filter(var -> var instanceof Variable.Array)
                    .map(var -> (Variable.Array) var)
                    .filter(var -> var.getArray() != null)
                    .flatMap(var -> var.getArray().getMembers().stream())
                    .filter(var -> var.getType() == Type.STRING)
                    .map(var -> (Variable.Str) var)
                    .filter(var -> var.getValue().equals(this))
                    .forEach(var -> HOLDERS.add(Pair.make(var, ELEMENT)));
        });

        PAPYRUS.getActiveScripts().values().stream().forEach(thread -> {
            final Unknown4 U4 = thread.getUnknown4();
            if (U4 != null && U4.TSTRING != null && U4.TSTRING.equals(this)) {
                HOLDERS.add(Pair.make(U4, thread));
            }
        });

        PAPYRUS.getActiveScripts().values().stream().flatMap(thread -> thread.getStackFrames().stream())
                .parallel().forEach(frame -> {
                    if (Objects.equals(frame.getEvent(), this)
                            || Objects.equals(frame.getDocString(), this)
                            || Objects.equals(frame.getStatus(), this)) {
                        HOLDERS.add(Pair.make(frame));
                    }

                    Stream.concat(frame.getFunctionLocals().stream(), frame.getFunctionParams().stream())
                            .filter(member -> member.getName().equals(this))
                            .forEach(member -> HOLDERS.add(Pair.make(member, frame)));
                });

        MESSAGES
                .filter(p -> Objects.equals(p.B.getEvent(), this))
                .forEach(p -> HOLDERS.add(Pair.make(p.A, p.B)));
         */if (LINKS.isNotEmpty()) {
            BUILDER.append(String.format("<p>This string occurs %d times in this save.</p>", LINKS.size))
            LINKS.forEach { link: String? -> BUILDER.append(link).append("<br/>") }
        }
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @return The `WStringElement` that that the `TString`
     * points to.
     */
    fun toWString(): WStringElement {
        return WSTR
    }

    /**
     * @return The `WStringElement` that that the `TString`
     * points to.
     */
    fun toIString(): IString {
        return WSTR
    }

    /**
     * @return The length of the `TString`.
     * @see java.lang.String.length
     */
    fun length(): Int {
        return WSTR.length
    }

    /**
     *
     * @Override
     */
    override fun toString(): String {
        return WSTR.toString()
    }

    /**
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String? {
        return makeLink("string", INDEX, this.toString())
    }

    private val INDEX: Int
    private val WSTR: WStringElement

    companion object {
        /**
         * Creates a new `TString` that is unindexed not part of a table.
         * It can only be used for comparisons.
         *
         * @param cs The contents for the new `TString`.
         * @return A new `TString`. It can't be used for anything except
         * comparison.
         */
        fun makeUnindexed(cs: CharSequence?): TString {
            return object : TString(cs) {
                override fun matches(analysis: Analysis?, mod: String?): Boolean {
                    return false
                }

                override fun write(output: PlatformByteBuffer?) {
                    throw UnsupportedOperationException("Not supported.")
                }

                override fun calculateSize(): Int {
                    throw UnsupportedOperationException("Not supported.")
                }
            }
        }

        /**
         *
         * @param s1
         * @param s2
         * @return
         */
        fun compare(s1: TString?, s2: TString?): Int {
            val W1 = s1?.WSTR
            val W2 = s2?.WSTR
            return WStringElement.compare(W1, W2)
        }
    }
}