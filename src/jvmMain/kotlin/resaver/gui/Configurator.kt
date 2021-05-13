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

import GenericSupplier
import javafx.stage.FileChooser
import mu.KLoggable
import mu.KLogger
import resaver.Game
import resaver.Mod
import resaver.Mod.Companion.createMod
import resaver.ReSaver
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.Scanner
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.function.Predicate
import java.util.prefs.Preferences
import java.util.regex.Pattern
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Displays dialog boxes for configuring `ModChooser` and
 * `SaveWindow`.
 *
 * @author Mark Fairchild
 */
class Configurator {
    companion object:KLoggable {
        /**
         * Generalized way to get a `Path`.
         *
         * @param owner
         * @param defval `Supplier` for getting a default
         * `Path`.
         * @param request `Supplier` for asking the user to supply the
         * `Path`.
         * @param check A `Predicate` for verifying the
         * `Path`.
         * @param interactive A flag indicating whether prompting the user is
         * allowed.
         * @return
         */

        fun choosePathModal(
            owner: SaveWindow,
            defval: GenericSupplier<Path?>?,
            request: GenericSupplier<Path?>?,
            check: Predicate<Path?>,
            interactive: Boolean
        ): Path? {
            return try {
                val PROMPT = FutureTask { choosePath(defval, request, check, interactive) }
                if (interactive) {
                    val MODAL = ModalProgressDialog(owner, "File Selection", PROMPT)
                    MODAL.isVisible = true
                } else {
                    PROMPT.run()
                }
                PROMPT.get()
            } catch (ex: InterruptedException) {
                logger.info(ex) { "Interrupted while displaying FileChooser." }
                null
            } catch (ex: ExecutionException) {
                logger.info(ex) { "Interrupted while displaying FileChooser." }
                null
            }
        }

        /**
         * Generalized way to get a `Path`.
         *
         * @param defval A `Supplier` for getting a default
         * `Path`.
         * @param request A `Supplier` for asking the user to supply the
         * `Path`.
         * @param check A `Predicate` for checking the validity of the
         * `Path`.
         * @param interactive A flag indicating whether prompting the user is
         * allowed.
         * @return
         */
        fun choosePath(
            defval: GenericSupplier<Path?>?,
            request: GenericSupplier<Path?>?,
            check: Predicate<Path?>,
            interactive: Boolean
        ): Path? {
            if (defval != null) {
                val DEFAULT = defval.invoke()
                if (check.test(DEFAULT)) {
                    return DEFAULT
                }
            }
            if (interactive && request != null) {
                val REQUESTED = request.invoke()
                if (check.test(REQUESTED)) {
                    return REQUESTED
                }
            }
            return null
        }

        /**
         * Shows a file chooser dialog to allow the user to export a plugins list.
         *
         * @param parent The parent component.
         * @param savefile The savefile for which the list is being generated.
         * @return A `Path` pointing to the export file, or
         * `null` if a file was not selected.
         */

        fun selectPluginsExport(parent: SaveWindow, savefile: Path?): Path? {
            logger.info { "Choosing an export file." }
            val previousExport = previousPluginsExport
            val exportPath: Path = if (null != savefile && previousExport != null && Files.exists(previousExport.parent)) {
                previousExport.resolveSibling(savefile.fileName.toString() + ".txt")
            } else if (null != savefile) {
                savefile.resolveSibling(savefile.fileName.toString() + ".txt")
            } else previousExport ?: MYGAMES
            if (parent.isJavaFXAvailable) {
                val CHOOSER = FileChooser()
                CHOOSER.title = "Enter name for export file:"
                val FX_FILTER = FileChooser.ExtensionFilter(TEXTFILES.description, "**.TXT")
                CHOOSER.extensionFilters.add(FX_FILTER)
                if (Files.isDirectory(exportPath)) {
                    CHOOSER.initialDirectory = exportPath.toFile()
                } else {
                    CHOOSER.initialDirectory = exportPath.parent.toFile()
                    CHOOSER.initialFileName = exportPath.fileName.toString()
                }
                while (true) {
                    val exportFile = CHOOSER.showSaveDialog(null)
                    if (exportFile == null) {
                        logger.info("User cancelled.")
                        return null
                    }

                    // Append the ".txt" if necessary.
                    val selection = if (TEXTFILES.accept(exportFile)) exportFile.toPath() else exportFile.toPath()
                        .resolveSibling(exportFile.name + ".txt")
                    if (Files.exists(selection) && !Files.isWritable(selection)) {
                        val MSG = "That directory isn't writeable:\n$selection"
                        JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE)
                    } else {
                        setPreviousPluginsExport(selection)
                        return selection
                    }
                }
            } else {
                val CHOOSER = JFileChooser()
                CHOOSER.isMultiSelectionEnabled = false
                CHOOSER.dialogTitle = "Enter name for export file:"
                CHOOSER.fileFilter = TEXTFILES
                while (true) {
                    val result = CHOOSER.showSaveDialog(parent)
                    val exportFile = CHOOSER.selectedFile
                    if (result == JFileChooser.CANCEL_OPTION || CHOOSER.selectedFile == null) {
                        logger.info { "User cancelled." }
                        return null
                    }

                    // Append the ".txt" if necessary.
                    val selection = if (TEXTFILES.accept(exportFile)) exportFile.toPath() else exportFile.toPath()
                        .resolveSibling(exportFile.name + ".txt")
                    if (Files.exists(selection) && !Files.isWritable(selection)) {
                        val MSG = "That directory isn't writeable:\n$selection"
                        JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE)
                    } else if (Files.exists(selection)) {
                        val MSG = "That file already exists. Replace it?"
                        val overwrite = JOptionPane.showConfirmDialog(
                            parent,
                            MSG,
                            "File Exists",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        )
                        if (overwrite == JOptionPane.OK_OPTION) {
                            setPreviousPluginsExport(selection)
                            return selection
                        }
                    } else {
                        setPreviousPluginsExport(selection)
                        return selection
                    }
                }
            }
        }

        /**
         * Shows a file chooser dialog to allow the user to select a new savefile
         * file.
         *
         * @param parent The parent component.
         * @param game Which game the save is for.
         * @return A `Path` pointing to the savefile file, or
         * `null` if a file was not selected.
         */

        fun selectNewSaveFile(parent: SaveWindow, game: Game): Path? {
            logger.info { "Choosing a savefile." }
            val previousSave = previousSave
            val startingDirectory =
                if (previousSave != null && validDir(previousSave.parent)) previousSave.parent else MYGAMES
            val startingFile = if (validateSavegame(previousSave!!)) previousSave else null
            if (parent.isJavaFXAvailable) {
                val CHOOSER = FileChooser()
                CHOOSER.title = "Enter name for savefile:"
                val FX_FILTER = FileChooser.ExtensionFilter(game.FILTER.description, "**." + game.SAVE_EXT)
                CHOOSER.extensionFilters.add(FX_FILTER)
                CHOOSER.initialDirectory = startingDirectory.toFile()
                if (startingFile != null) {
                    CHOOSER.initialFileName = startingFile.fileName.toString()
                }
                while (true) {
                    val selected = CHOOSER.showSaveDialog(null)
                    if (null == selected) {
                        logger.info { "User cancelled." }
                        return null
                    }

                    // Append the file extension if necessary.
                    val selection = if (game.FILTER.accept(selected)) selected.toPath() else selected.toPath()
                        .resolveSibling(selected.name + "." + game.SAVE_EXT)
                    if (Files.exists(selection) && !Files.isWritable(selection)) {
                        val MSG = "That directory isn't writeable:\n$selection"
                        JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE)
                    } else {
                        return setPreviousSave(selection)
                    }
                }
            } else {
                val CHOOSER = JFileChooser()
                CHOOSER.isMultiSelectionEnabled = false
                CHOOSER.dialogTitle = "Enter name for savefile:"
                CHOOSER.fileFilter = game.FILTER
                while (true) {
                    val result = CHOOSER.showSaveDialog(parent)
                    val selected = CHOOSER.selectedFile
                    if (result == JFileChooser.CANCEL_OPTION || null == selected) {
                        logger.info { "User cancelled." }
                        return null
                    }

                    // Append the file extension if necessary.
                    val selection = if (game.FILTER.accept(selected)) selected.toPath() else selected.toPath()
                        .resolveSibling(selected.name + "." + game.SAVE_EXT)
                    if (Files.exists(selection) && !Files.isWritable(selection)) {
                        val MSG = "That directory isn't writeable:\n$selection"
                        JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE)
                    } else {
                        setPreviousSave(selection)
                        return confirmSaveFile(parent, game, selection)
                    }
                }
            }
        }

        /**
         * Confirms that the user wishes to overwrite a savefile.
         *
         * @param parent The parent component.
         * @param game Which game the save is for.
         * @param selectedPath The `Path` to confirm.
         * @return A `File` pointing to the savefile file, or
         * `null` if a file was not selected.
         */

        fun confirmSaveFile(parent: SaveWindow, game: Game, selectedPath: Path): Path? {
            logger.info { "Choosing a savefile." }
            return if (Files.exists(selectedPath) && !Files.isWritable(selectedPath)) {
                val MSG = "That directory isn't writeable:\n$selectedPath"
                val TITLE = "Can't Write"
                JOptionPane.showMessageDialog(parent, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
                null
            } else if (Files.exists(selectedPath)) {
                val MSG = "That file already exists. Replace it?"
                val TITLE = "File Exists"
                logger.warn { MSG }
                val overwrite = JOptionPane.showConfirmDialog(
                    parent,
                    MSG,
                    TITLE,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                when (overwrite) {
                    JOptionPane.YES_OPTION -> selectedPath
                    JOptionPane.CANCEL_OPTION -> null
                    else -> selectNewSaveFile(parent, game)
                }
            } else {
                selectedPath
            }
        }

        /**
         * Shows a file chooser dialog to allow the user to select a savefile.
         *
         * @param parent The parent component.
         * @return A `File` pointing to the savefile, or
         * `null` if a file was not selected.
         */

        fun selectSaveFile(parent: SaveWindow): Path? {
            logger.info { "Choosing a savefile." }
            val previousSave = previousSave
            val startingDirectory =
                if (previousSave != null && validDir(previousSave.parent)) previousSave.parent else MYGAMES
            val startingFile = if (validateSavegame(previousSave!!)) previousSave else null
            if (parent.isJavaFXAvailable) {
                val CHOOSER = FileChooser()
                CHOOSER.title = "Select savefile:"
                val FX_FILTER = FileChooser.ExtensionFilter(Game.FILTER_ALL.description, "**.ESS", "**.FOS", "**.SAV0")
                CHOOSER.extensionFilters.add(FX_FILTER)
                CHOOSER.initialDirectory = startingDirectory.toFile()
                if (startingFile != null) {
                    CHOOSER.initialFileName = startingFile.fileName.toString()
                }
                while (true) {
                    val selected = CHOOSER.showOpenDialog(null)
                    if (selected == null) {
                        logger.info { "User cancelled." }
                        return null
                    } else if (!validateSavegame(selected.toPath())) {
                        val MSG = "That does not seem to be a valid savegame."
                        JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE)
                    } else {
                        return setPreviousSave(selected.toPath())
                    }
                }
            } else {
                val CHOOSER = JFileChooser()
                CHOOSER.isMultiSelectionEnabled = false
                CHOOSER.dialogTitle = "Select savefile:"
                CHOOSER.actionMap["viewTypeDetails"].actionPerformed(null)
                CHOOSER.fileFilter = Game.FILTER_ALL
                if (startingFile != null) {
                    CHOOSER.selectedFile = startingFile.toFile()
                } else {
                    CHOOSER.currentDirectory = startingDirectory.toFile()
                }
                while (true) {
                    val result = CHOOSER.showOpenDialog(parent)
                    val selected = CHOOSER.selectedFile
                    if (result == JFileChooser.CANCEL_OPTION || null == selected) {
                        logger.info { "User cancelled." }
                        return null
                    } else if (!validateSavegame(selected.toPath())) {
                        val MSG = "That does not seem to be a valid savegame."
                        JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE)
                    } else {
                        return setPreviousSave(selected.toPath())
                    }
                }
            }
        }

        /**
         * Shows a file chooser dialog to allow the user to select where
         * ModOrganizer 2's ini file is. The result (if any) will be stored in the
         * settings.
         *
         * @param parent The parent component.
         * @param game The game whose directory should be selected.
         * @return A `File` pointing to the selected ModOrganizer ini
         * file, or `null` if a file was not selected.
         */

        fun selectMO2Ini(parent: SaveWindow?, game: Game): Path? {
            logger.info { "Choosing the ModOrganizer path." }
            val CHOOSER = JFileChooser()
            CHOOSER.dialogTitle = "Locate ModOrganizer.ini"
            CHOOSER.fileSelectionMode = JFileChooser.FILES_ONLY
            CHOOSER.isMultiSelectionEnabled = false
            if (validateMODir(getMO2Ini(game)!!)) {
                logger.info { "Choosing a ModOrganizer path: trying the pre-existing path." }
                CHOOSER.selectedFile = getMO2Ini(game)!!.toFile()
            } else {
                CHOOSER.currentDirectory = MO2ROOT.toFile()
            }
            while (true) {
                loadChooserPrefs(CHOOSER)
                val result = CHOOSER.showDialog(parent, "Select")
                val file = CHOOSER.selectedFile
                saveChooserPrefs(CHOOSER)
                if (null == file || result == JFileChooser.CANCEL_OPTION) {
                    logger.info { "User cancelled." }
                    return null
                } else if (!validateMO2Ini(game, file.toPath())) {
                    if (!Files.exists(file.toPath())) {
                        val MSG = "That file doesn't exist:\n$file"
                        JOptionPane.showMessageDialog(parent, MSG, "Doesn't Exist", JOptionPane.ERROR_MESSAGE)
                    } else if (!Files.isReadable(file.toPath())) {
                        val MSG = "That file isn't readable:\n$file"
                        JOptionPane.showMessageDialog(parent, MSG, "Not readable", JOptionPane.ERROR_MESSAGE)
                    } else {
                        val MSG = "That directory doesn't seem to contain Mod Organizer:\n$file"
                        JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE)
                    }
                } else {
                    return setMO2Ini(game, file.toPath())
                }
            }
        }

        /**
         * Shows a file chooser dialog to allow the user to select where a game is
         * located.
         *
         * @param parent The parent component.
         * @param game The game whose directory should be selected.
         * @return A `File` pointing to the selected game directory, or
         * `null` if a directory was not selected.
         */

        fun selectGameDirectory(parent: SaveWindow?, game: Game): Path? {
            logger.info { "Choosing the $game directory." }
            val CHOOSER = JFileChooser()
            CHOOSER.dialogTitle = "Select ${game.NAME} directory"
            CHOOSER.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            CHOOSER.isMultiSelectionEnabled = false
            val PREV_DIR = getGameDirectory(game)
            if (validateGameDirectory(game, PREV_DIR!!)) {
                logger.info { "Trying to use the stored value for the game directory." }
                CHOOSER.selectedFile = PREV_DIR.toFile()
            }
            while (true) {
                loadChooserPrefs(CHOOSER)
                val result = CHOOSER.showOpenDialog(parent)
                val file = CHOOSER.selectedFile
                saveChooserPrefs(CHOOSER)
                if (null == file || result == JFileChooser.CANCEL_OPTION) {
                    return null
                } else if (!validateGameDirectory(game, file.toPath())) {
                    val MSG = "This directory doesn't seem to be the ${game.NAME} directory."
                    JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE)
                } else {
                    return setGameDirectory(game, file.toPath())
                }
            }
        }

        /**
         * Shows a file chooser dialog to allow the user to select the watch
         * directory.
         *
         * @param parent The parent component.
         * @param game The game whose directory should be selected.
         * @return A `Path` pointing to the savefile directory, or
         * `null` if a directory was not selected.
         */

        fun selectSavefileDirectory(parent: SaveWindow?, game: Game): Path? {
            logger.info { "Choosing a directory to watch." }
            val CHOOSER = JFileChooser()
            CHOOSER.dialogTitle = "Select folder to watch"
            CHOOSER.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            CHOOSER.isMultiSelectionEnabled = false
            val previousDir = getSaveDirectory(game)
            if (validDir(previousDir)) {
                logger.info { "Trying to use the stored value for the savefile directory." }
                CHOOSER.selectedFile = previousDir.toFile()
            } else {
                CHOOSER.selectedFile = MO2ROOT.toFile()
            }
            while (true) {
                loadChooserPrefs(CHOOSER)
                val result = CHOOSER.showOpenDialog(parent)
                val file = CHOOSER.selectedFile
                saveChooserPrefs(CHOOSER)
                if (null == file || result == JFileChooser.CANCEL_OPTION) {
                    logger.info { "User cancelled." }
                    return null
                } else if (Files.exists(file.toPath()) && !Files.isReadable(file.toPath())) {
                    val MSG = "That directory isn't readable:\n$file"
                    JOptionPane.showMessageDialog(parent, MSG, "Not Readable", JOptionPane.ERROR_MESSAGE)
                } else {
                    return setSaveDirectory(game, file.toPath())
                }
            }
        }

        /**
         * Validates a directory, checking if it contains a valid installation of
         * ModOrganizer.
         *
         * @param dir The directory to validate.
         * @return True if the directory contains ModOrganizer, false otherwise.
         */
        fun validateMODir(dir: Path): Boolean {
            return validDir(dir) && Files.exists(dir.resolve("mods"))
        }

        /**
         * Validates a directory, checking if it contains a valid installation of a
         * game.
         *
         * @param game The game to check for.
         * @param dir The directory to validate.
         * @return True if the directory contains the game, false otherwise.
         */

        fun validateGameDirectory(game: Game, dir: Path): Boolean {
            return validDir(dir) && dir.fileName == game.GAME_DIRECTORY && Files.exists(dir.resolve(game.EXECUTABLE))
        }

        /**
         * Validates a file, checking if it is a savefile. In practice this just
         * means that is a file, it exists, it is readable, and it has the "ESS" or
         * "FOS" extension.
         *
         * @param path The file to validate.
         * @return True if the file is probably a savefile.
         */

        fun validateSavegame(path: Path?): Boolean {
            return validFile(path) && Game.FILTER_ALL.accept(path?.toFile())
        }

        /**
         * Validates an ini, checking if it contains a valid installation of
         * ModOrganizer.
         *
         * @param mo2Ini The ini file to validate and store.
         */

        fun storeMO2Ini(mo2Ini: Path) {
            if (!validFile(mo2Ini)) {
                return
            }
            try {
                Scanner(mo2Ini).use { SCANNER ->
                    val TOKENS: MutableMap<String, String?> = TreeMap()
                    while (SCANNER.hasNextLine()) {
                        val TOKEN = SCANNER.nextLine()
                        val MATCHER = KEY_VALUE.matcher(TOKEN)
                        if (MATCHER.find()) {
                            val KEY = MATCHER.group(1).toLowerCase()
                            val VALUE = getFirst(MATCHER.group(2), MATCHER.group(3))
                            TOKENS[KEY] = VALUE
                        }
                    }
                    val GAME_NAME = TOKENS["gamename"]
                    val GAME_DIR = TOKENS["gamepath"]
                    val GAME_PATH = Paths.get(GAME_DIR!!)
                    logger.info { "Scanned $mo2Ini" }
                    logger.info { "GameName=$GAME_NAME" }
                    if (!Files.exists(GAME_PATH)) {
                        logger.warn { "Directory $GAME_PATH missing." }
                        return
                    }
                    try {
                        val GAME = Game.valueOf(GAME_NAME!!)
                        setMO2Ini(GAME, mo2Ini)
                        JOptionPane.showMessageDialog(
                            null,
                            "Stored MO2 ini file for " + GAME.NAME,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } catch (ex: IllegalArgumentException) {
                    }
                    for (VALUE in Game.VALUES) {
                        if (GAME_PATH.endsWith(VALUE.GAME_DIRECTORY)) {
                            setMO2Ini(VALUE, mo2Ini)
                            JOptionPane.showMessageDialog(
                                null,
                                "Stored MO2 ini file for " + VALUE.NAME,
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                            break
                        }
                    }
                }
            } catch (ex: IOException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
            } catch (ex: RuntimeException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
            }
        }

        /**
         * Validates an ini, checking if it contains a valid installation of
         * ModOrganizer.
         *
         * @param game
         * @param mo2Ini The ini file to validate.
         * @return True if the ini file exists and is readable and contains relevant
         * info.
         */

        fun validateMO2Ini(game: Game?, mo2Ini: Path): Boolean {
            if (!validFile(mo2Ini)) {
                return false
            }
            try {
                Scanner(mo2Ini).use { SCANNER ->
                    val TOKENS: MutableMap<String, String?> = TreeMap()
                    while (SCANNER.hasNext()) {
                        val TOKEN = SCANNER.next()
                        val MATCHER = KEY_VALUE.matcher(TOKEN)
                        if (MATCHER.find()) {
                            val KEY = MATCHER.group(1).toLowerCase()
                            val VALUE = getFirst(MATCHER.group(2), MATCHER.group(3))
                            TOKENS[KEY] = VALUE
                        }
                    }
                    val GAME_NAME = TOKENS["gamename"]
                    val PROFILE_NAME = TOKENS["selected_profile"]
                    val BASEDIR_NAME = TOKENS["base_directory"]
                    val BASEDIR = if (BASEDIR_NAME == null) mo2Ini.parent else Paths.get(BASEDIR_NAME)
                    val MODS = BASEDIR.resolve("mods")
                    val PROFILES = BASEDIR.resolve("profiles")
                    val PROFILE = PROFILES.resolve(PROFILE_NAME)
                    logger.info { "Scanned $mo2Ini" }
                    logger.info { "GameName=$GAME_NAME" }
                    logger.info { "selected_profile=$PROFILE_NAME" }
                    logger.info { "base_directory=$BASEDIR_NAME" }
                    return if (!Files.exists(MODS)) {
                        logger.warn { "Directory $MODS missing." }
                        false
                    } else if (!Files.exists(PROFILES)) {
                        logger.warn { "Directory $PROFILES missing." }
                        false
                    } else if (!Files.exists(PROFILE)) {
                        logger.warn { "Directory $PROFILE missing." }
                        false
                    } else {
                        validateMODir(BASEDIR)
                    }
                }
            } catch (ex: IOException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
                return false
            } catch (ex: RuntimeException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
                return false
            }
        }

        /**
         * Analyzes the ModOrganizer 2 directories and returns a list of mod names,
         * in the order they appear in the currently selected profile's mod list.
         *
         * @param game The game to analyze.
         * @param mo2Ini The ModOrganizer ini file.
         * @return The list of Mods, or null if the modlist file could not be read
         * for any reason.
         */
        fun analyzeModOrganizer2(game: Game?, mo2Ini: Path): List<Mod>? {
            try {
                Scanner(mo2Ini).use { SCANNER ->
                    val TOKENS: MutableMap<String, String?> = TreeMap()
                    while (SCANNER.hasNext()) {
                        val TOKEN = SCANNER.next()
                        val MATCHER = KEY_VALUE.matcher(TOKEN)
                        if (MATCHER.find()) {
                            val KEY = MATCHER.group(1).toLowerCase()
                            val VALUE = getFirst(MATCHER.group(2), MATCHER.group(3))
                            TOKENS[KEY] = VALUE
                        }
                    }
                    val GAME_NAME = TOKENS["gamename"]
                    val PROFILE_NAME = TOKENS["selected_profile"]
                    val BASEDIR_NAME = TOKENS["base_directory"]
                    val BASEDIR = if (BASEDIR_NAME == null) mo2Ini.parent else Paths.get(BASEDIR_NAME)
                    val MODS = BASEDIR.resolve("mods")
                    val PROFILES = BASEDIR.resolve("profiles")
                    val PROFILE = PROFILES.resolve(PROFILE_NAME!!)
                    logger.info { "Scanned $mo2Ini" }
                    logger.info { "GameName=$GAME_NAME" }
                    logger.info { "selected_profile=$PROFILE_NAME" }
                    logger.info { "base_directory=$BASEDIR_NAME" }
                    return if (!Files.exists(MODS)) {
                        logger.warn { "Directory $MODS missing." }
                        null
                    } else if (!Files.exists(PROFILES)) {
                        logger.warn { "Directory $PROFILES missing." }
                        null
                    } else if (!Files.exists(PROFILE)) {
                        logger.warn { "Directory $PROFILE missing." }
                        null
                    } else {
                        analyzeModDirectory(game, PROFILE, MODS)
                    }
                }
            } catch (ex: IOException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
                return null
            } catch (ex: RuntimeException) {
                logger.info(ex) { "Problem while parsing MO2 ini file." }
                return null
            }
        }

        /**
         * Analyzes the ModOrganizer directory and returns a list of mod names, in
         * the order they appear in the currently selected profile's mod list.
         *
         * @param game The game to analyze.
         * @param profile The ModOrganizer profile.
         * @param modDir The ModOranizer mod folder.
         * @return The list of Mods, or null if the modlist file could not be read
         * for any reason.
         */
        fun analyzeModDirectory(game: Game?, profile: Path, modDir: Path): List<Mod>? {
            logger.info { "Attempting to analyze the Mod Organizer directory." }
            return try {
                val MOD_LIST = profile.resolve(MODLIST_PATH)
                logger.info { "Reading the profile's \"ModList.txt\"." }
                val MODNAMES: MutableList<String> = LinkedList()
                Files.newBufferedReader(MOD_LIST).use { input ->
                    logger.info { "Reading from \"ModList.txt\"." }
                    while (input.ready()) {
                        val line = input.readLine()
                        val matcher = MODLIST_REGEX.matcher(line)
                        if (matcher.matches()) {
                            if (matcher.group(1) == "+") {
                                MODNAMES.add(matcher.group(2))
                            }
                        }
                    }
                    logger.info { String.format("\"ModList.txt\" contained %d mod names.", MODNAMES.size) }
                }
                val MODS: MutableList<Mod> = ArrayList()
                for (MODNAME in MODNAMES) {
                    val path = modDir.resolve(MODNAME)
                    val mod = createMod(game, path)
                    if (mod != null) {
                        MODS.add(mod)
                    }
                }
                logger.info { String.format("analyzeModDirectory: checked %d mods.", MODS.size) }
                MODS
            } catch (ex: IOException) {
                logger.error { "Something went wrong while analyzing ModOrganizer. Let's not stress about what it was, let's just fail and return." }
                null
            }
        }

        /**
         *
         * @param chooser
         */
        private fun saveChooserPrefs(chooser: JFileChooser) {
            PREFS.putInt("chooserWidth", chooser.size.width)
            PREFS.putInt("chooserHeight", chooser.size.height)
            PREFS.putInt("chooserX", chooser.location.x)
            PREFS.putInt("chooserY", chooser.location.y)
        }

        /**
         *
         * @param chooser
         */
        private fun loadChooserPrefs(chooser: JFileChooser) {
            val width = PREFS.getInt("chooserWidth", chooser.size.width)
            val height = PREFS.getInt("chooserHeight", chooser.size.height)
            val x = PREFS.getInt("chooserX", chooser.location.x)
            val y = PREFS.getInt("chooserY", chooser.location.y)
            chooser.setSize(width, height)
            chooser.setLocation(x, y)
        }

        /**
         * Getter for a game's directory field.
         *
         * @param game The game.
         * @return The directory.
         */

        fun getGameDirectory(game: Game): Path? {
            val KEY = game.NAME + "_directory"
            val path = PREFS[KEY, ""]
            return if (path.isEmpty()) null else Paths.get(path)
        }

        /**
         * Setter for the a game's directory field.
         *
         * @param game The game.
         * @param dir The new directory.
         * @return The specified `Path`.
         */
        fun setGameDirectory(game: Game, dir: Path?): Path? {
            val KEY = game.NAME + "_directory"
            if (dir == null) {
                PREFS.remove(KEY)
            } else {
                PREFS.put(KEY, dir.toString())
            }
            return dir
        }

        /**
         * Getter for the mod organizer ini field.
         *
         * @param game The game whose MO ini file should be stored.
         * @return The ini file.
         */

        fun getMO2Ini(game: Game): Path? {
            val gameDir = MO2ROOT.resolve(game.NAME)
            val iniFile = gameDir.resolve("ModOrganizer.ini")
            var defPath = iniFile.parent
            while (!Files.exists(defPath) && defPath.nameCount > 0) {
                defPath = defPath.parent
            }
            val path = PREFS["modOrganizerIni_$game", defPath.toString()]
            return if (path.isEmpty()) {
                null
            } else Paths.get(path)
        }

        /**
         * Setter for the mod organizer ini field.
         *
         * @param game The game whose MO ini file should be stored.
         * @param file The new ini file.
         * @return The specified `Path`.
         */
        fun setMO2Ini(game: Game, file: Path?): Path? {
            val KEY = "modOrganizerIni_$game"
            if (file == null) {
                PREFS.remove(KEY)
            } else {
                PREFS.put(KEY, file.toString())
            }
            return file
        }

        /**
         * Getter for the mod organizer ini field.
         *
         * @param game The game whose MO ini file should be stored.
         * @return The ini file.
         */

        fun getSaveDirectory(game: Game): Path {
            val DEFAULT = MYGAMES.resolve(game.SAVE_DIRECTORY)
            val STORED = Paths.get(PREFS["saveDirectory_$game", DEFAULT.toString()])
            return when {
                validDir(STORED) -> {
                    STORED
                }
                validDir(DEFAULT) -> {
                    DEFAULT
                }
                else -> {
                    MYGAMES
                }
            }
        }

        /**
         * Setter for the mod organizer ini field.
         *
         * @param game The game whose MO ini file should be stored.
         * @param file The new ini file.
         * @return The specified `Path`.
         */
        fun setSaveDirectory(game: Game, file: Path?): Path? {
            val KEY = "saveDirectory_$game"
            if (file == null) {
                PREFS.remove(KEY)
            } else {
                PREFS.put(KEY, file.toString())
            }
            return file
        }

        /**
         * Getter for the previous save field.
         *
         * @return The file.
         */
        val previousSave: Path?
            get() {
                val path = PREFS["previousSave", ""]
                return if (path.isEmpty()) {
                    null
                } else Paths.get(path)
            }

        /**
         * Setter for the previous save field.
         *
         * @param file The new file.
         * @return The specified `Path`.
         */
        fun setPreviousSave(file: Path?): Path? {
            val KEY = "previousSave"
            if (file == null) {
                PREFS.remove(KEY)
            } else {
                PREFS.put(KEY, file.toString())
            }
            return file
        }

        /**
         * Getter for the previous plugins export field.
         *
         * @return The file.
         */
        val previousPluginsExport: Path?
            get() {
                val path = PREFS["previousPluginsExport", ""]
                return if (path.isEmpty()) {
                    null
                } else Paths.get(path)
            }

        /**
         * Setter for the previous plugins export field.
         *
         * @param file The new file.
         * @return The specified `Path`.
         */
        fun setPreviousPluginsExport(file: Path?): Path? {
            val KEY = "previousPluginsExport"
            if (file == null) {
                PREFS.remove(KEY)
            } else {
                PREFS.put(KEY, file.toString())
            }
            return file
        }

        /**
         * Test is a path refers to a file that exists and is readable.
         *
         * @param path
         * @return
         */
        fun validFile(path: Path?): Boolean {
            return if (null == path) {
                logger.info { "validFile check: null." }
                false
            } else if (!Files.isRegularFile(path)) {
                logger.info { "validFile check: irregular." }
                false
            } else if (!Files.isReadable(path)) {
                logger.info { "validFile check: unreadable." }
                false
            } else {
                true
            }
        }

        /**
         * Test is a path refers to a directory that exists and is readable.
         *
         * @param path
         * @return
         */
        fun validDir(path: Path?): Boolean {
            return if (null == path) {
                false
            } else Files.isDirectory(path) && Files.exists(path) && Files.isReadable(path)
        }

        /**
         * Test is a path refers to a file can be written.
         *
         * @param path
         * @return
         */

        fun validWrite(path: Path?): Boolean {
            return if (null == path) {
                false
            } else if (Files.exists(path) && !Files.isRegularFile(path)) {
                false
            } else if (Files.exists(path) && !Files.isWritable(path)) {
                false
            } else {
                Files.isWritable(path.parent)
            }
        }

        /**
         *
         * @param items
         * @return
         */
        fun getFirst(vararg items: Path): Path? {
            return Arrays.stream(items)
                .filter { obj: Path? -> Objects.nonNull(obj) }
                .filter { path: Path? -> Files.exists(path!!) }
                .findFirst().orElse(null)
        }

        /**
         *
         * @param items
         * @return
         */
        fun getFirst(vararg items: String?): String? {
            return Arrays.stream(items).filter { obj: String? -> Objects.nonNull(obj) }.findFirst().orElse(null)
        }

        const val MODS_PATH = "mods"
        const val PROFILES_PATH = "profiles"
        const val INI_PATH = "ModOrganizer.ini"
        const val MODLIST_PATH = "modlist.txt"
        const val MODLIST_PATTERN = "^([+-])(.+)$"
        val MODLIST_REGEX = Pattern.compile(MODLIST_PATTERN)!!
        val GLOB_INI = FileSystems.getDefault().getPathMatcher("glob:**.ini")!!
        private val TEXTFILES = FileNameExtensionFilter("Text file", "txt")
        private val PREFS = Preferences.userNodeForPackage(ReSaver::class.java)
        private val KEY_VALUE = Pattern.compile("^(.+)=(?:@ByteArray\\((.+)\\)|(.+))$", Pattern.CASE_INSENSITIVE)
        private val MO2ROOT = Paths.get(System.getProperty("user.home"), "appData", "local", "ModOrganizer")
        private val MYGAMES = JFileChooser().fileSystemView.defaultDirectory.toPath().resolve("My Games")
        override val logger: KLogger
            get() = logger()
    }
}