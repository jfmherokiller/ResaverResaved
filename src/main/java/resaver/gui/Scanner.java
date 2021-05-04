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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import resaver.Game;
import resaver.Mod;
import resaver.ResaverFormatting;
import resaver.esp.ESP;
import resaver.esp.PluginData;
import resaver.esp.StringsFile;
import resaver.ess.ESS;
import resaver.ess.Plugin;
import resaver.ess.PluginInfo;

/**
 *
 * @author Mark Fairchild
 */
public class Scanner extends SwingWorker<resaver.Analysis, Double> {

    /**
     *
     * @param window
     * @param save
     * @param gameDir
     * @param mo2Ini
     * @param doAfter
     * @param progress
     */
    public Scanner(SaveWindow window, ESS save, Path gameDir, Path mo2Ini, Runnable doAfter, Consumer<String> progress) {
        this.WINDOW = Objects.requireNonNull(window, "The window field must not be null.");
        this.SAVE = Objects.requireNonNull(save, "The save field must not be null.");
        this.GAME_DIR = Objects.requireNonNull(gameDir, "The game directory field must not be null.");
        this.MO2_INI = mo2Ini;
        this.DOAFTER = doAfter;
        this.PROGRESS = Objects.requireNonNull(progress);
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    protected resaver.Analysis doInBackground() throws Exception {
        final mf.Timer TIMER = mf.Timer.startNew("Load Plugins");
        final Game GAME = this.SAVE.getHeader().GAME;
        this.WINDOW.addWindowListener(this.LISTENER);
        this.PROGRESS.accept("Initializing");

        try {

            final PluginInfo PLUGINS = this.SAVE.getPluginInfo();
            LOG.info("Scanning plugins.");

            final List<Mod> MODS = new ArrayList<>(1024);
            final Mod CORE = new Mod(GAME, GAME_DIR.resolve("data"));
            MODS.add(CORE);

            if (null != this.MO2_INI) {
                this.PROGRESS.accept("Analyzing MO2");
                LOG.info("Checking Mod Organizer 2.");
                final java.util.List<Mod> MOMODS = Configurator.analyzeModOrganizer2(GAME, this.MO2_INI);
                MODS.addAll(MOMODS);
            }

            this.PROGRESS.accept("Organizing");

            final Map<Plugin, Path> PLUGINFILEMAP = MODS.stream()
                    .flatMap(mod -> mod.getESPFiles().stream())
                    .filter(path -> PLUGINS.getPaths().containsKey(path.getFileName()))
                    .collect(Collectors.toMap(
                            path -> PLUGINS.getPaths().get(path.getFileName()),
                            path -> path,
                            (p1, p2) -> p2));

            final Map<Plugin, Mod> PLUGIN_MOD_MAP = new HashMap<>();

            MODS.stream().map(mod -> mod.getESPFiles().stream()
                    .filter(path -> PLUGINS.getPaths().containsKey(path.getFileName()))
                    .collect(Collectors.toMap(
                            path -> PLUGINS.getPaths().get(path.getFileName()),
                            path -> mod)))
                    .forEach(PLUGIN_MOD_MAP::putAll);

            // The language. Eventually make this selectable?
            final String LANGUAGE = (GAME.isSkyrim() ? "english" : "en");

            // Analyze scripts from mods. 
            final Mod.Analysis PROFILEANALYSIS = MODS.stream()
                    .map(Mod::getAnalysis)
                    .reduce(new Mod.Analysis(), Mod.Analysis::merge);

            final List<Path> ERR_ARCHIVE = new LinkedList<>();
            final List<Path> ERR_SCRIPTS = new LinkedList<>();
            final List<Path> ERR_STRINGS = new LinkedList<>();

            List<StringsFile> STRINGSFILES = new ArrayList<>();
            Map<Path, Path> SCRIPT_ORIGINS = new LinkedHashMap<>();

            // Read StringsFiles and scripts.
            final mf.Counter COUNTER = new mf.Counter(MODS.size());

            for (Mod mod : MODS) {
                if (mod == CORE) {
                    COUNTER.click();
                    this.PROGRESS.accept(String.format("Reading %s's data", GAME.getNAME()));
                } else {
                    this.PROGRESS.accept(String.format("Reading %s: mod data for %s", COUNTER.eval(), mod.getShortName()));
                }

                final Mod.ModReadResults RESULTS = mod.readData(PLUGINS, LANGUAGE);
                STRINGSFILES.addAll(RESULTS.STRINGSFILES);
                SCRIPT_ORIGINS.putAll(RESULTS.SCRIPT_ORIGINS);

                ERR_ARCHIVE.addAll(RESULTS.ARCHIVE_ERRORS);
                ERR_SCRIPTS.addAll(RESULTS.SCRIPT_ERRORS);
                ERR_STRINGS.addAll(RESULTS.STRINGS_ERRORS);

                RESULTS.getErrorFiles().forEach(v -> {
                    final String MSG = String.format("Couldn't read %s from %s.", v, mod);
                    LOG.warning(MSG);
                });
            }

            this.PROGRESS.accept("Combine StringsFiles");

            // Map plugins to their stringsfiles.
            Map<Plugin, List<StringsFile>> PLUGIN_STRINGS = STRINGSFILES.stream()
                    .collect(Collectors.groupingBy(stringsFile -> stringsFile.PLUGIN));

            // The master stringtable.
            final resaver.esp.StringTable STRINGTABLE = new resaver.esp.StringTable();
            PLUGINS.stream()
                    .filter(PLUGIN_STRINGS::containsKey)
                    .forEach(plugin -> STRINGTABLE.populateFromFiles(PLUGIN_STRINGS.get(plugin), plugin));

            // Create the database for plugin data.
            final List<Path> ERR_PLUGINS = java.util.Collections.synchronizedList(new java.util.LinkedList<>());

            final Map<Plugin, PluginData> PLUGIN_DATA = new HashMap<>();
            final Map<Plugin, Long> SIZES = new HashMap<>();

            COUNTER.reset(PLUGINS.getSize());

            for (Plugin plugin : PLUGINS.getAllPlugins()) {
                this.PROGRESS.accept(String.format("Parsing %s: %s", COUNTER.eval(), plugin.indexName()));

                if (!PLUGINFILEMAP.containsKey(plugin)) {
                    ERR_PLUGINS.add(Paths.get(plugin.NAME));
                    LOG.info(String.format("Plugin %s could not be found.", plugin));

                } else {
                    try {
                        final PluginData INFO = ESP.skimPlugin(PLUGINFILEMAP.get(plugin), GAME, plugin, PLUGINS);
                        PLUGIN_DATA.put(plugin, INFO);

                        LOG.info(String.format("Scanned plugin: %6d names and %5.1f kb script data from %s", INFO.getNameCount(), INFO.getScriptDataSize() / 1024.0f, plugin.indexName()));
                        assert plugin != null;
                        assert INFO.getScriptDataSize() >= 0;
                        SIZES.put(plugin, INFO.getScriptDataSize());

                    } catch (ClosedByInterruptException ex) {
                        throw ex;
                    } catch (RuntimeException | IOException ex) {
                        ERR_PLUGINS.add(Paths.get(plugin.NAME));
                        LOG.log(Level.WARNING, String.format("Error reading plugin: %s.", plugin.indexName()), ex);
                        ex.printStackTrace(System.err);
                    }
                }
            }

            this.PROGRESS.accept("Creating analysis");

            final resaver.Analysis ANALYSIS = new resaver.Analysis(PROFILEANALYSIS, PLUGIN_DATA, STRINGTABLE);
            if (null != this.SAVE) {
                this.WINDOW.setAnalysis(ANALYSIS);
            }

            TIMER.stop();
            LOG.info(String.format("Plugin scanning completed, took %s", TIMER.getFormattedTime()));

            // Find the worst offenders for script data size.
            final List<Plugin> OFFENDERS = SIZES.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .map(Map.Entry::getKey)
                    .limit(3).collect(Collectors.toList());

            final StringBuilder BUF = new StringBuilder();
            double scriptDataSize = ANALYSIS.getScriptDataSize() / 1048576.0;

            if (scriptDataSize > 32.0) {
                BUF.append("Done scanning plugins.\nTotal script data: ")
                        .append(String.format("%1.2f", scriptDataSize))
                        .append("mb.\nHighest data usage:\n");

                OFFENDERS.stream().forEach(plugin -> {
                    final double SIZE = SIZES.get(plugin) / 1048576.0;
                    BUF.append(String.format("%3.2f mb for %s.\n", SIZE, plugin));
                });
            }

            final java.util.function.Function<Path, CharSequence> NAMER = p -> p.getFileName().toString();

            if (ERR_PLUGINS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d plugin could not be read.", ERR_PLUGINS, 10, NAMER));
            } else if (ERR_PLUGINS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d plugins could not be read.", ERR_PLUGINS, 10, NAMER));
            }

            if (ERR_ARCHIVE.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d Archive file could not be read.", ERR_ARCHIVE, 3, NAMER));
            } else if (ERR_ARCHIVE.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d Archive files could not be read.", ERR_ARCHIVE, 3, NAMER));
            }

            if (ERR_STRINGS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\nOne Strings file could not be read.", ERR_STRINGS, 3, NAMER));
            } else if (ERR_STRINGS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d Strings files could not be read.", ERR_STRINGS, 3, NAMER));
            }

            if (ERR_SCRIPTS.size() == 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d Script file could not be read.", ERR_SCRIPTS, 3, NAMER));
            } else if (ERR_SCRIPTS.size() > 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d Script files could not be read.", ERR_SCRIPTS, 3, NAMER));
            }

            if (BUF.length() > 0) {
                JOptionPane.showMessageDialog(this.WINDOW, BUF.toString(), "Done", JOptionPane.INFORMATION_MESSAGE);
            }

            /*
            final DefaultMutableTreeNode TREE_ROOT = new DefaultMutableTreeNode("Script Conflicts", true);
            final DefaultTreeModel TREE_MODEL = new DefaultTreeModel(TREE_ROOT, true);
            final SortedMap<IString, SortedSet<String>> CONFLICTS = new TreeMap<>(ANALYSIS.SCRIPT_ORIGINS);
            CONFLICTS.forEach((scriptName, modNames) -> {
                if (modNames.size() > 1) {
                    final DefaultMutableTreeNode SCRIPT_NODE = new DefaultMutableTreeNode(scriptName, true);
                    modNames.stream()
                            .map(modName -> new DefaultMutableTreeNode(modName, false))
                            .forEach(modNode -> SCRIPT_NODE.add(modNode));
                    TREE_ROOT.add(SCRIPT_NODE);
                }
            });

            final JDialog RESULTS = new JDialog(this.WINDOW, "Results", true);
            final JScrollPane SCROLLER = new JScrollPane(new JTree(TREE_MODEL));
            RESULTS.setContentPane(SCROLLER);
            RESULTS.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            RESULTS.pack();
            RESULTS.setVisible(true);
             */
            return ANALYSIS;

        } catch (ClosedByInterruptException ex) {
            LOG.severe("Parsing terminated.");
            return null;

        } catch (Exception | Error ex) {
            final String MSG = String.format("Error reading plugins. %s", ex.getMessage());
            LOG.log(Level.SEVERE, MSG, ex);
            JOptionPane.showMessageDialog(this.WINDOW, MSG, "Read Error", JOptionPane.ERROR_MESSAGE);
            return null;

        } finally {
            this.WINDOW.removeWindowListener(this.LISTENER);

            if (this.DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER);
            }

        }
    }

    final private SaveWindow WINDOW;
    final private ESS SAVE;
    final private Path GAME_DIR;
    final private Path MO2_INI;
    final private Runnable DOAFTER;
    final private Consumer<String> PROGRESS;
    static final private Logger LOG = Logger.getLogger(Scanner.class.getCanonicalName());

    final private WindowAdapter LISTENER = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (!isDone()) {
                cancel(true);
            }
        }
    };

}
