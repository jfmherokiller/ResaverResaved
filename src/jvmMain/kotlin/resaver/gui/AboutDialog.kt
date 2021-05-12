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

import mu.KLoggable
import mu.KLogger
import java.awt.Window
import java.io.IOException
import java.net.JarURLConnection
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JOptionPane

/**
 *
 * @author Mark
 */
class AboutDialog {
    companion object:KLoggable {
        @JvmStatic
        fun show(window: Window?) {
            val BUF = StringBuilder()
                .append("ReSaver was developed for YOU personally. I hope you enjoy it.")
                .append("\n\n")
                .append(version)
                .append("\n\nCopyright Mark Fairchild 2016.")
                .append("\nDistributed under the Apache 2.0 license.\n")
                .append(
                    "\nRunning on ${System.getProperty("os.name")} (${System.getProperty("os.version")} ${
                        System.getProperty(
                            "os.arch"
                        )
                    })"
                )
                .append(String.format("\n%1.2fgb memory available.", Runtime.getRuntime().maxMemory() / 1073741824.0))
                .append("\nJava path: ${System.getProperty("java.home")}")
                .append("\nJava vendor: ${System.getProperty("java.vendor")}")
                .append("\nJava version: ${System.getProperty("java.version")}")
            val ICON = logo
            if (ICON == null) {
                JOptionPane.showMessageDialog(window, BUF.toString(), "About", JOptionPane.ERROR_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(window, BUF.toString(), "About", JOptionPane.ERROR_MESSAGE, ICON)
            }
        }

        private val logo: ImageIcon?
            get() = try {
                val URL = AboutDialog::class.java.classLoader.getResource(ICON_FILENAME)
                if (URL == null) {
                    logger.warn{"Couldn't get $ICON_FILENAME resource URL."}
                }
                val IMAGE = ImageIO.read(URL)
                if (IMAGE == null) {
                    logger.warn{"Couldn't load $ICON_FILENAME into a BufferedImage."}
                }
                ImageIcon(IMAGE)
            } catch (ex: IOException) {
                null
            } catch (ex: IllegalArgumentException) {
                null
            } catch (ex: NullPointerException) {
                null
            }

        @JvmStatic
        val version: CharSequence
            get() = try {
                val RES = SaveWindow::class.java.getResource(SaveWindow::class.java.simpleName + ".class")
                val CONN = RES?.openConnection() as JarURLConnection
                val MANIFEST = CONN.manifest
                val ATTR = MANIFEST.mainAttributes
                StringBuilder()
                    .append(ATTR.getValue("Implementation-Version"))
                    .append('.')
                    .append(ATTR.getValue("Implementation-Build"))
                    .append(" (")
                    .append(ATTR.getValue("Built-Date"))
                    .append(")")
            } catch (ex: IOException) {
                "(development version)"
            } catch (ex: ClassCastException) {
                "(development version)"
            }
        private const val ICON_FILENAME = "CatsInSunbeam.jpg"
        override val logger: KLogger
            get() = logger()
    }
}