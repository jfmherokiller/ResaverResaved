/*
 * Copyright 2020 Mark.
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
package mf

import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.IntConsumer

/**
 * A class for counting.
 *
 * @author Mark Fairchild
 */
class Counter(newLimit: Int) {
    /**
     * Increases the count by one. Any listeners will be notified.
     */
    fun click() {
        counter++
        if (percentListeners != null && percentListeners!!.isNotEmpty()) {
            val percent = counter.toDouble() / limit.toDouble()
            percentListeners!!.forEach(Consumer { l: DoubleConsumer -> l.accept(percent) })
        }
        if (countListeners != null && countListeners!!.isNotEmpty()) {
            countListeners!!.forEach(Consumer { l: IntConsumer -> l.accept(counter) })
        }
    }

    /**
     * Increases the count by a non-negative value. Any listeners will be
     * notified.
     *
     * @param num The number of clicks. Must be non-negative.
     */
    fun click(num: Int) {
        require(num >= 0) { "Clicks must be non-negative: $num" }
        counter += num
        if (percentListeners != null && percentListeners!!.isNotEmpty()) {
            val percent = percentage
            percentListeners!!.forEach(Consumer { l: DoubleConsumer -> l.accept(percent) })
        }
        if (countListeners != null && countListeners!!.isNotEmpty()) {
            countListeners!!.forEach(Consumer { l: IntConsumer -> l.accept(counter) })
        }
    }

    /**
     * Decreases the count by one. The listeners will not be notified.
     *
     * @throws IllegalStateException Thrown if the count would become negative.
     */
    fun unclick() {
        check(counter != 0) { "Counter can't be negative." }
        counter--
    }

    /**
     * Decreases the count by a non-negative value. The listeners will not be
     * notified.
     *
     * @param num The number of clicks. Must be non-negative.
     * @throws IllegalStateException Thrown if the count would become negative.
     */
    fun unclick(num: Int) {
        require(num >= 0) { "Clicks must be non-negative: $num" }
        check(counter >= num) { "Counter can't be negative." }
        counter -= num
    }

    /**
     * Sets a new limit and resets the count to zero.
     *
     * @param newLimit The new limit. Must be non-negative.
     */
    fun reset(newLimit: Int) {
        require(newLimit >= 0) { "Limit must be non-negative: $newLimit" }
        counter = 0
        limit = newLimit
        val width = limit.toString().length
        format = "%${width}d/%${width}d"
    }

    /**
     * Equivalent to calling click() and toString().
     *
     * @return The string representation.
     */
    fun eval(): String {
        this.click()
        return this.toString()
    }

    /**
     * @return The current count as a percentage of the limit.
     */
    val percentage: Double
        get() = counter.toDouble() / limit.toDouble()

    /**
     * @return A string representation of the form `val/limit`.
     */
    override fun toString(): String {
        return String.format(format!!, counter, limit)
    }

    /**
     * Adds a listener that will receive the current count whenever it
     * increases.
     *
     * @param listener The listener.
     */
    fun addCountListener(listener: IntConsumer) {
        if (countListeners == null) {
            countListeners = mutableListOf()
        }
        countListeners!!.add(listener)
    }

    /**
     * Adds a listener that will receive the current count as a
     * percentage whenever it increases.
     *
     * @param listener The listener.
     */
    fun addPercentListener(listener: DoubleConsumer) {
        if (percentListeners == null) {
            percentListeners = mutableListOf()
        }
        percentListeners!!.add(listener)
    }

    /**
     * Removes a listener.
     * @param listener The listener.
     */
    fun removeCountListener(listener: IntConsumer) {
        if (countListeners != null) {
            countListeners!!.remove(listener)
        }
    }

    /**
     * Removes a listener.
     * @param listener The listener.
     */
    fun removePercentListener(listener: DoubleConsumer) {
        if (percentListeners != null) {
            percentListeners!!.remove(listener)
        }
    }

    /**
     * @return The current count.
     */
    var counter = 0
        private set
    private var limit = 0
    private var format: String? = null
    private var percentListeners: MutableList<DoubleConsumer>?
    private var countListeners: MutableList<IntConsumer>?

    /**
     * Create a new `Counter` with the specified limit.
     *
     * @param newLimit The limit for the counter. Must be non-negative.
     */
    init {
        require(newLimit >= 0) { "Limit must be non-negative: $newLimit" }
        reset(newLimit)
        percentListeners = null
        countListeners = null
    }
}