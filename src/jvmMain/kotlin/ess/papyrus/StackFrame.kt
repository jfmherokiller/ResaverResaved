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
import ess.papyrus.Parameter.Companion.createTerm
import ess.papyrus.VarType.Companion.read
import ess.papyrus.Variable.Companion.read
import ess.papyrus.Variable.Companion.readList
import resaver.Analysis
import resaver.IString
import resaver.IString.Companion.format
import resaver.ListException
import resaver.pex.Opcode
import java.util.*
import java.util.regex.Pattern

/**
 * Describes a stack frame in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class StackFrame(input: PlatformByteBuffer, thread: ActiveScript?, context: PapyrusContext) : PapyrusElement, AnalyzableElement,
    Linkable, HasVariables {
    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output?.putInt(VARIABLES.size)
        FLAG.write(output)
        FN_Var_TYPE.write(output)
        scriptName.write(output)
        BASENAME.write(output)
        event.write(output)
        STATUS.ifPresent { s: TString? -> s!!.write(output) }
        output?.put(OPCODE_MAJORVERSION)
        output?.put(OPCODE_MINORVERSION)
        RETURNTYPE.write(output)
        docString.write(output)
        FN_USERFLAGS.write(output)
        FN_FLAGS.write(output)
        output?.putShort(FN_PARAMS.size.toShort())
        for (param in FN_PARAMS) {
            param.write(output)
        }
        output?.putShort(FN_LOCALS.size.toShort())
        for (local in FN_LOCALS) {
            local.write(output)
        }
        output?.putShort(CODE.size.toShort())
        for (opcode in CODE) {
            opcode.write(output)
        }
        output?.putInt(PTR)
        owner.write(output)
        for (`var` in VARIABLES) {
            `var`!!.write(output)
        }
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1
        sum += FN_Var_TYPE.calculateSize()
        sum += scriptName.calculateSize()
        sum += BASENAME.calculateSize()
        sum += event.calculateSize()
        sum += STATUS.map { obj: TString? -> obj!!.calculateSize() }.orElse(0)
        sum += 2
        sum += RETURNTYPE.calculateSize()
        sum += docString.calculateSize()
        sum += 5
        sum += 2
        val sum1 = FN_PARAMS.sumOf { it.calculateSize() }
        sum += sum1
        sum += 2
        val result1 = FN_LOCALS.sumOf { it.calculateSize() }
        sum += result1
        sum += 2
        val sum2 = CODE.sumOf { it.calculateSize() }
        sum += sum2
        sum += 4
        sum += owner.calculateSize()
        sum += 4
        val result = VARIABLES.sumOf { it!!.calculateSize() }
        sum += result
        return sum
    }

    /**
     * Replaces the opcodes of the `StackFrame` with NOPs.
     */
    fun zero() {
        for (i in CODE.indices) {
            CODE[i] = OpcodeData.NOP
        }
    }

    /**
     * @return The qualified name of the function being executed.
     */
    val fName: IString
        get() = format("%s.%s", scriptName, event)

    /**
     * @return The status.
     */
    val status: TString?
        get() = STATUS.orElse(null)

    /**
     * @return The function parameter list.
     */
    val functionParams: List<FunctionParam>
        get() = FN_PARAMS

    /**
     * @return The function locals list.
     */
    val functionLocals: List<FunctionLocal>
        get() = FN_LOCALS

    /**
     * @return The function opcode data list.
     */
    val opcodeData: List<OpcodeData>
        get() = CODE

    /**
     * @see HasVariables.getVariables
     * @return
     */
    override val variables: List<Variable>
        get() = VARIABLES.filterNotNull()

    /**
     * @see HasVariables.getDescriptors
     * @return
     */
    override val descriptors: List<MemberDesc>
        get() {
            return functionParams + functionLocals
        }

    /**
     * @see HasVariables.setVariable
     * @param index
     * @param newVar
     */
    override fun setVariable(index: Int, newVar: Variable?) {
        require(!(index < 0 || index >= VARIABLES.size)) { "Invalid variable index: $index" }
        VARIABLES[index] = newVar
    }

    /**
     * @return A flag indicating if the `StackFrame` is undefined.
     */
    val isUndefined: Boolean
        get() {
            if (isNative) {
                return false
            } else if (script != null) {
                return script.isUndefined
            }
            return false
        }

    /**
     * @return A flag indicating if the `StackFrame` is running a
     * static method.
     */
    val isStatic: Boolean
        get() = FN_FLAGS.getFlag(0)

    /**
     * @return A flag indicating if the `StackFrame` is running a
     * native method.
     */
    val isNative: Boolean
        get() = FN_FLAGS.getFlag(1)

    /**
     * @return A flag indicating if the `StackFrame` zeroed.
     */
    val isZeroed: Boolean
        get() {
            if (isNative || null == CODE || CODE.isEmpty()) return false
            for (op in CODE) {
                if (OpcodeData.NOP != op) {
                    return false
                }
            }
            return true
        }

    /**
     * @see ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        assert(null != THREAD)
        val frameIndex = THREAD!!.stackFrames.indexOf(this)
        if (frameIndex < 0) {
            return "invalid"
        }
        if (null != target) {
            if (target is Variable) {
                val varIndex = VARIABLES.indexOf(target)
                if (varIndex >= 0) {
                    return makeLink("frame", THREAD.iD, frameIndex, varIndex, this.toString())
                }
            } else {
                var result: Variable? = null
                for (v in VARIABLES) {
                    if (v!!.hasRef()) {
                        if (v.referent === target) {
                            result = v
                            break
                        }
                    }
                }
                if (result == null) {
                    val varIndex = VARIABLES.indexOf(result)
                    if (varIndex >= 0) {
                        return makeLink("frame", THREAD.iD, frameIndex, varIndex, this.toString())
                    }
                }
            }
        }
        return makeLink("frame", THREAD.iD, frameIndex, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        BUF.append(if (isZeroed) "ZEROED " else "")
        BUF.append(if (isUndefined) "#" else "")
        BUF.append(BASENAME)
        BUF.append(if (isUndefined) "#." else ".")
        BUF.append(event)
        BUF.append("()")
        if (isStatic) {
            BUF.append(" ").append("static")
        }
        if (isNative) {
            BUF.append(" ").append("native")
        }
        return BUF.toString()
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER = StringBuilder()

        //BUILDER.append("<html><h3>STACKFRAME</h3>");
        BUILDER.append("<html><h3>STACKFRAME")
        if (isZeroed) {
            BUILDER.append(" (ZEROED)")
        }
        BUILDER.append("<br/>")
        if (!RETURNTYPE.isEmpty && !RETURNTYPE.equals("None")) {
            BUILDER.append(RETURNTYPE).append(" ")
        }
        if (isUndefined) {
            BUILDER.append("#")
        }
        BUILDER.append("$scriptName.$event()")
        if (isStatic) {
            BUILDER.append(" static")
        }
        if (isNative) {
            BUILDER.append(" native")
        }
        BUILDER.append("</h3>")
        if (isZeroed) {
            BUILDER.append("<p><em>WARNING: FUNCTION TERMINATED!</em><br/>This function has been terminated and all of its instructions erased.</p>")
        } else if (isUndefined) {
            BUILDER.append("<p><em>WARNING: SCRIPT MISSING!</em><br/>Selecting \"Remove Undefined Instances\" will terminate the entire thread containing this frame.</p>")
        }

        /*if (null != analysis) {
            SortedSet<String> providers = analysis.FUNCTION_ORIGINS.get(this.getFName());
            if (null != providers) {
                String probablyProvider = providers.last();
                BUILDER.append(String.format("<p>Probably running code from mod %s.</p>", probablyProvider));

                if (providers.size() > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>");
                    providers.forEach(mod -> BUILDER.append(String.format("<li>%s", mod)));
                    BUILDER.append("</ul>");
                }
            }
        }*/when {
            owner is VarNull -> {
                BUILDER.append("<p>Owner: <em>UNOWNED</em></p>")
            }
            null != OWNER -> {
                BUILDER.append("<p>Owner: ${OWNER?.toHTML(this)}</p>")
            }
            isStatic -> {
                BUILDER.append("<p>Static method, no owner.</p>")
            }
            else -> {
                BUILDER.append("<p>Owner: ${owner.toHTML(this)}</p>")
            }
        }
        BUILDER.append("<p>")
        BUILDER.append("Script: ${script?.toHTML(this) ?: scriptName}<br/>")
        BUILDER.append("Base: $BASENAME<br/>")
        BUILDER.append("Event: $event<br/>")
        BUILDER.append("Status: $STATUS<br/>")
        BUILDER.append("Flag: $FLAG<br/>")
        BUILDER.append("Function type: $FN_Var_TYPE<br/>")
        BUILDER.append("Function return type: $RETURNTYPE<br/>")
        BUILDER.append("Function docstring: $docString<br/>")
        BUILDER.append(
            String.format(
                "%d parameters, %d locals, %d values.<br/>",
                FN_PARAMS.size,
                FN_LOCALS.size,
                VARIABLES.size
            )
        )
        BUILDER.append("Status: $STATUS<br/>")
        BUILDER.append("Function flags: $FN_FLAGS<br/>")
        BUILDER.append("Function user flags:<br/>${FN_USERFLAGS.toHTML()}")
        BUILDER.append(String.format("Opcode version: %d.%d<br/>", OPCODE_MAJORVERSION, OPCODE_MINORVERSION))
        BUILDER.append("</p>")
        if (CODE.size > 0) {
            BUILDER.append("<hr/><p>PAPYRUS BYTECODE:</p>")
            BUILDER.append("<code><pre>")
            val OPS: List<OpcodeData> = ArrayList(CODE)
            for (opcodeData in OPS.subList(0, PTR)) {
                BUILDER.append("   $opcodeData\n")
            }
            BUILDER.append("==><b>${OPS[PTR]}</b>\n")
            for (v in OPS.subList(PTR + 1, CODE.size)) {
                BUILDER.append("   $v\n")
            }
            BUILDER.append("</pre></code>")
        } else {
            BUILDER.append("<p><em>Papyrus bytecode not available.</em></p>")
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
        val OWNERS = analysis?.SCRIPT_ORIGINS?.get(scriptName.toIString()) ?: return false
        return OWNERS.contains(mod)
    }

    private val THREAD: ActiveScript?
    private val FLAG: Flags.FlagsByte
    private val FN_Var_TYPE: VarType

    /**
     * @return The name of the script being executed.
     */
    val scriptName: TString

    /**
     * @return The script being executed.
     */
    val script: Script?
    private val BASENAME: TString

    /**
     * @return The event name.
     */
    val event: TString
    private val STATUS: Optional<TString>
    private val OPCODE_MAJORVERSION: Byte
    private val OPCODE_MINORVERSION: Byte
    private val RETURNTYPE: TString

    /**
     * @return The docstring.
     */
    val docString: TString
    private val FN_USERFLAGS: Flags.FlagsInt
    private val FN_FLAGS: Flags.FlagsByte
    private val FN_PARAMS: MutableList<FunctionParam>
    private val FN_LOCALS: MutableList<FunctionLocal>
    private var CODE: MutableList<OpcodeData> = mutableListOf()
    private val PTR: Int

    /**
     * @return The owner, which should be a form or an instance.
     */
    val owner: Variable
    private var VARIABLES: MutableList<Variable?> = mutableListOf()
    private var OWNER: GameElement? = null

    companion object {
        /**
         * Eliminates ::temp variables from an OpcodeData list.
         *
         * @param instructions
         * @param locals
         * @param types
         * @param terms
         */
        fun preMap(
            instructions: List<OpcodeData?>,
            locals: List<MemberDesc?>?,
            types: List<MemberDesc>,
            terms: MutableMap<Parameter?, Parameter?>,
            ptr: Int
        ): String {
            val BUF = StringBuilder()
            for (i in instructions.indices) {
                val op = instructions[i] ?: continue
                val params = ArrayList(op.parameters)
                val del = makeTerm(op.opcode, params, types, terms)
                if (del) {
                    BUF.append(if (i == ptr) "<b>==></b>" else "   ")
                    BUF.append("<em><font color=\"lightgray\">")
                    BUF.append(op.opcode)
                    for (p in params) {
                        BUF.append(", ").append(p!!.toValueString())
                    }
                    BUF.append("<font color=\"black\"></em>\n")
                } else {
                    BUF.append(if (i == ptr) "<b>==>" else "   ")
                    BUF.append(op.opcode)
                    for (p in params) {
                        BUF.append(", ").append(p!!.toValueString())
                    }
                    BUF.append(if (i == ptr) "</b>\n" else "\n")
                }
            }
            return BUF.toString()
        }

        /**
         *
         * @param op
         * @param args
         * @param types
         * @param terms
         * @return
         */
        fun makeTerm(
            op: Opcode,
            args: MutableList<Parameter?>,
            types: List<MemberDesc>,
            terms: MutableMap<Parameter?, Parameter?>
        ): Boolean {
            val term: String
            val method: String
            val obj: String
            val dest: String
            val arg: String
            val prop: String
            val operand1: String
            val operand2: String
            val subArgs: List<String>
            val type: WStringElement
            return when (op) {
                Opcode.IADD, Opcode.FADD, Opcode.STRCAT -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 + $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ISUB, Opcode.FSUB -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 - $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.IMUL, Opcode.FMUL -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 * $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.IDIV, Opcode.FDIV -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 / $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.IMOD -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = String.format("%s %% %s", operand1, operand2)
                    processTerm(args, terms, 0, term)
                }
                Opcode.RETURN -> {
                    replaceVariables(args, terms, -1)
                    false
                }
                Opcode.CALLMETHOD -> {
                    replaceVariables(args, terms, 2)
                    method = args[0]!!.toValueString()
                    obj = args[1]!!.toValueString()
                    val result1: MutableList<String> = mutableListOf()
                    for (v1 in args
                        .subList(3, args.size)) {
                        val paren1 = v1!!.paren()
                        result1.add(paren1)
                    }
                    subArgs = result1
                    term = "$obj.$method${paramList(subArgs)}"
                    processTerm(args, terms, 2, term)
                }
                Opcode.CALLPARENT -> {
                    replaceVariables(args, terms, 1)
                    method = args[0]!!.toValueString()
                    val list1: MutableList<String> = mutableListOf()
                    for (parameter in args
                        .subList(3, args.size)) {
                        val s = parameter!!.paren()
                        list1.add(s)
                    }
                    subArgs = list1
                    term = "parent.$method${paramList(subArgs)}"
                    processTerm(args, terms, 1, term)
                }
                Opcode.CALLSTATIC -> {
                    replaceVariables(args, terms, 2)
                    obj = args[0]!!.toValueString()
                    method = args[1]!!.toValueString()
                    val list: MutableList<String> = mutableListOf()
                    for (v in args
                        .subList(3, args.size)) {
                        val paren = v!!.paren()
                        list.add(paren)
                    }
                    subArgs = list
                    term = "$obj.$method${paramList(subArgs)}"
                    processTerm(args, terms, 2, term)
                }
                Opcode.NOT -> {
                    replaceVariables(args, terms, 0)
                    term = "!${args[1]!!.paren()}"
                    processTerm(args, terms, 0, term)
                }
                Opcode.INEG, Opcode.FNEG -> {
                    replaceVariables(args, terms, 0)
                    term = "-${args[1]!!.paren()}"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ASSIGN -> {
                    replaceVariables(args, terms, 0)
                    term = "${args[1]}"
                    processTerm(args, terms, 0, term)
                }
                Opcode.CAST -> {
                    replaceVariables(args, terms, 0)
                    dest = args[0]!!.toValueString()
                    arg = args[1]!!.paren()
                    var result: MemberDesc? = null
                    for (memberDesc in types) {
                        if (memberDesc.name.equals(dest)) {
                            result = memberDesc
                            break
                        }
                    }
                    type = result?.type?.toWString()!!
                    term = if (type.equals("bool")) {
                        arg
                    } else {
                        "($type)$arg"
                    }
                    processTerm(args, terms, 0, term)
                }
                Opcode.PROPGET -> {
                    replaceVariables(args, terms, 2)
                    obj = args[1]!!.toValueString()
                    prop = args[0]!!.toValueString()
                    term = "$obj.$prop"
                    processTerm(args, terms, 2, term)
                }
                Opcode.PROPSET -> {
                    replaceVariables(args, terms, -1)
                    false
                }
                Opcode.CMP_EQ -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 == $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.CMP_LT -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 < $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.CMP_LE -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 <= $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.CMP_GT -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 > $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.CMP_GE -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[1]!!.paren()
                    operand2 = args[2]!!.paren()
                    term = "$operand1 >= $operand2"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ARR_CREATE -> {
                    val size = args[1]!!.intValue
                    dest = args[0]!!.toValueString()
                    var found: MemberDesc? = null
                    for (t in types) {
                        if (t.name.equals(dest)) {
                            found = t
                            break
                        }
                    }
                    type = found?.type?.toWString()!!
                    val subtype = type.toString().substring(0, type.length - 2)
                    term = "new $subtype[$size]"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ARR_LENGTH -> {
                    replaceVariables(args, terms, 0)
                    term = "${args[1]}.length"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ARR_GET -> {
                    replaceVariables(args, terms, 0)
                    operand1 = args[2]!!.toValueString()
                    operand2 = args[1]!!.toValueString()
                    term = "$operand2[$operand1]"
                    processTerm(args, terms, 0, term)
                }
                Opcode.ARR_SET -> {
                    replaceVariables(args, terms, -1)
                    false
                }
                Opcode.JMPT, Opcode.JMPF -> {
                    replaceVariables(args, terms, -1)
                    false
                }
                Opcode.JMP, Opcode.ARR_FIND, Opcode.ARR_RFIND -> false
                else -> false
            }
        }

        /**
         * @param args
         * @param terms
         * @param destPos
         * @param term
         */
        fun processTerm(
            args: List<Parameter?>,
            terms: MutableMap<Parameter?, Parameter?>,
            destPos: Int,
            term: String?
        ): Boolean {
            if (destPos >= args.size || !(args[destPos]!!.type === ParamType.IDENTIFIER)) {
                return false
            }
            val dest = args[destPos]
            if (!dest!!.isTemp) {
                return false
            }
            terms[dest] = createTerm(term)
            return true
        }

        /**
         * Replaces certain variables with terms. In particular, all temp variables
         * and autovar names should be replaced.
         *
         * @param args
         * @param terms
         * @param exclude
         */
        fun replaceVariables(args: MutableList<Parameter?>, terms: MutableMap<Parameter?, Parameter?>, exclude: Int) {
            for (i in args.indices) {
                val arg = args[i]
                if (terms.containsKey(arg) && i != exclude) {
                    args[i] = terms[arg]
                } else if (arg!!.isAutovar) {
                    val MATCHER = AUTOVAR_REGEX.matcher(arg.toValueString())
                    MATCHER.matches()
                    val prop = MATCHER.group(1)
                    terms[arg] = createTerm(prop)
                    args[i] = terms[arg]
                }
            }
        }

        /**
         * Creates a function parameter list style string for a `List`.
         *
         * @param <T>
         * @param params
         * @return
        </T> */
        fun <T> paramList(params: List<T>): String {
            val parts = params.joinToString(", ","(",")")
            return parts
        }

        val AUTOVAR_REGEX: Pattern = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE)
    }

    /**
     * Creates a new `StackFrame` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param thread The `ActiveScript` parent.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     * @throws PapyrusElementException
     */
    init {
        val variableCount = input.getInt()
        if (variableCount < 0 || variableCount > 50000) {
            throw PapyrusFormatException("Invalid variableCount $variableCount")
        }
        THREAD = thread
        FLAG = Flags.FlagsByte(input)
        FN_Var_TYPE = read(input)
        scriptName = context.readTString(input)
        script = context.findScript(scriptName)
        BASENAME = context.readTString(input)
        event = context.readTString(input)
        STATUS =
            if (!FLAG.getFlag(0) && FN_Var_TYPE === VarType.NULL) Optional.of(context.readTString(input)) else Optional.empty()
        OPCODE_MAJORVERSION = input.getByte()
        OPCODE_MINORVERSION = input.getByte()
        RETURNTYPE = context.readTString(input)
        docString = context.readTString(input)
        FN_USERFLAGS = Flags.FlagsInt(input)
        FN_FLAGS = Flags.FlagsByte(input)
        val functionParameterCount = UtilityFunctions.toUnsignedInt(input.getShort())
        assert(functionParameterCount in 0..2047) { "Invalid functionParameterCount $functionParameterCount" }
        FN_PARAMS = mutableListOf()
        for (i in 0 until functionParameterCount) {
            val param = FunctionParam(input, context)
            FN_PARAMS.add(param)
        }
        val functionLocalCount = UtilityFunctions.toUnsignedInt(input.getShort())
        assert(functionLocalCount in 0..2047) { "Invalid functionLocalCount $functionLocalCount" }
        FN_LOCALS = ArrayList(functionLocalCount)
        for (i in 0 until functionLocalCount) {
            val local = FunctionLocal(input, context)
            FN_LOCALS.add(local)
        }
        val opcodeCount = UtilityFunctions.toUnsignedInt(input.getShort())
        assert(0 <= opcodeCount)
        try {
            CODE = mutableListOf()
            for (i in 0 until opcodeCount) {
                val opcode = OpcodeData(input, context)
                CODE.add(opcode)
            }
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read StackFrame OpcodeData.", ex, this)
        }
        PTR = input.getInt()
        assert(0 <= PTR)
        owner = read(input, context)
        try {
            VARIABLES = readList(input, variableCount, context).toMutableList()
        } catch (ex: ListException) {
            throw PapyrusElementException("Faileed to read StackFrame variables.", ex, this)
        }
        OWNER = if (owner is VarRef) {
            owner.referent
        } else {
            null
        }
    }
}