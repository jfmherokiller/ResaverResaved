/*
 * Copyright 2019 Mark.
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

import resaver.Game
import resaver.ess.papyrus.Worrier
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.nio.file.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JOptionPane
import javax.swing.SwingWorker

/**
 *
 * @author Mark
 */
class Watcher(window: SaveWindow?, worrier: Worrier?) {
    @Synchronized
    fun start(newWatchDir: Path?) {
        require(!(newWatchDir != null && !Configurator.validDir(newWatchDir))) { "Invalid watch directory." }
        if (!isRunning || watchDir != newWatchDir) {
            stop()
            watchDir = newWatchDir
            worker = WatchWorker()
            worker!!.execute()
        }
    }

    /**
     *
     */
    @Synchronized
    fun resume() {
        if (isRunning) {
            return
        }
        worker = WatchWorker()
        worker!!.execute()
    }

    /**
     *
     */
    @Synchronized
    fun stop() {
        while (isRunning) {
            worker!!.cancel(true)
        }
        worker = null
    }

    /**
     *
     * @return
     */
    val isRunning: Boolean
        get() = worker != null && !worker!!.isDone

    /**
     *
     * @author Mark
     */
    internal inner class WatchWorker : SwingWorker<Path?, Double?>() {
        /**
         *
         * @return @throws Exception
         */
        @Synchronized
        @Throws(Exception::class)
        override fun doInBackground(): Path? {
            val watchDirectories: List<Path> = if (null != watchDir) {
                listOf(watchDir!!)
            } else {
                val list: MutableList<Path> = ArrayList()
                for (VALUE in Game.VALUES) {
                    val saveDirectory = Configurator.getSaveDirectory(VALUE)
                    if (Files.exists(saveDirectory)) {
                        list.add(saveDirectory)
                    }
                }
                list
            }
            val FS = FileSystems.getDefault()
            try {
                FS.newWatchService().use { WATCHSERVICE ->
                    val REGKEYS: MutableMap<WatchKey, Path> = HashMap()
                    for (dir in watchDirectories) {
                        LOG.info("WATCHER: initializing for $dir")
                        REGKEYS[dir.register(WATCHSERVICE, StandardWatchEventKinds.ENTRY_CREATE)] = dir
                    }
                    while (true) {
                        val EVENTKEY = WATCHSERVICE.take()
                        //final WatchKey EVENTKEY = WATCHSERVICE.poll(1, TimeUnit.SECONDS);
                        if (EVENTKEY == null || !EVENTKEY.isValid) {
                            LOG.info("INVALID EVENTKEY")
                            break
                        }
                        for (event in EVENTKEY.pollEvents()) {
                            if (event.kind() === StandardWatchEventKinds.OVERFLOW) {
                                LOG.info("WATCHER OVERFLOW")
                                continue
                            }
                            val NAME = (event as WatchEvent<Path?>).context()
                            val FULL = REGKEYS[EVENTKEY]!!.resolve(NAME!!)
                            if (Files.exists(FULL) && MATCHER.matches(FULL)) {
                                LOG.info("WATCHER: Trying to open $FULL.")
                                var i = 0
                                while (i < 50 && !Files.isReadable(FULL)) {
                                    LOG.info("Waiting for $FULL to be readable.")
                                    //this.wait(250, 0)
                                    i++
                                }
                                if (Configurator.validateSavegame(FULL)) {
                                    val OPENER = Opener(WINDOW, FULL, WORRIER, null)
                                    OPENER.execute()
                                } else {
                                    LOG.info("WATCHER: Invalid file $FULL.")
                                }
                            }
                        }
                        if (!EVENTKEY.reset()) {
                            break
                        }
                    }
                }
            } catch (ex: InterruptedException) {
                LOG.info("WatcherService interrupted.")
            } catch (ex: ClosedWatchServiceException) {
                LOG.info("WatcherService interrupted.")
            } catch (ex: IOException) {
                val MSG = String.format("Error.\n%s", ex.message)
                JOptionPane.showMessageDialog(WINDOW, MSG, "Watch Error", JOptionPane.ERROR_MESSAGE)
                LOG.log(Level.SEVERE, "Watcher Error.", ex)
            } finally {
                return watchDir
            }
        }
    }

    private val WINDOW: SaveWindow = window!!
    private val WORRIER: Worrier = worrier!!
    private var worker: WatchWorker? = null
    private var watchDir: Path? = null

    companion object {
        private val LOG = Logger.getLogger(Opener::class.java.canonicalName)
        private val MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{fos,ess}")
    }

    /**
     *
     * @param window
     * @param worrier
     */
    init {
        WINDOW.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                stop()
            }
        })
    }
}