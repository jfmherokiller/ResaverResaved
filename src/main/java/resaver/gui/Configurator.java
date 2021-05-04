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
package resaver.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import resaver.Mod;
import resaver.Game;

/**
 * Displays dialog boxes for configuring <code>ModChooser</code> and
 * <code>SaveWindow</code>.
 *
 * @author Mark Fairchild
 */
abstract public class Configurator {

    /**
     * Generalized way to get a <code>Path</code>.
     *
     * @param owner
     * @param defval <code>Supplier</code> for getting a default
     * <code>Path</code>.
     * @param request <code>Supplier</code> for asking the user to supply the
     * <code>Path</code>.
     * @param check A <code>Predicate</code> for verifying the
     * <code>Path</code>.
     * @param interactive A flag indicating whether prompting the user is
     * allowed.
     * @return
     */
    static public Path choosePathModal(SaveWindow owner, Supplier<Path> defval, Supplier<Path> request, Predicate<Path> check, boolean interactive) {
        try {
            final FutureTask<Path> PROMPT = new FutureTask<>(() -> choosePath(defval, request, check, interactive));

            if (interactive) {
                final ModalProgressDialog MODAL = new ModalProgressDialog(owner, "File Selection", PROMPT);
                MODAL.setVisible(true);
                return PROMPT.get();
            } else {
                PROMPT.run();
                return PROMPT.get();
            }

        } catch (InterruptedException | ExecutionException ex) {
            LOG.log(Level.SEVERE, "Interrupted while displaying FileChooser.", ex);
            return null;
        }
    }

    /**
     * Generalized way to get a <code>Path</code>.
     *
     * @param defval A <code>Supplier</code> for getting a default
     * <code>Path</code>.
     * @param request A <code>Supplier</code> for asking the user to supply the
     * <code>Path</code>.
     * @param check A <code>Predicate</code> for checking the validity of the
     * <code>Path</code>.
     * @param interactive A flag indicating whether prompting the user is
     * allowed.
     * @return
     */
    static public Path choosePath(Supplier<Path> defval, Supplier<Path> request, Predicate<Path> check, boolean interactive) {
        if (defval != null) {
            final Path DEFAULT = defval.get();
            if (check.test(DEFAULT)) {
                return DEFAULT;
            }
        }

        if (interactive && request != null) {
            final Path REQUESTED = request.get();
            if (check.test(REQUESTED)) {
                return REQUESTED;
            }
        }

        return null;
    }

