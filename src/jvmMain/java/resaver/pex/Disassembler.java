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
package resaver.pex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resaver.IString;
import resaver.pex.Pex.Function.Instruction;

import static resaver.pex.Opcode.*;

/**
 * Class for handling the disassembly of PEX file assembly code.
 *
 * @author Mark Fairchild
 */
final public class Disassembler {

    /**
     * Premapping removes all of the <code>::temp</code> variables and builds
     * more complex arguments. So <code><pre>
     * ARR_LENGTH [::temp1, ::EquipSurfaceFeet_var]
     * ASSIGN [a, ::temp1]
     * </pre></code> becomes <code><pre>
     * ASSIGN [a, ::EquipSurfaceFeet_var]
     * </pre></code>
     *
     * @param block The list of instructions to premap.
     * @param types List of variable types for parameters and local variables.
     * @param terms Map from identifiers to the terms they represent.
     *
     */
    static void preMap(@NotNull List<Instruction> block, @NotNull List<VariableType> types, @NotNull TermMap terms) {
        for (int i = 0; i < block.size(); i++) {
            Instruction inst = block.get(i);
            if (null == inst) {
                continue;
            }

            boolean del = makeTerm(inst.OPCODE, inst.ARGS, types, terms);
            if (del) {
                block.set(i, null);
            }
        }
    }

    /**
     * Disassembles blocks of assembly code.
     *
     * @param block The block of code to disassemble.
     * @param types List of variable types for parameters and local variables.
     * @param indent The indent level.
     * @return A list of disassembled instructions.
     * @throws DisassemblyException
     */
    @NotNull
    static List<String> disassemble(@NotNull List<Instruction> block, @NotNull List<VariableType> types, int indent)
            throws DisassemblyException {

        final List<String> CODE = new LinkedList<>();
        int ptr = 0;

        // Loop through the list of instructions. Conditional instructions
        // get handed off to another function.
        try {
            while (ptr < block.size()) {
                Instruction inst = block.get(ptr);

                // Skip any instruction that was removed by the premapper.
                if (null == inst) {
                    ptr++;
                    continue;
                }

                // If a conditional instruction is found, handle it in it's
                // own area.
                if (inst.OPCODE.isConditional()) {
                    int[] CONDITIONAL = detectConditional(block, ptr);
                    if (null == CONDITIONAL) {

                        throw new DisassemblyException("Incorrect conditional block.");
                    }

                    int ending = ptr + CONDITIONAL[0];
                    List<Instruction> subBlock = block.subList(ptr, ending);
                    final List<String> SUB = disassembleConditional(subBlock, types, indent, false);
                    CODE.addAll(SUB);
                    ptr += CONDITIONAL[0];

                } // Non-conditional instructions get handled here.
                else {
                    final String SUB = disassembleInstruction(inst, types, indent);
                    CODE.add(SUB);
                    ptr++;
                }
            }

            // When the end of the block is reached, return what we have.
            return CODE;

        } catch (DisassemblyException ex) {
            // If there's a disassembly error, output the bytecode to the
            // end of the code block. Then pass along the exception.
            CODE.addAll(ex.getPartial());
            int pdel = ptr + ex.getPtrDelta();

            //for (int i = ptr; i < block.size(); i++) {
            for (int i = pdel; i < block.size(); i++) {
                Instruction inst = block.get(i);
                if (null != inst) {
                    CODE.add(String.format("%s%s", Disassembler.tab(indent + 1), inst));
                } else {
                    CODE.add(String.format("%sDELETED", Disassembler.tab(indent + 1)));
                }
            }
            throw new DisassemblyException("Error disassembling.", CODE, block.size(), ex);
        }
    }

