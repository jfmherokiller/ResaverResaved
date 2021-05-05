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

import java.io.IOException
import java.nio.ByteBuffer


/**
 * Describes a local variable or parameter of a Function. A VariableType is
 * essential an ordered pair consisting of a name and a type.
 *
 * @author Mark Fairchild
 */
internal class VariableType private constructor(input: ByteBuffer, strings: StringTable, role: Role) {
    /**
     * The role of the `VariableType`.
     */
    enum class Role {
        PARAM, LOCAL
    }

    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer?) {
        name.write(output)
        TYPE.write(output)
    }

    /**
     * Calculates the size of the VariableType, in bytes.
     *
     * @return The size of the VariableType.
     */
    fun calculateSize(): Int {
        return 4
    }

    /**
     * @return A flag indicating whether the `VariableType` is a temp
     * or not.
     */
    val isTemp: Boolean
        get() = TEMP_PATTERN.containsMatchIn(name.toString())

    /**
     * Collects all of the strings used by the VariableType and adds them to a
     * set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<StringTable.TString?>) {
        strings.add(name)
        strings.add(TYPE)
    }

    /**
     * Pretty-prints the VariableType.
     *
     * @return A string representation of the VariableType.
     */
    override fun toString(): String {
        val FORMAT = "%s %s"
        return String.format(FORMAT, TYPE, name)
    }

    /**
     * @return Checks if the `VariableType` is a local variable.
     */
    val isLocal: Boolean
        get() = ROLE == Role.LOCAL

    /**
     * @return Checks if the `VariableType` is a parameter.
     */
    val isParam: Boolean
        get() = ROLE == Role.PARAM
    @JvmField
    var name: StringTable.TString
    @JvmField
    val TYPE: StringTable.TString
    val ROLE: Role

    companion object {
        /**
         * Creates a VariableType by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun readLocal(input: ByteBuffer, strings: StringTable): VariableType {
            return VariableType(input, strings, Role.LOCAL)
        }

        /**
         * Creates a VariableType by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the `Pex`.
         * @throws IOException Exceptions aren't handled.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun readParam(input: ByteBuffer, strings: StringTable): VariableType {
            return VariableType(input, strings, Role.PARAM)
        }

        val TEMP_PATTERN = Regex("^::.+$")
    }

    /**
     * Creates a VariableType by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param strings The `StringTable` for the `Pex`.
     * @param role The role, as a function parameter or local variable.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        name = strings.read(input)
        TYPE = strings.read(input)
        ROLE = role
    }
}