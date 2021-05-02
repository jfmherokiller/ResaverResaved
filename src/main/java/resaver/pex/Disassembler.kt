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

import resaver.IString
import resaver.pex.VData.None
import resaver.pex.VData.StrLit
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Class for handling the disassembly of PEX file assembly code.
 *
 * @author Mark Fairchild
 */
object Disassembler {
    /**
     * Premapping removes all of the `::temp` variables and builds
     * more complex arguments. So `<pre>
     * ARR_LENGTH [::temp1, ::EquipSurfaceFeet_var]
     * ASSIGN [a, ::temp1]
    </pre>` *  becomes `<pre>
     * ASSIGN [a, ::EquipSurfaceFeet_var]
    </pre>` *
     *
     * @param block The list of instructions to premap.
     * @param types List of variable types for parameters and local variables.
     * @param terms Map from identifiers to the terms they represent.
     */
    fun preMap(block: MutableList<Pex.Function.Instruction?>, types: List<VariableType>, terms: TermMap) {
        for (i in block.indices) {
            val inst = block[i] ?: continue
            val del = makeTerm(inst.OPCODE, inst.ARGS, types, terms)
            if (del) {
                block[i] = null
            }
        }
    }

    /**
     * Disassembles blocks of assembly code.
     *
     * @param block  The block of code to disassemble.
     * @param types  List of variable types for parameters and local variables.
     * @param indent The indent level.
     * @return A list of disassembled instructions.
     * @throws DisassemblyException
     */
    @Throws(DisassemblyException::class)
    fun disassemble(
        block: List<Pex.Function.Instruction?>,
        types: MutableList<VariableType>,
        indent: Int
    ): List<String> {
        val CODE: MutableList<String> = LinkedList()
        var ptr = 0

        // Loop through the list of instructions. Conditional instructions
        // get handed off to another function.
        return try {
            while (ptr < block.size) {
                val inst = block[ptr]

                // Skip any instruction that was removed by the premapper.
                if (null == inst) {
                    ptr++
                    continue
                }

                // If a conditional instruction is found, handle it in it's
                // own area.
                if (inst.OPCODE.isConditional) {
                    val CONDITIONAL = detectConditional(block, ptr)
                        ?: throw DisassemblyException("Incorrect conditional block.")
                    val ending = ptr + CONDITIONAL[0]
                    val subBlock = block.subList(ptr, ending)
                    val SUB = disassembleConditional(subBlock, types, indent, false)
                    CODE.addAll(SUB)
                    ptr += CONDITIONAL[0]
                } // Non-conditional instructions get handled here.
                else {
                    val SUB = disassembleInstruction(inst, types, indent)
                    CODE.add(SUB)
                    ptr++
                }
            }

            // When the end of the block is reached, return what we have.
            CODE
        } catch (ex: DisassemblyException) {
            // If there's a disassembly error, output the bytecode to the
            // end of the code block. Then pass along the exception.
            CODE.addAll(ex.partial)
            val pdel = ptr + ex.ptrDelta

            //for (int i = ptr; i < block.size(); i++) {
            var i = pdel
            while (i < block.size) {
                val inst = block[i]
                if (null != inst) {
                    CODE.add(String.format("%s%s", tab(indent + 1), inst))
                } else {
                    CODE.add(String.format("%sDELETED", tab(indent + 1)))
                }
                i++
            }
            throw DisassemblyException("Error disassembling.", CODE, block.size, ex)
        }
    }

