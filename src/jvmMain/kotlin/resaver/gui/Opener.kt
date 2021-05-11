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

import resaver.ProgressModel
import ess.ModelBuilder
import mu.KotlinLogging
import java.awt.Dialog
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Path
import javax.swing.*
import javax.swing.Timer

/**
 *
 * @author Mark Fairchild
 */
private val logger = KotlinLogging.logger {}
class Opener(window: SaveWindow?, savefile: Path, worrier: ess.papyrus.Worrier, doAfter: Runnable?) : SwingWorker<ess.ESS?, Double?>() {
    /**
     *
     * @return @throws Exception
     */
    @Throws(Exception::class)
    override fun doInBackground(): ess.ESS? {
        if (!Configurator.validateSavegame(SAVEFILE)) {
            return null
        }
        WINDOW.progressIndicator.start("Loading")
        WINDOW.addWindowListener(LISTENER)
        WINDOW.clearESS()
        return try {
            logger.info{"================"}
            logger.info{"Reading from savegame file \"$SAVEFILE\"."}
            val PROGRESS = ProgressModel()
            val MB = ModelBuilder(PROGRESS)
            WINDOW.progressIndicator.setModel(PROGRESS)
            val RESULT = ess.ESS.readESS(SAVEFILE, MB)
            WORRIER.check(RESULT)
            RESULT.MODEL?.let { WINDOW.setESS(RESULT.SAVE_FILE, RESULT.ESS, it, WORRIER.shouldDisableSaving()) }
            if (WORRIER.shouldWorry() || WORRIER.shouldDisableSaving()) {
                Thread(Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation") as Runnable).start()
                val TITLE = "Save Read"
                val JOP = JOptionPane(
                    TextDialog(WORRIER.message),
                    if (WORRIER.shouldWorry()) JOptionPane.ERROR_MESSAGE else JOptionPane.INFORMATION_MESSAGE
                )
                val DIALOG = JOP.createDialog(WINDOW, TITLE)
                DIALOG.modalityType = Dialog.ModalityType.DOCUMENT_MODAL
                DIALOG.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
                if (!WINDOW.isFocused) {
                    Timer(5000) { e: ActionEvent? -> DIALOG.isVisible = false }.start()
                }
                DIALOG.isVisible = true
            }
            if (DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER)
            }
            RESULT.ESS
        } catch (ex: Exception) {
            val MSG = "Error while reading file \"${SAVEFILE.fileName}\".\n${ex.message}"
            logger.error(ex) { MSG}
            JOptionPane.showMessageDialog(WINDOW, MSG, "Read Error", JOptionPane.ERROR_MESSAGE)
            null
        } catch (ex: Error) {
            val MSG = "Error while reading file \"${SAVEFILE.fileName}\".\n${ex.message}"
            logger.error(ex) { MSG}
            JOptionPane.showMessageDialog(WINDOW, MSG, "Read Error", JOptionPane.ERROR_MESSAGE)
            null
        } finally {
            WINDOW.removeWindowListener(LISTENER)
            WINDOW.progressIndicator.stop()
        }
    }

    private val SAVEFILE: Path = savefile
    private val WINDOW: SaveWindow = window!!
    private val WORRIER: ess.papyrus.Worrier = worrier
    private val DOAFTER: Runnable? = doAfter
    private val LISTENER: WindowAdapter = object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            if (!isDone) {
                cancel(true)
            }
        }
    }

    companion object {
    }

    /**
     *
     * @param window
     * @param savefile
     * @param worrier
     * @param doAfter
     */
    init {
        Configurator.setPreviousSave(savefile)
    }
}