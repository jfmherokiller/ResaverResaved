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

import PlatformByteBuffer
import resaver.Game
import resaver.IString
import resaver.IString.Companion.format
import resaver.Scheme
import resaver.pex.Opcode.Companion.read
import resaver.pex.VariableType.Companion.readLocal
import resaver.pex.VariableType.Companion.readParam
import java.io.IOException
import kotlin.experimental.and

/**
 * Describes an object from a PEX file.
 *
 * @author Mark Fairchild
 */
class Pex internal constructor(input: PlatformByteBuffer, game: Game, flags: List<UserFlag?>?, strings: StringTable) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: PlatformByteBuffer) {
        NAME.write(output)
        size = calculateSize()
        output.putInt(size)
        PARENTNAME!!.write(output)
        DOCSTRING!!.write(output)
        if (GAME.isFO4) {
            output.put(CONSTFLAG)
        }
        output.putInt(USERFLAGS)
        AUTOSTATENAME.write(output)
        if (GAME.isFO4) {
            output.putShort(STRUCTS!!.size.toShort())
            STRUCTS!!.forEach { struct ->
                struct.write(output)
            }
        }
        output.putShort(VARIABLES.size.toShort())
        VARIABLES.forEach { `var` ->
            `var`.write(output)
        }
        output.putShort(PROPERTIES.size.toShort())
        PROPERTIES.forEach { prop ->
            prop.write(output)
        }
        output.putShort(STATES.size.toShort())
        STATES.forEach { state ->
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
        val sum2 = VARIABLES.sumOf { it.calculateSize() }
        sum += sum2
        val result1 = PROPERTIES.sumOf { it.calculateSize() }
        sum += result1
        val sum1 = STATES.sumOf { it.calculateSize() }
        sum += sum1
        if (GAME.isFO4) {
            sum += 1
            sum += 2
            val result = STRUCTS!!.sumOf { it.calculateSize() }
            sum += result
        }
        return sum
    }

    /**
     * Collects all of the strings used by the Pex and adds them to a set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<TString?>) {
        strings.add(NAME)
        strings.add(PARENTNAME)
        strings.add(DOCSTRING)
        strings.add(AUTOSTATENAME)
        STRUCTS!!.forEach { STRUCT ->
            STRUCT.collectStrings(strings)
        }
        VARIABLES.forEach { VARIABLE ->
            VARIABLE.collectStrings(strings)
        }
        PROPERTIES.forEach { PROPERTY ->
            PROPERTY.collectStrings(strings)
        }
        STATES.forEach { f ->
            f.collectStrings(strings)
        }
    }

    /**
     * Retrieves a set of the struct names in this `Pex`.
     *
     * @return A `Set` of struct names.
     */
    val structNames: Set<IString>
        get() {
            val set: MutableSet<IString> = hashSetOf()
            STRUCTS!!.mapTo(set) { it.NAME }
            return set
        }

    /**
     * Retrieves a set of the property names in this `Pex`.
     *
     * @return A `Set` of property names.
     */
    val propertyNames: Set<IString>
        get() {
            return PROPERTIES
                .asSequence()
                .map { it.NAME }
                .toSet()
        }

    /**
     * Retrieves a set of the variable names in this `Pex`.
     *
     * @return A `Set` of property names.
     */
    val variableNames: Set<IString>
        get() {
            val set: MutableSet<IString> = hashSetOf()
            VARIABLES.mapTo(set) { it.NAME }
            return set
        }

    /**
     * Retrieves a set of the function names in this `Pex`.
     *
     * @return A `Set` of function names.
     */
    val functionNames: Set<IString>
        get() {
            val NAMES: MutableSet<IString> = HashSet()
            for (state in STATES) {
                state.FUNCTIONS.mapTo(NAMES) { it.fullName }
            }
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
        for (flag in USERFLAGDEFS) {
            if (flag.matches(userFlags)) {
                FLAGS.add(flag)
            }
        }
        return FLAGS
    }