    /**
     * Disassembles conditional blocks.
     *
     * @param block  The block of code to disassemble.
     * @param types  List of variable types for parameters and local variables.
     * @param indent The indent level.
     * @param elseif A flag indicating whether this is an ELSEIF block.
     * @return
     * @throws DisassemblyException
     */
    @Throws(DisassemblyException::class)
    fun disassembleConditional(
        block: List<Pex.Function.Instruction?>,
        types: MutableList<VariableType>,
        indent: Int,
        elseif: Boolean
    ): List<String> {

        // Make sure that this is ACTUALLY a conditional block.
        assert(null != detectConditional(block, 0)) { "Not a conditional block." }

        // This is a stack algorithm. We walk through the conditional block,
        // gradually assembling the conditional term.
        var rhs = false // Flag indicating whether this is the right-hand side of a predicate.
        var end = false // Flag indicating that the end of the term has been reached.
        var lhs = StringBuilder() // The left-hand side, this is where the term is accumulated.
        val stack1 = LinkedList<Boolean>()
        val stack2 = LinkedList<String>()

        // Step through until we reach an actual instruction of some kind.
        var ptr = 0
        while (ptr < block.size && !end) {

            // Get the next instruction. If it's null, then it's been stripped
            // and we can skip it.
            val inst = block[ptr]
            if (null == inst) {
                ptr++
                continue
            }

            // Get the opcode. 
            val OP = inst.OPCODE

            // Get these, we'll need them later.
            val IF = detectIF(block, ptr)
            val WHILE = detectWHILE(block, ptr)
            end = null != IF || null != WHILE || !OP.isConditional

            // Make the operator, if this is a conditional instruction.
            // Otherwise make the statement.
            var operator: String
            var term: String
            var offset: Int
            if (OP.isConditional) {
                operator = if (OP === Opcode.JMPF) "&&" else "||"
                term = inst.ARGS[0].toString() //.paren();
                offset = (inst.ARGS[1] as VData.Int).value
            } else {
                operator = "INVALID"
                term = inst.ARGS[1].toString() //.paren();
                offset = Int.MIN_VALUE
            }

            // Balance the parentheses.
            if (!rhs && !end) {
                lhs.append(term).append(" ").append(operator).append(" ")
                rhs = true
            } else if (!rhs && end) {
                lhs.append(term)
            } else if (rhs && !end) {
                lhs.append(term)
                while (!stack1.isEmpty() && rhs) {
                    rhs = stack1.pop()
                    lhs = StringBuilder(stack2.pop().toString() + "(" + lhs + ")")
                }
                lhs = StringBuilder("($lhs) $operator ")
            } else if (rhs && end) {
                lhs.append(term)
                while (!stack1.isEmpty() && rhs) {
                    rhs = stack1.pop()
                    lhs = StringBuilder(stack2.pop().toString() + "(" + lhs + ")")
                }
            }

            // Look for a subclause.
            val subclause = !end && block.subList(ptr + 1, ptr + offset)
                .stream()
                .filter { obj: Pex.Function.Instruction? -> Objects.nonNull(obj) }
                .anyMatch { v: Pex.Function.Instruction? -> v!!.OPCODE.isConditional }
            if (subclause) {
                stack1.push(rhs)
                stack2.push(lhs.toString())
                rhs = false
                lhs = StringBuilder()
            }
            if (end) {
                return if (null != IF) {
                    val subBlock = block.subList(ptr, block.size)
                    disassembleIfElseBlock(lhs.toString(), subBlock, types, indent, elseif)
                } else if (null != WHILE) {
                    val subBlock = block.subList(ptr, block.size)
                    disassembleLoop(lhs.toString(), subBlock, types, indent)
                } else {
                    val s = disassembleSimple(0, lhs.toString(), inst.ARGS, types, indent)
                    listOf(s)
                }
            }
            ptr++
        }
        throw IllegalStateException("Should never have got here.")
    }