    /**
     * Shows a file chooser dialog to allow the user to export a plugins list.
     *
     * @param parent The parent component.
     * @param savefile The savefile for which the list is being generated.
     * @return A <code>Path</code> pointing to the export file, or
     * <code>null</code> if a file was not selected.
     */
    static public Path selectPluginsExport(SaveWindow parent, Path savefile) {
        LOG.info("Choosing an export file.");

        Path previousExport = getPreviousPluginsExport();
        Path exportPath;

        if (null != savefile && previousExport != null && Files.exists(previousExport.getParent())) {
            exportPath = previousExport.resolveSibling(savefile.getFileName().toString() + ".txt");
        } else if (null != savefile) {
            exportPath = savefile.resolveSibling(savefile.getFileName().toString() + ".txt");
        } else if (null != previousExport) {
            exportPath = previousExport;
        } else {
            exportPath = MYGAMES;
        }

        if (parent.isJavaFXAvailable()) {
            javafx.stage.FileChooser CHOOSER = new javafx.stage.FileChooser();
            CHOOSER.setTitle("Enter name for export file:");
            javafx.stage.FileChooser.ExtensionFilter FX_FILTER = new javafx.stage.FileChooser.ExtensionFilter(TEXTFILES.getDescription(), "**.TXT");
            CHOOSER.getExtensionFilters().add(FX_FILTER);

            if (Files.isDirectory(exportPath)) {
                CHOOSER.setInitialDirectory(exportPath.toFile());
            } else {
                CHOOSER.setInitialDirectory(exportPath.getParent().toFile());
                CHOOSER.setInitialFileName(exportPath.getFileName().toString());
            }

            while (true) {
                File exportFile = CHOOSER.showSaveDialog(null);

                if (exportFile == null) {
                    LOG.fine("User cancelled.");
                    return null;
                }

                // Append the ".txt" if necessary.
                Path selection = TEXTFILES.accept(exportFile)
                        ? exportFile.toPath()
                        : exportFile.toPath().resolveSibling(exportFile.getName() + ".txt");

                if (Files.exists(selection) && !Files.isWritable(selection)) {
                    final String MSG = String.format("That directory isn't writeable:\n%s", selection);
                    JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE);
                } else {
                    setPreviousPluginsExport(selection);
                    return selection;
                }
            }
        } else {
            final JFileChooser CHOOSER = new JFileChooser();
            CHOOSER.setMultiSelectionEnabled(false);
            CHOOSER.setDialogTitle("Enter name for export file:");
            CHOOSER.setFileFilter(TEXTFILES);

            while (true) {
                int result = CHOOSER.showSaveDialog(parent);
                File exportFile = CHOOSER.getSelectedFile();

                if (result == JFileChooser.CANCEL_OPTION || CHOOSER.getSelectedFile() == null) {
                    LOG.fine("User cancelled.");
                    return null;
                }

                // Append the ".txt" if necessary.
                Path selection = TEXTFILES.accept(exportFile)
                        ? exportFile.toPath()
                        : exportFile.toPath().resolveSibling(exportFile.getName() + ".txt");

                if (Files.exists(selection) && !Files.isWritable(selection)) {
                    final String MSG = String.format("That directory isn't writeable:\n%s", selection);
                    JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE);
                } else if (Files.exists(selection)) {
                    final String MSG = "That file already exists. Replace it?";
                    int overwrite = JOptionPane.showConfirmDialog(parent, MSG, "File Exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (overwrite == JOptionPane.OK_OPTION) {
                        setPreviousPluginsExport(selection);
                        return selection;
                    }
                } else {
                    setPreviousPluginsExport(selection);
                    return selection;
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
     * @return A <code>Path</code> pointing to the savefile file, or
     * <code>null</code> if a file was not selected.
     */
    static public Path selectNewSaveFile(SaveWindow parent, Game game) {
        LOG.info("Choosing a savefile.");

        Path previousSave = getPreviousSave();

        Path startingDirectory = previousSave != null && Configurator.validDir(previousSave.getParent())
                ? previousSave.getParent()
                : MYGAMES;

        Path startingFile = Configurator.validateSavegame(previousSave)
                ? previousSave
                : null;

        if (parent.isJavaFXAvailable()) {
            javafx.stage.FileChooser CHOOSER = new javafx.stage.FileChooser();
            CHOOSER.setTitle("Enter name for savefile:");
            javafx.stage.FileChooser.ExtensionFilter FX_FILTER = new javafx.stage.FileChooser.ExtensionFilter(game.getFILTER().getDescription(), "**." + game.getSAVE_EXT());
            CHOOSER.getExtensionFilters().add(FX_FILTER);
            CHOOSER.setInitialDirectory(startingDirectory.toFile());
            if (startingFile != null) {
                CHOOSER.setInitialFileName(startingFile.getFileName().toString());
            }

            while (true) {
                File selected = CHOOSER.showSaveDialog(null);
                if (null == selected) {
                    LOG.fine("User cancelled.");
                    return null;
                }

                // Append the file extension if necessary.
                Path selection = game.getFILTER().accept(selected)
                        ? selected.toPath()
                        : selected.toPath().resolveSibling(selected.getName() + "." + game.getSAVE_EXT());

                if (Files.exists(selection) && !Files.isWritable(selection)) {
                    final String MSG = String.format("That directory isn't writeable:\n%s", selection);
                    JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE);
                } else {
                    return setPreviousSave(selection);
                }
            }
        } else {
            final JFileChooser CHOOSER = new JFileChooser();
            CHOOSER.setMultiSelectionEnabled(false);
            CHOOSER.setDialogTitle("Enter name for savefile:");
            CHOOSER.setFileFilter(game.getFILTER());

            while (true) {
                int result = CHOOSER.showSaveDialog(parent);
                File selected = CHOOSER.getSelectedFile();

                if (result == JFileChooser.CANCEL_OPTION || null == selected) {
                    LOG.fine("User cancelled.");
                    return null;
                }

                // Append the file extension if necessary.
                Path selection = game.getFILTER().accept(selected)
                        ? selected.toPath()
                        : selected.toPath().resolveSibling(selected.getName() + "." + game.getSAVE_EXT());

                if (Files.exists(selection) && !Files.isWritable(selection)) {
                    final String MSG = String.format("That directory isn't writeable:\n%s", selection);
                    JOptionPane.showMessageDialog(parent, MSG, "Can't Write", JOptionPane.ERROR_MESSAGE);
                } else {
                    setPreviousSave(selection);
                    return confirmSaveFile(parent, game, selection);
                }
            }
        }
    }

    /**
     * Confirms that the user wishes to overwrite a savefile.
     *
     * @param parent The parent component.
     * @param game Which game the save is for.
     * @param selectedPath The <code>Path</code> to confirm.
     * @return A <code>File</code> pointing to the savefile file, or
     * <code>null</code> if a file was not selected.
     */
    static public Path confirmSaveFile(SaveWindow parent, Game game, Path selectedPath) {
        LOG.info("Choosing a savefile.");

        if (Files.exists(selectedPath) && !Files.isWritable(selectedPath)) {
            final String MSG = String.format("That directory isn't writeable:\n%s", selectedPath);
            final String TITLE = "Can't Write";
            JOptionPane.showMessageDialog(parent, MSG, TITLE, JOptionPane.ERROR_MESSAGE);
            return null;

        } else if (Files.exists(selectedPath)) {
            final String MSG = "That file already exists. Replace it?";
            final String TITLE = "File Exists";
            LOG.warning(MSG);
            int overwrite = JOptionPane.showConfirmDialog(parent, MSG, TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            switch (overwrite) {
                case JOptionPane.YES_OPTION:
                    return selectedPath;
                case JOptionPane.CANCEL_OPTION:
                    return null;
                default:
                    return selectNewSaveFile(parent, game);
            }

        } else {
            return selectedPath;
        }
    }

    /**
     * Shows a file chooser dialog to allow the user to select a savefile.
     *
     * @param parent The parent component.
     * @return A <code>File</code> pointing to the savefile, or
     * <code>null</code> if a file was not selected.
     */
    static public Path selectSaveFile(SaveWindow parent) {
        LOG.info("Choosing a savefile.");

        Path previousSave = getPreviousSave();

        Path startingDirectory = previousSave != null && Configurator.validDir(previousSave.getParent())
                ? previousSave.getParent()
                : MYGAMES;

        Path startingFile = Configurator.validateSavegame(previousSave)
                ? previousSave
                : null;

        if (parent.isJavaFXAvailable()) {
            javafx.stage.FileChooser CHOOSER = new javafx.stage.FileChooser();
            CHOOSER.setTitle("Select savefile:");
            javafx.stage.FileChooser.ExtensionFilter FX_FILTER = new javafx.stage.FileChooser.ExtensionFilter(Game.Companion.getFILTER_ALL().getDescription(), "**.ESS", "**.FOS", "**.SAV0");
            CHOOSER.getExtensionFilters().add(FX_FILTER);
            CHOOSER.setInitialDirectory(startingDirectory.toFile());
            if (startingFile != null) {
                CHOOSER.setInitialFileName(startingFile.getFileName().toString());
            }

            while (true) {
                File selected = CHOOSER.showOpenDialog(null);

                if (selected == null) {
                    LOG.fine("User cancelled.");
                    return null;
                } else if (!validateSavegame(selected.toPath())) {
                    final String MSG = "That does not seem to be a valid savegame.";
                    JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE);
                } else {
                    return setPreviousSave(selected.toPath());
                }
            }
        } else {
            final JFileChooser CHOOSER = new JFileChooser();
            CHOOSER.setMultiSelectionEnabled(false);
            CHOOSER.setDialogTitle("Select savefile:");
            CHOOSER.getActionMap().get("viewTypeDetails").actionPerformed(null);
            CHOOSER.setFileFilter(Game.Companion.getFILTER_ALL());
            if (startingFile != null) {
                CHOOSER.setSelectedFile(startingFile.toFile());
            } else {
                CHOOSER.setCurrentDirectory(startingDirectory.toFile());
            }

            while (true) {
                int result = CHOOSER.showOpenDialog(parent);
                File selected = CHOOSER.getSelectedFile();

                if (result == JFileChooser.CANCEL_OPTION || null == selected) {
                    LOG.fine("User cancelled.");
                    return null;
                } else if (!validateSavegame(selected.toPath())) {
                    final String MSG = "That does not seem to be a valid savegame.";
                    JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE);
                } else {
                    return setPreviousSave(selected.toPath());
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
     * @return A <code>File</code> pointing to the selected ModOrganizer ini
     * file, or <code>null</code> if a file was not selected.
     */
    static public Path selectMO2Ini(SaveWindow parent, Game game) {
        LOG.info("Choosing the ModOrganizer path.");

        final JFileChooser CHOOSER = new JFileChooser();
        CHOOSER.setDialogTitle("Locate ModOrganizer.ini");
        CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        CHOOSER.setMultiSelectionEnabled(false);

        if (validateMODir(getMO2Ini(game))) {
            LOG.fine("Choosing a ModOrganizer path: trying the pre-existing path.");
            CHOOSER.setSelectedFile(getMO2Ini(game).toFile());
        } else {
            CHOOSER.setCurrentDirectory(MO2ROOT.toFile());
        }

        for (;;) {
            loadChooserPrefs(CHOOSER);
            int result = CHOOSER.showDialog(parent, "Select");
            java.io.File file = CHOOSER.getSelectedFile();
            saveChooserPrefs(CHOOSER);

            if (null == file || result == JFileChooser.CANCEL_OPTION) {
                LOG.fine("User cancelled.");
                return null;
            } else if (!validateMO2Ini(game, file.toPath())) {
                if (!Files.exists(file.toPath())) {
                    final String MSG = String.format("That file doesn't exist:\n%s", file);
                    JOptionPane.showMessageDialog(parent, MSG, "Doesn't Exist", JOptionPane.ERROR_MESSAGE);
                } else if (!Files.isReadable(file.toPath())) {
                    final String MSG = String.format("That file isn't readable:\n%s", file);
                    JOptionPane.showMessageDialog(parent, MSG, "Not readable", JOptionPane.ERROR_MESSAGE);
                } else {
                    final String MSG = String.format("That directory doesn't seem to contain Mod Organizer:\n%s", file);
                    JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                return setMO2Ini(game, file.toPath());
            }
        }
    }

    /**
     * Shows a file chooser dialog to allow the user to select where a game is
     * located.
     *
     * @param parent The parent component.
     * @param game The game whose directory should be selected.
     * @return A <code>File</code> pointing to the selected game directory, or
     * <code>null</code> if a directory was not selected.
     */
    static public Path selectGameDirectory(SaveWindow parent, Game game) {
        LOG.info(String.format("Choosing the %s directory.", game));

        final JFileChooser CHOOSER = new JFileChooser();
        CHOOSER.setDialogTitle(String.format("Select %s directory", game.getNAME()));
        CHOOSER.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        CHOOSER.setMultiSelectionEnabled(false);

        final Path PREV_DIR = getGameDirectory(game);
        if (Configurator.validateGameDirectory(game, PREV_DIR)) {
            LOG.fine("Trying to use the stored value for the game directory.");
            CHOOSER.setSelectedFile(PREV_DIR.toFile());
        }

        for (;;) {
            loadChooserPrefs(CHOOSER);
            int result = CHOOSER.showOpenDialog(parent);
            java.io.File file = CHOOSER.getSelectedFile();
            saveChooserPrefs(CHOOSER);

            if (null == file || result == JFileChooser.CANCEL_OPTION) {
                return null;
            } else if (!validateGameDirectory(game, file.toPath())) {
                final String MSG = String.format("This directory doesn't seem to be the %s directory.", game.getNAME());
                JOptionPane.showMessageDialog(parent, MSG, "Invalid", JOptionPane.ERROR_MESSAGE);
            } else {
                return setGameDirectory(game, file.toPath());
            }
        }
    }

    /**
     * Shows a file chooser dialog to allow the user to select the watch
     * directory.
     *
     * @param parent The parent component.
     * @param game The game whose directory should be selected.
     * @return A <code>Path</code> pointing to the savefile directory, or
     * <code>null</code> if a directory was not selected.
     */
    static public Path selectSavefileDirectory(SaveWindow parent, Game game) {
        LOG.info("Choosing a directory to watch.");

        final JFileChooser CHOOSER = new JFileChooser();
        CHOOSER.setDialogTitle("Select folder to watch");
        CHOOSER.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        CHOOSER.setMultiSelectionEnabled(false);

        Path previousDir = getSaveDirectory(game);

        if (Configurator.validDir(previousDir)) {
            LOG.fine("Trying to use the stored value for the savefile directory.");
            CHOOSER.setSelectedFile(previousDir.toFile());
        } else {
            CHOOSER.setSelectedFile(MO2ROOT.toFile());
        }

        for (;;) {
            loadChooserPrefs(CHOOSER);
            int result = CHOOSER.showOpenDialog(parent);
            java.io.File file = CHOOSER.getSelectedFile();
            saveChooserPrefs(CHOOSER);

            if (null == file || result == JFileChooser.CANCEL_OPTION) {
                LOG.fine("User cancelled.");
                return null;
            } else if (Files.exists(file.toPath()) && !Files.isReadable(file.toPath())) {
                final String MSG = String.format("That directory isn't readable:\n%s", file);
                JOptionPane.showMessageDialog(parent, MSG, "Not Readable", JOptionPane.ERROR_MESSAGE);
            } else {
                return setSaveDirectory(game, file.toPath());
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
    static public boolean validateMODir(Path dir) {
        return validDir(dir) && Files.exists(dir.resolve("mods"));
    }

    /**
     * Validates a directory, checking if it contains a valid installation of a
     * game.
     *
     * @param game The game to check for.
     * @param dir The directory to validate.
     * @return True if the directory contains the game, false otherwise.
     */
    static public boolean validateGameDirectory(Game game, Path dir) {
        return validDir(dir) && dir.getFileName().equals(game.getGAME_DIRECTORY()) && Files.exists(dir.resolve(game.getEXECUTABLE()));
    }

    /**
     * Validates a file, checking if it is a savefile. In practice this just
     * means that is a file, it exists, it is readable, and it has the "ESS" or
     * "FOS" extension.
     *
     * @param path The file to validate.
     * @return True if the file is probably a savefile.
     */
    static public boolean validateSavegame(Path path) {
        return validFile(path) && Game.Companion.getFILTER_ALL().accept(path.toFile());
    }

    /**
     * Validates an ini, checking if it contains a valid installation of
     * ModOrganizer.
     *
     * @param mo2Ini The ini file to validate and store.
     */
    static public void storeMO2Ini(Path mo2Ini) {
        if (!validFile(mo2Ini)) {
            return;
        }

        try (final java.util.Scanner SCANNER = new java.util.Scanner(mo2Ini)) {
            final Map<String, String> TOKENS = new java.util.TreeMap<>();

            while (SCANNER.hasNextLine()) {
                final String TOKEN = SCANNER.nextLine();
                Matcher MATCHER = KEY_VALUE.matcher(TOKEN);
                if (MATCHER.find()) {
                    final String KEY = MATCHER.group(1).toLowerCase();
                    final String VALUE = getFirst(MATCHER.group(2), MATCHER.group(3));
                    TOKENS.put(KEY, VALUE);
                }
            }

            final String GAME_NAME = TOKENS.get("gamename");
            final String GAME_DIR = TOKENS.get("gamepath");
            final Path GAME_PATH = Paths.get(GAME_DIR);
            LOG.info(String.format("Scanned %s", mo2Ini));
            LOG.info(String.format("GameName=%s", GAME_NAME));

            if (!Files.exists(GAME_PATH)) {
                LOG.warning(String.format("Directory %s missing.", GAME_PATH));
                return;
            }

            try {
                final Game GAME = Game.valueOf(GAME_NAME);
                setMO2Ini(GAME, mo2Ini);
                JOptionPane.showMessageDialog(null, "Stored MO2 ini file for " + GAME.getNAME(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IllegalArgumentException ex) {
            }

            Game.Companion.getVALUES().stream()
                    .filter(game -> GAME_PATH.endsWith(game.getGAME_DIRECTORY()))
                    .findFirst()
                    .ifPresent(game -> {
                        setMO2Ini(game, mo2Ini);
                        JOptionPane.showMessageDialog(null, "Stored MO2 ini file for " + game.getNAME(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    });

        } catch (IOException | RuntimeException ex) {
            LOG.log(Level.WARNING, "Problem while parsing MO2 ini file.", ex);
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
    static public boolean validateMO2Ini(Game game, Path mo2Ini) {
        if (!validFile(mo2Ini)) {
            return false;
        }

        try (final java.util.Scanner SCANNER = new java.util.Scanner(mo2Ini)) {
            final Map<String, String> TOKENS = new java.util.TreeMap<>();

            while (SCANNER.hasNext()) {
                final String TOKEN = SCANNER.next();
                Matcher MATCHER = KEY_VALUE.matcher(TOKEN);
                if (MATCHER.find()) {
                    final String KEY = MATCHER.group(1).toLowerCase();
                    final String VALUE = getFirst(MATCHER.group(2), MATCHER.group(3));
                    TOKENS.put(KEY, VALUE);
                }
            }

            final String GAME_NAME = TOKENS.get("gamename");
            final String PROFILE_NAME = TOKENS.get("selected_profile");
            final String BASEDIR_NAME = TOKENS.get("base_directory");

            final Path BASEDIR = BASEDIR_NAME == null
                    ? mo2Ini.getParent()
                    : Paths.get(BASEDIR_NAME);

            final Path MODS = BASEDIR.resolve("mods");
            final Path PROFILES = BASEDIR.resolve("profiles");
            final Path PROFILE = PROFILES.resolve(PROFILE_NAME);

            LOG.info(String.format("Scanned %s", mo2Ini));
            LOG.info(String.format("GameName=%s", GAME_NAME));
            LOG.info(String.format("selected_profile=%s", PROFILE_NAME));
            LOG.info(String.format("base_directory=%s", BASEDIR_NAME));

            if (!Files.exists(MODS)) {
                LOG.warning(String.format("Directory %s missing.", MODS));
                return false;
            } else if (!Files.exists(PROFILES)) {
                LOG.warning(String.format("Directory %s missing.", PROFILES));
                return false;
            } else if (!Files.exists(PROFILE)) {
                LOG.warning(String.format("Directory %s missing.", PROFILE));
                return false;
            } else {
                return Configurator.validateMODir(BASEDIR);
            }

        } catch (IOException | RuntimeException ex) {
            LOG.log(Level.WARNING, "Problem while parsing MO2 ini file.", ex);
            return false;
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
     *
     */
    static public List<Mod> analyzeModOrganizer2(Game game, Path mo2Ini) {
        try (final java.util.Scanner SCANNER = new java.util.Scanner(mo2Ini)) {
            final Map<String, String> TOKENS = new java.util.TreeMap<>();

            while (SCANNER.hasNext()) {
                final String TOKEN = SCANNER.next();
                Matcher MATCHER = KEY_VALUE.matcher(TOKEN);
                if (MATCHER.find()) {
                    final String KEY = MATCHER.group(1).toLowerCase();
                    final String VALUE = getFirst(MATCHER.group(2), MATCHER.group(3));
                    TOKENS.put(KEY, VALUE);
                }
            }

            final String GAME_NAME = TOKENS.get("gamename");
            final String PROFILE_NAME = TOKENS.get("selected_profile");
            final String BASEDIR_NAME = TOKENS.get("base_directory");

            final Path BASEDIR = BASEDIR_NAME == null
                    ? mo2Ini.getParent()
                    : Paths.get(BASEDIR_NAME);

            final Path MODS = BASEDIR.resolve("mods");
            final Path PROFILES = BASEDIR.resolve("profiles");
            final Path PROFILE = PROFILES.resolve(PROFILE_NAME);

            LOG.info(String.format("Scanned %s", mo2Ini));
            LOG.info(String.format("GameName=%s", GAME_NAME));
            LOG.info(String.format("selected_profile=%s", PROFILE_NAME));
            LOG.info(String.format("base_directory=%s", BASEDIR_NAME));

            if (!Files.exists(MODS)) {
                LOG.warning(String.format("Directory %s missing.", MODS));
                return null;
            } else if (!Files.exists(PROFILES)) {
                LOG.warning(String.format("Directory %s missing.", PROFILES));
                return null;
            } else if (!Files.exists(PROFILE)) {
                LOG.warning(String.format("Directory %s missing.", PROFILE));
                return null;
            } else {
                return Configurator.analyzeModDirectory(game, PROFILE, MODS);
            }

        } catch (IOException | RuntimeException ex) {
            LOG.log(Level.WARNING, "Problem while parsing MO2 ini file.", ex);
            return null;
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
     *
     */
    static public List<Mod> analyzeModDirectory(Game game, Path profile, Path modDir) {
        LOG.info("Attempting to analyze the Mod Organizer directory.");

        try {
            final Path MOD_LIST = profile.resolve(Configurator.MODLIST_PATH);

            LOG.info("Reading the profile's \"ModList.txt\".");
            final List<String> MODNAMES = new java.util.LinkedList<>();

            try (BufferedReader input = Files.newBufferedReader(MOD_LIST)) {
                LOG.fine("Reading from \"ModList.txt\".");

                while (input.ready()) {
                    String line = input.readLine();
                    Matcher matcher = MODLIST_REGEX.matcher(line);
                    if (matcher.matches()) {
                        if (matcher.group(1).equals("+")) {
                            MODNAMES.add(matcher.group(2));
                        }
                    }
                }

                LOG.fine(String.format("\"ModList.txt\" contained %d mod names.", MODNAMES.size()));
            }

            final List<Mod> MODS = MODNAMES.parallelStream()
                    .map(modDir::resolve)
                    .map(path -> Mod.createMod(game, path))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            LOG.info(String.format("analyzeModDirectory: checked %d mods.", MODS.size()));
            return MODS;

        } catch (IOException ex) {
            LOG.severe("Something went wrong while analyzing ModOrganizer. Let's not stress about what it was, let's just fail and return.");
            return null;
        }
    }

    /**
     *
     * @param chooser
     */
    static private void saveChooserPrefs(JFileChooser chooser) {
        PREFS.putInt("chooserWidth", chooser.getSize().width);
        PREFS.putInt("chooserHeight", chooser.getSize().height);
        PREFS.putInt("chooserX", chooser.getLocation().x);
        PREFS.putInt("chooserY", chooser.getLocation().y);
    }

    /**
     *
     * @param chooser
     */
    static private void loadChooserPrefs(JFileChooser chooser) {
        int width = PREFS.getInt("chooserWidth", chooser.getSize().width);
        int height = PREFS.getInt("chooserHeight", chooser.getSize().height);
        int x = PREFS.getInt("chooserX", chooser.getLocation().x);
        int y = PREFS.getInt("chooserY", chooser.getLocation().y);
        chooser.setSize(width, height);
        chooser.setLocation(x, y);
    }

    /**
     * Getter for a game's directory field.
     *
     * @param game The game.
     * @return The directory.
     */
    static Path getGameDirectory(Game game) {
        final String KEY = game.getNAME() + "_directory";
        String path = PREFS.get(KEY, "");
        return path.isEmpty() ? null : Paths.get(path);
    }

    /**
     * Setter for the a game's directory field.
     *
     * @param game The game.
     * @param dir The new directory.
     * @return The specified <code>Path</code>.
     */
    static Path setGameDirectory(Game game, Path dir) {
        final String KEY = game.getNAME() + "_directory";
        if (dir == null) {
            PREFS.remove(KEY);
        } else {
            PREFS.put(KEY, dir.toString());
        }
        return dir;
    }

    /**
     * Getter for the mod organizer ini field.
     *
     * @param game The game whose MO ini file should be stored.
     * @return The ini file.
     */
    static Path getMO2Ini(Game game) {
        final Path gameDir = MO2ROOT.resolve(game.getNAME());
        final Path iniFile = gameDir.resolve("ModOrganizer.ini");

        Path defPath = iniFile.getParent();
        while (!Files.exists(defPath) && defPath.getNameCount() > 0) {
            defPath = defPath.getParent();
        }

        String path = PREFS.get("modOrganizerIni_" + game, defPath.toString());
        if (path.isEmpty()) {
            return null;
        }

        return Paths.get(path);
    }

    /**
     * Setter for the mod organizer ini field.
     *
     * @param game The game whose MO ini file should be stored.
     * @param file The new ini file.
     * @return The specified <code>Path</code>.
     */
    static Path setMO2Ini(Game game, Path file) {
        final String KEY = "modOrganizerIni_" + game;
        if (file == null) {
            PREFS.remove(KEY);
        } else {
            PREFS.put(KEY, file.toString());
        }
        return file;
    }

    /**
     * Getter for the mod organizer ini field.
     *
     * @param game The game whose MO ini file should be stored.
     * @return The ini file.
     */
    static Path getSaveDirectory(Game game) {
        final Path DEFAULT = MYGAMES.resolve(game.getSAVE_DIRECTORY());
        Path STORED = Paths.get(PREFS.get("saveDirectory_" + game, DEFAULT.toString()));
        if (validDir(STORED)) {
            return STORED;
        } else if (validDir(DEFAULT)) {
            return DEFAULT;
        } else {
            return MYGAMES;
        }
    }

    /**
     * Setter for the mod organizer ini field.
     *
     * @param game The game whose MO ini file should be stored.
     * @param file The new ini file.
     * @return The specified <code>Path</code>.
     */
    static Path setSaveDirectory(Game game, Path file) {
        final String KEY = "saveDirectory_" + game;
        if (file == null) {
            PREFS.remove(KEY);
        } else {
            PREFS.put(KEY, file.toString());
        }
        return file;
    }

    /**
     * Getter for the previous save field.
     *
     * @return The file.
     */
    public static Path getPreviousSave() {
        String path = PREFS.get("previousSave", "");
        if (path.isEmpty()) {
            return null;
        }

        return Paths.get(path);
    }

    /**
     * Setter for the previous save field.
     *
     * @param file The new file.
     * @return The specified <code>Path</code>.
     */
    static Path setPreviousSave(Path file) {
        final String KEY = "previousSave";
        if (file == null) {
            PREFS.remove(KEY);
        } else {
            PREFS.put(KEY, file.toString());
        }
        return file;
    }

    /**
     * Getter for the previous plugins export field.
     *
     * @return The file.
     */
    static Path getPreviousPluginsExport() {
        String path = PREFS.get("previousPluginsExport", "");
        if (path.isEmpty()) {
            return null;
        }

        return Paths.get(path);
    }

    /**
     * Setter for the previous plugins export field.
     *
     * @param file The new file.
     * @return The specified <code>Path</code>.
     */
    static Path setPreviousPluginsExport(Path file) {
        final String KEY = "previousPluginsExport";
        if (file == null) {
            PREFS.remove(KEY);
        } else {
            PREFS.put(KEY, file.toString());
        }
        return file;
    }

    /**
     * Test is a path refers to a file that exists and is readable.
     *
     * @param path
     * @return
     */
    static public boolean validFile(Path path) {
        if (null == path) {
            LOG.info("validFile check: null.");
            return false;
        } else if (!Files.isRegularFile(path)) {
            LOG.info("validFile check: irregular.");
            return false;
        } else if (!Files.isReadable(path)) {
            LOG.info("validFile check: unreadable.");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test is a path refers to a directory that exists and is readable.
     *
     * @param path
     * @return
     */
    static public boolean validDir(Path path) {
        if (null == path) {
            return false;
        }
        return Files.isDirectory(path) && Files.exists(path) && Files.isReadable(path);
    }

    /**
     * Test is a path refers to a file can be written.
     *
     * @param path
     * @return
     */
    static public boolean validWrite(Path path) {
        if (null == path) {
            return false;
        } else if (Files.exists(path) && !Files.isRegularFile(path)) {
            return false;
        } else if (Files.exists(path) && !Files.isWritable(path)) {
            return false;
        } else {
            return Files.isWritable(path.getParent());
        }
    }

    /**
     *
     * @param items
     * @return
     */
    static public Path getFirst(Path... items) {
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .filter(Files::exists)
                .findFirst().orElse(null);
    }

    /**
     *
     * @param items
     * @return
     */
    static public String getFirst(String... items) {
        return Arrays.stream(items).filter(Objects::nonNull).findFirst().orElse(null);
    }

    static final private Logger LOG = Logger.getLogger(Configurator.class.getCanonicalName());
    static final String MODS_PATH = "mods";
    static final String PROFILES_PATH = "profiles";
    static final String INI_PATH = "ModOrganizer.ini";
    static final String MODLIST_PATH = "modlist.txt";
    static final String MODLIST_PATTERN = "^([+-])(.+)$";
    static final Pattern MODLIST_REGEX = Pattern.compile(MODLIST_PATTERN);

    static final public PathMatcher GLOB_INI = FileSystems.getDefault().getPathMatcher("glob:**.ini");

    static private final FileNameExtensionFilter TEXTFILES = new FileNameExtensionFilter("Text file", "txt");
    static final private java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(resaver.ReSaver.class);
    static final private Pattern KEY_VALUE = Pattern.compile("^(.+)=(?:@ByteArray\\((.+)\\)|(.+))$", Pattern.CASE_INSENSITIVE);
    static private final Path MO2ROOT = Paths.get(System.getProperty("user.home"), "appData", "local", "ModOrganizer");
    static private final Path MYGAMES = new JFileChooser().getFileSystemView().getDefaultDirectory().toPath().resolve("My Games");

}
