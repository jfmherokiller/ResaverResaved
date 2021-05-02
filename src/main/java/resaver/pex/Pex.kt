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
package resaver.pex

import resaver.Game
import resaver.IString
import resaver.Scheme
import resaver.pex.Opcode.Companion.read
import resaver.pex.VData.Companion.readVariableData
import resaver.pex.VariableType.Companion.readLocal
import resaver.pex.VariableType.Companion.readParam
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.experimental.and

/**
 * Describes an object from a PEX file.
 *
 * @author Mark Fairchild
 */
class Pex internal constructor(input: ByteBuffer, game: Game, flags: List<UserFlag>, strings: StringTable) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer) {
        NAME.write(output)
        size = calculateSize()
        output.putInt(size)
        PARENTNAME.write(output)
        DOCSTRING.write(output)
        if (GAME.isFO4) {
            output.put(CONSTFLAG)
        }
        output.putInt(USERFLAGS)
        AUTOSTATENAME.write(output)
        if (GAME.isFO4) {
            output.putShort(STRUCTS!!.size.toShort())
            for (struct in STRUCTS!!) {
                struct.write(output)
            }
        }
        output.putShort(VARIABLES.size.toShort())
        for (`var` in VARIABLES) {
            `var`.write(output)
        }
        output.putShort(PROPERTIES.size.toShort())
        for (prop in PROPERTIES) {
            prop.write(output)
        }
        output.putShort(STATES.size.toShort())
        for (state in STATES) {
            state.write(output)
        }
    }

    /**
     * Calculates the size of the Pex, in bytes.
     *
     * @return The size of the Pex.
     */
    fun calculateSize(): Int {
        var sum = 0
        sum += 4 // size
        sum += 2 // parentClassName
        sum += 2 // DOCSTRING
        sum += 4 // userFlags
        sum += 2 // autoStateName
        sum += 6 // array sizes
        sum += VARIABLES.stream().mapToInt { obj: Variable -> obj.calculateSize() }.sum()
        sum += PROPERTIES.stream().mapToInt { obj: Property -> obj.calculateSize() }.sum()
        sum += STATES.stream().mapToInt { obj: State -> obj.calculateSize() }.sum()
        if (GAME.isFO4) {
            sum += 1
            sum += 2
            sum += STRUCTS!!.stream().mapToInt { obj: Struct -> obj.calculateSize() }.sum()
        }
        return sum
    }

    /**
     * Collects all of the strings used by the Pex and adds them to a set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<StringTable.TString>) {
        strings.add(NAME)
        strings.add(PARENTNAME)
        strings.add(DOCSTRING)
        strings.add(AUTOSTATENAME)
        STRUCTS!!.forEach(Consumer { f: Struct -> f.collectStrings(strings) })
        VARIABLES.forEach(Consumer { f: Variable -> f.collectStrings(strings) })
        PROPERTIES.forEach(Consumer { f: Property -> f.collectStrings(strings) })
        STATES.forEach(Consumer { f: State -> f.collectStrings(strings) })
    }

    /**
     * Retrieves a set of the struct names in this `Pex`.
     *
     * @return A `Set` of struct names.
     */
    val structNames: Set<IString>
        get() = STRUCTS!!.stream().map { p: Struct -> p.NAME }.collect(Collectors.toSet())

    /**
     * Retrieves a set of the property names in this `Pex`.
     *
     * @return A `Set` of property names.
     */
    val propertyNames: Set<IString>
        get() = PROPERTIES.stream().map { p: Property -> p.NAME }.collect(Collectors.toSet())

    /**
     * Retrieves a set of the variable names in this `Pex`.
     *
     * @return A `Set` of property names.
     */
    val variableNames: Set<IString>
        get() = VARIABLES.stream().map { p: Variable -> p.NAME }.collect(Collectors.toSet())

    /**
     * Retrieves a set of the function names in this `Pex`.
     *
     * @return A `Set` of function names.
     */
    val functionNames: Set<IString>
        get() {
            val NAMES: MutableSet<IString> = HashSet()
            STATES.forEach(Consumer { state: State ->
                state.FUNCTIONS.forEach(
                    Consumer { func: Function -> NAMES.add(func.fullName) })
            })
            return NAMES
        }

    /**
     * Returns a set of UserFlag objects matching a userFlags field.
     *
     * @param userFlags The flags to match.
     * @return The matching UserFlag objects.
     */
    fun getFlags(userFlags: Int): Set<UserFlag> {
        val FLAGS: MutableSet<UserFlag> = HashSet()
        USERFLAGDEFS.forEach(Consumer { flag: UserFlag ->
            if (flag.matches(userFlags)) {
                FLAGS.add(flag)
            }
        })
        return FLAGS
    }

    /**
     * Tries to disassemble the script.
     *
     * @param code  The code strings.
     * @param level Partial disassembly flag.
     */
    fun disassemble(code: MutableList<String?>, level: AssemblyLevel?) {
        val S = StringBuilder()
        S.append(String.format("ScriptName %s extends %s", NAME, PARENTNAME))
        val FLAGOBJS = getFlags(USERFLAGS)
        FLAGOBJS.forEach(Consumer { flag: UserFlag? -> S.append(" ").append(flag) })
        code.add(S.toString())
        if (DOCSTRING.isNotEmpty()) {
            code.add(String.format("{%s}\n", DOCSTRING))
        }
        code.add("")
        val AUTOVARS: MutableMap<Property, Variable> = HashMap()
        PROPERTIES.stream().filter { obj: Property -> obj.hasAutoVar() }
            .forEach { p: Property ->
                VARIABLES.stream().filter { v: Variable -> v.NAME.equals(p.AUTOVARNAME) }
                    .forEach { v: Variable -> AUTOVARS[p] = v }
            }
        val sortedProp: List<Property> = ArrayList(PROPERTIES)
        sortedProp.sortedWith(kotlin.Comparator.comparing { a: Property -> a.NAME })
        sortedProp.sortedWith(kotlin.Comparator.comparing { a: Property -> a.TYPE })
        val sortedVars: List<Variable> = ArrayList(VARIABLES)
        sortedVars.sortedWith(kotlin.Comparator.comparing { a: Variable -> a.NAME })
        sortedVars.sortedWith(kotlin.Comparator.comparing { a: Variable -> a.TYPE })
        code.add(";")
        code.add("; PROPERTIES")
        code.add(";")
        sortedProp.forEach(Consumer { v: Property -> v.disassemble(code, level, AUTOVARS) })
        code.add("")
        code.add(";")
        code.add("; VARIABLES")
        code.add(";")
        sortedVars.stream().filter { v: Variable -> !AUTOVARS.containsValue(v) }
            .forEach { v: Variable -> v.disassemble(code, level) }
        code.add("")
        code.add(";")
        code.add("; STATES")
        code.add(";")
        STATES.forEach(Consumer { v: State -> v.disassemble(code, level, AUTOSTATENAME.equals(v.NAME), AUTOVARS) })
    }

    /**
     * Pretty-prints the Pex.
     *
     * @return A string representation of the Pex.
     */
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append(String.format("ScriptName %s extends %s %s\n", NAME, PARENTNAME, getFlags(USERFLAGS)))
        buf.append(String.format("{%s}\n", DOCSTRING))
        buf.append(String.format("\tInitial state: %s\n", AUTOSTATENAME))
        buf.append("\n")
        PROPERTIES.forEach(Consumer { prop: Property -> buf.append(prop.toString()) })
        VARIABLES.forEach(Consumer { `var`: Variable -> buf.append(`var`.toString()) })
        STATES.forEach(Consumer { state: State -> buf.append(state.toString()) })
        return buf.toString()
    }

    val GAME: Game
    val NAME: StringTable.TString
    var size: Int
    val PARENTNAME: StringTable.TString
    val DOCSTRING: StringTable.TString
    var CONSTFLAG: Byte = 0
    val USERFLAGS: Int
    val AUTOSTATENAME: StringTable.TString
    private var STRUCTS: MutableList<Struct>? = null
    private val VARIABLES: MutableList<Variable>
    private val PROPERTIES: MutableList<Property>
    private val STATES: MutableList<State>
    private val AUTOVARMAP: MutableMap<Property, Variable>
    private val USERFLAGDEFS: List<UserFlag>
    private val STRINGS: StringTable

    /**
     * Describes a Struct from a PEX file.
     */
    inner class Struct(input: ByteBuffer, strings: StringTable) {
        /**
         * Write the this Struct to a `ByteBuffer`. No IO error
         * handling is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            this.NAME.write(output)
            output.putShort(MEMBERS.size.toShort())
            for (member in MEMBERS) {
                member.write(output)
            }
        }

        /**
         * Calculates the size of the Property, in bytes.
         *
         * @return The size of the Property.
         */
        fun calculateSize(): Int {
            var sum = 0
            sum += 2 // NAME
            sum += 2 // Count
            sum += MEMBERS.stream().mapToInt { obj: Member -> obj.calculateSize() }.sum()
            return sum
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            strings.add(this.NAME)
            MEMBERS.forEach(Consumer { f: Member -> f.collectStrings(strings) })
        }

        /**
         * Generates a qualified NAME for the object.
         *
         * @return A qualified NAME.
         */
        val fullName: IString
            get() = IString.format("%s.%s", this@Pex.NAME, this.NAME)
        val NAME: StringTable.TString
        private val MEMBERS: MutableList<Member>

        /**
         * Describes a Member of a Struct.
         */
        inner class Member(input: ByteBuffer, strings: StringTable) {
            /**
             * Write the this.ct to a `ByteBuffer`. No IO error
             * handling is performed.
             *
             * @param output The `ByteBuffer` to write.
             * @throws IOException IO errors aren't handled at all, they are
             * simply passed on.
             */
            @Throws(IOException::class)
            fun write(output: ByteBuffer) {
                this.NAME.write(output)
                TYPE.write(output)
                output.putInt(this.USERFLAGS)
                VALUE.write(output)
                output.put(this.CONSTFLAG)
                DOC.write(output)
            }

            /**
             * Calculates the size of the Property, in bytes.
             *
             * @return The size of the Property.
             */
            fun calculateSize(): Int {
                var sum = 0
                sum += 2 // NAME
                sum += 2 // type
                sum += 2 // docstring
                sum += 4 // userflags;
                sum += 1 // const flag
                sum += VALUE.calculateSize()
                return sum
            }

            /**
             * Collects all of the strings used by the Function and adds them to
             * a set.
             *
             * @param strings The set of strings.
             */
            fun collectStrings(strings: MutableSet<StringTable.TString>) {
                strings.add(this.NAME)
                strings.add(TYPE)
                strings.add(DOC)
                VALUE.collectStrings(strings)
            }

            /**
             * Generates a qualified NAME for the object.
             *
             * @return A qualified NAME.
             */
            val fullName: IString
                get() = IString.format("%s.%s.%s", this@Pex.NAME, this@Struct.NAME, this.NAME)

            /**
             * Pretty-prints the Member.
             *
             * @return A string representation of the Member.
             */
            override fun toString(): String {
                val buf = StringBuilder()
                if (this.CONSTFLAG.toInt() != 0) {
                    buf.append("const ")
                }
                buf.append(TYPE)
                buf.append(" ")
                buf.append(this.NAME)
                buf.append(" = ")
                buf.append(VALUE)
                return buf.toString()
            }

            val NAME: StringTable.TString
            val TYPE: StringTable.TString
            val DOC: StringTable.TString
            val USERFLAGS: Int
            val CONSTFLAG: Byte
            val VALUE: VData

            /**
             * Creates a Member by reading from a DataInput.
             *
             * @param input   A datainput for a Skyrim PEX file.
             * @param strings The `StringTable` for the
             * `Pex`.
             * @throws IOException Exceptions aren't handled.
             */
            init {
                this.NAME = strings.read(input)
                TYPE = strings.read(input)
                this.USERFLAGS = input.int
                VALUE = readVariableData(input, strings)
                this.CONSTFLAG = input.get()
                DOC = strings.read(input)
            }
        }

        /**
         * Creates a Struct by reading from a DataInput.
         *
         * @param input   A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            val numMembers = java.lang.Short.toUnsignedInt(input.short)
            MEMBERS = ArrayList(numMembers)
            for (i in 0 until numMembers) {
                MEMBERS.add(Member(input, strings))
            }
        }
    }

    /**
     * Describes a Property from a PEX file.
     */
    inner class Property(input: ByteBuffer, strings: StringTable) {
        /**
         * Write the this.ct to a `ByteBuffer`. No IO error handling
         * is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            this.NAME.write(output)
            TYPE.write(output)
            DOC.write(output)
            output.putInt(this.USERFLAGS)
            output.put(FLAGS)
            if (hasAutoVar()) {
                AUTOVARNAME!!.write(output)
            }
            if (hasReadHandler()) {
                READHANDLER!!.write(output)
            }
            if (hasWriteHandler()) {
                WRITEHANDLER!!.write(output)
            }
        }

        /**
         * Calculates the size of the Property, in bytes.
         *
         * @return The size of the Property.
         */
        fun calculateSize(): Int {
            var sum = 0
            sum += 2 // NAME
            sum += 2 // type
            sum += 2 // docstring
            sum += 4 // userflags;
            sum += 1 // flags
            if (hasAutoVar()) {
                sum += 2 // autovarname
            }
            if (hasReadHandler()) {
                sum += READHANDLER!!.calculateSize()
            }
            if (hasWriteHandler()) {
                sum += WRITEHANDLER!!.calculateSize()
            }
            return sum
        }

        /**
         * Indicates whether the `Property` is conditional.
         *
         * @return True if the `Property` is conditional, false
         * otherwise.
         */
        val isConditional: Boolean
            get() = this.USERFLAGS and 2 != 0

        /**
         * Indicates whether the `Property` is conditional.
         *
         * @return True if the `Property` is conditional, false
         * otherwise.
         */
        val isHidden: Boolean
            get() = this.USERFLAGS and 1 != 0

        /**
         * Indicates whether the `Property` has an autovar.
         *
         * @return True if the `Property` has an autovar, false
         * otherwise.
         */
        fun hasAutoVar(): Boolean {
            return (FLAGS and 4).toInt() != 0
        }

        /**
         * Indicates whether the `Property` has a read handler
         * function or not.
         *
         * @return True if the `Property` has a read handler, false
         * otherwise.
         */
        fun hasReadHandler(): Boolean {
            return (FLAGS and 5).toInt() == 1
        }

        /**
         * Indicates whether the `Property` has a write handler
         * function or not.
         *
         * @return True if the `Property` has a write handler, false
         * otherwise.
         */
        fun hasWriteHandler(): Boolean {
            return (FLAGS and 6).toInt() == 2
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            strings.add(this.NAME)
            strings.add(TYPE)
            strings.add(DOC)
            if (hasAutoVar()) {
                AUTOVARNAME?.let { strings.add(it) }
            }
            if (hasReadHandler()) {
                READHANDLER!!.collectStrings(strings)
            }
            if (hasWriteHandler()) {
                WRITEHANDLER!!.collectStrings(strings)
            }
        }

        /**
         * Generates a qualified NAME for the object.
         *
         * @return A qualified NAME.
         */
        val fullName: IString
            get() = IString.format("%s.%s", this@Pex.NAME, this.NAME)

        /**
         * Tries to disassemble the Property.
         *
         * @param code     The code strings.
         * @param level    Partial disassembly flag.
         * @param autovars Map of properties to their autovariable.
         */
        fun disassemble(code: MutableList<String?>, level: AssemblyLevel?, autovars: Map<Property, Variable>) {
            Objects.requireNonNull(autovars)
            val S = StringBuilder()
            S.append(String.format("%s Property %s", TYPE, this.NAME))
            if (autovars.containsKey(this) || hasAutoVar()) {
                assert(autovars.containsKey(this))
                assert(hasAutoVar())
                assert(autovars[this]!!.NAME.equals(AUTOVARNAME))
                val AUTOVAR = autovars[this]
                if (AUTOVAR!!.DATA.type !== DataType.NONE) {
                    S.append(" = ").append(AUTOVAR!!.DATA)
                }
                S.append(" AUTO")
                val FLAGOBJS = getFlags(AUTOVAR!!.USERFLAGS)
                FLAGOBJS.forEach(Consumer { flag: UserFlag -> S.append(" ").append(flag.toString()) })
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            FLAGOBJS.forEach(Consumer { flag: UserFlag -> S.append(" ").append(flag.toString()) })
            if (autovars.containsKey(this) || hasAutoVar()) {
                val AUTOVAR = autovars[this]
                S.append("  ;; --> ").append(AUTOVAR!!.NAME)
            }
            code.add(S.toString())
            if (DOC.isNotEmpty()) {
                code.add(String.format("{%s}", DOC))
            }
            if (hasReadHandler()) {
                assert(null != READHANDLER)
                READHANDLER!!.disassemble(code, level, "GET", autovars, 1)
            }
            if (hasWriteHandler()) {
                assert(null != WRITEHANDLER)
                WRITEHANDLER!!.disassemble(code, level, "SET", autovars, 1)
            }
            if (hasReadHandler() || hasWriteHandler()) {
                code.add("EndProperty")
            }
        }

        /**
         * Pretty-prints the Property.
         *
         * @return A string representation of the Property.
         */
        override fun toString(): String {
            val buf = StringBuilder()
            buf.append(String.format("\tProperty %s %s", TYPE.toString(), this.NAME.toString()))
            if (hasAutoVar()) {
                buf.append(String.format(" AUTO(%s) ", AUTOVARNAME))
            }
            buf.append(getFlags(this.USERFLAGS))
            buf.append(String.format("\n\t\tDoc: %s\n", DOC))
            buf.append(String.format("\t\tFlags: %d\n", FLAGS))
            if (hasReadHandler()) {
                buf.append("ReadHandler: ")
                buf.append(READHANDLER.toString())
            }
            if (hasWriteHandler()) {
                buf.append("WriteHandler: ")
                buf.append(WRITEHANDLER.toString())
            }
            buf.append("\n")
            return buf.toString()
        }

        val NAME: StringTable.TString
        val TYPE: StringTable.TString
        val DOC: StringTable.TString
        val USERFLAGS: Int
        val FLAGS: Byte
        var AUTOVARNAME: StringTable.TString? = null;
        private var READHANDLER: Function? = null
        private var WRITEHANDLER: Function? = null

        /**
         * Creates a Property by reading from a DataInput.
         *
         * @param input   A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            TYPE = strings.read(input)
            DOC = strings.read(input)
            this.USERFLAGS = input.int
            FLAGS = input.get()
            if (hasAutoVar()) {
                AUTOVARNAME = strings.read(input)
            } else {
                AUTOVARNAME = null
            }
            if (hasReadHandler()) {
                READHANDLER = Function(input, false, strings)
            } else {
                READHANDLER = null
            }
            if (hasWriteHandler()) {
                WRITEHANDLER = Function(input, false, strings)
            } else {
                WRITEHANDLER = null
            }
        }
    }

    /**
     * Describes a State in a PEX file.
     */
    inner class State(input: ByteBuffer, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            this.NAME!!.write(output)
            output.putShort(FUNCTIONS.size.toShort())
            for (function in FUNCTIONS) {
                function.write(output)
            }
        }

        /**
         * Calculates the size of the State, in bytes.
         *
         * @return The size of the State.
         */
        fun calculateSize(): Int {
            var sum = 0
            sum += 2 // NAME
            sum += 2 // array size
            sum += FUNCTIONS.stream().mapToInt { obj: Function -> obj.calculateSize() }.sum()
            return sum
        }

        /**
         * Collects all of the strings used by the State and adds them to a set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            this.NAME?.let { strings.add(it) }
            FUNCTIONS.forEach(Consumer { function: Function -> function.collectStrings(strings) })
        }

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code      The code strings.
         * @param level     Partial disassembly flag.
         * @param autostate A flag indicating if this state is the autostate.
         * @param autovars  Map of properties to their autovariable.
         */
        fun disassemble(
            code: MutableList<String?>,
            level: AssemblyLevel?,
            autostate: Boolean,
            autovars: Map<Property, Variable>
        ) {
            val RESERVED: MutableSet<IString?> = HashSet()
            RESERVED.add(IString.get("GoToState"))
            RESERVED.add(IString.get("GetState"))
            val S = StringBuilder()
            if (null == this.NAME || this.NAME.isEmpty()) {
                S.append(";")
            }
            if (autostate) {
                S.append("AUTO ")
            }
            S.append("State ")
            S.append(this.NAME)
            code.add(S.toString())
            code.add("")
            val INDENT = if (autostate) 0 else 1
            FUNCTIONS.stream()
                .filter { f: Function -> !RESERVED.contains(f.NAME) }
                .forEach { f: Function ->
                    f.disassemble(code, level, null, autovars, INDENT)
                    code.add("")
                }
            if (null == this.NAME || this.NAME.isEmpty()) {
                code.add(";EndState")
            } else {
                code.add("EndState")
            }
            code.add("")
        }

        /**
         * Pretty-prints the State.
         *
         * @return A string representation of the State.
         */
        override fun toString(): String {
            val buf = StringBuilder()
            buf.append(String.format("\tState %s\n", this.NAME))
            FUNCTIONS.forEach(Consumer { function: Function -> buf.append(function.toString()) })
            return buf.toString()
        }

        val NAME: StringTable.TString?
        val FUNCTIONS: MutableList<Function>

        /**
         * Creates a State by reading from a DataInput.
         *
         * @param input   A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            val numFunctions = java.lang.Short.toUnsignedInt(input.short)
            FUNCTIONS = ArrayList(numFunctions)
            for (i in 0 until numFunctions) {
                FUNCTIONS.add(Function(input, true, strings))
            }
        }
    }

    /**
     * Describes a Function and it's code.
     */
    inner class Function(input: ByteBuffer, named: Boolean, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`. No IO error handling
         * is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            if (null != this.NAME) {
                this.NAME!!.write(output)
            }
            RETURNTYPE!!.write(output)
            DOC!!.write(output)
            output.putInt(this.USERFLAGS)
            output.put(FLAGS)
            output.putShort(PARAMS.size.toShort())
            for (vt in PARAMS) {
                vt.write(output)
            }
            output.putShort(LOCALS.size.toShort())
            for (vt in LOCALS) {
                vt.write(output)
            }
            output.putShort(INSTRUCTIONS.size.toShort())
            for (inst in INSTRUCTIONS) {
                inst.write(output)
            }
        }

        /**
         * Calculates the size of the Function, in bytes.
         *
         * @return The size of the Function.
         */
        fun calculateSize(): Int {
            var sum = 0
            if (null != this.NAME) {
                sum += 2 // NAME
            }
            sum += 2 // returntype
            sum += 2 // docstring
            sum += 4 // userflags
            sum += 1 // flags
            sum += 6 // array sizes
            sum += PARAMS.stream().mapToInt { obj: VariableType -> obj.calculateSize() }.sum()
            sum += LOCALS.stream().mapToInt { obj: VariableType -> obj.calculateSize() }.sum()
            sum += INSTRUCTIONS.stream().mapToInt { obj: Instruction -> obj.calculateSize() }
                .sum()
            return sum
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            if (null != this.NAME) {
                strings.add(this.NAME!!)
            }
            strings.add(RETURNTYPE!!)
            DOC?.let { strings.add(it) }
            PARAMS.forEach(Consumer { param: VariableType -> param.collectStrings(strings) })
            LOCALS.forEach(Consumer { local: VariableType -> local.collectStrings(strings) })
            INSTRUCTIONS.forEach(Consumer { instr: Instruction -> instr.collectStrings(strings) })
        }

        /**
         * Generates a qualified NAME for the Function of the form
         * "OBJECT.FUNCTION".
         *
         * @return A qualified NAME.
         */
        val fullName: IString
            get() = if (this.NAME != null) {
                IString.format("%s.%s", this@Pex.NAME, this.NAME)
            } else {
                IString.format("%s.()", this@Pex.NAME)
            }

        /**
         * @return True if the function is global, false otherwise.
         */
        val isGlobal: Boolean
            get() = (FLAGS and 0x01).toInt() != 0

        /**
         * @return True if the function is native, false otherwise.
         */
        val isNative: Boolean
            get() = (FLAGS and 0x02).toInt() != 0

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code         The code strings.
         * @param level        Partial disassembly flag.
         * @param nameOverride Provides the function NAME; useful for functions
         * that don't have a NAME stored internally.
         * @param autovars     A map of properties to their autovars.
         * @param indent       The indent level.
         */
        fun disassemble(
            code: MutableList<String?>,
            level: AssemblyLevel?,
            nameOverride: String?,
            autovars: Map<Property, Variable>,
            indent: Int
        ) {
            val S = StringBuilder()
            S.append(Disassembler.tab(indent))
            if (null != RETURNTYPE && !RETURNTYPE.isEmpty() && !RETURNTYPE.equals("NONE")) {
                S.append(RETURNTYPE).append(" ")
            }
            if (null != nameOverride) {
                S.append(String.format("Function %s%s", nameOverride, Disassembler.paramList(PARAMS)))
            } else {
                S.append(String.format("Function %s%s", this.NAME, Disassembler.paramList(PARAMS)))
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            FLAGOBJS.forEach(Consumer { flag: UserFlag -> S.append(String.format(" $flag")) })
            if (isGlobal) {
                S.append(" GLOBAL")
            }
            if (isNative) {
                S.append(" NATIVE")
            }
            code.add(S.toString())
            if (null != DOC && !DOC.isEmpty()) {
                code.add(String.format("%s{%s}", Disassembler.tab(indent + 1), DOC))
            }
            val GROUPS: Set<IString> = LOCALS
                .stream()
                .filter(VariableType::isTemp)
                .map(VariableType::TYPE)
                .collect(Collectors.toSet())
            GROUPS.forEach(Consumer { t: IString ->
                val DECL = Disassembler.tab(indent + 1) +
                        "; " + t + ' ' +
                        LOCALS
                            .stream()
                            .filter(VariableType::isTemp)
                            .filter { v: VariableType -> v.TYPE === t }
                            .map(VariableType::name)
                            .collect(Collectors.joining(", "))
                code.add(DECL)
            })

            /*this.LOCALS.forEach(v -> {
                code.add(String.format("%s%s %s", Disassembler.tab(indent + 1), v.TYPE, v.NAME));
            });*/
            val types: MutableList<VariableType> = ArrayList(PARAMS)
            types.addAll(LOCALS)
            val terms = TermMap()
            autovars.forEach { (p: Property, v: Variable) -> terms[VData.ID(v.NAME)] = VData.Term(p.NAME.toString()) }
            val block = mutableListOf<Instruction>()
            when (level) {
                AssemblyLevel.STRIPPED -> Disassembler.preMap(block.toMutableList(), types, terms)
                AssemblyLevel.BYTECODE -> {
                    Disassembler.preMap(block.toMutableList(), types, terms)
                    block.forEach(Consumer { v: Instruction? ->
                        code.add(
                            String.format(
                                "%s%s",
                                Disassembler.tab(indent + 1),
                                v
                            )
                        )
                    })
                }
                AssemblyLevel.FULL -> try {
                    Disassembler.preMap(block.toMutableList(), types, terms)
                    val code2 = Disassembler.disassemble(block, types, indent + 1)
                    code.addAll(code2)
                } catch (ex: DisassemblyException) {
                    code.addAll(ex.partial)
                    val MSG = String.format("Error disassembling %s.", fullName)
                    throw IllegalStateException(MSG, ex)
                }
            }
            code.add(String.format("%sEndFunction", Disassembler.tab(indent)))
        }

        /**
         * Pretty-prints the Function.
         *
         * @return A string representation of the Function.
         */
        override fun toString(): String {
            val buf = StringBuilder()
            if (this.NAME != null) {
                buf.append(String.format("Function %s ", this.NAME))
            } else {
                buf.append("Function (UNNAMED) ")
            }
            buf.append(PARAMS.toString())
            buf.append(String.format(" returns %s\n", RETURNTYPE.toString()))
            buf.append(String.format("\tDoc: %s\n", DOC.toString()))
            buf.append(String.format("\tFlags: %s\n", getFlags(this.USERFLAGS)))
            buf.append("\tLocals: ")
            buf.append(LOCALS.toString())
            buf.append("\n\tBEGIN\n")
            INSTRUCTIONS.forEach(Consumer { instruction: Instruction ->
                buf.append("\t\t")
                buf.append(instruction.toString())
                buf.append("\n")
            })
            buf.append("\tEND\n\n")
            return buf.toString()
        }

        var NAME: StringTable.TString? = null
        val RETURNTYPE: StringTable.TString?
        val DOC: StringTable.TString?
        val USERFLAGS: Int
        val FLAGS: Byte
        private val PARAMS: MutableList<VariableType>
        private val LOCALS: MutableList<VariableType>
        private val INSTRUCTIONS: MutableList<Instruction>

        /**
         * Describes a single executable Instruction in a Function.
         */
        inner class Instruction {
            /**
             * Creates a new Instruction.
             *
             * @param code
             * @param args
             */
            constructor(code: Opcode, args: List<VData>?) {
                OP = code.ordinal.toByte()
                OPCODE = code
                ARGS = ArrayList(args)
            }

            /**
             * Creates an Instruction by reading from a DataInput.
             *
             * @param input   A datainput for a Skyrim PEX file.
             * @param strings The `StringTable` for the
             * `Pex`.
             * @throws IOException Exceptions aren't handled.
             */
            constructor(input: ByteBuffer, strings: StringTable) {
                OPCODE = read(input)
                OP = OPCODE.ordinal.toByte()
                if (OPCODE.ARGS > 0) {
                    ARGS = ArrayList(OPCODE.ARGS)
                    for (i in 0 until OPCODE.ARGS) {
                        ARGS.add(readVariableData(input, strings))
                    }
                } else if (OPCODE.ARGS < 0) {
                    ARGS = ArrayList(-OPCODE.ARGS)
                    for (i in 0 until 1 - OPCODE.ARGS) {
                        ARGS.add(readVariableData(input, strings))
                    }
                    val count = ARGS[-OPCODE.ARGS] as? VData.Int ?: throw IOException("Invalid instruction")
                    val numVargs = count.value
                    for (i in 0 until numVargs) {
                        ARGS.add(readVariableData(input, strings))
                    }
                } else {
                    ARGS = ArrayList(0)
                }
            }

            /**
             * Write the object to a `ByteBuffer`.
             *
             * @param output The `ByteBuffer` to write.
             * @throws IOException IO errors aren't handled at all, they are
             * simply passed on.
             */
            @Throws(IOException::class)
            fun write(output: ByteBuffer) {
                output.put(OP)
                for (vd in ARGS) {
                    vd.write(output)
                }
            }

            /**
             * Calculates the size of the Instruction, in bytes.
             *
             * @return The size of the Instruction.
             */
            fun calculateSize(): Int {
                var sum = 0
                sum += 1 // opcode
                sum += ARGS.stream().mapToInt { obj: VData -> obj.calculateSize() }.sum()
                return sum
            }

            /**
             * Collects all of the strings used by the Instruction and adds them
             * to a set.
             *
             * @param strings The set of strings.
             */
            fun collectStrings(strings: MutableSet<StringTable.TString>) {
                ARGS.forEach(Consumer { arg: VData -> arg.collectStrings(strings) })
            }

            /**
             * Pretty-prints the Instruction.
             *
             * @return A string representation of the Instruction.
             */
            override fun toString(): String {
                val FORMAT = "%s %s"
                return String.format(FORMAT, OPCODE, ARGS)
            }

            /**
             * Checks for instruction arguments that are in a replacement
             * scheme, and replaces them.
             *
             * @param scheme The replacement scheme.
             */
            fun remapVariables(scheme: Scheme) {
                val firstArg: Int
                firstArg = when (OPCODE) {
                    Opcode.CALLSTATIC -> 2
                    Opcode.CALLMETHOD, Opcode.CALLPARENT, Opcode.PROPGET, Opcode.PROPSET -> 1
                    else -> 0
                }

                // Remap identifiers 
                for (i in firstArg until ARGS.size) {
                    val arg = ARGS[i]
                    if (arg.type === DataType.IDENTIFIER) {
                        val id = arg as VData.ID
                        if (scheme.containsKey(id.getValue())) {
                            val newValue = scheme[id.getValue()]
                            val newStr = STRINGS.addString(newValue!!)
                            id.setValue(newStr)
                        }
                    }
                }
            }

            val OP: Byte
            val OPCODE: Opcode
            val ARGS: MutableList<VData>
        }

        /**
         * Creates a Function by reading from a DataInput.
         *
         * @param input   A datainput for a Skyrim PEX file.
         * @param named   A flag indicating whether to read a named function or a
         * nameless function.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            if (named) {
                this.NAME = strings.read(input)
            } else {
                this.NAME = null
            }
            RETURNTYPE = strings.read(input)
            DOC = strings.read(input)
            this.USERFLAGS = input.int
            FLAGS = input.get()
            val paramsCount = java.lang.Short.toUnsignedInt(input.short)
            PARAMS = ArrayList(paramsCount)
            for (i in 0 until paramsCount) {
                PARAMS.add(readParam(input, strings))
            }
            val localsCount = java.lang.Short.toUnsignedInt(input.short)
            LOCALS = ArrayList(localsCount)
            for (i in 0 until localsCount) {
                LOCALS.add(readLocal(input, strings))
            }
            val instructionsCount = java.lang.Short.toUnsignedInt(input.short)
            INSTRUCTIONS = ArrayList(instructionsCount)
            for (i in 0 until instructionsCount) {
                INSTRUCTIONS.add(Instruction(input, strings))
            }
        }
    }

    /**
     * Describes a PEX file variable entry. A variable consists of a NAME, a
     * type, user flags, and VData.
     */
    inner class Variable(input: ByteBuffer, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            this.NAME.write(output)
            TYPE.write(output)
            output.putInt(this.USERFLAGS)
            DATA.write(output)
            if (GAME.isFO4) {
                output.put(CONST)
            }
        }

        /**
         * Calculates the size of the VData, in bytes.
         *
         * @return The size of the VData.
         */
        fun calculateSize(): Int {
            var sum = 0
            sum += 2 // NAME
            sum += 2 // type
            sum += 4 // userflags
            sum += DATA.calculateSize()
            if (GAME.isFO4) {
                sum += 1
            }
            return sum
        }

        /**
         * Collects all of the strings used by the Variable and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            strings.add(this.NAME)
            strings.add(TYPE)
            DATA.collectStrings(strings)
        }

        /**
         * Indicates whether the `Property` is conditional.
         *
         * @return True if the `Property` is conditional, false
         * otherwise.
         */
        val isConditional: Boolean
            get() = this.USERFLAGS and 2 != 0

        /**
         * Tries to disassemble Instruction the script.
         *
         * @param code  The code strings.
         * @param level Partial disassembly flag.
         */
        fun disassemble(code: MutableList<String?>, level: AssemblyLevel?) {
            val S = StringBuilder()
            if (DATA.type !== DataType.NONE) {
                S.append(String.format("%s %s = %s", TYPE, this.NAME, DATA))
            } else {
                S.append(String.format("%s %s", TYPE, this.NAME))
            }
            if (CONST.toInt() != 0) {
                S.append(" ").append("const")
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            FLAGOBJS.forEach(Consumer { flag: UserFlag -> S.append(" ").append(flag.toString()) })
            code.add(S.toString())
        }

        /**
         * Pretty-prints the Variable.
         *
         * @return A string representation of the Variable.
         */
        override fun toString(): String {
            val FORMAT = "\tVariable %s %s = %s %s\n\n"
            return String.format(FORMAT, TYPE, this.NAME, DATA, getFlags(this.USERFLAGS))
        }

        val NAME: StringTable.TString = strings.read(input)
        val TYPE: StringTable.TString = strings.read(input)
        val USERFLAGS: Int = input.int
        val DATA: VData = readVariableData(input, strings)
        var CONST: Byte = 0

        /**
         * Creates a Variable by reading from a DataInput.
         *
         * @param input   A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            CONST = if (GAME.isFO4) {
                input.get()
            } else {
                0
            }
        }
    }

    companion object {
        private val _EXCLUDED = arrayOf(IString.get("player"), IString.get("playerref"))
        val EXCLUDED: Set<IString> = HashSet(listOf(*_EXCLUDED))
    }

    /**
     * Creates a PexObject by reading from a DataInput.
     *
     * @param input   A datainput for a Skyrim PEX file.
     * @param game    The game for which the script was compiled.
     * @param strings The `StringTable` for the `Pex`.
     * @param flag    The `UserFlag` list.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        Objects.requireNonNull(input)
        GAME = game
        USERFLAGDEFS = flags
        STRINGS = Objects.requireNonNull(strings)
        NAME = strings.read(input)
        size = input.int
        PARENTNAME = strings.read(input)
        DOCSTRING = strings.read(input)
        if (game.isFO4) {
            CONSTFLAG = input.get()
        } else {
            CONSTFLAG = -1
        }
        USERFLAGS = input.int
        AUTOSTATENAME = strings.read(input)
        AUTOVARMAP = HashMap()
        if (game.isFO4) {
            val numStructs = java.lang.Short.toUnsignedInt(input.short)
            STRUCTS = mutableListOf()
            for (i in 0 until numStructs) {
                STRUCTS!!.add(Struct(input, strings))
            }
        } else {
            STRUCTS = ArrayList(0)
        }
        val numVariables = java.lang.Short.toUnsignedInt(input.short)
        VARIABLES = ArrayList(numVariables)
        for (i in 0 until numVariables) {
            VARIABLES.add(Variable(input, strings))
        }
        val numProperties = java.lang.Short.toUnsignedInt(input.short)
        PROPERTIES = ArrayList(numProperties)
        for (i in 0 until numProperties) {
            PROPERTIES.add(Property(input, strings))
        }
        val numStates = java.lang.Short.toUnsignedInt(input.short)
        STATES = ArrayList(numStates)
        for (i in 0 until numStates) {
            STATES.add(State(input, strings))
        }
        PROPERTIES.forEach(Consumer { prop: Property ->
            if (prop.hasAutoVar()) {
                for (`var` in VARIABLES) {
                    if (prop.AUTOVARNAME!!.equals(`var`.NAME)) {
                        AUTOVARMAP[prop] = `var`
                        break
                    }
                }
                assert(AUTOVARMAP.containsKey(prop))
            }
        })
    }
}