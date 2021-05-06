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
package resaver;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import resaver.gui.Configurator;
import resaver.gui.SaveWindow;

/**
 * Entry class for ReSaver.
 *
 * @author Mark Fairchild
 */
@Command(name = "ReSaver", mixinStandardHelpOptions = true, version = "ReSaver 0.5.9.9", description = "")
public class ReSaver implements Callable<Integer> {

    public ReSaver() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ReSaver()).execute(args);
        //System.exit(exitCode);
    }

    /**
     */
    @Override
    public Integer call() {
        // Use the dark nimbus theme if specified.
        try {
            if (DARKTHEME_OPTION || PREFS.getBoolean("settings.darktheme", false)) {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                setDarkNimus();
            } else {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            LOG.log(Level.WARNING, "Couldn't set theme.", ex);
        }

        // Set the font scaling.
        float fontScale = Math.max(0.5f, PREFS.getFloat("settings.fontScale", 1.0f));

        for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
            if (key.toString().endsWith(".font")) {
                Font font = UIManager.getFont(key);
                Font biggerFont = font.deriveFont(fontScale * font.getSize2D());
                UIManager.put(key, biggerFont);
            }
        }

        // Set up logging stuff.
        LOG.getParent().getHandlers()[0].setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(LogRecord record) {
                final java.util.logging.Level LEVEL = record.getLevel();
                final String MSG = record.getMessage();
                final String SRC = record.getSourceClassName() + "." + record.getSourceMethodName();
                return String.format("%s: %s: %s\n", SRC, LEVEL, MSG);
            }
        });

        LOG.getParent().getHandlers()[0].setLevel(Level.INFO);

        // Check the autoparse setting.
        final Path PREVIOUS = Configurator.getPreviousSave();
        final SaveWindow WINDOW;

        if (PATH_PARAMETER != null && !PATH_PARAMETER.isEmpty() && Configurator.validateSavegame(PATH_PARAMETER.get(0))) {
            WINDOW = new SaveWindow(PATH_PARAMETER.get(0), AUTOPARSE_OPTION);
        } else if (REOPEN_OPTION && Configurator.validateSavegame(PREVIOUS)) {
            WINDOW = new SaveWindow(PREVIOUS, AUTOPARSE_OPTION);
        } else {
            WINDOW = new SaveWindow(null, false);
        }

        if (WATCH_OPTION) {
            WINDOW.setWatching(true);
        }

        java.awt.EventQueue.invokeLater(() -> WINDOW.setVisible(true));
        return 0;
    }

    /**
     * Sets swing to use a dark version of Nimbus.
     */
    static public void setDarkNimus() {
        UIManager.put("control", new Color(128, 128, 128));
        UIManager.put("info", new Color(128, 128, 128));
        UIManager.put("nimbusBase", new Color(18, 30, 49));
        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
        UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusGreen", new Color(176, 179, 50));
        UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
        UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
        UIManager.put("nimbusOrange", new Color(191, 98, 4));
        UIManager.put("nimbusRed", new Color(169, 46, 34));
        UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
        UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
        UIManager.put("text", new Color(230, 230, 230));
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOG.log(Level.WARNING, "Error setting Dark Nimbus theme.", ex);
        }
    }

    static final Logger LOG = Logger.getLogger(ReSaver.class.getCanonicalName());
    static final private java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(resaver.ReSaver.class);

    //@Option(names = {"-e", "--decompile"}, description = "Tries to decompile a script file. It's NOT as good as Champollion, but it can sometimes handle sabotaged pex files.")
    //private String DECOMPILE;

    @Option(names = {"-r", "--reopen"}, description = "Reopen the most recently opened savefile (ignored if a valid savefile is specified).")
    private boolean REOPEN_OPTION;

    @Option(names = {"-p", "--autoparse"}, description = "Automatically scan plugins for the specified savefile (ignored unless a savefile is specified or the -r option is used.")
    private boolean AUTOPARSE_OPTION;

    @Option(names = {"-d", "--darktheme"}, description = "Use the custom Dark Nimbus theme.")
    private boolean DARKTHEME_OPTION;

    @Option(names = {"-w", "--watch"}, description = "Automatically start watching the savefile directories.")
    private boolean WATCH_OPTION;

    @Parameters(description = "The savefile to open (optional).")
    private java.util.List<Path> PATH_PARAMETER;

}