    /**
     * @param out
     * @param condition
     * @param block
     * @param types
     * @param indent
     * @param elseif
     * @throws DisassemblyException
     */
    @Throws(DisassemblyException::class)
    fun disassembleIfElseBlock(
        condition: String?,
        block: List<Pex.Function.Instruction?>,
        types: MutableList<VariableType>,
        indent: Int,
        elseif: Boolean
    ): List<String> {
        val CODE: MutableList<String> = LinkedList()
        val IF = detectIF(block, 0)!!
        val offs1 = IF[0]
        val offs2 = IF[1]
        val begin = block[0]
        val end = block[offs1 - 1]
        val block1 = block.subList(1, offs1 - 1)
        val block2 = block.subList(offs1, offs1 + offs2 - 1)

        // Disassemble the IF block, which is relatively easy.
        if (elseif) {
            CODE.add(String.format("%sELSEIF %s", tab(indent), condition))
        } else {
            CODE.add(String.format("%sIF %s", tab(indent), condition))
        }
        try {
            val SUB1 = disassemble(block1, types, indent + 1)
            CODE.addAll(SUB1)
        } catch (ex: DisassemblyException) {
            CODE.addAll(ex.partial)
            val pdel = 1 + ex.ptrDelta
            throw DisassemblyException("IF block error.", CODE, pdel, ex)
        }

        // Try to find an ELSEIF block.
        var ptr = 0
        while (ptr < block2.size) {
            if (block2[ptr] == null) {
                ptr++
                continue
            }
            val CONDITIONAL = detectConditional(block2, ptr) ?: break

            // Found an ELSEIF.
            val ending = CONDITIONAL[0]
            val subBlock = block2.subList(ptr, ptr + ending)
            return try {
                val SUB2 = disassembleConditional(subBlock, types, indent, true)
                CODE.addAll(SUB2)
                CODE
            } catch (ex: DisassemblyException) {
                CODE.addAll(ex.partial)
                val pdel = 1 + ex.ptrDelta
                throw DisassemblyException("ELSEIF error.", CODE, pdel, ex)
            }
        }

        // If there's no ELSEIF block, output the ELSE block.  
        if (offs2 > 1) {
            try {
                val SUB3 = disassemble(block2, types, indent + 1)
                CODE.add(String.format("%sELSE", tab(indent)))
                CODE.addAll(SUB3)
                CODE.add(String.format("%sENDIF", tab(indent)))
            } catch (ex: DisassemblyException) {
                CODE.add(String.format("%sELSE", tab(indent)))
                CODE.addAll(ex.partial)
                CODE.add(String.format("%sENDIF", tab(indent)))
                val pdel = 2 + ex.ptrDelta
                throw DisassemblyException("ELSE block error.", CODE, pdel, ex)
            }
        } // If there's no ELSE, still need an ENDIF.
        else {
            CODE.add(String.format("%sENDIF", tab(indent)))
        }
        return CODE
    }

    /**
     * @param out
     * @param block
     * @param types
     * @param indent
     * @throws DisassemblyException
     */
    @Throws(DisassemblyException::class)
    fun disassembleLoop(
        condition: String?,
        block: List<Pex.Function.Instruction?>,
        types: MutableList<VariableType>,
        indent: Int
    ): List<String> {
        val CODE: MutableList<String> = LinkedList()
        val WHILE = detectWHILE(block, 0)
        val offset = Objects.requireNonNull(WHILE)?.get(0)
        val SUB = offset?.minus(1)?.let { block.subList(1, it) }?.let { disassemble(it, types, indent + 1) }
        CODE.add(String.format("%sWHILE %s", tab(indent), condition))
        CODE.addAll(SUB!!)
        CODE.add(String.format("%sENDWHILE", tab(indent)))
        return CODE
    }

    /**
     * @param block
     * @param ptr
     * @return
     */
    fun detectConditional(block: List<Pex.Function.Instruction?>, ptr: Int): IntArray? {
        if (block.isEmpty()) {
            return null
        }
        val begin = block[ptr]
        if (null == begin || !begin.OPCODE.isConditional) {
            return null
        }
        var subptr = ptr
        while (subptr < block.size) {
            val next = block[subptr]
            if (null == next) {
                subptr++
                continue
            }
            val IF = detectIF(block, subptr)
            val WHILE = detectWHILE(block, subptr)
            if (null != IF) {
                return intArrayOf(subptr - ptr + IF[0] + IF[1] - 1)
            } else if (null != WHILE) {
                return intArrayOf(subptr - ptr + WHILE[0])
            }
            if (next.OPCODE.isConditional.not()) {
                //return null;
            }
            subptr++
        }
        return null
    }