    /**
     * Tries to disassemble the script.
     *
     * @param code The code strings.
     * @param level Partial disassembly flag.
     */
    fun disassemble(code: MutableList<String?>, level: AssemblyLevel?) {
        val S = StringBuilder()
        if (PARENTNAME == null) {
            S.append("ScriptName $NAME")
        } else {
            S.append("ScriptName $NAME extends $PARENTNAME")
        }
        val FLAGOBJS = getFlags(USERFLAGS)
        for (flag in FLAGOBJS) {
            S.append(" ").append(flag)
        }
        code.add(S.toString())
        if (null != DOCSTRING && DOCSTRING.isNotEmpty()) {
            code.add("{$DOCSTRING}\n")
        }
        code.add("")
        val AUTOVARS: MutableMap<Property, Variable> = hashMapOf()
        for (p in PROPERTIES) {
            if (p.hasAutoVar()) {
                for (v in VARIABLES) {
                    if (v.NAME == p.AUTOVARNAME) {
                        AUTOVARS[p] = v
                    }
                }
            }
        }
        val sortedProp: List<Property> = listOf()
        sortedProp.sortedBy { a: Property -> a.NAME }
        sortedProp.sortedBy { a: Property -> a.TYPE }
        val sortedVars: List<Variable> = listOf()
        sortedVars.sortedBy  { a: Variable -> a.NAME }
        sortedVars.sortedBy { a: Variable -> a.TYPE }
        code.add(";")
        code.add("; PROPERTIES")
        code.add(";")
        sortedProp.forEach { property ->
            property.disassemble(code, level, AUTOVARS)
        }
        code.add("")
        code.add(";")
        code.add("; VARIABLES")
        code.add(";")
        sortedVars
            .asSequence()
            .filterNot { AUTOVARS.containsValue(it) }
            .forEach { it.disassemble(code, level) }
        code.add("")
        code.add(";")
        code.add("; STATES")
        code.add(";")
        for (v in STATES) {
            v.disassemble(code, level, AUTOSTATENAME == v.NAME, AUTOVARS)
        }
    }

