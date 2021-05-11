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
package resaver.gui

import javax.swing.JPanel
import kotlin.jvm.Synchronized
import resaver.ProgressModel
import java.awt.Cursor
import javax.swing.JLabel
import javax.swing.JProgressBar
import javax.swing.JComponent
import javax.swing.RootPaneContainer
import java.awt.event.MouseAdapter
import java.awt.FlowLayout
import java.awt.Dimension

/**
 * Displays a `JProgressBar` in a panel, while blocking the owner
 * window.
 *
 * @author Mark Fairchild
 */
class ProgressIndicator : JPanel() {
    /**
     * Sets the title and model of the `ProgressIndicator`..
     *
     * @param title The new title of the dialog.
     */
    @Synchronized
    fun start(title: String?) {
        this.start(title, null)
    }

    /**
     * Sets the title and model of the `ProgressIndicator`..
     *
     * @param title The new title of the dialog.
     * @param model The `ProgressModel` or null for indeterminate.
     */
    @Synchronized
    fun start(title: String?, model: ProgressModel?) {
        LABEL.text = title
        setModel(model)
        if (active > 0) {
            active++
        } else {
            active = 1
            LABEL.isVisible = true
            BAR.isVisible = true
            startWaitCursor(this.rootPane)
        }
    }

    fun setModel(model: ProgressModel?) {
        if (model == null) {
            BAR.isIndeterminate = true
            BAR.model = ProgressModel(1)
        } else {
            BAR.isIndeterminate = false
            BAR.model = model
        }
    }

    fun clearModel() {
        setModel(null)
    }

    /**
     *
     */
    @Synchronized
    fun stop() {
        assert(active > 0) { "Invalid call to ProgressIndicator.stop()." }
        if (active > 0) {
            active--
        }
        assert(active >= 0) { "Invalid call to ProgressIndicator.stop()." }
        if (active <= 0) {
            active = 0
            LABEL.isVisible = false
            BAR.isVisible = false
            stopWaitCursor(this.rootPane)
        }
    }

    private val LABEL: JLabel
    private val BAR: JProgressBar
    private var active: Int

    companion object {
        /**
         *
         * @param component
         */
        private fun startWaitCursor(component: JComponent) {
            val root = component.topLevelAncestor as RootPaneContainer
            root.glassPane.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
            root.glassPane.addMouseListener(NULLMOUSEADAPTER)
            root.glassPane.isVisible = true
        }

        private fun stopWaitCursor(component: JComponent) {
            val root = component.topLevelAncestor as RootPaneContainer
            root.glassPane.cursor = Cursor.getDefaultCursor()
            root.glassPane.removeMouseListener(NULLMOUSEADAPTER)
            root.glassPane.isVisible = false
        }

        private val NULLMOUSEADAPTER: MouseAdapter = object : MouseAdapter() {}
    }

    /**
     * Creates a new `ProgressIndicator`.
     *
     */
    init {
        super.setLayout(FlowLayout())
        LABEL = JLabel("Please wait.")
        BAR = JProgressBar()
        BAR.isIndeterminate = true
        val s = BAR.preferredSize
        val s_ = Dimension(2 * s.width / 3, s.height)
        BAR.preferredSize = s_
        LABEL.isVisible = false
        BAR.isVisible = false
        active = 0
        super.add(LABEL)
        super.add(BAR)
    }
}