    /**
     * Disassembles conditional blocks.
     *
     * @param block The block of code to disassemble.
     * @param types List of variable types for parameters and local variables.
     * @param indent The indent level.
     * @param elseif A flag indicating whether this is an ELSEIF block.
     * @return
     * @throws DisassemblyException
     */
    @NotNull
    static List<String> disassembleConditional(@NotNull List<Instruction> block, @NotNull List<VariableType> types, int indent, boolean elseif)
            throws DisassemblyException {

        // Make sure that this is ACTUALLY a conditional block.
        assert null != detectConditional(block, 0) : "Not a conditional block.";

        // This is a stack algorithm. We walk through the conditional block,
        // gradually assembling the conditional term.
        boolean rhs = false; // Flag indicating whether this is the right-hand side of a predicate.
        boolean end = false; // Flag indicating that the end of the term has been reached.
        StringBuilder lhs = new StringBuilder(); // The left-hand side, this is where the term is accumulated.
        LinkedList<Boolean> stack1 = new LinkedList<>();
        LinkedList<String> stack2 = new LinkedList<>();

        // Step through until we reach an actual instruction of some kind.
        for (int ptr = 0; ptr < block.size() && !end; ptr++) {
            // Get the next instruction. If it's null, then it's been stripped
            // and we can skip it.
            Instruction inst = block.get(ptr);
            if (null == inst) {
                continue;
            }

            // Get the opcode. 
            Opcode OP = inst.OPCODE;

            // Get these, we'll need them later.
            final int[] IF = detectIF(block, ptr);
            final int[] WHILE = detectWHILE(block, ptr);
            end = (null != IF) || (null != WHILE) || (!OP.isConditional());

            // Make the operator, if this is a conditional instruction.
            // Otherwise make the statement.
            String operator;
            String term;
            int offset;

            if (OP.isConditional()) {
                operator = (OP == JMPF ? "&&" : "||");
                term = inst.ARGS.get(0).toString();//.paren();
                offset = ((VDataInt) inst.ARGS.get(1)).getValue();
            } else {
                operator = "INVALID";
                term = inst.ARGS.get(1).toString();//.paren();
                offset = Integer.MIN_VALUE;
            }

            // Balance the parentheses.
            if (!rhs && !end) {
                lhs.append(term).append(" ").append(operator).append(" ");
                rhs = true;
            } else if (!rhs && end) {
                lhs.append(term);
            } else if (rhs && !end) {
                lhs.append(term);
                while (!stack1.isEmpty() && rhs) {
                    rhs = stack1.pop();
                    lhs = new StringBuilder(stack2.pop() + "(" + lhs + ")");
                }
                lhs = new StringBuilder("(" + lhs + ") " + operator + " ");
            } else if (rhs && end) {
                lhs.append(term);
                while (!stack1.isEmpty() && rhs) {
                    rhs = stack1.pop();
                    lhs = new StringBuilder(stack2.pop() + "(" + lhs + ")");
                }
            }

            // Look for a subclause.
            boolean subclause;
            boolean b = false;
            for (Instruction v : block.subList(ptr + 1, ptr + offset)) {
                if (v != null) {
                    if (v.OPCODE.isConditional()) {
                        b = true;
                        break;
                    }
                }
            }
            if (b) {
                subclause = !end;
            } else {
                subclause = false;
            }

            if (subclause) {
                stack1.push(rhs);
                stack2.push(lhs.toString());
                rhs = false;
                lhs = new StringBuilder();
            }

            if (end) {
                if (null != IF) {
                    List<Instruction> subBlock = block.subList(ptr, block.size());
                    return disassembleIfElseBlock(lhs.toString(), subBlock, types, indent, elseif);
                } else if (null != WHILE) {
                    List<Instruction> subBlock = block.subList(ptr, block.size());
                    return disassembleLoop(lhs.toString(), subBlock, types, indent);
                } else {
                    String s = disassembleSimple(0, lhs.toString(), inst.ARGS, types, indent);
                    return Collections.singletonList(s);
                }
            }
        }

        throw new IllegalStateException("Should never have got here.");
    }

