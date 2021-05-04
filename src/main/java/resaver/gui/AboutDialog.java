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
package resaver.gui;

import java.awt.Window;
import java.io.IOException;
import java.net.JarURLConnection;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 *
 * @author Mark
 */
public class AboutDialog {

    static public void show(Window window) {
        final StringBuilder BUF = new StringBuilder()
                .append("ReSaver was developed for YOU personally. I hope you enjoy it.")
                .append("\n\n")
                .append(AboutDialog.getVersion())
                .append("\n\nCopyright Mark Fairchild 2016.")
                .append("\nDistributed under the Apache 2.0 license.\n")
                .append(String.format("\nRunning on %s (%s %s)", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")))
                .append(String.format("\n%1.2fgb memory available.", Runtime.getRuntime().maxMemory() / 1073741824.0))
                .append(String.format("\nJava path: %s", System.getProperty("java.home")))
                .append(String.format("\nJava vendor: %s", System.getProperty("java.vendor")))
                .append(String.format("\nJava version: %s", System.getProperty("java.version")));

        final ImageIcon ICON = AboutDialog.getLogo();

        if (ICON == null) {
            JOptionPane.showMessageDialog(window, BUF.toString(), "About", 0);
        } else {
            JOptionPane.showMessageDialog(window, BUF.toString(), "About", 0, ICON);
        }
    }

    static public ImageIcon getLogo() {
        try {
            final java.net.URL URL = AboutDialog.class.getClassLoader().getResource(ICON_FILENAME);
            if (URL == null) {
                LOG.warning("Couldn't get " + ICON_FILENAME + " resource URL.");
            }

            final java.awt.image.BufferedImage IMAGE = javax.imageio.ImageIO.read(URL);
            if (IMAGE == null) {
                LOG.warning("Couldn't load " + ICON_FILENAME + " into a BufferedImage.");
            }

            final ImageIcon ICON = new ImageIcon(IMAGE);
            return ICON;

        } catch (IOException | IllegalArgumentException | NullPointerException ex) {
            return null;
        }

    }

    static public CharSequence getVersion() {
        try {
            final java.net.URL RES = SaveWindow.class.getResource(SaveWindow.class.getSimpleName() + ".class");
            final java.net.JarURLConnection CONN = (JarURLConnection) RES.openConnection();
            final java.util.jar.Manifest MANIFEST = CONN.getManifest();
            final java.util.jar.Attributes ATTR = MANIFEST.getMainAttributes();
            return new StringBuilder()
                    .append(ATTR.getValue("Implementation-Version"))
                    .append('.')
                    .append(ATTR.getValue("Implementation-Build"))
                    .append(" (")
                    .append(ATTR.getValue("Built-Date"))
                    .append(")");
        } catch (IOException | ClassCastException ex) {
            return "(development version)";
        }

    }

    static final private Logger LOG = Logger.getLogger(AboutDialog.class.getCanonicalName());
    static final private String ICON_FILENAME = "CatsInSunbeam.jpg";
}
