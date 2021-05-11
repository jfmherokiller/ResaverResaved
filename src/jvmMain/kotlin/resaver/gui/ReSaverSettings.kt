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
package resaver.gui

import resaver.Game
import resaver.ReSaver
import resaver.gui.Configurator.Companion.getGameDirectory
import resaver.gui.Configurator.Companion.getMO2Ini
import resaver.gui.Configurator.Companion.getSaveDirectory
import resaver.gui.Configurator.Companion.selectGameDirectory
import resaver.gui.Configurator.Companion.selectMO2Ini
import resaver.gui.Configurator.Companion.selectSavefileDirectory
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.swing.*

/**
 * Settings DialogBox.
 *
 * @author Mark Fairchild
 */
class ReSaverSettings(parent: SaveWindow?, currentGame: Game?) : JDialog(parent, true) {
    companion object {
        /**
         *
         * @param items
         * @return
         */
        fun getFirst(vararg items: Path?): Path? {
            return items.firstOrNull()
        }

        private val HOME = Paths.get(System.getProperty("user.home"), "appData", "local", "ModOrganizer")
        private val PREFS = Preferences.userNodeForPackage(ReSaver::class.java)
    }

    init {
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        super.setLocationRelativeTo(parent)
        val PANE = JTabbedPane()
        super.setContentPane(PANE)
        val TABGenal = JPanel()
        TABGenal.layout = BoxLayout(TABGenal, BoxLayout.PAGE_AXIS)
        PANE.add("General", TABGenal)
        val ALWAYSPARSE = JCheckBox("Always parse", PREFS.getBoolean("settings.alwaysParse", false))
        val PANELAParse = JPanel(FlowLayout(FlowLayout.LEADING))
        PANELAParse.add(ALWAYSPARSE)
        TABGenal.add(PANELAParse)
        ALWAYSPARSE.addActionListener { e: ActionEvent? ->
            PREFS.putBoolean(
                "settings.alwaysParse",
                ALWAYSPARSE.isSelected
            )
        }
        val DARKTHEME = JCheckBox("Use Dark Nimbus theme", PREFS.getBoolean("settings.darktheme", false))
        val PANELDark = JPanel(FlowLayout(FlowLayout.LEADING))
        PANELDark.add(DARKTHEME)
        TABGenal.add(PANELDark)
        DARKTHEME.addActionListener { e: ActionEvent? -> PREFS.putBoolean("settings.darktheme", DARKTHEME.isSelected) }
        val JAVAFX = JCheckBox("Use JavaFX native fileChooser", PREFS.getBoolean("settings.javafx", false))
        val PANELJavafx = JPanel(FlowLayout(FlowLayout.LEADING))
        PANELJavafx.add(JAVAFX)
        TABGenal.add(PANELJavafx)
        JAVAFX.addActionListener { e: ActionEvent? ->
            PREFS.putBoolean(
                "settings.javafx",
                JAVAFX.isSelected
            )
        }

        val LABELscaling = JLabel("Font scaling:")
            val SCALEFIELD = JFormattedTextField(PREFS.getFloat("settings.fontScale", 1.0f))
            val PANELscaling = JPanel(FlowLayout(FlowLayout.LEADING))
            LABELscaling.labelFor = SCALEFIELD
            PANELscaling.add(LABELscaling)
            PANELscaling.add(SCALEFIELD)
            SCALEFIELD.columns = 5
            TABGenal.add(PANELscaling)
            SCALEFIELD.addActionListener { e: ActionEvent? ->
                val n = SCALEFIELD.value as Number
                val fontScale = 5.0f.coerceAtMost(n.toFloat().coerceAtLeast(0.5f))
                PREFS.putFloat("settings.fontScale", fontScale)
                SCALEFIELD.value = fontScale
                for (key in UIManager.getLookAndFeelDefaults().keys) {
                    if (key.toString().endsWith(".font")) {
                        val font = UIManager.getFont(key)
                        val biggerFont = font.deriveFont(fontScale * font.size2D)
                        UIManager.put(key, biggerFont)
                    }
                }
            }

        for (game in Game.VALUES) {
            val TAB = JPanel()
            TAB.layout = BoxLayout(TAB, BoxLayout.PAGE_AXIS)
            PANE.add(game.NAME, TAB)
            val LABELgameDir = JLabel("Game directory:")
            val gamePart = getGameDirectory(game)
            val gameget = getFirst(gamePart, HOME).toString()
            val GAMEDIR = JTextField(gameget, 50)
            val PANELgameDir = JPanel(FlowLayout(FlowLayout.TRAILING))
            val BUTTONgameDir = JButton("Select")
            LABELgameDir.labelFor = GAMEDIR
            GAMEDIR.isEditable = false
            PANELgameDir.add(LABELgameDir)
            PANELgameDir.add(GAMEDIR)
            PANELgameDir.add(BUTTONgameDir)
            TAB.add(PANELgameDir)
            BUTTONgameDir.addActionListener { e: ActionEvent? ->
                val newPath = getFirst(
                    selectGameDirectory(parent, game)!!, getGameDirectory(game)!!, HOME
                )
                GAMEDIR.text = newPath.toString()
            }
            val LABELModOrg = JLabel("ModOrganizer2 ini file:")
            val MO2INI = JTextField(getFirst(getMO2Ini(game)!!, HOME).toString(), 50)
            val PANELModorg = JPanel(FlowLayout(FlowLayout.TRAILING))
            val BUTTONModOrg = JButton("Select")
            LABELModOrg.labelFor = MO2INI
            MO2INI.isEditable = false
            PANELModorg.add(LABELModOrg)
            PANELModorg.add(MO2INI)
            PANELModorg.add(BUTTONModOrg)
            TAB.add(PANELModorg)
            BUTTONModOrg.addActionListener { e: ActionEvent? ->
                val newPath = getFirst(
                    selectMO2Ini(parent, game)!!, getMO2Ini(game)!!, HOME
                )
                MO2INI.text = newPath.toString()
            }
            val LABELSavefileDir = JLabel("Savefile directory:")
            val SAVEDIR = JTextField(getFirst(getSaveDirectory(game), HOME).toString(), 50)
            val PANELSavefileDir = JPanel(FlowLayout(FlowLayout.TRAILING))
            val BUTTONSavefileDir = JButton("Select")
            LABELSavefileDir.labelFor = SAVEDIR
            SAVEDIR.isEditable = false
            PANELSavefileDir.add(LABELSavefileDir)
            PANELSavefileDir.add(SAVEDIR)
            PANELSavefileDir.add(BUTTONSavefileDir)
            TAB.add(PANELSavefileDir)
            BUTTONSavefileDir.addActionListener { e: ActionEvent? ->
                val newPath = getFirst(
                    selectSavefileDirectory(parent, game)!!, getSaveDirectory(game), HOME
                )
                SAVEDIR.text = newPath.toString()
            }
        }
        if (currentGame != null) {
            PANE.selectedIndex = Game.VALUES.indexOf(currentGame)
        }
    }
}