    /**
     *
     * @param out
     * @param condition
     * @param block
     * @param types
     * @param indent
     * @param elseif
     * @throws DisassemblyException
     */
    @NotNull
    static List<String> disassembleIfElseBlock(String condition, @NotNull List<Instruction> block, @NotNull List<VariableType> types, int indent, boolean elseif)
            throws DisassemblyException {

        final List<String> CODE = new LinkedList<>();
        final int[] IF = detectIF(block, 0);
        assert null != IF;

        int offs1 = IF[0];
        int offs2 = IF[1];
        Instruction begin = block.get(0);
        Instruction end = block.get(offs1 - 1);
        List<Instruction> block1 = block.subList(1, offs1 - 1);
        List<Instruction> block2 = block.subList(offs1, offs1 + offs2 - 1);

        // Disassemble the IF block, which is relatively easy.
        if (elseif) {
            CODE.add(String.format("%sELSEIF %s", tab(indent), condition));
        } else {
            CODE.add(String.format("%sIF %s", tab(indent), condition));
        }

        try {
            final List<String> SUB1 = disassemble(block1, types, indent + 1);
            CODE.addAll(SUB1);
        } catch (DisassemblyException ex) {
            CODE.addAll(ex.getPartial());
            int pdel = 1 + ex.getPtrDelta();
            throw new DisassemblyException("IF block error.", CODE, pdel, ex);
        }

        // Try to find an ELSEIF block.
        int ptr = 0;
        while (ptr < block2.size()) {
            if (block2.get(ptr) == null) {
                ptr++;
                continue;
            }

            final int[] CONDITIONAL = detectConditional(block2, ptr);
            if (null == CONDITIONAL) {
                break;
            }

            // Found an ELSEIF.
            int ending = CONDITIONAL[0];
            List<Instruction> subBlock = block2.subList(ptr, ptr + ending);
            try {
                final List<String> SUB2 = disassembleConditional(subBlock, types, indent, true);
                CODE.addAll(SUB2);
                return CODE;
            } catch (DisassemblyException ex) {
                CODE.addAll(ex.getPartial());
                int pdel = 1 + ex.getPtrDelta();
                throw new DisassemblyException("ELSEIF error.", CODE, pdel, ex);
            }
        }

        // If there's no ELSEIF block, output the ELSE block.  
        if (offs2 > 1) {
            try {
                final List<String> SUB3 = disassemble(block2, types, indent + 1);
                CODE.add(String.format("%sELSE", tab(indent)));
                CODE.addAll(SUB3);
                CODE.add(String.format("%sENDIF", tab(indent)));

            } catch (DisassemblyException ex) {
                CODE.add(String.format("%sELSE", tab(indent)));
                CODE.addAll(ex.getPartial());
                CODE.add(String.format("%sENDIF", tab(indent)));
                int pdel = 2 + ex.getPtrDelta();
                throw new DisassemblyException("ELSE block error.", CODE, pdel, ex);
            }

        } // If there's no ELSE, still need an ENDIF.
        else {
            CODE.add(String.format("%sENDIF", tab(indent)));
        }

        return CODE;
    }

    /**
     *
     * @param out
     * @param block
     * @param types
     * @param indent
     * @throws DisassemblyException
     */
    @NotNull
    static List<String> disassembleLoop(String condition, @NotNull List<Instruction> block, @NotNull List<VariableType> types, int indent)
            throws DisassemblyException {

        final List<String> CODE = new LinkedList<>();
        final int[] WHILE = detectWHILE(block, 0);
        int offset = WHILE[0];

        final List<String> SUB = disassemble(block.subList(1, offset - 1), types, indent + 1);

        CODE.add(String.format("%sWHILE %s", tab(indent), condition));
        CODE.addAll(SUB);
        CODE.add(String.format("%sENDWHILE", tab(indent)));
        return CODE;
    }

    /**
     *
     * @param block
     * @param ptr
     * @return
     */
    @Nullable
    static int[] detectConditional(@NotNull List<Instruction> block, int ptr) {
        if (block.isEmpty()) {
            return null;
        }

        Instruction begin = block.get(ptr);
        if (null == begin || !begin.OPCODE.isConditional()) {
            return null;
        }

        int subptr = ptr;
        while (subptr < block.size()) {
            Instruction next = block.get(subptr);
            if (null == next) {
                subptr++;
                continue;
            }

            final int[] IF = detectIF(block, subptr);
            final int[] WHILE = detectWHILE(block, subptr);
            if (null != IF) {
                return new int[]{subptr - ptr + IF[0] + IF[1] - 1};
            } else if (null != WHILE) {
                return new int[]{subptr - ptr + WHILE[0]};
            }

            if (!next.OPCODE.isConditional()) {
                //return null;
            }

            subptr++;
        }

        return null;
    }

    /**
     *
     * @param instructions
     * @param ptr
     * @return
     */
    @Nullable
    static int[] detectIF(@NotNull List<Instruction> instructions, int ptr) {
        if (instructions.isEmpty() || ptr >= instructions.size() || ptr < 0) {
            return null;
        }

        Instruction begin = instructions.get(ptr);

        if (null == begin || begin.OPCODE != JMPF) {
            return null;
        }

        int offset1 = ((VDataInt) begin.ARGS.get(1)).getValue();
        Instruction end = instructions.get(ptr + offset1 - 1);

        if (null == end || end.OPCODE != JMP) {
            return null;
        }

        int offset2 = ((VDataInt) end.ARGS.get(0)).getValue();
        if (offset2 <= 0) {
            return null;
        }

        return new int[]{offset1, offset2};
    }

