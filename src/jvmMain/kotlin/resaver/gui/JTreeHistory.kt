/*
 * Copyright 2018 Mark.
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

import mu.KLoggable
import mu.KLogger
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.TreePath

/**
 * A `JPanel` with two buttons that provides a history function
 * for a JTree.
 *
 * @author Mark Fairchild
 */
class JTreeHistory(tree: JTree) : JPanel(FlowLayout()) {
    /**
     * Initialize the swing and AWT components.
     *
     */
    private fun initComponents() {
        this.add(BTN_PREVSELECTION)
        this.add(BTN_NEXTSELECTION)
        BTN_PREVSELECTION.isEnabled = false
        BTN_NEXTSELECTION.isEnabled = false
        TREE.addTreeSelectionListener { e: TreeSelectionEvent? ->
            if (!LOCK.isLocked) {
                val selections = TREE.selectionPaths
                PREV_SELECTIONS.push(selections)
                NEXT_SELECTIONS.clear()
                while (PREV_SELECTIONS.size > 100) {
                    PREV_SELECTIONS.removeLast()
                }
                BTN_PREVSELECTION.isEnabled = PREV_SELECTIONS.size > 1
                BTN_NEXTSELECTION.isEnabled = NEXT_SELECTIONS.size > 0
                if (selections != null && selections.isNotEmpty()) {
                    logger.info{ "TreeSelection: selection = ${selections[0].lastPathComponent}" }
                } else {
                    logger.info{"TreeSelection: selection = nothing"}
                }
            } else {
                logger.info{"TreeSelection: selection - skipping"}
            }
        }
        BTN_PREVSELECTION.addActionListener { e: ActionEvent? ->
            LOCK.lock()
            try {
                assert(PREV_SELECTIONS.size > 1)
                assert(
                    Arrays.equals(
                        PREV_SELECTIONS.peek(),
                        TREE.selectionPaths
                    )
                ) { "Error: current path should equal top of previous stack." }
                NEXT_SELECTIONS.push(PREV_SELECTIONS.pop())
                BTN_PREVSELECTION.isEnabled = PREV_SELECTIONS.size > 1
                BTN_NEXTSELECTION.isEnabled = NEXT_SELECTIONS.size > 0
                val selections = PREV_SELECTIONS.peek()
                TREE.selectionPaths = selections
                if (selections != null && selections.isNotEmpty()) {
                    TREE.scrollPathToVisible(selections[0])
                    logger.info{ "TreeSelection: prev = ${selections[0].lastPathComponent}" }
                } else {
                    logger.info{"TreeSelection: prev = nothing"}
                }
            } finally {
                LOCK.unlock()
            }
        }
        BTN_NEXTSELECTION.addActionListener { e: ActionEvent? ->
            LOCK.lock()
            try {
                assert(NEXT_SELECTIONS.size > 0)
                assert(
                    Arrays.equals(
                        PREV_SELECTIONS.peek(),
                        TREE.selectionPaths
                    )
                ) { "Error: current path should equal top of previous stack." }
                PREV_SELECTIONS.push(NEXT_SELECTIONS.pop())
                BTN_PREVSELECTION.isEnabled = PREV_SELECTIONS.size > 1
                BTN_NEXTSELECTION.isEnabled = NEXT_SELECTIONS.size > 0
                val selections = PREV_SELECTIONS.peek()
                TREE.selectionPaths = selections
                if (selections != null && selections.isNotEmpty()) {
                    TREE.scrollPathToVisible(selections[0])
                    logger.info{ "TreeSelection: next = ${selections[0].lastPathComponent}" }
                } else {
                    logger.info{"TreeSelection: next = nothing"}
                }
            } finally {
                LOCK.unlock()
            }
        }
    }

    private val BTN_PREVSELECTION: JButton
    private val BTN_NEXTSELECTION: JButton
    private val PREV_SELECTIONS: LinkedList<Array<TreePath>?>
    private val NEXT_SELECTIONS: LinkedList<Array<TreePath>?>
    private val TREE: JTree
    private val LOCK: ReentrantLock

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }

    /**
     * Creates a new `JTreeHistory`.
     *
     * @param tree The `JTree` to be monitored.
     */
    init {
        Objects.requireNonNull(tree)
        BTN_PREVSELECTION = JButton("Back")
        BTN_NEXTSELECTION = JButton("Next")
        BTN_PREVSELECTION.toolTipText = "Go backwards in the selection history."
        BTN_NEXTSELECTION.toolTipText = "Go forwards in the selection hstory."
        PREV_SELECTIONS = LinkedList()
        NEXT_SELECTIONS = LinkedList()
        this.LOCK = ReentrantLock()
        TREE = tree
        initComponents()
    }
}