    /**
     * @param instructions
     * @param ptr
     * @return
     */
    fun detectIF(instructions: List<Pex.Function.Instruction?>, ptr: Int): IntArray? {
        if (instructions.isEmpty() || ptr >= instructions.size || ptr < 0) {
            return null
        }
        val begin = instructions[ptr]
        if (null == begin || begin.OPCODE !== Opcode.JMPF) {
            return null
        }
        val offset1 = (begin.ARGS[1] as VData.Int).value
        val end = instructions[ptr + offset1 - 1]
        if (null == end || end.OPCODE !== Opcode.JMP) {
            return null
        }
        val offset2 = (end.ARGS[0] as VData.Int).value
        return if (offset2 <= 0) {
            null
        } else intArrayOf(offset1, offset2)
    }

    /**
     * @param instructions
     * @param ptr
     * @return
     */
    fun detectWHILE(instructions: List<Pex.Function.Instruction?>, ptr: Int): IntArray? {
        val begin = instructions[ptr]
        if (null == begin || begin.OPCODE !== Opcode.JMPF) {
            return null
        }
        val offset1 = (begin.ARGS[1] as VData.Int).value
        check(ptr + offset1 - 1 < instructions.size) { "Ptr out of range." }
        val end = instructions[ptr + offset1 - 1]
        if (null == end || end.OPCODE !== Opcode.JMP) {
            return null
        }
        val offset2 = (end.ARGS[0] as VData.Int).value
        if (offset2 > 0) {
            return null
        }
        assert(offset1 <= -offset2) { String.format("Offsets differ: while(%d), endwhile(%d)", offset1, offset2) }
        return intArrayOf(offset1)
    }

    /**
     * @param op
     * @param args
     * @param types
     * @param indent
     * @return
     */
    fun disassembleInstruction(inst: Pex.Function.Instruction, types: MutableList<VariableType>, indent: Int): String {
        val RHS = makeRHS(inst, types)
        return when (inst.OPCODE) {
            Opcode.NOP -> ""
            Opcode.IADD, Opcode.FADD, Opcode.ISUB, Opcode.FSUB, Opcode.IMUL, Opcode.FMUL, Opcode.IDIV, Opcode.FDIV, Opcode.IMOD, Opcode.STRCAT, Opcode.NOT, Opcode.INEG, Opcode.FNEG, Opcode.ASSIGN, Opcode.CAST, Opcode.CMP_EQ, Opcode.CMP_LT, Opcode.CMP_LE, Opcode.CMP_GT, Opcode.CMP_GE, Opcode.ARR_CREATE, Opcode.ARR_LENGTH, Opcode.ARR_GET -> disassembleSimple(
                0,
                RHS,
                inst.ARGS,
                types,
                indent
            )
            Opcode.CALLMETHOD, Opcode.CALLSTATIC, Opcode.PROPGET -> disassembleSimple(
                2,
                RHS,
                inst.ARGS,
                types,
                indent
            )
            Opcode.CALLPARENT, Opcode.ARR_FIND, Opcode.ARR_RFIND -> disassembleSimple(
                1,
                RHS,
                inst.ARGS,
                types,
                indent
            )
            Opcode.RETURN -> "${tab(indent)}RETURN $RHS"
            Opcode.PROPSET -> {
                val obj = inst.ARGS[1]
                val prop = inst.ARGS[0] as VData.ID
                "${tab(indent)}$obj.$prop = $RHS"
            }
            Opcode.ARR_SET -> {
                //VData.ID arr = (VData.ID) inst.getARGS().get(0);
                val idx = inst.ARGS[1]
                String.format("%s%s[%s] = %s", tab(indent), inst.ARGS[0], idx, RHS)
            }
            Opcode.JMPT, Opcode.JMPF, Opcode.JMP -> throw IllegalArgumentException("No code for handling this command: $inst")
            else -> throw IllegalArgumentException("No code for handling this command: $inst")
        }
    }