    /**
     *
     * @param instructions
     * @param ptr
     * @return
     */
    @Nullable
    static int[] detectWHILE(@NotNull List<Instruction> instructions, int ptr) {
        Instruction begin = instructions.get(ptr);

        if (null == begin || begin.OPCODE != JMPF) {
            return null;
        }

        int offset1 = ((VDataInt) begin.ARGS.get(1)).getValue();
        if (ptr + offset1 - 1 >= instructions.size()) {
            throw new IllegalStateException("Ptr out of range.");
        }
        Instruction end = instructions.get(ptr + offset1 - 1);

        if (null == end || end.OPCODE != JMP) {
            return null;
        }

        int offset2 = ((VDataInt) end.ARGS.get(0)).getValue();
        if (offset2 > 0) {
            return null;
        }

        assert offset1 <= -offset2 : String.format("Offsets differ: while(%d), endwhile(%d)", offset1, offset2);
        return new int[]{offset1};
    }

    /**
     *
     * @param op
     * @param args
     * @param types
     * @param indent
     * @return
     */
    static String disassembleInstruction(@NotNull Instruction inst, @NotNull List<VariableType> types, int indent) {
        final String RHS = makeRHS(inst, types);

        switch (inst.OPCODE) {
            case NOP:
                return "";

            case IADD:
            case FADD:
            case ISUB:
            case FSUB:
            case IMUL:
            case FMUL:
            case IDIV:
            case FDIV:
            case IMOD:
            case STRCAT:
            case NOT:
            case INEG:
            case FNEG:
            case ASSIGN:
            case CAST:
            case CMP_EQ:
            case CMP_LT:
            case CMP_LE:
            case CMP_GT:
            case CMP_GE:
            case ARR_CREATE:
            case ARR_LENGTH:
            case ARR_GET:
                return disassembleSimple(0, RHS, inst.ARGS, types, indent);

            case CALLMETHOD:
            case CALLSTATIC:
            case PROPGET:
                return disassembleSimple(2, RHS, inst.ARGS, types, indent);

            case CALLPARENT:
            case ARR_FIND:
            case ARR_RFIND:
                return disassembleSimple(1, RHS, inst.ARGS, types, indent);

            case RETURN:
                if (null == RHS) {
                    return String.format("%sRETURN", tab(indent));
                } else {
                    return String.format("%sRETURN %s", tab(indent), RHS);
                }

            case PROPSET:
                VData obj = inst.ARGS.get(1);
                VDataID prop = (VDataID) inst.ARGS.get(0);
                return String.format("%s%s.%s = %s", tab(indent), obj, prop, RHS);

            case ARR_SET:
                //VData.ID arr = (VData.ID) inst.ARGS.get(0);
                VData arr = inst.ARGS.get(0);
                VData idx = inst.ARGS.get(1);
                return String.format("%s%s[%s] = %s", tab(indent), arr, idx, RHS);

            case JMPT:
            case JMPF:
            case JMP:
            default:
                throw new IllegalArgumentException("No code for handling this command: " + inst);
        }
    }

    /**
     *
     * @param out
     * @param dest
     * @param term
     * @param args
     * @param types
     * @param indent
     */
    static String disassembleSimple(int lhsPos, String rhs, @NotNull List<VData> args, @NotNull List<VariableType> types, int indent) {
        if (lhsPos < 0 || lhsPos >= args.size()) {
            throw new IllegalArgumentException();
        }

        VData lhs = args.get(lhsPos);

        if (null == lhs || lhs instanceof VDataNone) {
            return String.format("%s%s", tab(indent), rhs);

        } else if (!(lhs instanceof VDataID)) {
            return String.format("%s%s = %s", tab(indent), lhs, rhs);
        }

        VDataID var = (VDataID) lhs;

        if (var.isTemp()) {
            assert false;
            throw new IllegalArgumentException();

        } else if (var.isNonevar()) {
            return String.format("%s%s", tab(indent), rhs);

        } else {
            final StringBuilder S = new StringBuilder();
            S.append(tab(indent));

            // Insert variable declaration.
            for (VariableType type : types) {
                if (type.name.equals(var.getValue()) && type.isLocal()) {
                    types.remove(type);
                    S.append(type.TYPE).append(" ");
                    break;
                }
            }

            S.append(String.format("%s = %s", lhs, rhs));
            return S.toString();
        }
    }

