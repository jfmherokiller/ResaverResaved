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

import ess.papyrus.*
import mu.KLoggable
import mu.KLogger
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*

import java.util.logging.Logger
import java.util.regex.Pattern
import javax.swing.*

/**
 *
 * @author Mark
 */
class BatchCleaner(window: SaveWindow?, save: ess.ESS?) : SwingWorker<Boolean, Double?>() {
    /**
     *
     * @return @throws Exception
     */
    @Throws(Exception::class)
    override fun doInBackground(): Boolean {
        WINDOW.progressIndicator.start("Batch cleaning")
        WINDOW.addWindowListener(LISTENER)
        return try {
            var batch: String? = null

            // If no batch script was provided, throw up a dialog with a 
            // text area and let the user paste one in.
            if (null == batch) {
                val TEXT = JTextArea()
                TEXT.columns = 50
                TEXT.rows = 10
                TEXT.lineWrap = false
                TEXT.wrapStyleWord = false
                val SCROLLER = JScrollPane(TEXT)
                SCROLLER.border = BorderFactory.createTitledBorder("Enter Scripts")
                val TITLE = "Batch Clean"
                val result = JOptionPane.showConfirmDialog(
                    WINDOW,
                    SCROLLER,
                    TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                )
                if (result == JOptionPane.CANCEL_OPTION) {
                    return false
                }
                batch = TEXT.text
            }

            // If we still have no batch script, just exit.
            if (null == batch || batch.isEmpty()) {
                return false
            }

            // Split the input into lines.
            val LINES = batch.split("\n".toRegex()).toTypedArray()
            val CLEAN_NAMES: MutableSet<Definition> = TreeSet()

            // I had 99 problems, so I used regular expressions. Now I have 100 problems.
            // (script name)(optional .pex extension)(@@ followed by deletion prompt)
            val PATTERN = "^([^.@\\s]+)(?:\\.pex)?(?:\\s*@@\\s*(.*))?"
            val REGEX = Pattern.compile(PATTERN, Pattern.CASE_INSENSITIVE)

            // Now iterate through the lines.
            for (line in LINES) {
                // Match the regex.
                val MATCHER = REGEX.matcher(line)
                if (!MATCHER.find()) {
                    assert(false)
                }

                // For debugging.
                val groups: MutableList<String> = LinkedList()
                for (i in 0..MATCHER.groupCount()) {
                    groups.add(MATCHER.group(i))
                }
                System.out.printf("Groups = %d: %s\n", MATCHER.groupCount(), groups)

                // Retrieve group 1, the definition name.
                val NAME = MATCHER.group(1).trim { it <= ' ' }
                val DEF = CONTEXT.findAny(TString.makeUnindexed(NAME))
                if (DEF != null) {
                    // Group 2 is an optional deletion prompt.
                    if (null == MATCHER.group(2)) {
                        CLEAN_NAMES.add(DEF)
                        logger.info{String.format("Definition present, adding to cleaning list: %s", DEF)}
                    } else {
                        logger.info{String.format("Definition present, prompting for deletion: %s", DEF)}
                        val PROMPT = MATCHER.group(2).trim { it <= ' ' }
                        val MSG = String.format("Delete %s?\n%s", DEF, PROMPT)
                        val TITLE = "Confirm"
                        val result = JOptionPane.showConfirmDialog(
                            WINDOW,
                            MSG,
                            TITLE,
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        )
                        if (result == JOptionPane.OK_OPTION) {
                            CLEAN_NAMES.add(DEF)
                        } else if (result == JOptionPane.CANCEL_OPTION) {
                            return false
                        }
                    }
                }
            }

            // If no scripts matched, abort.
            if (CLEAN_NAMES.isEmpty()) {
                val MSG = "There were no matches."
                val TITLE = "No matches"
                JOptionPane.showMessageDialog(WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
                return false
            }
            val BUF = StringBuilder()
            BUF.append("The following scripts will be cleaned: \n\n")
            CLEAN_NAMES.forEach { v: Definition? -> BUF.append(v).append('\n') }
            val TEXT = JTextArea(BUF.toString())
            TEXT.columns = 40
            TEXT.isEditable = false
            val SCROLLER = JScrollPane(TEXT)
            val TITLE = "Batch Clean"
            val result = JOptionPane.showConfirmDialog(
                WINDOW,
                SCROLLER,
                TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )
            if (result == JOptionPane.NO_OPTION) {
                return false
            }
            val PAPYRUS = SAVE.papyrus
            val THREADS: MutableSet<ActiveScript> = hashSetOf()
            for (def in CLEAN_NAMES) {
                if (def is Script) {
                    if (PAPYRUS != null) {
                        for (activeScript in PAPYRUS.activeScripts.values) {
                            if (activeScript.hasScript(def)) {
                                THREADS.add(activeScript)
                            }
                        }
                    }
                }
            }
            THREADS.forEach { obj: ActiveScript -> obj.zero() }
            val REMOVED = SAVE.papyrus?.removeElements(CLEAN_NAMES)
            if (REMOVED != null) {
                WINDOW.deleteNodesFor(REMOVED)
            }
            var scripts = 0L
            if (REMOVED != null) {
                for (papyrusElement in REMOVED) {
                    if (papyrusElement is Script) {
                        scripts++
                    }
                }
            }
            var scriptInstances = 0L
            if (REMOVED != null) {
                for (papyrusElement in REMOVED) {
                    if (papyrusElement is ScriptInstance) {
                        scriptInstances++
                    }
                }
            }
            var structs = 0L
            if (REMOVED != null) {
                for (papyrusElement in REMOVED) {
                    if (papyrusElement is Struct) {
                        structs++
                    }
                }
            }
            var structsInstances = 0L
            if (REMOVED != null) {
                for (papyrusElement in REMOVED) {
                    if (papyrusElement is StructInstance) {
                        structsInstances++
                    }
                }
            }
            var references = 0L
            if (REMOVED != null) {
                for (v in REMOVED) {
                    if (v is Reference) {
                        references++
                    }
                }
            }
            val threads = THREADS.size.toLong()
            val MSG = String.format(
                "Cleaned %d scripts and %d corresponding instances.\nCleaned %s structs and %d corresponding instances.\nCleaned %d references.\n%d threads were terminated.",
                scripts,
                scriptInstances,
                structs,
                structsInstances,
                references,
                threads
            )
            JOptionPane.showMessageDialog(WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            true
        } finally {
            WINDOW.removeWindowListener(LISTENER)
            WINDOW.progressIndicator.stop()
        }
    }

    private val WINDOW: SaveWindow = Objects.requireNonNull(window, "The window field must not be null.")!!
    private val SAVE: ess.ESS = Objects.requireNonNull(save, "The save field must not be null.")!!
    private val CONTEXT: PapyrusContext = SAVE.papyrus!!.context
    private val LISTENER: WindowAdapter = object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            if (!isDone) {
                cancel(true)
            }
        }
    }

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }

}