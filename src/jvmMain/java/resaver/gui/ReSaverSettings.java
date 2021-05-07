/*
 * Copyright 2020 Mark.
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

import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resaver.Game;

/**
 * Settings DialogBox.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class ReSaverSettings extends JDialog {

    public ReSaverSettings(SaveWindow parent, @Nullable Game currentGame) {
        super(parent, true);
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setLocationRelativeTo(parent);

        final JTabbedPane PANE = new JTabbedPane();
        super.setContentPane(PANE);

        {
            final JPanel TAB = new JPanel();
            TAB.setLayout(new BoxLayout(TAB, BoxLayout.PAGE_AXIS));
            PANE.add("General", TAB);

            {
                final JCheckBox ALWAYSPARSE = new JCheckBox("Always parse", PREFS.getBoolean("settings.alwaysParse", false));
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(ALWAYSPARSE);
                TAB.add(PANEL);
                ALWAYSPARSE.addActionListener(e -> PREFS.putBoolean("settings.alwaysParse", ALWAYSPARSE.isSelected()));
            }

            {
                final JCheckBox DARKTHEME = new JCheckBox("Use Dark Nimbus theme", PREFS.getBoolean("settings.darktheme", false));
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(DARKTHEME);
                TAB.add(PANEL);
                DARKTHEME.addActionListener(e -> PREFS.putBoolean("settings.darktheme", DARKTHEME.isSelected()));
            }

            {
                final JCheckBox JAVAFX = new JCheckBox("Use JavaFX native fileChooser", PREFS.getBoolean("settings.javafx", false));
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));
                PANEL.add(JAVAFX);
                TAB.add(PANEL);
                JAVAFX.addActionListener(e -> PREFS.putBoolean("settings.javafx", JAVAFX.isSelected()));
            }

            {
                final JLabel LABEL = new JLabel("Font scaling:");
                final JFormattedTextField SCALEFIELD = new JFormattedTextField(PREFS.getFloat("settings.fontScale", 1.0f));
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.LEADING));

                LABEL.setLabelFor(SCALEFIELD);
                PANEL.add(LABEL);
                PANEL.add(SCALEFIELD);
                SCALEFIELD.setColumns(5);
                TAB.add(PANEL);

                SCALEFIELD.addActionListener(e -> {
                    Number n = (Number) SCALEFIELD.getValue();
                    float fontScale = Math.min(5.0f, Math.max(n.floatValue(), 0.5f));
                    PREFS.putFloat("settings.fontScale", fontScale);
                    SCALEFIELD.setValue(fontScale);

                    for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
                        if (key.toString().endsWith(".font")) {
                            Font font = UIManager.getFont(key);
                            Font biggerFont = font.deriveFont(fontScale * font.getSize2D());
                            UIManager.put(key, biggerFont);
                        }
                    }
                });
            }

        }

        Game.VALUES.forEach((game) -> {
            JPanel TAB = new JPanel();
            TAB.setLayout(new BoxLayout(TAB, BoxLayout.PAGE_AXIS));
            PANE.add(game.getNAME(), TAB);

            {
                final JLabel LABEL = new JLabel("Game directory:");
                final JTextField GAMEDIR = new JTextField(getFirst(Configurator.getGameDirectory(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton("Select");
                LABEL.setLabelFor(GAMEDIR);
                GAMEDIR.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(GAMEDIR);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectGameDirectory(parent, game), Configurator.getGameDirectory(game), HOME);
                    GAMEDIR.setText(newPath.toString());
                });
            }

            {
                final JLabel LABEL = new JLabel("ModOrganizer2 ini file:");
                final JTextField MO2INI = new JTextField(getFirst(Configurator.getMO2Ini(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton("Select");
                LABEL.setLabelFor(MO2INI);
                MO2INI.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(MO2INI);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectMO2Ini(parent, game), Configurator.getMO2Ini(game), HOME);
                    MO2INI.setText(newPath.toString());
                });
            }

            {
                final JLabel LABEL = new JLabel("Savefile directory:");
                final JTextField SAVEDIR = new JTextField(getFirst(Configurator.getSaveDirectory(game), HOME).toString(), 50);
                final JPanel PANEL = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                final JButton BUTTON = new JButton("Select");
                LABEL.setLabelFor(SAVEDIR);
                SAVEDIR.setEditable(false);
                PANEL.add(LABEL);
                PANEL.add(SAVEDIR);
                PANEL.add(BUTTON);
                TAB.add(PANEL);

                BUTTON.addActionListener(e -> {
                    Path newPath = getFirst(Configurator.selectSavefileDirectory(parent, game), Configurator.getSaveDirectory(game), HOME);
                    SAVEDIR.setText(newPath.toString());
                });
            }
        });

        if (currentGame != null) {
            PANE.setSelectedIndex(Game.VALUES.indexOf(currentGame));
        }
    }

    /**
     *
     * @param items
     * @return
     */
    static public Path getFirst(@NotNull Path... items) {
        return Arrays.stream(items).filter(Objects::nonNull).findFirst().orElse(null);
    }

    static private final Path HOME = Paths.get(System.getProperty("user.home"), "appData", "local", "ModOrganizer");
    static final private java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(resaver.ReSaver.class);

}