    /**
     *
     * @param op
     * @param args
     * @param types
     * @param terms
     * @return
     */
    static boolean makeTerm(@NotNull Opcode op, @NotNull List<VData> args, @NotNull List<VariableType> types, @NotNull TermMap terms) {
        String term;
        VData operand1, operand2, obj, prop, arr, search, idx;
        VDataID method;
        List<String> subArgs;

        switch (op) {
            case IADD:
            case FADD:
            case STRCAT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s + %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case ISUB:
            case FSUB:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s - %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case IMUL:
            case FMUL:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s * %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case IDIV:
            case FDIV:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s / %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case IMOD:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s %% %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case RETURN:
                replaceVariables(args, terms, -1);
                return false;

            case CALLMETHOD:
                replaceVariables(args, terms, 2);
                method = (VDataID) args.get(0);
                obj = args.get(1);
                //.paren())
                List<String> list = new ArrayList<>();
                for (VData vData : args
                        .subList(4, args.size())) {
                    String toString = vData.toString();
                    list.add(toString);
                }
                subArgs = list;
                term = String.format("%s.%s%s", obj, method, paramList(subArgs));
                return processTerm(args, terms, 2, term);

            case CALLPARENT:
                replaceVariables(args, terms, 1);
                method = (VDataID) args.get(0);
                //.paren())
                List<String> result = new ArrayList<>();
                for (VData vData : args
                        .subList(3, args.size())) {
                    String toString = vData.toString();
                    result.add(toString);
                }
                subArgs = result;
                term = String.format("parent.%s%s", method, paramList(subArgs));
                return processTerm(args, terms, 1, term);

            case CALLSTATIC:
                replaceVariables(args, terms, 2);
                obj = args.get(0);
                method = (VDataID) args.get(1);
                List<String> list1 = new ArrayList<>();
                for (VData vData : args
                        .subList(4, args.size())) {
                    String toString = vData.toString();
                    list1.add(toString);
                }
                subArgs = list1;
                term = String.format("%s.%s%s", obj, method, paramList(subArgs));
                return processTerm(args, terms, 2, term);

            case NOT:
                replaceVariables(args, terms, 0);
                term = String.format("!%s", args.get(1).paren());
                return processTerm(args, terms, 0, term);

            case INEG:
            case FNEG:
                replaceVariables(args, terms, 0);
                term = String.format("-%s", args.get(1).paren());
                return processTerm(args, terms, 0, term);

            case ASSIGN:
                replaceVariables(args, terms, 0);
                term = String.format("%s", args.get(1));
                return processTerm(args, terms, 0, term);

            case CAST: {
                replaceVariables(args, terms, 0);
                VDataID dest = (VDataID) args.get(0);
                VData arg = args.get(1);
                IString name = dest.getValue();
                Optional<VariableType> found = Optional.empty();
                for (VariableType t : types) {
                    if (t.name.equals(name)) {
                        found = Optional.of(t);
                        break;
                    }
                }
                IString type = found.get().TYPE;

                if (type.equals(IString.get("bool"))) {
                    term = arg.toString();
                } else if (type.equals(IString.get("string"))) {
                    term = arg.toString();
                } else {
                    term = String.format("%s as %s", arg.paren(), type);
                }
                return processTerm(args, terms, 0, term);
            }

            case PROPGET:
                replaceVariables(args, terms, 2);
                obj = args.get(1);
                prop = args.get(0);
                term = String.format("%s.%s", obj, prop);
                return processTerm(args, terms, 2, term);

            case PROPSET:
                replaceVariables(args, terms, -1);
                return false;

            case CMP_EQ:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s == %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case CMP_LT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s < %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case CMP_LE:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s <= %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case CMP_GT:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s > %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case CMP_GE:
                replaceVariables(args, terms, 0);
                operand1 = args.get(1);
                operand2 = args.get(2);
                term = String.format("%s >= %s", operand1.paren(), operand2.paren());
                return processTerm(args, terms, 0, term);

            case ARR_CREATE:
                VData size = args.get(1);
                VDataID dest = (VDataID) args.get(0);
                IString name = dest.getValue();
                Optional<VariableType> found = Optional.empty();
                for (VariableType t : types) {
                    if (t.name.equals(name)) {
                        found = Optional.of(t);
                        break;
                    }
                }
                IString type = found.get().TYPE;
                String subtype = type.toString().substring(0, type.length() - 2);
                term = String.format("new %s[%s]", subtype, size);
                return processTerm(args, terms, 0, term);

            case ARR_LENGTH:
                replaceVariables(args, terms, 0);
                term = String.format("%s.length", args.get(1));
                return processTerm(args, terms, 0, term);

            case ARR_GET:
                replaceVariables(args, terms, 0);
                idx = args.get(2);
                arr = args.get(1);
                term = String.format("%s[%s]", arr, idx);
                return processTerm(args, terms, 0, term);

            case ARR_SET:
                replaceVariables(args, terms, -1);
                return false;

            case JMPT:
            case JMPF:
                replaceVariables(args, terms, -1);
                return false;

            case ARR_FIND:
                replaceVariables(args, terms, 1);
                arr = args.get(0);
                search = args.get(2);
                idx = (VDataInt) args.get(3);
                term = String.format("%s.find(%s, %s)", arr, search, idx);
                return processTerm(args, terms, 1, term);

            case ARR_RFIND:
                replaceVariables(args, terms, 1);
                arr = args.get(0);
                search = args.get(2);
                idx = (VDataInt) args.get(3);
                term = String.format("%s.rfind(%s, %s)", arr, search, idx);
                return processTerm(args, terms, 1, term);

            case JMP:
            default:
                return false;
        }
    }

