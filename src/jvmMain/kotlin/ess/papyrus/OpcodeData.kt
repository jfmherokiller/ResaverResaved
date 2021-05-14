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
import resaver.ListException
import resaver.pex.Opcode
import java.nio.BufferUnderflowException


/**
 * Describes a script member in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
class OpcodeData : PapyrusElement {
    /**
     * Creates a new `OpcodeData` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `PapyrusContext` info.
     * @throws PapyrusFormatException
     * @throws ListException
     */
    constructor(input: PlatformByteBuffer, context: PapyrusContext?) {
        val code = input.getByte().toInt()
        if (code < 0 || code >= OPCODES.size) {
            throw PapyrusFormatException("Invalid opcode: $code")
        }
        opcode = OPCODES[code]
        PARAMETERS = mutableListOf()
        val fixedCount = opcode.fixedCount
        for (i in 0 until fixedCount) {
            try {
                val `var` = context?.let { Parameter.read(input, it) }
                if (`var` != null) {
                    PARAMETERS.add(`var`)
                }
            } catch (ex: PapyrusFormatException) {
                throw ListException(i, fixedCount, ex)
            } catch (ex: BufferUnderflowException) {
                throw ListException(i, fixedCount, ex)
            }
        }
        if (opcode.hasExtraTerms()) {
            val extraCount = PARAMETERS.last().intValue
            for (i in 0 until extraCount) {
                try {
                    val `var` = context?.let { Parameter.read(input, it) }
                    if (`var` != null) {
                        PARAMETERS.add(`var`)
                    }
                } catch (ex: PapyrusFormatException) {
                    throw ListException(i + fixedCount, extraCount + fixedCount, ex)
                } catch (ex: BufferUnderflowException) {
                    throw ListException(i + fixedCount, extraCount + fixedCount, ex)
                }
            }
        }
    }

    /**
     * Creates a new `OpcodeData` for the NOP instruction.
     */
    private constructor() {
        opcode = Opcode.NOP
        PARAMETERS = mutableListOf()
    }

    /**
     * @see ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(opcode.ordinal.toByte())
        PARAMETERS.forEach { `var`: Parameter -> `var`.write(output) }
    }

    /**
     * @see ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 1
        val result = PARAMETERS.sumOf { it.calculateSize() }
        sum += result
        return sum
    }

    /**
     * @return The list of instruction parameters.
     */
    val parameters: List<Parameter>
        get() = PARAMETERS

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUILDER = StringBuilder()
        BUILDER.append(opcode)
        PARAMETERS.forEach { p: Parameter -> BUILDER.append(' ').append(p.toValueString()) }
        return BUILDER.toString()
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 29 * hash + opcode.hashCode()
        hash = 29 * hash + PARAMETERS.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as OpcodeData
        return if (opcode !== other2.opcode) {
            false
        } else PARAMETERS == other2.PARAMETERS
    }

    /**
     * @return The opcode.
     */
    val opcode: Opcode
    private val PARAMETERS: MutableList<Parameter>

    companion object {
        /**
         * A reusable instance of the NOP instruction.
         */
        val NOP = OpcodeData()
        private val OPCODES = Opcode.values()
    }
}