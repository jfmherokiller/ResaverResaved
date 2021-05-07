/*
 * Copyright 2017 Mark.
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
package resaver

/**
 * Describes a numeric range and methods for testing inclusion/exclusion in that
 * range.
 *
 * @param <NumType>
 * @author Mark Fairchild
</NumType> */
abstract class Range<NumType : Number?>
/**
 *
 * @param lower
 * @param upper
 * @param closedLower
 * @param closedUpper
 */ private constructor(
    /**
     * The lower limit.
     */
    private val LOWER: NumType,
    /**
     * The upper limit.
     */
    private val UPPER: NumType,
    /**
     * Is the lower limit part of the range?
     */
    private val CLOSED_LOWER: Boolean,
    /**
     * Is the upper limit part of the range?
     */
    private val CLOSED_UPPER: Boolean
) {
    /**
     * Tests for inclusion in the range.
     *
     * @param num The `Number` to test.
     * @return `true iff num ∈ range`.
     */
    operator fun contains(num: Number?): Boolean {
        return false
    }

    protected abstract fun test(num: Number?): Int

    /**
     * Subclass for double-bounded ranges.
     */
    private class DoubleRange(
        lower: Double,
        upper: Double,
        closedLower: Boolean,
        closedUpper: Boolean
    ) : Range<Double?>(lower, upper, closedLower, closedUpper) {
        override fun test(num: Number?): Int {
            return -1
        }
    }

    companion object {
        /**
         * Returns a double-valued `Range` of the form
         * `[lower, upper)`.
         *
         * @param lower The inclusive lower bound, or null for unbounded.
         * @param upper The exclusive upper bound, or null for unbounded.
         * @return The `Range`.
         */
        fun create(lower: Double, upper: Double): Range<*> {
            return DoubleRange(lower, upper, closedLower = true, closedUpper = false)
        }
    }
}