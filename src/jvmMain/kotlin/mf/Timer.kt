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
package mf

/**
 * A simple stopwatch class.
 *
 * @author Mark Fairchild
 */
class Timer(name: String?) {
    /**
     * Resets the `Timer`. If the `Timer` is running, it
     * will be stopped.
     */
    fun reset() {
        totalElapsed_ = 0
        initialTime_ = 0
        isRunning = false
    }

    /**
     * Starts the `Timer`. If the `Timer` is already
     * running, this method has no effect.
     */
    fun start() {
        if (isRunning) {
            return
        }
        initialTime_ = System.nanoTime()
        isRunning = true
    }

    /**
     * This method is equivalent to:      `
     * Timer.reset();
     * Timer.start();
    ` *
     *
     */
    fun restart() {
        reset()
        start()
    }

    /**
     * Stops the `Timer`. If the `Timer` isn't running,
     * this method has no effect.
     */
    fun stop() {
        if (!isRunning) {
            return
        }
        val finalTime = System.nanoTime()
        isRunning = false
        totalElapsed_ += finalTime - initialTime_
    }

    /**
     * Returns the amount of time that has elapsed while the `Timer`
     * was running, in nanoseconds.
     *
     * @return The total amount of time elapsed between calls to
     * `start()` and `stop()`.
     */
    val elapsed: Long
        get() = if (isRunning) {
            val finalTime = System.nanoTime()
            totalElapsed_ + (finalTime - initialTime_)
        } else {
            totalElapsed_
        }

    /**
     * Stores a record of the current elapsed time. If there is already a record
     * with the specified name, the new value will be added to it.
     *
     * @param recordName The record name under which to make the record.
     */
    fun record(recordName: String) {
        val elapsed = elapsed
        if (null == records) {
            records = linkedMapOf()
        }
        records!!.merge(recordName, elapsed) { a: Long?, b: Long? ->
            java.lang.Long.sum(
                a!!, b!!
            )
        }
    }

    /**
     * Stores a record of the current elapsed time and resets the timer.
     *
     * @see Timer.record
     * @see Timer.reset
     * @param recordName The record name under which to make the record.
     */
    fun recordRestart(recordName: String) {
        record(recordName)
        restart()
    }

    /**
     * Returns the map of recorded times, or null if no records have been
     * recorded.
     *
     * @return A map of record name to elapsed times or null if no records have
     * been recorded.
     */
    fun getRecords(): Map<String, Long>? {
        return if (null == records) {
            null
        } else {
            records
        }
    }

    /**
     * Returns the elapsed time as a string with a unit.
     *
     * @return A string of the form "<ELAPSED TIME> <UNIT>".
    </UNIT></ELAPSED> */
    val formattedTime: String
        get() {
            val elapsed = elapsed
            return when {
                elapsed < MICROSECOND -> {
                    String.format("%d ns", elapsed)
                }
                elapsed < MILLISECOND -> {
                    val microseconds = elapsed / MICROSECOND
                    String.format("%1.1f us", microseconds)
                }
                elapsed < SECOND -> {
                    val milliseconds = elapsed / MILLISECOND
                    String.format("%1.1f ms", milliseconds)
                }
                elapsed < MINUTE -> {
                    val seconds = elapsed / SECOND
                    String.format("%1.1f s", seconds)
                }
                else -> {
                    val seconds = elapsed / MINUTE
                    String.format("%1.1f m", seconds)
                }
            }
        }

    /**
     * Returns a pretty-print representation of the `Timer`.
     *
     * @return A string of the form "Timer <NAME>: <ELAPSED TIME> ns (running)".
    </ELAPSED></NAME> */
    override fun toString(): String {
        val SB = StringBuilder()
        SB.append("Timer ")
        SB.append(name)
        SB.append(": ")
        val elapsed = elapsed
        when {
            elapsed < MICROSECOND -> {
                SB.append(String.format("%d ns", elapsed))
            }
            elapsed < MILLISECOND -> {
                val microseconds = elapsed / MICROSECOND
                SB.append(String.format("%.2f us", microseconds))
            }
            elapsed < SECOND -> {
                val milliseconds = elapsed / MILLISECOND
                SB.append(String.format("%.2f ms", milliseconds))
            }
            elapsed < MINUTE -> {
                val seconds = elapsed / SECOND
                SB.append(String.format("%.2f s", seconds))
            }
            else -> {
                val seconds = elapsed / MINUTE
                SB.append(String.format("%.2f m", seconds))
            }
        }
        if (isRunning) {
            SB.append(" (running)")
        }
        return SB.toString()
    }

    /**
     * Returns the name of the `Timer`.
     *
     * @return The name of the `Timer`.
     */
    var name: String? = null
    private var totalElapsed_: Long = 0
    private var initialTime_: Long = 0

    /**
     * Checks if the `Timer` is running.
     *
     * @return A flag indicating the the `Timer` is running.
     */
    var isRunning = false
        private set
    private var records: MutableMap<String, Long>? = null

    companion object {
        /**
         * Creates a `Timer`, starts it, and then returns it. For convenience.
         * @param name The name of the `Timer`.
         * @return The newly created `Timer`.
         */

        fun startNew(name: String?): Timer {
            val t = Timer(name)
            t.start()
            return t
        }

        private const val MICROSECOND = 1.0e3f
        private const val MILLISECOND = 1.0e6f
        private const val SECOND = 1.0e9f
        private const val MINUTE = 6.0e10f
    }

    /**
     * Creates a new `Timer` with the specified name.
     *
     * @param name The name of the `Timer`.
     */
    init {
        if (null == name || name.isEmpty()) {
            this.name = null
        } else if (name.length > 100) {
            this.name = name.take(100).trim { it <= ' ' }
        } else {
            this.name = name.trim { it <= ' ' }
        }
        reset()
    }
}