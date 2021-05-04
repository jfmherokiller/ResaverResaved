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

import java.util.*

/**
 * Keeps stats on what is done during remapping.
 *
 * @author Mark Fairchild
 */
class RemappingStats {
    /**
     * Returns the value of the script counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val scripts: Int
        get() = COUNTS.getOrDefault(Key.SCRIPT, 0)

    /**
     * Returns the value of the object variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val objectVariables: Int
        get() = COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0)

    /**
     * Returns the value of the autovars counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val autovars: Int
        get() = COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0)

    /**
     * Returns the value of the conditional variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val conditionals: Int
        get() = COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0)

    /**
     * Returns the value of the functions counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val functions: Int
        get() = COUNTS.getOrDefault(Key.FUNCTIONS, 0)

    /**
     * Returns the value of the local variables counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val localVariables: Int
        get() = COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0)

    /**
     * Returns the value of the parameters counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val parameters: Int
        get() = COUNTS.getOrDefault(Key.PARAMETERS, 0)

    /**
     * Returns the value of the docstrings counter.
     *
     * @return Value of the counter or zero if the counter has
     * never been incremented.
     */
    val docStrings: Int
        get() = COUNTS.getOrDefault(Key.DOCSTRING, 0)

    /**
     * Increments the number of scripts.
     */
    fun incScripts() {
        var `val` = COUNTS.getOrDefault(Key.SCRIPT, 0)
        `val`++
        COUNTS[Key.SCRIPT] = `val`
    }

    /**
     * Increments the number of object variables.
     */
    fun incObjectVariables() {
        var `val` = COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0)
        `val`++
        COUNTS[Key.OBJECT_VARIABLE] = `val`
    }

    /**
     * Increments the number of autovars.
     */
    fun incAutoVariables() {
        var `val` = COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0)
        `val`++
        COUNTS[Key.AUTOVAR_VARIABLE] = `val`
    }

    /**
     * Increments the number of conditional autovars.
     */
    fun incConditionals() {
        var `val` = COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0)
        `val`++
        COUNTS[Key.CONDITIONAL_VARIABLE] = `val`
    }

    /**
     * Increments the number of functions.
     */
    fun incFunctions() {
        var `val` = COUNTS.getOrDefault(Key.FUNCTIONS, 0)
        `val`++
        COUNTS[Key.FUNCTIONS] = `val`
    }

    /**
     * Increments the number of local variables.
     */
    fun incLocalVariables() {
        var `val` = COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0)
        `val`++
        COUNTS[Key.LOCAL_VARIABLE] = `val`
    }

    /**
     * Increments the number of parameters.
     */
    fun incParameters() {
        var `val` = COUNTS.getOrDefault(Key.PARAMETERS, 0)
        `val`++
        COUNTS[Key.PARAMETERS] = `val`
    }

    /**
     * Increments the number of docstrings.
     */
    fun incDocStrings() {
        var `val` = COUNTS.getOrDefault(Key.DOCSTRING, 0)
        `val`++
        COUNTS[Key.DOCSTRING] = `val`
    }

    /**
     *
     * @return
     */
    override fun toString(): String {
        return String.format("Processed %d scripts.\n", scripts) + String.format(
            "Renamed %d object variables.\n",
            objectVariables - conditionals
        ) + String.format(
            "%d of them were property autovariables.\n",
            autovars - conditionals
        ) + String.format(
            "Skipped %d conditional autovariables.\n",
            conditionals
        ) + String.format(
            "Processed %d functions.\n",
            functions
        ) + String.format(
            "Renamed %d function local variables.\n",
            localVariables
        ) + String.format("Renamed %d function parameters.\n", parameters) + String.format(
            "Stripped %d docstrings.\n",
            docStrings
        )
    }

    private val COUNTS: MutableMap<Key, Int>

    private enum class Key {
        SCRIPT, OBJECT_VARIABLE, AUTOVAR_VARIABLE, CONDITIONAL_VARIABLE, FUNCTIONS, LOCAL_VARIABLE, PARAMETERS, DOCSTRING
    }

    /**
     * Creates a new `RemappingStats` with all counters set to zero.
     */
    init {
        COUNTS = Collections.synchronizedMap(EnumMap(Key::class.java))
    }
}