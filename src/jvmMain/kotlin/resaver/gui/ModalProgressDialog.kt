/*
 * Copyright 2016 Mark.
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

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.*

/**
 * Displays a modal dialog box with a message, blocking the UI until some
 * specified task is complete.
 *
 * @author Mark Fairchild
 */
class ModalProgressDialog(private val OWNER: SaveWindow, title: String?, task: Runnable) : JDialog(
    OWNER, title, ModalityType.APPLICATION_MODAL
) {
    /**
     * Initialize the swing and AWT components.
     *
     */
    private fun initComponents(task: Runnable) {
        this.preferredSize = Dimension(420, 100)
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        BAR.preferredSize = Dimension(400, 30)
        TOPPANEL.add(LABEL)
        BOTTOMPANEL.add(BAR)
        this.layout = BorderLayout()
        this.add(TOPPANEL, BorderLayout.PAGE_START)
        this.add(BOTTOMPANEL, BorderLayout.PAGE_END)
        pack()
        this.isResizable = false
        setLocationRelativeTo(this.owner)
        addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent) {
                Objects.requireNonNull(task)
                val doThings = Runnable {
                    task.run()
                    isVisible = false
                }
                if (OWNER.isJavaFXAvailable) {
                    try {
                        val PLATFORM = Class.forName("javafx.application.Platform")
                        val RUNLATER = PLATFORM.getMethod("runLater", Runnable::class.java)
                        RUNLATER.invoke(null, doThings)
                    } catch (ex: ReflectiveOperationException) {
                        SwingUtilities.invokeLater(doThings)
                    }
                } else {
                    SwingUtilities.invokeLater(doThings)
                }
            }

            override fun windowClosing(e: WindowEvent) {}
            override fun windowClosed(e: WindowEvent) {
                dispose()
            }
        })
    }

    private val TOPPANEL: JPanel = JPanel()
    private val BOTTOMPANEL: JPanel = JPanel()
    private val LABEL: JLabel = JLabel("Please Wait.")
    private val BAR: JProgressBar = JProgressBar()

    /**
     * Create a new `ModalProgressDialog`.
     *
     * @param owner The owning `Frame`, which will be blocked.
     * @param title The title of the dialog.
     * @param task The task to perform while the owner is blocked.
     */
    init {
        initComponents(task)
    }
}