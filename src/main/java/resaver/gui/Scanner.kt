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

import mf.Counter
import mf.Timer.Companion.startNew
import resaver.Analysis
import resaver.Mod
import resaver.ResaverFormatting
import resaver.esp.ESP
import resaver.esp.PluginData
import resaver.esp.StringTable
import resaver.esp.StringsFile
import resaver.ess.ESS
import resaver.ess.Plugin
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.nio.channels.ClosedByInterruptException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

/**
 *
 * @author Mark Fairchild
 */
class Scanner(
    window: SaveWindow?,
    save: ESS?,
    gameDir: Path,
    mo2Ini: Path?,
    doAfter: Runnable?,
    progress: Consumer<String>
) : SwingWorker<Analysis?, Double?>() {
    /**
     *
     * @return @throws Exception
     */
    @Throws(Exception::class)
    override fun doInBackground(): Analysis? {
        val TIMER = startNew("Load Plugins")
        val GAME = SAVE.header.GAME
        WINDOW.addWindowListener(LISTENER)
        PROGRESS.accept("Initializing")
        return try {
            val PLUGINS = SAVE.pluginInfo
            LOG.info("Scanning plugins.")
            val MODS: MutableList<Mod> = ArrayList(1024)
            val CORE = Mod(GAME, GAME_DIR.resolve("data"))
            MODS.add(CORE)
            if (null != MO2_INI) {
                PROGRESS.accept("Analyzing MO2")
                LOG.info("Checking Mod Organizer 2.")
                val MOMODS = Configurator.analyzeModOrganizer2(GAME, MO2_INI)!!
                MODS.addAll(MOMODS)
            }
            PROGRESS.accept("Organizing")
            val PLUGINFILEMAP: MutableMap<Plugin?, Path> = HashMap()
            MODS
                .asSequence()
                .flatMap { it.espFiles.asSequence() }
                .filter { PLUGINS.paths.containsKey(it.fileName) }
                .forEach { PLUGINFILEMAP[PLUGINS.paths[it.fileName]] = it }
            val PLUGIN_MOD_MAP: MutableMap<Plugin?, Mod?> = hashMapOf()
            MODS.forEach { MOD ->
                val collect: MutableMap<Plugin?, Mod?> = hashMapOf()
                for (path in MOD.espFiles) {
                    if (PLUGINS.paths.containsKey(path.fileName)) {
                        check(collect.put(PLUGINS.paths[path.fileName], MOD) == null) { "Duplicate key" }
                    }
                }
                PLUGIN_MOD_MAP.putAll(collect)
            }
            assert(GAME != null)
            val LANGUAGE = if (GAME!!.isSkyrim) "english" else "en"

            // Analyze scripts from mods.
            var PROFILEANALYSIS = Mod.Analysis()
            MODS
                .asSequence()
                .map { it.analysis }
                .forEach { PROFILEANALYSIS = PROFILEANALYSIS.merge(it) }
            val ERR_ARCHIVE: MutableList<Path> = mutableListOf()
            val ERR_SCRIPTS: MutableList<Path> = mutableListOf()
            val ERR_STRINGS: MutableList<Path> = mutableListOf()
            val STRINGSFILES: MutableList<StringsFile> = mutableListOf()
            val SCRIPT_ORIGINS: MutableMap<Path, Path> = mutableMapOf()

            // Read StringsFiles and scripts.
            val COUNTER = Counter(MODS.size)
            MODS.forEach { mod ->
                if (mod === CORE) {
                    COUNTER.click()
                    PROGRESS.accept(String.format("Reading %s's data", GAME.NAME))
                } else {
                    PROGRESS.accept(String.format("Reading %s: mod data for %s", COUNTER.eval(), mod.shortName))
                }
                val RESULTS = mod.readData(PLUGINS, LANGUAGE)
                STRINGSFILES.addAll(RESULTS.STRINGSFILES)
                SCRIPT_ORIGINS.putAll(RESULTS.SCRIPT_ORIGINS)
                ERR_ARCHIVE.addAll(RESULTS.ARCHIVE_ERRORS)
                ERR_SCRIPTS.addAll(RESULTS.SCRIPT_ERRORS)
                ERR_STRINGS.addAll(RESULTS.STRINGS_ERRORS)
                RESULTS.errorFiles.forEach { v: Path? ->
                    val MSG = String.format("Couldn't read %s from %s.", v, mod)
                    LOG.warning(MSG)
                }
            }
            PROGRESS.accept("Combine StringsFiles")

            // Map plugins to their stringsfiles.
            val PLUGIN_STRINGS: MutableMap<Plugin, MutableList<StringsFile>> = hashMapOf()
            STRINGSFILES.forEach { stringsFile ->
                PLUGIN_STRINGS.getOrPut(stringsFile.PLUGIN) { arrayListOf() }
                    .add(stringsFile)
            }

            // The master stringtable.
            val STRINGTABLE = StringTable()
            PLUGINS.stream()
                .filter { key1: Plugin -> PLUGIN_STRINGS.containsKey(key1) }
                .forEach { plugin: Plugin -> STRINGTABLE.populateFromFiles(PLUGIN_STRINGS[plugin], plugin) }

            // Create the database for plugin data.
            val ERR_PLUGINS = Collections.synchronizedList(LinkedList<Path>())
            val PLUGIN_DATA: MutableMap<Plugin?, PluginData> = hashMapOf()
            val SIZES: MutableMap<Plugin, Long> = hashMapOf()
            COUNTER.reset(PLUGINS.size)
            PLUGINS.allPlugins.forEach { plugin ->
                PROGRESS.accept(String.format("Parsing %s: %s", COUNTER.eval(), plugin.indexName()))
                if (!PLUGINFILEMAP.containsKey(plugin)) {
                    ERR_PLUGINS.add(Paths.get(plugin.NAME))
                    LOG.info(String.format("Plugin %s could not be found.", plugin))
                } else {
                    try {
                        val INFO = ESP.skimPlugin(PLUGINFILEMAP[plugin], GAME, plugin, PLUGINS)
                        PLUGIN_DATA[plugin] = INFO
                        LOG.info(
                            String.format(
                                "Scanned plugin: %6d names and %5.1f kb script data from %s",
                                INFO.nameCount,
                                INFO.scriptDataSize / 1024.0f,
                                plugin.indexName()
                            )
                        )
                        assert(INFO.scriptDataSize >= 0)
                        SIZES[plugin] = INFO.scriptDataSize
                    } catch (ex: ClosedByInterruptException) {
                        throw ex
                    } catch (ex: RuntimeException) {
                        ERR_PLUGINS.add(Paths.get(plugin.NAME))
                        LOG.log(Level.WARNING, String.format("Error reading plugin: %s.", plugin.indexName()), ex)
                        ex.printStackTrace(System.err)
                    } catch (ex: IOException) {
                        ERR_PLUGINS.add(Paths.get(plugin.NAME))
                        LOG.log(Level.WARNING, String.format("Error reading plugin: %s.", plugin.indexName()), ex)
                        ex.printStackTrace(System.err)
                    }
                }
            }
            PROGRESS.accept("Creating analysis")
            val ANALYSIS = Analysis(PROFILEANALYSIS, PLUGIN_DATA, STRINGTABLE)
            WINDOW.setAnalysis(ANALYSIS)
            TIMER.stop()
            LOG.info("Plugin scanning completed, took " + TIMER.formattedTime)

            // Find the worst offenders for script data size.
            val toSort: MutableList<Map.Entry<Plugin, Long>> = mutableListOf()
            for (pluginLongEntry in SIZES.entries) {
                toSort.add(pluginLongEntry)
            }
            toSort.sortWith { (_, value), (_, value1) ->
                (value1).compareTo(value)
            }
            val OFFENDERS: MutableList<Plugin> = mutableListOf()
            var limit: Long = 3
            for ((key: Plugin) in toSort) {
                if (limit-- == 0L) {
                    break
                }
                OFFENDERS.add(key)
            }
            val BUF = StringBuilder()
            val scriptDataSize = ANALYSIS.scriptDataSize / 1048576.0
            if (scriptDataSize > 32.0) {
                BUF.append("Done scanning plugins.\nTotal script data: ")
                    .append(String.format("%1.2f", scriptDataSize))
                    .append("mb.\nHighest data usage:\n")
                OFFENDERS.forEach { plugin ->
                    val SIZE = SIZES[plugin]!! / 1048576.0
                    BUF.append(String.format("%3.2f mb for %s.\n", SIZE, plugin))
                }
            }
            val NAMER = Function<Path, CharSequence> { p: Path -> p.fileName.toString() }
            if (ERR_PLUGINS.size == 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d plugin could not be read.", ERR_PLUGINS, 10, NAMER))
            } else if (ERR_PLUGINS.size > 1) {
                BUF.append(ResaverFormatting.makeTextList("\n\n%d plugins could not be read.", ERR_PLUGINS, 10, NAMER))
            }
            if (ERR_ARCHIVE.size == 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\n%d Archive file could not be read.",
                        ERR_ARCHIVE,
                        3,
                        NAMER
                    )
                )
            } else if (ERR_ARCHIVE.size > 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\n%d Archive files could not be read.",
                        ERR_ARCHIVE,
                        3,
                        NAMER
                    )
                )
            }
            if (ERR_STRINGS.size == 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\nOne Strings file could not be read.",
                        ERR_STRINGS,
                        3,
                        NAMER
                    )
                )
            } else if (ERR_STRINGS.size > 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\n%d Strings files could not be read.",
                        ERR_STRINGS,
                        3,
                        NAMER
                    )
                )
            }
            if (ERR_SCRIPTS.size == 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\n%d Script file could not be read.",
                        ERR_SCRIPTS,
                        3,
                        NAMER
                    )
                )
            } else if (ERR_SCRIPTS.size > 1) {
                BUF.append(
                    ResaverFormatting.makeTextList(
                        "\n\n%d Script files could not be read.",
                        ERR_SCRIPTS,
                        3,
                        NAMER
                    )
                )
            }
            if (BUF.isNotEmpty()) {
                JOptionPane.showMessageDialog(WINDOW, BUF.toString(), "Done", JOptionPane.INFORMATION_MESSAGE)
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
             */ANALYSIS
        } catch (ex: ClosedByInterruptException) {
            LOG.severe("Parsing terminated.")
            null
        } catch (ex: Exception) {
            val MSG = "Error reading plugins. ${ex.message}"
            LOG.log(Level.SEVERE, MSG, ex)
            JOptionPane.showMessageDialog(WINDOW, MSG, "Read Error", JOptionPane.ERROR_MESSAGE)
            null
        } catch (ex: Error) {
            val MSG = String.format("Error reading plugins. %s", ex.message)
            LOG.log(Level.SEVERE, MSG, ex)
            JOptionPane.showMessageDialog(WINDOW, MSG, "Read Error", JOptionPane.ERROR_MESSAGE)
            null
        } finally {
            WINDOW.removeWindowListener(LISTENER)
            if (DOAFTER != null) {
                SwingUtilities.invokeLater(DOAFTER)
            }
        }
    }

    private val WINDOW: SaveWindow = Objects.requireNonNull(window, "The window field must not be null.")!!
    private val SAVE: ESS = Objects.requireNonNull(save, "The save field must not be null.")!!
    private val GAME_DIR: Path = Objects.requireNonNull(gameDir, "The game directory field must not be null.")
    private val MO2_INI: Path? = mo2Ini
    private val DOAFTER: Runnable? = doAfter
    private val PROGRESS: Consumer<String> = Objects.requireNonNull(progress)
    private val LISTENER: WindowAdapter = object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            if (!isDone) {
                cancel(true)
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(Scanner::class.java.canonicalName)
    }

}