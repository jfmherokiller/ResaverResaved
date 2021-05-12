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
package resaver

import mu.KLoggable
import mu.KLogger
import picocli.CommandLine
import java.awt.Color
import java.awt.EventQueue
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.prefs.Preferences
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
/**
 * Entry class for ReSaver.
 *
 * @author Mark Fairchild
 */
@CommandLine.Command(
    name = "ReSaver",
    mixinStandardHelpOptions = true,
    version = ["ReSaver 0.5.9.9"],
    description = [""]
)
class ReSaver : Callable<Int> {
    /**
     */
    override fun call(): Int {
        // Use the dark nimbus theme if specified.
        try {
            if (DARKTHEME_OPTION || PREFS.getBoolean("settings.darktheme", false)) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                setDarkNimus()
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            }
        } catch (ex: ClassNotFoundException) {
            logger.warn(ex) {"Couldn't set theme."}
        } catch (ex: InstantiationException) {
            logger.warn(ex) {"Couldn't set theme."}
        } catch (ex: IllegalAccessException) {
            logger.warn(ex) {"Couldn't set theme."}
        } catch (ex: UnsupportedLookAndFeelException) {
            logger.warn(ex) {"Couldn't set theme."}
        }

        // Set the font scaling.
        val fontScale = 0.5f.coerceAtLeast(PREFS.getFloat("settings.fontScale", 1.0f))
        for (key in UIManager.getLookAndFeelDefaults().keys) {
            if (key.toString().endsWith(".font")) {
                val font = UIManager.getFont(key)
                val biggerFont = font.deriveFont(fontScale * font.size2D)
                UIManager.put(key, biggerFont)
            }
        }
        //val specallog = (logger.underlyingLogger as Logger)
        // Set up logging stuff.
       // specallog.parent.handlers[0].formatter = object : Formatter() {
       //     override fun format(record: LogRecord): String {
       //         val LEVEL = record.level
       //         val MSG = record.message
       //         val SRC = "${record.sourceClassName}.${record.sourceMethodName}"
       //         return "$SRC: $LEVEL: $MSG\n"
       //     }
       // }
        //specallog.parent.handlers[0].level = Level.INFO

        // Check the autoparse setting.
        val PREVIOUS = resaver.gui.Configurator.previousSave
        val WINDOW: resaver.gui.SaveWindow = if (PATH_PARAMETER != null && PATH_PARAMETER!!.isNotEmpty() && resaver.gui.Configurator.validateSavegame(
                PATH_PARAMETER!![0])) {
            resaver.gui.SaveWindow(PATH_PARAMETER!![0], AUTOPARSE_OPTION)
            } else if (REOPEN_OPTION && PREVIOUS?.let { resaver.gui.Configurator.validateSavegame(it) } == true) {
            resaver.gui.SaveWindow(PREVIOUS, AUTOPARSE_OPTION)
            } else {
            resaver.gui.SaveWindow(null, false)
            }
        if (WATCH_OPTION) {
            WINDOW.setWatching(true)
        }
        EventQueue.invokeLater { WINDOW.isVisible = true }
        return 0
    }

    //@Option(names = {"-e", "--decompile"}, description = "Tries to decompile a script file. It's NOT as good as Champollion, but it can sometimes handle sabotaged pex files.")
    //private String DECOMPILE;
    @CommandLine.Option(
        names = ["-r", "--reopen"],
        description = ["Reopen the most recently opened savefile (ignored if a valid savefile is specified)."]
    )
    public var REOPEN_OPTION = false

    @CommandLine.Option(
        names = ["-p", "--autoparse"],
        description = ["Automatically scan plugins for the specified savefile (ignored unless a savefile is specified or the -r option is used."]
    )
    public var AUTOPARSE_OPTION = false

    @CommandLine.Option(names = ["-d", "--darktheme"], description = ["Use the custom Dark Nimbus theme."])
    public var DARKTHEME_OPTION = false

    @CommandLine.Option(
        names = ["-w", "--watch"],
        description = ["Automatically start watching the savefile directories."]
    )
    public var WATCH_OPTION = false

    @CommandLine.Parameters(description = ["The savefile to open (optional)."])
    private var PATH_PARAMETER: List<Path>? = null

    companion object:KLoggable {
        /**
         * @param args the command line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(ReSaver()).execute(*args)
            //System.exit(exitCode);
        }

        /**
         * Sets swing to use a dark version of Nimbus.
         */
        fun setDarkNimus() {
            UIManager.put("control", Color(128, 128, 128))
            UIManager.put("info", Color(128, 128, 128))
            UIManager.put("nimbusBase", Color(18, 30, 49))
            UIManager.put("nimbusAlertYellow", Color(248, 187, 0))
            UIManager.put("nimbusDisabledText", Color(128, 128, 128))
            UIManager.put("nimbusFocus", Color(115, 164, 209))
            UIManager.put("nimbusGreen", Color(176, 179, 50))
            UIManager.put("nimbusInfoBlue", Color(66, 139, 221))
            UIManager.put("nimbusLightBackground", Color(18, 30, 49))
            UIManager.put("nimbusOrange", Color(191, 98, 4))
            UIManager.put("nimbusRed", Color(169, 46, 34))
            UIManager.put("nimbusSelectedText", Color(255, 255, 255))
            UIManager.put("nimbusSelectionBackground", Color(104, 93, 156))
            UIManager.put("text", Color(230, 230, 230))
            try {
                for (info in UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus" == info.name) {
                        UIManager.setLookAndFeel(info.className)
                        break
                    }
                }
            } catch (ex: ClassNotFoundException) {
                logger.warn(ex) {"Error setting Dark Nimbus theme."}
            } catch (ex: InstantiationException) {
                logger.warn(ex) {"Error setting Dark Nimbus theme."}
            } catch (ex: IllegalAccessException) {
                logger.warn(ex) {"Error setting Dark Nimbus theme."}
            } catch (ex: UnsupportedLookAndFeelException) {
                logger.warn(ex) { "Error setting Dark Nimbus theme."}
            }
        }
        private val PREFS = Preferences.userNodeForPackage(ReSaver::class.java)
        override val logger: KLogger
            get() = logger()
    }
}