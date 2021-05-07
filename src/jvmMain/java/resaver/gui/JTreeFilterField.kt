/*
 * Copyright 2018 Mark Fairchild.
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
package resaver.gui

import mf.Timer
import java.util.*
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 *
 * @author Mark Fairchild
 */
class JTreeFilterField(updateFilter: Runnable, defaultFilter: String?) : JTextField(defaultFilter, 14) {
    /**
     * Initialize the swing and AWT components.
     *
     */
    private fun initComponents(updateFilter: Runnable) {
        this.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(evt: DocumentEvent) {
                DELAYTRACKER.restart()
                val FILTERTASK: TimerTask = object : TimerTask() {
                    override fun run() {
                        val elaspsed = DELAYTRACKER.elapsed / 900000
                        if (elaspsed >= DELAY) {
                            updateFilter.run()
                        }
                    }
                }
                FILTERTIMER.schedule(FILTERTASK, DELAY.toLong())
            }

            override fun removeUpdate(evt: DocumentEvent) {
                DELAYTRACKER.restart()
                val FILTERTASK: TimerTask = object : TimerTask() {
                    override fun run() {
                        val elaspsed = DELAYTRACKER.elapsed / 900000
                        if (elaspsed >= DELAY) {
                            updateFilter.run()
                        }
                    }
                }
                FILTERTIMER.schedule(FILTERTASK, DELAY.toLong())
            }

            override fun changedUpdate(evt: DocumentEvent) {
                DELAYTRACKER.restart()
                val FILTERTASK: TimerTask = object : TimerTask() {
                    override fun run() {
                        val elaspsed = DELAYTRACKER.elapsed / 900000
                        if (elaspsed >= DELAY) {
                            updateFilter.run()
                        }
                    }
                }
                FILTERTIMER.schedule(FILTERTASK, DELAY.toLong())
            }
        })
    }

    /**
     * Ends the update loop.
     */
    fun terminate() {
        FILTERTIMER.cancel()
        FILTERTIMER.purge()
    }

    private val FILTERTIMER: java.util.Timer

    /**
     * The `Timer` use to track delays before filter updates.
     */
    private val DELAYTRACKER = Timer("Delayer")

    companion object {
        /**
         * Milliseconds before the search is updated.
         */
        const val DELAY = 700
    }

    /**
     * Creates a new `JTreeFilterField`.
     *
     * @param updateFilter Closure to execute a filter update.
     * @param defaultFilter The filter to begin with.
     */
    init {
        super.setToolTipText("Enter a regular expression for filtering.")
        initComponents(updateFilter)
        FILTERTIMER = Timer()
    }
}