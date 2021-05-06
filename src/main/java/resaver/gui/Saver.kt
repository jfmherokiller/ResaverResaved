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
import resaver.ess.ESS
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

/**
 *
 * @author Mark Fairchild
 */
class Saver(window: SaveWindow?, saveFile: Path, save: ESS?, doAfter: Runnable?) : SwingWorker<ESS?, Double?>() {
    /**
     *
     * @return @throws Exception
     */
    @Throws(Exception::class)
    override fun doInBackground(): ESS? {
        if (!Configurator.validWrite(SAVEFILE)) {
            return null
        }
        WINDOW.progressIndicator.start("Saving")
        WINDOW.addWindowListener(LISTENER)
        return try {
            LOG.info("================")
            LOG.info(String.format("Writing to savegame file \"%s\".", SAVEFILE))
            val MODEL = ProgressModel()
            WINDOW.progressIndicator.setModel(MODEL)
            val watcherRunning = WINDOW.watcher.isRunning
            WINDOW.watcher.stop()
            val RESULT = ESS.writeESS(SAVE, SAVEFILE)
            if (watcherRunning) {
                WINDOW.watcher.resume()
            }
            val time = RESULT.TIME_S
            val size = RESULT.SIZE_MB
            val MSG = StringBuilder()
            MSG.append("The savefile was successfully written.")
            MSG.append(String.format("\nWrote %1.1f mb in %1.1f seconds.", size, time))
            if (null != RESULT.BACKUP_FILE) {
                MSG.append(String.format("\nBackup written to %s.", RESULT.BACKUP_FILE))
            }
            if (RESULT.ESS.hasCosave()) {
                MSG.append(String.format(
                    "\n%s co-save was copied.",
                    RESULT.GAME.COSAVE_EXT.uppercase(Locale.getDefault())
                ))
            }
            val TITLE = "Save Written"
            JOptionPane.showMessageDialog(WINDOW, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            WINDOW.resetTitle(SAVEFILE)
            if (DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER)
            }
            SAVE
        } catch (ex: Exception) {
            val MSG = String.format("Error while writing file \"%s\".\n%s", SAVEFILE.fileName, ex.message)
            LOG.log(Level.SEVERE, MSG, ex)
            JOptionPane.showMessageDialog(WINDOW, MSG, "Write Error", JOptionPane.ERROR_MESSAGE)
            null
        } catch (ex: Error) {
            val MSG = String.format("Error while writing file \"%s\".\n%s", SAVEFILE.fileName, ex.message)
            LOG.log(Level.SEVERE, MSG, ex)
            JOptionPane.showMessageDialog(WINDOW, MSG, "Write Error", JOptionPane.ERROR_MESSAGE)
            null
        } finally {
            WINDOW.removeWindowListener(LISTENER)
            WINDOW.progressIndicator.stop()
        }
    }

    private val SAVEFILE: Path = Objects.requireNonNull(saveFile, "The saveFile field must not be null.")
    private val WINDOW: SaveWindow = Objects.requireNonNull(window, "The window field must not be null.")!!
    private val SAVE: ESS = Objects.requireNonNull(save, "The save field must not be null.")!!
    private val DOAFTER: Runnable? = doAfter
    private val LISTENER: WindowAdapter = object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            if (!isDone) {
                cancel(true)
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(Saver::class.java.canonicalName)
    }

}