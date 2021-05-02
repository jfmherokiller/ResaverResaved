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
 * Stores stats about a script or group of scripts.
 *
 * @author Mark Fairchild
 */
class ScriptStats {
    /**
     * Returns the number of transient strings that can potentially be saved by
     * restringing.
     *
     * @return The number of local variables and function parameters.
     */
    val transientStrings: Int
        get() = localVariables + parameters

    /**
     * Returns the number of permanent strings that can potentially be saved by
     * restringing.
     *
     * @return The number of non-conditional object variables.
     */
    val permanentStrings: Int
        get() = objectVariables - conditionals

    /**
     * Returns the value of the object variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val objectVariables: Int
        get() = COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0)

    /**
     * Returns the value of the autovars counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val autovars: Int
        get() = COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0)

    /**
     * Returns the value of the conditional variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val conditionals: Int
        get() = COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0)

    /**
     * Returns the value of the local variables counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val localVariables: Int
        get() = COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0)

    /**
     * Returns the value of the parameters counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val parameters: Int
        get() = COUNTS.getOrDefault(Key.PARAMETERS, 0)

    /**
     * Returns the value of the docstrings counter.
     *
     * @return Value of the counter or zero if the counter has never been
     * incremented.
     */
    val docStrings: Int
        get() = COUNTS.getOrDefault(Key.DOCSTRING, 0)

    /**
     * Modifies the number of object variables.
     *
     * @param delta The change to the stat.
     */
    fun modObjectVariables(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.OBJECT_VARIABLE, 0)
        `val` += delta
        COUNTS[Key.OBJECT_VARIABLE] = `val`
    }

    /**
     * Modifies the number of autovars.
     *
     * @param delta The change to the stat.
     */
    fun modAutoVariables(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.AUTOVAR_VARIABLE, 0)
        `val` += delta
        COUNTS[Key.AUTOVAR_VARIABLE] = `val`
    }

    /**
     * Modifies the number of conditional autovars.
     *
     * @param delta The change to the stat.
     */
    fun modConditionals(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.CONDITIONAL_VARIABLE, 0)
        `val` += delta
        COUNTS[Key.CONDITIONAL_VARIABLE] = `val`
    }

    /**
     * Modifies the number of local variables.
     *
     * @param delta The change to the stat.
     */
    fun modLocalVariables(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.LOCAL_VARIABLE, 0)
        `val` += delta
        COUNTS[Key.LOCAL_VARIABLE] = `val`
    }

    /**
     * Modifies the number of parameters.
     *
     * @param delta The change to the stat.
     */
    fun modParameters(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.PARAMETERS, 0)
        `val` += delta
        COUNTS[Key.PARAMETERS] = `val`
    }

    /**
     * Modifies the number of docstrings.
     *
     * @param delta The change to the stat.
     */
    fun modDocStrings(delta: Int) {
        var `val` = COUNTS.getOrDefault(Key.DOCSTRING, 0)
        `val` += delta
        COUNTS[Key.DOCSTRING] = `val`
    }

    /**
     * Clears the stats.
     */
    fun clear() {
        COUNTS.clear()
    }

    /**
     * Adds the counts from one `ScriptStats` to another.
     *
     * @param other The counts to add.
     */
    fun add(other: ScriptStats) {
        modAutoVariables(other.autovars)
        modConditionals(other.conditionals)
        modDocStrings(other.docStrings)
        modLocalVariables(other.localVariables)
        modObjectVariables(other.objectVariables)
        modParameters(other.parameters)
    }

    /**
     * @return
     */
    /*@Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        
        BUF.append(String.format("Processed %d scripts.\n", this.getScripts()));
        BUF.append(String.format("Renamed %d object variables.\n", this.getObjectVariables() - this.getConditionals()));
        BUF.append(String.format("%d of them were property autovariables.\n", this.getAutovars() - this.getConditionals()));
        BUF.append(String.format("Skipped %d conditional autovariables.\n", this.getConditionals()));
        BUF.append(String.format("Processed %d functions.\n", this.getFunctions()));
        BUF.append(String.format("Renamed %d function local variables.\n", this.getLocalVariables()));
        BUF.append(String.format("Renamed %d function parameters.\n", this.getParameters()));
        BUF.append(String.format("Stripped %d docstrings.\n", this.getDocStrings()));
        
        return BUF.toString();
    }*/
    private val COUNTS: MutableMap<Key, Int>

    private enum class Key {
        OBJECT_VARIABLE, AUTOVAR_VARIABLE, CONDITIONAL_VARIABLE, LOCAL_VARIABLE, PARAMETERS, DOCSTRING
    }

    companion object {
        /**
         * Combines a `Collection` of `ScripStat> objects.
         *
         * @param statsGroup A `Collection` of stats.
         * @return A combined `ScriptStats` object containing the sum of
         * all the individual stats.
        ` */
        fun combine(statsGroup: Collection<ScriptStats>): ScriptStats {
            Objects.requireNonNull(statsGroup)
            val COMBINED = ScriptStats()
            for (stats in statsGroup) {
                COMBINED.modAutoVariables(stats.autovars)
                COMBINED.modConditionals(stats.conditionals)
                COMBINED.modDocStrings(stats.docStrings)
                COMBINED.modLocalVariables(stats.localVariables)
                COMBINED.modObjectVariables(stats.objectVariables)
                COMBINED.modParameters(stats.parameters)
            }
            return COMBINED
        }
    }

    /**
     * Creates a new `ModStats` with all counters set to zero.
     */
    init {
        COUNTS = Collections.synchronizedMap(EnumMap(Key::class.java))
    }
}