    /**
     * Pretty-prints the Pex.
     *
     * @return A string representation of the Pex.
     */
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("ScriptName $NAME extends $PARENTNAME ${getFlags(USERFLAGS)}\n")
        buf.append("{$DOCSTRING}\n")
        buf.append("\tInitial state: $AUTOSTATENAME\n")
        buf.append("\n")
        for (prop in PROPERTIES) {
            buf.append(prop.toString())
        }
        for (`var` in VARIABLES) {
            buf.append(`var`.toString())
        }
        for (state in STATES) {
            buf.append(state.toString())
        }
        return buf.toString()
    }

    val GAME: Game
    val NAME: TString
    var size: Int
    val PARENTNAME: TString?
    val DOCSTRING: TString?
    var CONSTFLAG: Byte = 0
    val USERFLAGS: Int
    val AUTOSTATENAME: TString
    private var STRUCTS: MutableList<Struct>? = null
    private val VARIABLES: MutableList<Variable>
    private val PROPERTIES: MutableList<Property>
    private val STATES: MutableList<State>
    private val AUTOVARMAP: MutableMap<Property, Variable>
    private val USERFLAGDEFS: List<UserFlag>
    private val STRINGS: StringTable

    /**
     * Describes a Struct from a PEX file.
     *
     */
    inner class Struct(input: PlatformByteBuffer, strings: StringTable) {
        /**
         * Write the this Struct to a `ByteBuffer`. No IO error
         * handling is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: PlatformByteBuffer) {
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
            var result = 0
            for (MEMBER in MEMBERS) {
                val calculateSize = MEMBER.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<TString?>) {
            strings.add(this.NAME)
            for (f in MEMBERS) {
                f.collectStrings(strings)
            }
        }

        /**
         * Generates a qualified NAME for the object.
         *
         * @return A qualified NAME.
         */
        val fullName: IString
            get() = format("%s.%s", this@Pex.NAME, this.NAME)
        val NAME: TString
        private val MEMBERS: MutableList<Member>

        /**
         * Describes a Member of a Struct.
         *
         */
        inner class Member(input: PlatformByteBuffer, strings: StringTable) {
            /**
             * Write the this.ct to a `ByteBuffer`. No IO error
             * handling is performed.
             *
             * @param output The `ByteBuffer` to write.
             * @throws IOException IO errors aren't handled at all, they are
             * simply passed on.
             */
            @Throws(IOException::class)
            fun write(output: PlatformByteBuffer) {
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
            fun collectStrings(strings: MutableSet<TString?>) {
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
                get() = format("%s.%s.%s", this@Pex.NAME, this@Struct.NAME, this.NAME)

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

            val NAME: TString
            val TYPE: TString
            val DOC: TString
            val USERFLAGS: Int
            val CONSTFLAG: Byte
            val VALUE: VData

            /**
             * Creates a Member by reading from a DataInput.
             *
             * @param input A datainput for a Skyrim PEX file.
             * @param strings The `StringTable` for the
             * `Pex`.
             * @throws IOException Exceptions aren't handled.
             */
            init {
                this.NAME = strings.read(input)
                TYPE = strings.read(input)
                this.USERFLAGS = input.getInt()
                VALUE = VData.readVariableData(input, strings)
                this.CONSTFLAG = input.getByte()
                DOC = strings.read(input)
            }
        }

        /**
         * Creates a Struct by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            val numMembers = UtilityFunctions.toUnsignedInt(input.getShort())
            MEMBERS = ArrayList(numMembers)
            for (i in 0 until numMembers) {
                MEMBERS.add(Member(input, strings))
            }
        }
    }

    /**
     * Describes a Property from a PEX file.
     *
     */
    inner class Property(input: PlatformByteBuffer, strings: StringTable) {
        /**
         * Write the this.ct to a `ByteBuffer`. No IO error handling
         * is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: PlatformByteBuffer) {
            this.NAME.write(output)
            TYPE.write(output)
            DOC!!.write(output)
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
            return (FLAGS and 4.toByte()).toInt() != 0
        }

        /**
         * Indicates whether the `Property` has a read handler
         * function or not.
         *
         * @return True if the `Property` has a read handler, false
         * otherwise.
         */
        fun hasReadHandler(): Boolean {
            return (FLAGS and 5.toByte()).toInt() == 1
        }

        /**
         * Indicates whether the `Property` has a write handler
         * function or not.
         *
         * @return True if the `Property` has a write handler, false
         * otherwise.
         */
        fun hasWriteHandler(): Boolean {
            return (FLAGS and 6.toByte()).toInt() == 2
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<TString?>) {
            strings.add(this.NAME)
            strings.add(TYPE)
            strings.add(DOC)
            if (hasAutoVar()) {
                strings.add(AUTOVARNAME)
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
            get() = format("%s.%s", this@Pex.NAME, this.NAME)

        /**
         * Tries to disassemble the Property.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param autovars Map of properties to their autovariable.
         */
        fun disassemble(code: MutableList<String?>, level: AssemblyLevel?, autovars: Map<Property, Variable>) {
            val S = StringBuilder()
            S.append("$TYPE Property ${this.NAME}")
            if (autovars.containsKey(this) || hasAutoVar()) {
                assert(autovars.containsKey(this))
                assert(hasAutoVar())
                assert(autovars[this]!!.NAME == AUTOVARNAME)
                val AUTOVAR = autovars[this]
                if (AUTOVAR!!.DATA.type !== DataType.NONE) {
                    S.append(" = ").append(AUTOVAR!!.DATA)
                }
                S.append(" AUTO")
                val FLAGOBJS = getFlags(AUTOVAR!!.USERFLAGS)
                for (flag in FLAGOBJS) {
                    S.append(" ").append(flag.toString())
                }
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            for (flag in FLAGOBJS) {
                S.append(" ").append(flag.toString())
            }
            if (autovars.containsKey(this) || hasAutoVar()) {
                val AUTOVAR = autovars[this]
                S.append("  ;; --> ").append(AUTOVAR!!.NAME)
            }
            code.add(S.toString())
            if (null != DOC && DOC.isNotEmpty()) {
                code.add("{$DOC}")
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
            buf.append("\tProperty $TYPE ${this.NAME}")
            if (hasAutoVar()) {
                buf.append(" AUTO($AUTOVARNAME) ")
            }
            buf.append(getFlags(this.USERFLAGS))
            buf.append("\n\t\tDoc: $DOC\n")
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

        val NAME: TString
        val TYPE: TString
        val DOC: TString?
        val USERFLAGS: Int
        val FLAGS: Byte
        var AUTOVARNAME: TString? = null
        private var READHANDLER: Function? = null
        private var WRITEHANDLER: Function? = null

        /**
         * Creates a Property by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            TYPE = strings.read(input)
            DOC = strings.read(input)
            this.USERFLAGS = input.getInt()
            FLAGS = input.getByte()
            AUTOVARNAME = if (hasAutoVar()) {
                strings.read(input)
            } else {
                null
            }
            READHANDLER = if (hasReadHandler()) {
                Function(input, false, strings)
            } else {
                null
            }
            WRITEHANDLER = if (hasWriteHandler()) {
                Function(input, false, strings)
            } else {
                null
            }
        }
    }

    /**
     * Describes a State in a PEX file.
     *
     */
    inner class State(input: PlatformByteBuffer, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: PlatformByteBuffer) {
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
            var result = 0
            for (FUNCTION in FUNCTIONS) {
                val calculateSize = FUNCTION.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        /**
         * Collects all of the strings used by the State and adds them to a set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<TString?>) {
            strings.add(this.NAME)
            for (function in FUNCTIONS) {
                function.collectStrings(strings)
            }
        }

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param autostate A flag indicating if this state is the autostate.
         * @param autovars Map of properties to their autovariable.
         */
        fun disassemble(
            code: MutableList<String?>,
            level: AssemblyLevel?,
            autostate: Boolean,
            autovars: Map<Property, Variable>
        ) {
            val RESERVED: MutableSet<IString?> = hashSetOf()
            RESERVED.add(IString["GoToState"])
            RESERVED.add(IString["GetState"])
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
            for (f in FUNCTIONS) {
                if (!RESERVED.contains(f.NAME)) {
                    f.disassemble(code, level, null, autovars, INDENT)
                    code.add("")
                }
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
            buf.append("\tState ${this.NAME}\n")
            for (function in FUNCTIONS) {
                buf.append(function.toString())
            }
            return buf.toString()
        }

        val NAME: TString?
        val FUNCTIONS: MutableList<Function>

        /**
         * Creates a State by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            val numFunctions = UtilityFunctions.toUnsignedInt(input.getShort())
            FUNCTIONS = ArrayList(numFunctions)
            for (i in 0 until numFunctions) {
                FUNCTIONS.add(Function(input, true, strings))
            }
        }
    }

    /**
     * Describes a Function and it's code.
     *
     */
    inner class Function(input: PlatformByteBuffer, named: Boolean, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`. No IO error handling
         * is performed.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: PlatformByteBuffer) {
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
            var result1 = 0
            for (PARAM in PARAMS) {
                val calculateSize1 = PARAM.calculateSize()
                result1 += calculateSize1
            }
            sum += result1
            var sum1 = 0
            for (LOCAL in LOCALS) {
                val i = LOCAL.calculateSize()
                sum1 += i
            }
            sum += sum1
            var result = 0
            for (INSTRUCTION in INSTRUCTIONS) {
                val calculateSize = INSTRUCTION.calculateSize()
                result += calculateSize
            }
            sum += result
            return sum
        }

        /**
         * Collects all of the strings used by the Function and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<TString?>) {
            if (null != this.NAME) {
                strings.add(this.NAME)
            }
            strings.add(RETURNTYPE)
            strings.add(DOC)
            for (param in PARAMS) {
                param.collectStrings(strings)
            }
            for (local in LOCALS) {
                local.collectStrings(strings)
            }
            for (instr in INSTRUCTIONS) {
                instr.collectStrings(strings)
            }
        }

        /**
         * Generates a qualified NAME for the Function of the form
         * "OBJECT.FUNCTION".
         *
         * @return A qualified NAME.
         */
        val fullName: IString
            get() = if (this.NAME != null) {
                format("%s.%s", this@Pex.NAME, this.NAME)
            } else {
                format("%s.()", this@Pex.NAME)
            }

        /**
         * @return True if the function is global, false otherwise.
         */
        val isGlobal: Boolean
            get() = (FLAGS and 0x01.toByte()).toInt() != 0

        /**
         * @return True if the function is native, false otherwise.
         */
        val isNative: Boolean
            get() = (FLAGS and 0x02.toByte()).toInt() != 0

        /**
         * Tries to disassembleInstruction the script.
         *
         * @param code The code strings.
         * @param level Partial disassembly flag.
         * @param nameOverride Provides the function NAME; useful for functions
         * that don't have a NAME stored internally.
         * @param autovars A map of properties to their autovars.
         * @param indent The indent level.
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
            if (null != RETURNTYPE && RETURNTYPE.isNotEmpty() && !RETURNTYPE.equals("NONE")) {
                S.append(RETURNTYPE).append(" ")
            }
            if (null != nameOverride) {
                S.append("Function $nameOverride${Disassembler.paramList(PARAMS)}")
            } else {
                S.append("Function ${this.NAME}${Disassembler.paramList(PARAMS)}")
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            for (flag in FLAGOBJS) {
                S.append(" ").append(flag.toString())
            }
            if (isGlobal) {
                S.append(" GLOBAL")
            }
            if (isNative) {
                S.append(" NATIVE")
            }
            code.add(S.toString())
            if (null != DOC && DOC.isNotEmpty()) {
                code.add("${Disassembler.tab(indent + 1)}{$DOC}")
            }
            val GROUPS: MutableSet<IString> = HashSet()
            for (LOCAL in LOCALS) {
                if (LOCAL.isTemp) {
                    val type = LOCAL.TYPE
                    GROUPS.add(type)
                }
            }
            for (t in GROUPS) {
                val DECL = StringBuilder()
                DECL.append(Disassembler.tab(indent + 1))
                DECL.append("; ").append(t).append(' ')
                val output = LOCALS
                    .filter { it.isTemp && it.TYPE === t }
                    .joinToString { it.name }
                DECL.append(output)
                code.add(DECL.toString())
            }

            /*this.LOCALS.forEach(v -> {
                code.add(String.format("%s%s %s", Disassembler.tab(indent + 1), v.TYPE, v.NAME));
            });*/
            val types: MutableList<VariableType> = ArrayList(PARAMS)
            types.addAll(LOCALS)
            val terms = TermMap()
            for ((p, value) in autovars) {
                terms[VDataID(value.NAME)] = VDataTerm(p.NAME.toString())
            }
            val block: List<Instruction> = ArrayList(
                INSTRUCTIONS
            )
            when (level) {
                AssemblyLevel.STRIPPED -> Disassembler.preMap(block.toMutableList(), types, terms)
                AssemblyLevel.BYTECODE -> {
                    Disassembler.preMap(block.toMutableList(), types, terms)
                    for (v in block) {
                        code.add("${Disassembler.tab(indent + 1)}$v")
                    }
                }
                AssemblyLevel.FULL -> try {
                    Disassembler.preMap(block.toMutableList(), types, terms)
                    val code2 = Disassembler.disassemble(block, types, indent + 1)
                    code.addAll(code2)
                } catch (ex: DisassemblyException) {
                    code.addAll(ex.partial)
                    val MSG = "Error disassembling $fullName."
                    throw IllegalStateException(MSG, ex)
                }
            }
            code.add("${Disassembler.tab(indent)}EndFunction")
        }

        /**
         * Pretty-prints the Function.
         *
         * @return A string representation of the Function.
         */
        override fun toString(): String {
            val buf = StringBuilder()
            if (this.NAME != null) {
                buf.append("Function ${this.NAME} ")
            } else {
                buf.append("Function (UNNAMED) ")
            }
            buf.append(PARAMS.toString())
            buf.append(" returns ${RETURNTYPE.toString()}\n")
            buf.append("\tDoc: ${DOC.toString()}\n")
            buf.append("\tFlags: ${getFlags(this.USERFLAGS)}\n")
            buf.append("\tLocals: ")
            buf.append(LOCALS.toString())
            buf.append("\n\tBEGIN\n")
            for (instruction in INSTRUCTIONS) {
                buf.append("\t\t")
                buf.append(instruction.toString())
                buf.append("\n")
            }
            buf.append("\tEND\n\n")
            return buf.toString()
        }

        var NAME: TString? = null
        val RETURNTYPE: TString?
        val DOC: TString?
        val USERFLAGS: Int
        val FLAGS: Byte
        private val PARAMS: MutableList<VariableType>
        private val LOCALS: MutableList<VariableType>
        private val INSTRUCTIONS: MutableList<Instruction>

        /**
         * Describes a single executable Instruction in a Function.
         *
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
             * @param input A datainput for a Skyrim PEX file.
             * @param strings The `StringTable` for the
             * `Pex`.
             * @throws IOException Exceptions aren't handled.
             */
            constructor(input: PlatformByteBuffer, strings: StringTable) {
                OPCODE = read(input)
                OP = OPCODE.ordinal.toByte()
                when {
                    OPCODE.ARGS > 0 -> {
                        ARGS = ArrayList(OPCODE.ARGS)
                        for (i in 0 until OPCODE.ARGS) {
                            ARGS.add(VData.readVariableData(input, strings))
                        }
                    }
                    OPCODE.ARGS < 0 -> {
                        ARGS = ArrayList(-OPCODE.ARGS)
                        for (i in 0 until 1 - OPCODE.ARGS) {
                            ARGS.add(VData.readVariableData(input, strings))
                        }
                        val count = ARGS[-OPCODE.ARGS] as? VDataInt ?: throw IOException("Invalid instruction")
                        val numVargs = count.value
                        for (i in 0 until numVargs) {
                            ARGS.add(VData.readVariableData(input, strings))
                        }
                    }
                    else -> {
                        ARGS = mutableListOf()
                    }
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
            fun write(output: PlatformByteBuffer) {
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
                var result = 0
                for (ARG in ARGS) {
                    val calculateSize = ARG.calculateSize()
                    result += calculateSize
                }
                sum += result
                return sum
            }

            /**
             * Collects all of the strings used by the Instruction and adds them
             * to a set.
             *
             * @param strings The set of strings.
             */
            fun collectStrings(strings: MutableSet<TString?>?) {
                for (arg in ARGS) {
                    arg.collectStrings(strings)
                }
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
                val firstArg: Int = when (OPCODE) {
                    Opcode.CALLSTATIC -> 2
                    Opcode.CALLMETHOD, Opcode.CALLPARENT, Opcode.PROPGET, Opcode.PROPSET -> 1
                    else -> 0
                }

                // Remap identifiers 
                for (i in firstArg until ARGS.size) {
                    val arg = ARGS[i]
                    if (arg.type === DataType.IDENTIFIER) {
                        val id = arg as VDataID
                        if (scheme.containsKey(id.value)) {
                            val newValue = scheme[id.value]
                            val newStr = STRINGS.addString(newValue)
                            id.value = newStr
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
         * @param input A datainput for a Skyrim PEX file.
         * @param named A flag indicating whether to read a named function or a
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
            this.USERFLAGS = input.getInt()
            FLAGS = input.getByte()
            val paramsCount = UtilityFunctions.toUnsignedInt(input.getShort())
            PARAMS = ArrayList(paramsCount)
            for (i in 0 until paramsCount) {
                PARAMS.add(readParam(input, strings))
            }
            val localsCount = UtilityFunctions.toUnsignedInt(input.getShort())
            LOCALS = ArrayList(localsCount)
            for (i in 0 until localsCount) {
                LOCALS.add(readLocal(input, strings))
            }
            val instructionsCount = UtilityFunctions.toUnsignedInt(input.getShort())
            INSTRUCTIONS = ArrayList(instructionsCount)
            for (i in 0 until instructionsCount) {
                INSTRUCTIONS.add(Instruction(input, strings))
            }
        }
    }

    /**
     * Describes a PEX file variable entry. A variable consists of a NAME, a
     * type, user flags, and VData.
     *
     */
    inner class Variable(input: PlatformByteBuffer, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: PlatformByteBuffer) {
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
        fun collectStrings(strings: MutableSet<TString?>) {
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
         * @param code The code strings.
         * @param level Partial disassembly flag.
         */
        fun disassemble(code: MutableList<String?>, level: AssemblyLevel?) {
            val S = StringBuilder()
            if (DATA.type !== DataType.NONE) {
                S.append("$TYPE ${this.NAME} = $DATA")
            } else {
                S.append("$TYPE ${this.NAME}")
            }
            if (CONST.toInt() != 0) {
                S.append(" ").append("const")
            }
            val FLAGOBJS = getFlags(this.USERFLAGS)
            for (flag in FLAGOBJS) {
                S.append(" ").append(flag.toString())
            }
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

        val NAME: TString
        val TYPE: TString
        val USERFLAGS: Int
        val DATA: VData
        var CONST: Byte = 0

        /**
         * Creates a Variable by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            this.NAME = strings.read(input)
            TYPE = strings.read(input)
            this.USERFLAGS = input.getInt()
            DATA = VData.readVariableData(input, strings)
            CONST = if (GAME.isFO4) {
                input.getByte()
            } else {
                0
            }
        }
    }

    companion object {
        private val _EXCLUDED = arrayOf(IString["player"], IString["playerref"])
        val EXCLUDED: Set<IString> = hashSetOf(*_EXCLUDED)
    }

    /**
     * Creates a PexObject by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param game The game for which the script was compiled.
     * @param strings The `StringTable` for the `Pex`.
     * @param flags The `UserFlag` list.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        GAME = game
        USERFLAGDEFS = flags!!.filterNotNull()
        STRINGS = strings
        NAME = strings.read(input)
        size = input.getInt()
        PARENTNAME = strings.read(input)
        DOCSTRING = strings.read(input)
        CONSTFLAG = if (game.isFO4) {
            input.getByte()
        } else {
            -1
        }
        USERFLAGS = input.getInt()
        AUTOSTATENAME = strings.read(input)
        AUTOVARMAP = hashMapOf()
        if (game.isFO4) {
            val numStructs = UtilityFunctions.toUnsignedInt(input.getShort())
            STRUCTS = arrayListOf()
            for (i in 0 until numStructs) {
                STRUCTS!!.add(Struct(input, strings))
            }
        } else {
            STRUCTS = arrayListOf()
        }
        val numVariables = UtilityFunctions.toUnsignedInt(input.getShort())
        VARIABLES = ArrayList(numVariables)
        for (i in 0 until numVariables) {
            VARIABLES.add(Variable(input, strings))
        }
        val numProperties = UtilityFunctions.toUnsignedInt(input.getShort())
        PROPERTIES = ArrayList(numProperties)
        for (i in 0 until numProperties) {
            PROPERTIES.add(Property(input, strings))
        }
        val numStates = UtilityFunctions.toUnsignedInt(input.getShort())
        STATES = ArrayList(numStates)
        for (i in 0 until numStates) {
            STATES.add(State(input, strings))
        }
        for (prop in PROPERTIES) {
            if (prop.hasAutoVar()) {
                for (`var` in VARIABLES) {
                    if (prop.AUTOVARNAME!!.equals(`var`.NAME)) {
                        AUTOVARMAP[prop] = `var`
                        break
                    }
                }
                assert(AUTOVARMAP.containsKey(prop))
            }
        }
    }
}