    static String makeRHS(@NotNull Instruction inst, @NotNull List<VariableType> types/*, TermMap terms*/) {
        switch (inst.OPCODE) {
            case IADD:
            case FADD:
            case STRCAT: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s + %s", operand1.paren(), operand2.paren());
            }
            case ISUB:
            case FSUB: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s - %s", operand1.paren(), operand2.paren());
            }
            case IMUL:
            case FMUL: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s * %s", operand1.paren(), operand2.paren());
            }
            case IDIV:
            case FDIV: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s / %s", operand1.paren(), operand2.paren());
            }
            case IMOD: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s %% %s", operand1.paren(), operand2.paren());
            }

            case RETURN: {
                return inst.ARGS.get(0).toString();
            }

            case CALLMETHOD: {
                VDataID method = (VDataID) inst.ARGS.get(0);
                VData obj = inst.ARGS.get(1);
                //.paren())
                List<String> subArgs = new ArrayList<>();
                for (VData vData : inst.ARGS
                        .subList(4, inst.ARGS.size())) {
                    String toString = vData.toString();
                    subArgs.add(toString);
                }
                return String.format("%s.%s%s", obj, method, paramList(subArgs));
            }

            case CALLPARENT: {
                VDataID method = (VDataID) inst.ARGS.get(0);
                //.paren())
                List<String> subArgs = new ArrayList<>();
                for (VData vData : inst.ARGS
                        .subList(3, inst.ARGS.size())) {
                    String toString = vData.toString();
                    subArgs.add(toString);
                }
                return String.format("parent.%s%s", method, paramList(subArgs));
            }

            case CALLSTATIC: {
                VData obj = inst.ARGS.get(0);
                VDataID method = (VDataID) inst.ARGS.get(1);
                //.paren())
                List<String> subArgs = new ArrayList<>();
                for (VData vData : inst.ARGS
                        .subList(4, inst.ARGS.size())) {
                    String toString = vData.toString();
                    subArgs.add(toString);
                }
                return String.format("%s.%s%s", obj, method, paramList(subArgs));
            }

            case NOT:
                return String.format("NOT %s", inst.ARGS.get(1).paren());

            case INEG:
            case FNEG:
                return String.format("-%s", inst.ARGS.get(1).paren());

            case ASSIGN:
                return inst.ARGS.get(1).toString();

            case CAST: {
                VDataID dest = (VDataID) inst.ARGS.get(0);
                VData arg = inst.ARGS.get(1);
                IString name = dest.getValue();
                Optional<VariableType> found = Optional.empty();
                for (VariableType t : types) {
                    if (t.name.equals(name)) {
                        found = Optional.of(t);
                        break;
                    }
                }
                IString type = found.get().TYPE;
                return String.format("%s as %s", arg.paren(), type);
            }

            case PROPGET: {
                VData obj = inst.ARGS.get(1);
                VData prop = inst.ARGS.get(0);
                return String.format("%s.%s", obj, prop);
            }

            case PROPSET: {
                return inst.ARGS.get(2).toString();//.paren();
            }

            case CMP_EQ: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s == %s", operand1.paren(), operand2.paren());
            }

            case CMP_LT: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s < %s", operand1.paren(), operand2.paren());
            }

            case CMP_LE: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s <= %s", operand1.paren(), operand2.paren());
            }

            case CMP_GT: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s > %s", operand1.paren(), operand2.paren());
            }

            case CMP_GE: {
                VData operand1 = inst.ARGS.get(1);
                VData operand2 = inst.ARGS.get(2);
                return String.format("%s >= %s", operand1.paren(), operand2.paren());
            }

            case ARR_CREATE: {
                VData size = inst.ARGS.get(1);
                VDataID dest = (VDataID) inst.ARGS.get(0);
                IString name = dest.getValue();
                Optional<VariableType> found = Optional.empty();
                for (VariableType t : types) {
                    if (t.name.equals(name)) {
                        found = Optional.of(t);
                        break;
                    }
                }
                IString type = found.get().TYPE;
                String subtype = type.toString().substring(0, type.length() - 2);
                return String.format("new %s[%s]", subtype, size);
            }

            case ARR_LENGTH: {
                return String.format("%s.length", inst.ARGS.get(1));
            }

            case ARR_GET: {
                VData idx = inst.ARGS.get(2);
                VData arr = inst.ARGS.get(1);
                return String.format("%s[%s]", arr, idx);
            }

            case ARR_SET: {
                return inst.ARGS.get(2).toString();//.paren();
            }

            case JMPT:
            case JMPF: {
                return inst.ARGS.get(0).toString();//.paren();
            }

            case JMP:
            case ARR_FIND:
            case ARR_RFIND:
            default:
                return String.format("%s", inst.ARGS);
        }
    }

    /**
     * @param args
     * @param terms
     * @param destPos
     * @param positions
     */
    static boolean processTerm(@NotNull List<VData> args, @NotNull TermMap terms, int destPos, String term) {
        if (destPos >= args.size() || !(args.get(destPos) instanceof VDataID)) {
            return false;
        }
        VDataID dest = (VDataID) args.get(destPos);

        if (!dest.isTemp()) {
            return false;
        }

        terms.put(dest, new VDataTerm(term));
        return true;
    }

    /**
     * Replaces certain variables with terms. In particular, all temp variables
     * and autovar names should be replaced.
     *
     * @param args
     * @param terms
     * @param exclude
     */
    static void replaceVariables(@NotNull List<VData> args, @NotNull TermMap terms, int exclude) {
        for (int i = 0; i < args.size(); i++) {
            VData arg = args.get(i);
            if (arg instanceof VDataID) {
                VDataID id = (VDataID) arg;

                if (terms.containsKey(id) && i != exclude) {
                    args.set(i, terms.get(id));

                } else if (id.isAutovar()) {
                    final Matcher MATCHER = AUTOVAR_REGEX.matcher(id.toString());
                    MATCHER.matches();
                    String prop = MATCHER.group(1);
                    terms.put(id, new VDataTerm(prop));
                    args.set(i, terms.get(id));
                }

            } else if (arg instanceof VDataStr) {
                VDataStr str = (VDataStr) arg;
                args.set(i, new VDataStrLit(str.getString().toString()));
            }
        }
    }

    /**
     *
     * @param <T>
     * @param params
     * @return
     */
    static <T> String paramList(@NotNull List<T> params) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (T param : params) {
            String toString = param.toString();
            joiner.add(toString);
        }
        return joiner.toString();
    }

    /**
     *
     * @param n
     * @return
     */
    @NotNull
    static public String tab(int n) {
        final StringBuilder BUF = new StringBuilder();
        for (int i = 0; i < n; i++) {
            BUF.append('\t');
        }
        return BUF.toString();
    }

    static final Pattern AUTOVAR_REGEX = Pattern.compile("^::(.+)_var$", Pattern.CASE_INSENSITIVE);
}