    /**
     * @param out
     * @param dest
     * @param term
     * @param args
     * @param types
     * @param indent
     */
    fun disassembleSimple(
        lhsPos: Int,
        rhs: String?,
        args: List<VData?>,
        types: MutableList<VariableType>,
        indent: Int
    ): String {
        require(!(lhsPos < 0 || lhsPos >= args.size))
        val lhs = args[lhsPos]
        if (null == lhs || lhs is None) {
            return String.format("%s%s", tab(indent), rhs)
        } else if (lhs !is VData.ID) {
            return String.format("%s%s = %s", tab(indent), lhs, rhs)
        }
        val `var` = lhs
        return if (`var`.isTemp) {
            assert(false)
            throw IllegalArgumentException()
        } else if (`var`.isNonevar) {
            String.format("%s%s", tab(indent), rhs)
        } else {
            val S = StringBuilder()
            S.append(tab(indent))

            // Insert variable declaration.
            types.stream()
                .filter { v: VariableType -> v.name.equals(`var`.getValue()) && v.isLocal }
                .findAny()
                .ifPresent { v: VariableType ->
                    types.remove(v)
                    S.append(v.TYPE).append(" ")
                }
            S.append(String.format("%s = %s", lhs, rhs))
            S.toString()
        }
    }

    /**
     * @param op
     * @param args
     * @param types
     * @param terms
     * @return
     */
    fun makeTerm(op: Opcode?, args: MutableList<VData>, types: List<VariableType>, terms: TermMap): Boolean {
        val term: String
        val operand1: VData?
        val operand2: VData?
        val obj: VData?
        val prop: VData?
        val arr: VData?
        val search: VData?
        val idx: VData?
        val method: VData.ID?
        val subArgs: List<String>
        return when (op) {
            Opcode.IADD, Opcode.FADD, Opcode.STRCAT -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s + %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.ISUB, Opcode.FSUB -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s - %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.IMUL, Opcode.FMUL -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s * %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.IDIV, Opcode.FDIV -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s / %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.IMOD -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s %% %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.RETURN, Opcode.JMPT, Opcode.JMPF, Opcode.ARR_SET, Opcode.PROPSET -> {
                replaceVariables(args, terms, -1)
                false
            }
            Opcode.CALLMETHOD -> {
                replaceVariables(args, terms, 2)
                method = args[0] as VData.ID?
                obj = args[1]
                subArgs = args
                    .subList(4, args.size)
                    .stream()
                    .map { obj: VData? -> obj.toString() } //.paren())
                    .collect(Collectors.toList())
                term = String.format("%s.%s%s", obj, method, paramList(subArgs))
                processTerm(args, terms, 2, term)
            }
            Opcode.CALLPARENT -> {
                replaceVariables(args, terms, 1)
                method = args[0] as VData.ID?
                subArgs = args
                    .subList(3, args.size)
                    .stream()
                    .map { obj: VData? -> obj.toString() } //.paren())
                    .collect(Collectors.toList())
                term = String.format("parent.%s%s", method, paramList(subArgs))
                processTerm(args, terms, 1, term)
            }
            Opcode.CALLSTATIC -> {
                replaceVariables(args, terms, 2)
                obj = args[0]
                method = args[1] as VData.ID?
                subArgs = args
                    .subList(4, args.size)
                    .stream()
                    .map { obj: VData? -> obj.toString() }
                    .collect(Collectors.toList())
                term = String.format("%s.%s%s", obj, method, paramList(subArgs))
                processTerm(args, terms, 2, term)
            }
            Opcode.NOT -> {
                replaceVariables(args, terms, 0)
                term = String.format("!%s", args[1].paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.INEG, Opcode.FNEG -> {
                replaceVariables(args, terms, 0)
                term = String.format("-%s", args[1].paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.ASSIGN -> {
                replaceVariables(args, terms, 0)
                term = String.format("%s", args[1])
                processTerm(args, terms, 0, term)
            }
            Opcode.CAST -> {
                replaceVariables(args, terms, 0)
                val dest = args[0] as VData.ID?
                val arg = args[1]
                val name: IString = dest!!.getValue()
                var type = IString.get("")
                val part = types.stream().filter { t: VariableType -> t.name.equals(name) }.findFirst()
                if (part.isPresent) {
                    type = part.get().TYPE
                }
                term = if (type.equals(IString.get("bool"))) {
                    arg.toString()
                } else if (type.equals(IString.get("string"))) {
                    arg.toString()
                } else {
                    String.format("%s as %s", arg.paren(), type)
                }
                processTerm(args, terms, 0, term)
            }
            Opcode.PROPGET -> {
                replaceVariables(args, terms, 2)
                obj = args[1]
                prop = args[0]
                term = String.format("%s.%s", obj, prop)
                processTerm(args, terms, 2, term)
            }
            Opcode.CMP_EQ -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s == %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.CMP_LT -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s < %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.CMP_LE -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s <= %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.CMP_GT -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s > %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.CMP_GE -> {
                replaceVariables(args, terms, 0)
                operand1 = args[1]
                operand2 = args[2]
                term = String.format("%s >= %s", operand1.paren(), operand2.paren())
                processTerm(args, terms, 0, term)
            }
            Opcode.ARR_CREATE -> {
                val size = args[1]
                val dest = args[0] as VData.ID?
                val name: IString = dest!!.getValue()
                val type: IString =
                    types.stream().filter { t: VariableType -> t.name.equals(name) }.findFirst().get().TYPE
                val subtype = type.toString().substring(0, type.length - 2)
                term = String.format("new %s[%s]", subtype, size)
                processTerm(args, terms, 0, term)
            }
            Opcode.ARR_LENGTH -> {
                replaceVariables(args, terms, 0)
                term = String.format("%s.length", args[1])
                processTerm(args, terms, 0, term)
            }
            Opcode.ARR_GET -> {
                replaceVariables(args, terms, 0)
                idx = args[2]
                arr = args[1]
                term = String.format("%s[%s]", arr, idx)
                processTerm(args, terms, 0, term)
            }
            Opcode.ARR_FIND -> {
                replaceVariables(args, terms, 1)
                arr = args[0]
                search = args[2]
                idx = args[3] as VData.Int?
                term = String.format("%s.find(%s, %s)", arr, search, idx)
                processTerm(args, terms, 1, term)
            }
            Opcode.ARR_RFIND -> {
                replaceVariables(args, terms, 1)
                arr = args[0]
                search = args[2]
                idx = args[3] as VData.Int?
                term = String.format("%s.rfind(%s, %s)", arr, search, idx)
                processTerm(args, terms, 1, term)
            }
            Opcode.JMP -> false
            else -> false
        }
    }

    fun makeRHS(inst: Pex.Function.Instruction, types: List<VariableType> /*, TermMap terms*/): String {
        return when (inst.OPCODE) {
            Opcode.IADD, Opcode.FADD, Opcode.STRCAT -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s + %s", operand1.paren(), operand2.paren())
            }
            Opcode.ISUB, Opcode.FSUB -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s - %s", operand1.paren(), operand2.paren())
            }
            Opcode.IMUL, Opcode.FMUL -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s * %s", operand1.paren(), operand2.paren())
            }
            Opcode.IDIV, Opcode.FDIV -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s / %s", operand1.paren(), operand2.paren())
            }
            Opcode.IMOD -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s %% %s", operand1.paren(), operand2.paren())
            }
            Opcode.RETURN, Opcode.JMPT, Opcode.JMPF -> {
                inst.ARGS[0].toString()
            }
            Opcode.CALLMETHOD -> {
                val method = inst.ARGS[0] as VData.ID
                val obj = inst.ARGS[1]
                val subArgs = inst.ARGS
                    .subList(4, inst.ARGS.size)
                    .stream()
                    .map { obj: VData -> obj.toString() } //.paren())
                    .collect(Collectors.toList())
                String.format("%s.%s%s", obj, method, paramList(subArgs))
            }
            Opcode.CALLPARENT -> {
                val method = inst.ARGS[0] as VData.ID
                val subArgs = inst.ARGS
                    .subList(3, inst.ARGS.size)
                    .stream()
                    .map { obj: VData -> obj.toString() } //.paren())
                    .collect(Collectors.toList())
                String.format("parent.%s%s", method, paramList(subArgs))
            }
            Opcode.CALLSTATIC -> {
                val obj = inst.ARGS[0]
                val method = inst.ARGS[1] as VData.ID
                val subArgs = inst.ARGS
                    .subList(4, inst.ARGS.size)
                    .stream()
                    .map { obj: VData -> obj.toString() } //.paren())
                    .collect(Collectors.toList())
                String.format("%s.%s%s", obj, method, paramList(subArgs))
            }
            Opcode.NOT -> String.format("NOT %s", inst.ARGS[1].paren())
            Opcode.INEG, Opcode.FNEG -> String.format("-%s", inst.ARGS[1].paren())
            Opcode.ASSIGN -> inst.ARGS[1].toString()
            Opcode.CAST -> {
                val dest = inst.ARGS[0] as VData.ID
                val arg = inst.ARGS[1]
                val name: IString = dest.getValue()
                val type: IString =
                    types.stream().filter { t: VariableType -> t.name.equals(name) }.findFirst().get().TYPE
                String.format("%s as %s", arg.paren(), type)
            }
            Opcode.PROPGET -> {
                val obj = inst.ARGS[1]
                val prop = inst.ARGS[0]
                String.format("%s.%s", obj, prop)
            }
            Opcode.PROPSET, Opcode.ARR_SET -> {
                inst.ARGS[2].toString() //.paren();
            }
            Opcode.CMP_EQ -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s == %s", operand1.paren(), operand2.paren())
            }
            Opcode.CMP_LT -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s < %s", operand1.paren(), operand2.paren())
            }
            Opcode.CMP_LE -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s <= %s", operand1.paren(), operand2.paren())
            }
            Opcode.CMP_GT -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s > %s", operand1.paren(), operand2.paren())
            }
            Opcode.CMP_GE -> {
                val operand1 = inst.ARGS[1]
                val operand2 = inst.ARGS[2]
                String.format("%s >= %s", operand1.paren(), operand2.paren())
            }
            Opcode.ARR_CREATE -> {
                val size = inst.ARGS[1]
                val dest = inst.ARGS[0] as VData.ID
                val name: IString = dest.getValue()
                val type: IString =
                    types.stream().filter { t: VariableType -> t.name.equals(name) }.findFirst().get().TYPE
                val subtype = type.toString().substring(0, type.length - 2)
                String.format("new %s[%s]", subtype, size)
            }
            Opcode.ARR_LENGTH -> {
                String.format("%s.length", inst.ARGS[1])
            }
            Opcode.ARR_GET -> {
                val idx = inst.ARGS[2]
                val arr = inst.ARGS[1]
                String.format("%s[%s]", arr, idx)
            } //.paren();
            Opcode.JMP, Opcode.ARR_FIND, Opcode.ARR_RFIND -> String.format("%s", inst.ARGS)
            else -> String.format("%s", inst.ARGS)
        }
    }

    /**
     * @param args
     * @param terms
     * @param destPos
     * @param positions
     */
    fun processTerm(args: List<VData?>, terms: TermMap, destPos: Int, term: String?): Boolean {
        if (destPos >= args.size || args[destPos] !is VData.ID) {
            return false
        }
        val dest = (args[destPos] as VData.ID?)!!
        if (!dest.isTemp) {
            return false
        }
        terms[dest] = VData.Term(term!!)
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
    fun replaceVariables(args: MutableList<VData>, terms: TermMap, exclude: Int) {
        for (i in args.indices) {
            val arg = args[i]
            if (arg is VData.ID) {
                val id = arg
                if (terms.containsKey(id) && i != exclude) {
                    args[i] = terms[id]!!
                } else if (id.isAutovar) {
                    val MATCHER = AUTOVAR_REGEX.matcher(id.toString())
                    MATCHER.matches()
                    val prop = MATCHER.group(1)
                    terms[id] = VData.Term(prop)
                    args[i] = terms[id]!!
                }
            } else if (arg is VData.Str) {
                args[i] = StrLit(arg.string.toString())
            }
        }
    }

    /**
     * @param <T>
     * @param params
     * @return
    </T> */
    fun <T> paramList(params: List<T>): String {
        return params.stream()
            .map { obj: T -> obj.toString() }
            .collect(Collectors.joining(", ", "(", ")"))
    }

    /**
     * @param n
     * @return
     */
    fun tab(n: Int): String {
        val BUF = StringBuilder()
        for (i in 0 until n) {
            BUF.append('\t')
        }
        return BUF.toString()
    }

    val AUTOVAR_REGEX = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE)
}