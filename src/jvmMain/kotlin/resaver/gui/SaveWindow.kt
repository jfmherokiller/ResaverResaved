/*
 * Copyright 2016 Mark.
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


import PlatformByteBuffer
import ess.*
import ess.ESS.Companion.readESS
import ess.ESS.Companion.verifyIdentical
import ess.papyrus.*
import mf.Duad
import mf.Duad.Companion.make
import mf.JValueMenuItem
import mf.Timer
import mu.KLoggable
import mu.KLogger
import resaver.*
import resaver.gui.AboutDialog.Companion.show
import resaver.gui.AboutDialog.Companion.version
import resaver.gui.AutoCompletion.Companion.enable
import resaver.gui.Configurator.Companion.choosePathModal
import resaver.gui.Configurator.Companion.confirmSaveFile
import resaver.gui.Configurator.Companion.getGameDirectory
import resaver.gui.Configurator.Companion.getMO2Ini
import resaver.gui.Configurator.Companion.selectGameDirectory
import resaver.gui.Configurator.Companion.selectMO2Ini
import resaver.gui.Configurator.Companion.selectNewSaveFile
import resaver.gui.Configurator.Companion.selectPluginsExport
import resaver.gui.Configurator.Companion.selectSaveFile
import resaver.gui.Configurator.Companion.storeMO2Ini
import resaver.gui.Configurator.Companion.validWrite
import resaver.gui.Configurator.Companion.validateGameDirectory
import resaver.gui.Configurator.Companion.validateMO2Ini
import resaver.gui.Configurator.Companion.validateSavegame
import resaver.gui.DataAnalyzer.Companion.showDataAnalyzer
import resaver.gui.FilterMaker.Companion.createFilter
import resaver.pex.AssemblyLevel
import resaver.pex.PexFile.Companion.readScript
import java.awt.*
import java.awt.event.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.function.Predicate
import java.util.logging.Logger
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.plaf.basic.BasicComboBoxRenderer
import javax.swing.text.BadLocationException
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

/**
 * A window that displays savegame data and allows limited editing.
 *
 * @author Mark Fairchild
 */
class SaveWindow(path: Path?, autoParse: Boolean) : JFrame() {
    /**
     * Initialize the swing and AWT components.
     *
     * @param path The `Path` to open.
     * @param autoParse Automatically parse the specified savefile.
     */
    private fun initComponents(path: Path?, autoParse: Boolean) {
        resetTitle(null)
        dropTarget = ReSaverDropTarget { f: Path -> open(f, PREFS.getBoolean("settings.alwaysParse", false)) }
        TREE.addTreeSelectionListener { updateContextInformation() }
        DATASCROLLER.border = BorderFactory.createTitledBorder(DATASCROLLER.border, "Data")
        INFOSCROLLER.border = BorderFactory.createTitledBorder(INFOSCROLLER.border, "Information")
        MAINSPLITTER.resizeWeight = 0.5
        MAINSPLITTER.setDividerLocation(0.5)
        RIGHTSPLITTER!!.resizeWeight = 0.66
        RIGHTSPLITTER.setDividerLocation(0.5)
        MAINPANEL.minimumSize = Dimension(350, 400)
        PLUGINCOMBO.renderer = PluginListCellRenderer()
        PLUGINCOMBO.prototypeDisplayValue = Plugin.PROTOTYPE
        PLUGINCOMBO.toolTipText = "Select a plugin for filtering."
        enable(PLUGINCOMBO)
        enable(MODCOMBO)
        PROGRESSPANEL.add(LBL_MEMORY)
        PROGRESSPANEL.add(LBL_WATCHING)
        PROGRESSPANEL.add(LBL_SCANNING)
        PROGRESSPANEL.add(progressIndicator)
        STATUSPANEL.add(PROGRESSPANEL, BorderLayout.LINE_START)
        STATUSPANEL.add(TREEHISTORY, BorderLayout.LINE_END)
        //reason for this seen here https://web.archive.org/web/20210512224921/https://bugs.openjdk.java.net/browse/JDK-4295814
        TREESCROLLER.viewport.scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE
        FILTERPANEL.add(FILTERFIELD)
        FILTERPANEL.add(PLUGINCOMBO)
        FILTERPANEL.add(BTN_CLEAR_FILTER)
        MODPANEL.add(MODLABEL)
        MODPANEL.add(MODCOMBO)
        MODPANEL.isVisible = false
        MODCOMBO.renderer = ModListCellRenderer()
        TOPPANEL.layout = BoxLayout(TOPPANEL, BoxLayout.Y_AXIS)
        TOPPANEL.add(MODPANEL)
        TOPPANEL.add(FILTERPANEL)
        MAINPANEL.add(TREESCROLLER, BorderLayout.CENTER)
        MAINPANEL.add(TOPPANEL, BorderLayout.PAGE_START)
        MAINPANEL.add(STATUSPANEL, BorderLayout.PAGE_END)
        FILEMENU.add(MI_LOAD)
        FILEMENU.add(MI_SAVE)
        FILEMENU.add(MI_SAVEAS)
        FILEMENU.addSeparator()
        FILEMENU.add(MI_LOADESPS)
        FILEMENU.add(MI_WATCHSAVES)
        FILEMENU.addSeparator()
        FILEMENU.add(MI_EXPORTPLUGINS)
        FILEMENU.addSeparator()
        FILEMENU.add(MI_EXIT)
        FILEMENU.setMnemonic('f')
        CLEANMENU.add(MI_SHOWUNATTACHED)
        CLEANMENU.add(MI_SHOWUNDEFINED)
        CLEANMENU.add(MI_SHOWMEMBERLESS)
        CLEANMENU.add(MI_SHOWCANARIES)
        CLEANMENU.add(MI_SHOWNULLREFS)
        CLEANMENU.add(MI_SHOWNONEXISTENTCREATED)
        CLEANMENU.add(MI_SHOWLONGSTRINGS)
        CLEANMENU.add(MI_SHOWDELETED)
        CLEANMENU.add(MI_SHOWEMPTY)
        CLEANMENU.add(MI_CHANGEFILTER)
        CLEANMENU.add(MI_CHANGEFORMFILTER)
        CLEANMENU.addSeparator()
        CLEANMENU.add(MI_REMOVEUNATTACHED)
        CLEANMENU.add(MI_REMOVEUNDEFINED)
        CLEANMENU.add(MI_RESETHAVOK)
        CLEANMENU.add(MI_CLEANSEFORMLISTS)
        CLEANMENU.add(MI_REMOVENONEXISTENT)
        CLEANMENU.addSeparator()
        CLEANMENU.add(MI_BATCHCLEAN)
        CLEANMENU.add(MI_KILL)
        CLEANMENU.setMnemonic('c')
        OPTIONSMENU.add(MI_USEMO2)
        OPTIONSMENU.add(MI_SHOWMODS)
        OPTIONSMENU.add(MI_SETTINGS)
        OPTIONSMENU.setMnemonic('o')
        DATAMENU.add(MI_LOOKUPID)
        DATAMENU.add(MI_LOOKUPBASE)
        DATAMENU.add(MI_ANALYZE_ARRAYS)
        DATAMENU.add(MI_COMPARETO)
        DATAMENU.setMnemonic('d')
        MI_LOOKUPID.isEnabled = false
        MI_LOOKUPBASE.isEnabled = false
        MI_LOADESPS.isEnabled = false
        HELPMENU.add(MI_SHOWLOG)
        HELPMENU.add(MI_ABOUT)
        HELPMENU.setMnemonic('h')
        MENUBAR.add(FILEMENU)
        MENUBAR.add(CLEANMENU)
        MENUBAR.add(OPTIONSMENU)
        MENUBAR.add(DATAMENU)
        MENUBAR.add(HELPMENU)
        MI_EXIT.addActionListener { exitWithPrompt() }
        MI_LOAD.addActionListener { openWithPrompt() }
        MI_LOADESPS.addActionListener { scanESPs(true) }
        MI_WATCHSAVES.addActionListener {
            PREFS.putBoolean(
                "settings.watch",
                MI_WATCHSAVES.isSelected
            )
        }
        MI_WATCHSAVES.addActionListener { setWatching(MI_WATCHSAVES.isSelected) }
        MI_SAVE.addActionListener { save(false, null) }
        MI_SAVEAS.addActionListener { save(true, null) }
        MI_EXPORTPLUGINS.addActionListener { exportPlugins() }
        MI_SETTINGS.addActionListener { showSettings() }
        MI_SHOWUNATTACHED.addActionListener { updateFilters(false) }
        MI_SHOWUNDEFINED.addActionListener { updateFilters(false) }
        MI_SHOWMEMBERLESS.addActionListener { updateFilters(false) }
        MI_SHOWCANARIES.addActionListener { updateFilters(false) }
        MI_SHOWNULLREFS.addActionListener { updateFilters(false) }
        MI_SHOWNONEXISTENTCREATED.addActionListener { updateFilters(false) }
        MI_SHOWLONGSTRINGS.addActionListener { updateFilters(false) }
        MI_SHOWDELETED.addActionListener { updateFilters(false) }
        MI_SHOWEMPTY.addActionListener { updateFilters(false) }
        MI_CHANGEFILTER.addActionListener { setChangeFlagFilter() }
        MI_CHANGEFORMFILTER!!.addActionListener { setChangeFormFlagFilter() }
        MI_REMOVEUNATTACHED.addActionListener { cleanUnattached() }
        MI_REMOVEUNDEFINED.addActionListener { cleanUndefined() }
        MI_RESETHAVOK.addActionListener { resetHavok() }
        MI_CLEANSEFORMLISTS.addActionListener { cleanseFormLists() }
        MI_REMOVENONEXISTENT.addActionListener { cleanNonexistent() }
        MI_BATCHCLEAN.addActionListener { batchClean() }
        MI_KILL.addActionListener { kill() }
        MI_SHOWMODS.addActionListener { setAnalysis(analysis) }
        MI_SHOWMODS.addActionListener {
            PREFS.putBoolean(
                "settings.showMods",
                MI_SHOWMODS.isSelected
            )
        }
        MI_LOOKUPID.addActionListener { lookupID() }
        MI_LOOKUPBASE.addActionListener { lookupBase() }
        MI_ANALYZE_ARRAYS.addActionListener {
            showDataAnalyzer(
                save!!.papyrus!!.arraysBlock
            )
        }
        MI_COMPARETO.addActionListener { compareTo() }
        MI_SHOWLOG.addActionListener { showLog() }
        MI_ABOUT.addActionListener { show(this) }
        MI_USEMO2.addActionListener { PREFS.putBoolean("settings.useMO2", MI_USEMO2.isSelected) }
        MI_EXIT.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK)
        MI_LOAD.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)
        MI_LOADESPS.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK)
        MI_SAVE.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK)
        MI_SAVEAS.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK)
        MI_BATCHCLEAN.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_DOWN_MASK)
        BTN_CLEAR_FILTER.addActionListener { e: ActionEvent? -> updateFilters(true) }
        MODCOMBO.toolTipText = "Select a mod for filtering."
        LBL_MEMORY.toolTipText = "Current memory usage."
        LBL_WATCHING.toolTipText = "When you save your game, ReSaver will automatcally open the new savefile."
        LBL_SCANNING.toolTipText = "ReSaver is scanning your plugins so that it can add proper names to everything."
        BTN_CLEAR_FILTER.toolTipText = "Clear all filters."
        MI_ABOUT.toolTipText =
            "Shows version information, system information, and an original colour photograph of cats."
        MI_SHOWLOG.toolTipText = "Show ReSaver's internal log. For development purposes only."
        MI_ANALYZE_ARRAYS.toolTipText =
            "Displays the dataAnalyzer for the 'Arrays' section, which hasn't been fully decoded yet. For development purposes only."
        MI_COMPARETO.toolTipText = "Compare the current savefile to another one. For development purposes only."
        MI_CHANGEFILTER.toolTipText =
            "Sets a ChangeFlag filter. ChangeFlags describe what kind of changes are present in ChangeForms."
        MI_CHANGEFORMFILTER.toolTipText =
            "Sets a ChangeFormFlag filter. ChangeFormFlags are part of a ChangeForm and modify the Form's flags. You can examine those flags in xEdit for more information."
        MI_REMOVENONEXISTENT.toolTipText =
            "Removes ScriptInstances attached to non-existent ChangeForms. These ScriptInstances can be left behind when in-game objects are created and then destroyed. Cleaning them can cause some mods to stop working though."
        MI_REMOVEUNATTACHED.toolTipText =
            "Removes ScriptInstances that aren't attached to anything. These ScriptInstances are usually left behind when mods are uinstalled. However in Fallout 4 they are used deliberately, so use caution when removing them."
        MI_REMOVEUNDEFINED.toolTipText =
            "Removes Scripts and ScriptInstances for which the script itself is missing, as well as terminating any ActiveScripts associated with them. SKSE and F4SE usually remove these automatically; if it doesn't, there's probably a good reason. So use caution when removing them."
        val BORDER: Border = BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        )
        LBL_MEMORY.border = BORDER
        LBL_WATCHING.border = BORDER
        LBL_SCANNING.border = BORDER
        val DEFFONT = LBL_MEMORY.font
        val MONO = Font(Font.MONOSPACED, DEFFONT.style, DEFFONT.size)
        LBL_MEMORY.font = MONO
        LBL_WATCHING.font = MONO
        LBL_SCANNING.font = MONO
        LBL_WATCHING.isVisible = false
        LBL_SCANNING.isVisible = false
        LOG.parent.addHandler(LOGWINDOW.handler)
        this.jMenuBar = MENUBAR
        this.contentPane = MAINSPLITTER
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        this.preferredSize = Dimension(800, 600)
        pack()
        restoreWindowPosition()
        setLocationRelativeTo(null)
        MI_SHOWNONEXISTENTCREATED.addItemListener { e: ItemEvent ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val WARN = "Non-existent forms are used intentionally by some mods. Use caution when deleting them."
                val WARN_TITLE = "Warning"
                JOptionPane.showMessageDialog(this, WARN, WARN_TITLE, JOptionPane.WARNING_MESSAGE)
            }
        }
        setWatching(MI_WATCHSAVES.isSelected)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(evt: WindowEvent) {
                exitWithPrompt()
            }

            override fun windowOpened(evt: WindowEvent) {
                if (validateSavegame(path)) {
                    val parse = autoParse || PREFS.getBoolean("settings.alwaysParse", false)
                    if (path != null) {
                        this@SaveWindow.open(path, parse)
                    }
                } else {
                    this@SaveWindow.open()
                }
                updateFilters(false)
            }
        })
        try {
            val INPUT = this.javaClass.classLoader.getResourceAsStream("Disk.png")
            val ICON: Image = ImageIO.read(INPUT)
            super.setIconImage(ICON)
        } catch (ex: IOException) {
            logger.warn{"Failed to load icon."}
        } catch (ex: NullPointerException) {
            logger.warn{"Failed to load icon."}
        } catch (ex: IllegalArgumentException) {
            logger.warn{"Failed to load icon."}
        }
        MODCOMBO.addItemListener { e: ItemEvent? -> updateFilters(false) }
        PLUGINCOMBO.addItemListener { e: ItemEvent? -> updateFilters(false) }
        TREE.setDeleteHandler { elements: Map<Element, FilterTreeModel.Node> ->
            deletePaths(
                elements.toMap()
            )
        }
        TREE.setEditHandler { element: Element -> editElement(element) }
        TREE.setPurgeHandler { plugins: MutableList<Plugin> -> purgePlugins(plugins.toMutableList(),
            purgeScripts = true,
            purgeForms = true
        ) }
        TREE.setDeleteFormsHandler { plugin: Plugin? -> purgePlugins(mutableListOf(plugin),
            purgeScripts = false,
            purgeForms = true
        ) }
        TREE.setDeleteInstancesHandler { plugin: Plugin? -> purgePlugins(mutableListOf(plugin),
            purgeScripts = true,
            purgeForms = false
        ) }
        TREE.setFilterPluginsHandler { anObject: Plugin? -> PLUGINCOMBO.setSelectedItem(anObject) }
        TREE.setZeroThreadHandler { threads: List<ActiveScript> -> zeroThreads(threads) }
        TREE.setFindHandler { element: Element? -> this.findElement(element) }
        TREE.setCleanseFLSTHandler { flst: ChangeFormFLST -> cleanseFormList(flst) }
        TREE.setCompressionHandler { newCompressionType: CompressionType? -> setCompressionType(newCompressionType) }
        LBL_MEMORY.initialize()
    }

    /**
     * Clears the modified flag.
     *
     * @param savefile A new value for the path.
     */
    fun resetTitle(savefile: Path?) {
        modified = false
        if (save == null) {
            val TITLE = "ReSaver $version: (no save loaded)"
            title = TITLE
        } else {
            // Get the filesize if possible.
            val size: Float = try {
                if (savefile != null) {
                    Files.size(savefile).toFloat() / 1048576.0f
                } else {
                    0.0f
                }
            } catch (ex: IOException) {
                logger.warn(ex) {"Error setting title."}
                Float.NEGATIVE_INFINITY
            }

            // Get the file digest.
            val DIGEST = save!!.digest

            // Make an abbreviated filename.
            val fullName = savefile?.fileName.toString()
            val MAXLEN = 80
            val NAME = if (fullName.length > MAXLEN) "${fullName.substring(0, MAXLEN)}..." else fullName
            val TITLE = String.format("ReSaver $version: $NAME (%1.2f mb, digest = %08x)", size, DIGEST)
            title = TITLE
        }
    }

    /**
     * Sets the modified flag.
     */
    fun setModified() {
        modified = true
    }

    /**
     * Sets the `Analysis`.
     *
     * @param newAnalysis The mod data, or null if there is no mod data
     * available.
     */
    fun setAnalysis(newAnalysis: Analysis?) {
        if (newAnalysis != analysis) {
            analysis = newAnalysis
            updateContextInformation()
            save!!.addNames(analysis!!)
            refreshTree()
        }
        if (null != analysis) {
            MI_LOOKUPID.isEnabled = true
            MI_LOOKUPBASE.isEnabled = true
        } else {
            MI_LOOKUPID.isEnabled = false
            MI_LOOKUPBASE.isEnabled = false
        }
        if (null == analysis || !MI_SHOWMODS.isSelected) {
            MODCOMBO.model = DefaultComboBoxModel()
            MODPANEL.isVisible = false
        } else {
            val MODS = analysis!!.MODS.sortedWith { a:Mod, b:Mod -> a.getName().compareTo(b.getName(),ignoreCase = true) }.toTypedArray()
            val modModel = DefaultComboBoxModel(MODS)
            modModel.insertElementAt(null, 0)
            MODCOMBO.model = modModel
            MODCOMBO.selectedIndex = 0
            MODPANEL.isVisible = true
        }
        refreshTree()
    }

    /**
     * Regenerates the treeview if the underlying model has changed.
     *
     */
    fun refreshTree() {
        if (null == save) {
            return
        }
        TREE.model!!.refresh()
        TREE.updateUI()
    }

    /**
     * Clears the `ESS`.
     */
    fun clearESS() {
        MI_SAVE.isEnabled = false
        MI_EXPORTPLUGINS.isEnabled = false
        MI_REMOVEUNATTACHED.isEnabled = false
        MI_REMOVEUNDEFINED.isEnabled = false
        MI_RESETHAVOK.isEnabled = false
        MI_CLEANSEFORMLISTS.isEnabled = false
        MI_REMOVENONEXISTENT.isEnabled = false
        MI_LOOKUPID.isEnabled = false
        MI_LOOKUPBASE.isEnabled = false
        MI_LOADESPS.isEnabled = false
        PLUGINCOMBO.model = DefaultComboBoxModel()
        save = null
        resetTitle(null)
        clearContextInformation()
        val TITLE = "ReSaver $version: (no save loaded)"
        title = TITLE
    }

    /**
     * Sets the `ESS` containing the papyrus section to display.
     *
     * @param savefile The file that contains the `ESS`.
     * @param newSave The new `ESS`.
     * @param model The `FilterTreeModel`.
     * @param disableSaving A flag indicating that saving should be disabled.
     */
    fun setESS(savefile: Path, newSave: ESS, model: FilterTreeModel, disableSaving: Boolean) {

        logger.info{"================"}
        logger.info{"setESS"}
        TIMER.restart()

        // If the game is Skyrim Legendary, and the string table bug was
        // detected, disable the save menu command.
        if (disableSaving) {
            MI_SAVE.isEnabled = false
            MI_SAVEAS.isEnabled = false
        } else {
            MI_SAVE.isEnabled = true
            MI_SAVEAS.isEnabled = true
        }

        // Enable editing functions.
        MI_EXPORTPLUGINS.isEnabled = true
        MI_REMOVEUNATTACHED.isEnabled = true
        MI_REMOVEUNDEFINED.isEnabled = true
        MI_RESETHAVOK.isEnabled = true
        MI_CLEANSEFORMLISTS.isEnabled = true
        MI_REMOVENONEXISTENT.isEnabled = true
        MI_LOADESPS.isEnabled = true

        // Clear the context info box.
        clearContextInformation()

        // Set the save field.
        save = newSave

        // Set up the Plugins combobox.
        val PLUGINS: MutableList<Plugin?> = mutableListOf()
        PLUGINS.addAll(newSave.pluginInfo.fullPlugins)
        PLUGINS.addAll(newSave.pluginInfo.litePlugins)
        PLUGINS.sortWith { obj: Plugin?, other: Plugin? -> obj!!.compareTo(other) }
        PLUGINS.add(0, null)
        val pluginModel = DefaultComboBoxModel(PLUGINS.toTypedArray())

        // If a plugin was previously selected, attempt to re-select it.
        if (null != PLUGINCOMBO.selectedItem && PLUGINCOMBO.selectedItem is Plugin) {
            val PREV = PLUGINCOMBO.selectedItem as Plugin
            PLUGINCOMBO.model = pluginModel
            PLUGINCOMBO.setSelectedItem(PREV)
        } else {
            PLUGINCOMBO.model = pluginModel
            PLUGINCOMBO.setSelectedIndex(0)
        }

        // Rebuild the tree.
        TREE.setESS(newSave, model, filter)
        refreshTree()
        val path = model.getPath(model.root!!)
        TREE.selectionPath = path
        resetTitle(savefile)
        TIMER.stop()
        logger.info{"Treeview initialized, took ${TIMER.formattedTime}."}
    }

    /**
     * Updates the setFilter.
     *
     * @param model The model to which the filters should be applied.
     */
    private fun createFilter(model: FilterTreeModel): Boolean {
        logger.info{"Creating filters."}
        val MOD = MODCOMBO.getItemAt(MODCOMBO.selectedIndex)
        val PLUGIN = PLUGINCOMBO.selectedItem as Plugin?
        val TXT = FILTERFIELD.text
        val mainfilter = if (save == null) null else createFilter(
            save!!,
            MOD, PLUGIN, TXT, analysis,
            MI_SHOWUNDEFINED.isSelected,
            MI_SHOWUNATTACHED.isSelected,
            MI_SHOWMEMBERLESS.isSelected,
            MI_SHOWCANARIES.isSelected,
            MI_SHOWNULLREFS.isSelected,
            MI_SHOWNONEXISTENTCREATED.isSelected,
            MI_SHOWLONGSTRINGS.isSelected,
            MI_SHOWDELETED.isSelected,
            MI_SHOWEMPTY.isSelected,
            MI_CHANGEFILTER.getValue(),
            MI_CHANGEFORMFILTER!!.getValue()
        )
        if (null == mainfilter) {
            filter = null
            model.removeFilter()
        } else {
            filter = mainfilter
            model.setFilter(filter!!)
        }
        return true
    }

    /**
     * Updates the setFilter.
     *
     * @param clear A flag indicating to clear the filters instead of reading
     * the setFilter settings.
     */
    private fun updateFilters(clear: Boolean) {
        PREFS.put("settings.regex", FILTERFIELD.text)
        if (null == save) {
            SwingUtilities.invokeLater {
                try {
                    TIMER.restart()
                    logger.info{"Updating filters."}
                    if (clear) {
                        MI_SHOWNONEXISTENTCREATED.isSelected = false
                        MI_SHOWNULLREFS.isSelected = false
                        MI_SHOWUNDEFINED.isSelected = false
                        MI_SHOWUNATTACHED.isSelected = false
                        MI_SHOWMEMBERLESS.isSelected = false
                        MI_SHOWCANARIES.isSelected = false
                        MI_SHOWLONGSTRINGS.isSelected = false
                        MI_SHOWDELETED.isSelected = false
                        MI_SHOWEMPTY.isSelected = false
                        FILTERFIELD.text = ""
                        MODCOMBO.selectedItem = null
                        MI_CHANGEFILTER.setValue(null)
                        MI_CHANGEFORMFILTER!!.setValue(null)
                        PLUGINCOMBO.selectedItem = null
                    }
                    this.createFilter(TREE.model!!)
                } finally {
                    TIMER.stop()
                    logger.info{"Filter updated, took ${TIMER.formattedTime}."}
                }
            }
        } else {
            val MODEL = ProgressModel(10)
            progressIndicator.start("Updating")
            progressIndicator.setModel(MODEL)
            SwingUtilities.invokeLater {
                try {
                    TIMER.restart()
                    logger.info{"Updating filters."}
                    val path = TREE.selectionPath
                    if (clear) {
                        MI_SHOWNONEXISTENTCREATED.isSelected = false
                        MI_SHOWNULLREFS.isSelected = false
                        MI_SHOWUNDEFINED.isSelected = false
                        MI_SHOWUNATTACHED.isSelected = false
                        MI_SHOWMEMBERLESS.isSelected = false
                        MI_SHOWCANARIES.isSelected = false
                        MI_SHOWLONGSTRINGS.isSelected = false
                        MI_SHOWDELETED.isSelected = false
                        MI_SHOWEMPTY.isSelected = false
                        FILTERFIELD.text = ""
                        MODCOMBO.selectedItem = null
                        PLUGINCOMBO.selectedItem = null
                        MI_CHANGEFILTER.setValue(null)
                        MI_CHANGEFORMFILTER!!.setValue(null)
                    }
                    MODEL.value = 2
                    val result = this.createFilter(TREE.model!!)
                    if (!result) {
                        return@invokeLater
                    }
                    MODEL.value = 5
                    refreshTree()
                    MODEL.value = 9
                    if (null != path) {
                        logger.info{"Updating filter: restoring path = $path"}
                        if (path.lastPathComponent == null) {
                            TREE.clearSelection()
                            clearContextInformation()
                        } else {
                            TREE.selectionPath = path
                            TREE.scrollPathToVisible(path)
                        }
                    }
                    MODEL.value = 10
                } finally {
                    TIMER.stop()
                    progressIndicator.stop()
                    logger.info{"Filter updated, took ${TIMER.formattedTime}."}
                }
            }
        }
    }

    /**
     * Exits the application immediately.
     */
    fun exit() {
        SwingUtilities.invokeLater {
            saveWindowPosition()
            try {
                PREFS.flush()
            } catch (ex: BackingStoreException) {
                logger.warn(ex){"Error saving preferences."}
            }
            FILTERFIELD.terminate()
            LBL_MEMORY.terminate()
            this.isVisible = false
            dispose()
            if (JFXPANEL != null) {
                terminateJavaFX()
            }
        }
    }

    /**
     * Exits the application after checking if the user wishes to save.
     */
    fun exitWithPrompt() {
        if (null != save && modified) {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save the current file first?",
                "Save First?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )
            when (result) {
                JOptionPane.CANCEL_OPTION -> return
                JOptionPane.YES_OPTION -> save(false) { exit() }
                JOptionPane.NO_OPTION -> exit()
            }
        } else {
            exit()
        }
    }

    /**
     * Saves the currently loaded save, if any.
     *
     * @param promptForFile A flag indicating the the user should be asked what
     * filename to use.
     * @param doAfter A task to run after the save is complete.
     */
    private fun save(promptForFile: Boolean, doAfter: Runnable?) {
        if (null == save) {
            return
        }
        try {
            val PROMPT = FutureTask( Callable<Path?> {
                val newSaveFile = if (promptForFile) selectNewSaveFile(this, save!!.header.GAME!!) else confirmSaveFile(
                    this,
                    save!!.header.GAME!!,
                    save!!.originalFile
                )
                if (validWrite(newSaveFile)) {
                    return@Callable newSaveFile
                }
                null
            })
            val MODAL = ModalProgressDialog(this, "File Selection", PROMPT)
            MODAL.isVisible = true
            val SAVEFILE = PROMPT.get()
            if (!validWrite(SAVEFILE)) {
                return
            }
            val SAVER = Saver(this, SAVEFILE!!, save, doAfter)
            SAVER.execute()
        } catch (ex: InterruptedException) {
            logger.error(ex) {"Error while saving."}
        } catch (ex: ExecutionException) {
            logger.error(ex){"Error while saving."}
        }
    }

    /**
     * Starts a batch cleaning operation.
     */
    private fun batchClean() {
        if (null == save) {
            return
        }
        val CLEANER = BatchCleaner(this, save)
        CLEANER.execute()
    }

    /**
     * Opens a savefile, preceded with a prompt to save the current one.
     *
     */
    fun openWithPrompt() {
        if (null == save || !modified) {
            this.open()
        } else {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save the current file first?",
                "Save First?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )
            when (result) {
                JOptionPane.YES_OPTION -> save(false) { this.open() }
                JOptionPane.NO_OPTION -> this.open()
                JOptionPane.CANCEL_OPTION -> {
                }
                else -> {
                }
            }
        }
    }

    /**
     * Opens a save file.
     *
     */
    fun open() {
        val SAVEFILE = choosePathModal(this,
            defval = null,
            request = { selectSaveFile(this) }, check = { obj: Path? -> validateSavegame(obj) },
            interactive = true
        )
        if (SAVEFILE != null) {
            open(SAVEFILE, PREFS.getBoolean("settings.alwaysParse", false))
        }
    }

    /**
     * Opens a save file.
     *
     * @param path The savefile or script to read.
     * @param parse
     */
    fun open(path: Path, parse: Boolean) {
        when {
            validateSavegame(path) -> {
                if (scanner != null) {
                    setScanning(false)
                    scanner!!.cancel(true)
                    scanner = null
                }
                val doAfter = if (parse) Runnable { scanESPs(false) } else null
                val OPENER = Opener(this, path, WORRIER, doAfter)
                OPENER.execute()
            }
            Mod.GLOB_SCRIPT.matches(path) -> {
                try {
                    val SCRIPT = readScript(path)
                    val SOURCE: MutableList<String?> = mutableListOf()
                    SCRIPT.disassemble(SOURCE, AssemblyLevel.FULL)
                    val joinbe = SOURCE.joinToString(separator = "<br/>", prefix = "<pre>", postfix = "</pre>")
                    val TEXT = TextDialog(joinbe)
                    JOptionPane.showMessageDialog(this, TEXT, path.fileName.toString(), JOptionPane.INFORMATION_MESSAGE)
                } catch (ex: IOException) {
                    logger.warn(ex){"Error while decompiling drag-and-drop script."}
                    JOptionPane.showMessageDialog(this, ex.message, "Decompile Error", JOptionPane.ERROR_MESSAGE)
                } catch (ex: RuntimeException) {
                    logger.warn(ex){"Error while decompiling drag-and-drop script."}
                    JOptionPane.showMessageDialog(this, ex.message, "Decompile Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            Configurator.GLOB_INI.matches(path) -> {
                storeMO2Ini(path)
            }
        }
    }

    /**
     * Scans ESPs for contextual information.
     *
     * @param interactive A flag indicating whether to prompt the user.
     */
    fun scanESPs(interactive: Boolean) {
        if (save == null) {
            return
        }
        if (scanner != null) {
            setScanning(false)
            scanner!!.cancel(true)
            scanner = null
        }
        val GAME = save!!.header.GAME
        val GAME_DIR = choosePathModal(
            this,
            { getGameDirectory(GAME!!) },
            { selectGameDirectory(this@SaveWindow, GAME!!) },
            { path: Path? ->
                validateGameDirectory(
                    GAME!!, path!!
                )
            },
            interactive
        )
        val MO2_INI = if (MI_USEMO2.isSelected) choosePathModal(
            this,
            { getMO2Ini(GAME!!) },
            { selectMO2Ini(this, GAME!!) },
            { path: Path? -> validateMO2Ini(GAME, path!!) },
            interactive
        ) else null
        if (GAME_DIR != null) {
            scanner = Scanner(this, save, GAME_DIR, MO2_INI, { setScanning(false) }) { msg: String -> updateScan(msg) }
            scanner!!.execute()
            setScanning(true)
        }
    }

    /**
     * Display the settings dialogbox.
     */
    private fun showSettings() {
        val currentGame = if (null == save) null else save!!.header.GAME
        val settings = ReSaverSettings(this, currentGame)
        settings.pack()
        settings.isVisible = true
    }

    /**
     * Exports a list of plugins.
     */
    private fun exportPlugins() {
        val EXPORT = choosePathModal(this,
            null,
             { selectPluginsExport(this, save!!.originalFile) }, { obj: Path? -> validWrite(obj) },
            true
        ) ?: return
        try {
            Files.newBufferedWriter(EXPORT).use { out ->
                for (plugin in save!!.pluginInfo.fullPlugins) {
                    out.write(plugin.NAME)
                    out.write('\n'.code)
                }
                val MSG = "Plugins list exported."
                JOptionPane.showMessageDialog(this@SaveWindow, MSG, "Success", JOptionPane.INFORMATION_MESSAGE)
            }
        } catch (ex: IOException) {
            val MSG = "Error while writing file \"${EXPORT.fileName}\".\n${ex.message}"
            logger.error(ex) {"Error while exporting plugin list."}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, "Write Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Prompts the user for a name, and finds the corresponding ID.
     */
    private fun lookupID() {
        val MSG = "Enter the name of the object or NPC:"
        val TITLE = "Enter Name"
        val searchTerm = JOptionPane.showInputDialog(this, MSG, TITLE, JOptionPane.QUESTION_MESSAGE)
        if (null == searchTerm || searchTerm.trim { it <= ' ' }.isEmpty()) {
            return
        }
        val matches = analysis!!.find(searchTerm)
        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matches were found.", "No matches", JOptionPane.ERROR_MESSAGE)
            return
        }
        val BUF = StringBuilder()
        BUF.append("The following matches were found:\n\n")
        for (id in matches) {
            BUF.append(String.format("%08x", id))
            val pluginIndex = id ushr 24
            val PLUGINS = save!!.pluginInfo.fullPlugins
            if (pluginIndex < PLUGINS.size) {
                val PLUGIN = PLUGINS[pluginIndex]
                BUF.append(" (").append(PLUGIN).append(")")
            }
            BUF.append('\n')
        }
        JOptionPane.showMessageDialog(this, BUF.toString(), "Matches", JOptionPane.INFORMATION_MESSAGE)
        println(matches)
    }

    /**
     * Prompts the user for the name or ID of a reference, and finds the id/name
     * of the base object.
     */
    private fun lookupBase() {}
    private fun showDataAnalyzer(data: PlatformByteBuffer) {
        showDataAnalyzer(this, data, save!!)
    }

    private fun compareTo() {
        if (save == null) {
            return
        }
        val otherPath = choosePathModal(this, null,{ selectSaveFile(this) }, { obj: Path? -> validateSavegame(obj) }, true) ?: return
        try {
            val RESULT = readESS(otherPath, ModelBuilder(ProgressModel(1)))
            verifyIdentical(save!!, RESULT.ESS)
            JOptionPane.showMessageDialog(this, "No mismatches detected.", "Match", JOptionPane.INFORMATION_MESSAGE)
        } catch (ex: RuntimeException) {
            JOptionPane.showMessageDialog(this, ex.message, "MisMatch", JOptionPane.ERROR_MESSAGE)
        } catch (ex: IOException) {
            JOptionPane.showMessageDialog(this, ex.message, "MisMatch", JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Removes unattached script instances (instances with no valid Ref).
     *
     */
    private fun cleanUnattached() {
        try {
            if (null == save) {
                return
            }
            logger.info{"Cleaning unattached instances."}
            val papyrus = save!!.papyrus
            val REMOVED = papyrus!!.removeUnattachedInstances()
            val msg = String.format("Removed %d orphaned script instances.", REMOVED.size)
            logger.info{msg}
            JOptionPane.showMessageDialog(this, msg, "Cleaned", JOptionPane.INFORMATION_MESSAGE)
            if (REMOVED.isNotEmpty()) {
                deleteNodesFor(REMOVED)
                setModified()
            }
        } catch (ex: HeadlessException) {
            val MSG = "Error cleaning unattached scripts."
            val TITLE = "Cleaning Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Remove undefined script instances (instances with no Script).
     */
    private fun cleanUndefined() {
        try {
            if (null == save) {
                return
            }
            logger.info{"Cleaning undefined elements."}
            val papyrus = save!!.papyrus
            val REMOVED = papyrus!!.removeUndefinedElements()
            val TERMINATED = papyrus.terminateUndefinedThreads()
            val BUF = StringBuilder()
            if (REMOVED.isNotEmpty()) {
                BUF.append("Removed ").append(REMOVED.size).append(" undefined elements.")
            }
            if (TERMINATED.isNotEmpty()) {
                BUF.append("Terminated ").append(TERMINATED).append(" undefined threads.")
            }
            val MSG = BUF.toString()
            logger.info{MSG}
            JOptionPane.showMessageDialog(this, MSG, "Cleaned", JOptionPane.INFORMATION_MESSAGE)
            if (REMOVED.isNotEmpty()) {
                deleteNodesFor(REMOVED)
                setModified()
            }
        } catch (ex: HeadlessException) {
            val MSG = "Error cleaning undefined elements."
            val TITLE = "Cleaning Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     *
     */
    private fun resetHavok() {
        if (null != save) {
            save!!.resetHavok()
            JOptionPane.showMessageDialog(this, "Not implemented yet.")
            setModified()
        }
    }

    /**
     *
     */
    private fun cleanseFormLists() {
        try {
            if (null == save) {
                return
            }
            logger.info{"Cleansing formlists."}
            val results = save!!.cleanseFormLists()
            if (results[0] == 0) {
                val MSG = "No nullrefs were found in any formlists."
                val TITLE = "No nullrefs found."
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            } else {
                setModified()
                val MSG = String.format("%d nullrefs were cleansed from %d formlists.", results[0], results[1])
                val TITLE = "Nullrefs cleansed."
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            }
            refreshTree()
        } catch (ex: HeadlessException) {
            val MSG = "Error cleansing formlists."
            val TITLE = "Cleansing Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     *
     * @param flst
     */
    private fun cleanseFormList(flst: ChangeFormFLST) {
        try {
            if (null == save) {
                return
            }
            logger.info{"Cleansing formlist $flst."}
            val result = flst.cleanse()
            if (result == 0) {
                val MSG = "No nullrefs were found."
                val TITLE = "No nullrefs found."
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            } else {
                setModified()
                val MSG = String.format("%d nullrefs were cleansed.", result)
                val TITLE = "Nullrefs cleansed."
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            }
            refreshTree()
        } catch (ex: HeadlessException) {
            val MSG = "Error cleansing formlists."
            val TITLE = "Cleansing Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Removes script instances attached to nonexistent created forms.
     */
    private fun cleanNonexistent() {
        // Check with the user first. This operation can mess up mods.
        val WARN = "This cleaning operation can cause some mods to stop working. Are you sure you want to do this?"
        val WARN_TITLE = "Warning"
        val confirm = JOptionPane.showConfirmDialog(this, WARN, WARN_TITLE, JOptionPane.YES_NO_OPTION)
        if (confirm != JOptionPane.YES_OPTION) {
            return
        }
        try {
            if (null == save) {
                return
            }
            logger.info{"Removing nonexistent created forms."}
            val REMOVED: Set<PapyrusElement?> = save!!.removeNonexistentCreated()
            if (REMOVED.isNotEmpty()) {
                val MSG = "No scripts attached to non-existent created forms were found."
                val TITLE = "No non-existent created"
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            } else {
                setModified()
                val MSG = String.format("%d instances were removed.", REMOVED.size)
                val TITLE = "Instances removed."
                logger.info{MSG}
                JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
            }
            deleteNodesFor(REMOVED)
        } catch (ex: HeadlessException) {
            val MSG = "Error cleansing non-existent created."
            val TITLE = "Cleansing Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Save minor settings like window position, size, and state.
     */
    private fun saveWindowPosition() {
        PREFS.putInt("settings.extendedState", this.extendedState)
        if (this.extendedState == NORMAL) {
            PREFS.putInt("settings.windowWidth", this.size.width)
            PREFS.putInt("settings.windowHeight", this.size.height)
            PREFS.putInt("settings.windowX", this.location.x)
            PREFS.putInt("settings.windowY", this.location.y)
            PREFS.putInt("settings.mainDivider", MAINSPLITTER.dividerLocation)
            PREFS.putInt("settings.rightDivider", RIGHTSPLITTER!!.dividerLocation)
            println("Pos = ${this.location}")
            println("Size = ${this.size}")
            println("Dividers = ${MAINSPLITTER.dividerLocation},${RIGHTSPLITTER.dividerLocation}")
        } else {
            PREFS.putInt("settings.mainDividerMax", MAINSPLITTER.dividerLocation)
            PREFS.putInt("settings.rightDividerMax", RIGHTSPLITTER!!.dividerLocation)
        }
    }

    /**
     * Loads minor settings like window position, size, and state.
     */
    private fun restoreWindowPosition() {
        if (this.extendedState == NORMAL) {
            val pos = this.location
            val size = this.size
            val x = PREFS.getInt("settings.windowX", pos.x)
            val y = PREFS.getInt("settings.windowY", pos.y)
            val width = PREFS.getInt("settings.windowWidth", size.width)
            val height = PREFS.getInt("settings.windowHeight", size.height)
            this.setLocation(x, y)
            this.setSize(width, height)
            var mainDividerLocation = MAINSPLITTER.dividerLocation.toFloat()
            mainDividerLocation = PREFS.getFloat("settings.mainDivider", mainDividerLocation)
            var rightDividerLocation = RIGHTSPLITTER!!.dividerLocation.toFloat()
            rightDividerLocation = PREFS.getFloat("settings.rightDivider", rightDividerLocation)
            MAINSPLITTER.setDividerLocation(0.1.coerceAtLeast(0.9.coerceAtMost(mainDividerLocation.toDouble())))
            RIGHTSPLITTER.setDividerLocation(0.1.coerceAtLeast(0.9.coerceAtMost(rightDividerLocation.toDouble())))
        } else {
            var mainDividerLocation = MAINSPLITTER.dividerLocation.toFloat()
            mainDividerLocation = PREFS.getFloat("settings.mainDividerMax", mainDividerLocation)
            var rightDividerLocation = RIGHTSPLITTER!!.dividerLocation.toFloat()
            rightDividerLocation = PREFS.getFloat("settings.rightDividerMax", rightDividerLocation)
            MAINSPLITTER.setDividerLocation(0.1.coerceAtLeast(0.9.coerceAtMost(mainDividerLocation.toDouble())))
            RIGHTSPLITTER.setDividerLocation(0.1.coerceAtLeast(0.9.coerceAtMost(rightDividerLocation.toDouble())))
        }
    }

    /**
     *
     */
    private fun kill() {
        try {
            val ELEMENTS = TREE.model!!.elements
            setModified()
            val REMOVED = save!!.removeElements(ELEMENTS)
            deleteNodesFor(REMOVED)
        } catch (ex: Exception) {
            val MSG = "Error cleansing formlists."
            val TITLE = "Cleansing Error"
            logger.error(ex){MSG}
            JOptionPane.showMessageDialog(this@SaveWindow, MSG, TITLE, JOptionPane.ERROR_MESSAGE)
        }
    }

    /**
     * Start or stop the watcher service.
     *
     * @param enabled Indicates whether to start or terminate the watcher.
     */
    fun setWatching(enabled: Boolean) {
        if (enabled && !watcher.isRunning) {
            LBL_WATCHING.isVisible = true
            watcher.start(null)
            if (!MI_WATCHSAVES.isSelected) {
                MI_WATCHSAVES.isSelected = true
            }
        } else if (!enabled && watcher.isRunning) {
            LBL_WATCHING.isVisible = false
            watcher.stop()
            if (MI_WATCHSAVES.isSelected) {
                MI_WATCHSAVES.isSelected = false
            }
        }
    }

    private fun updateScan(msg: String) {
        LBL_SCANNING.text = msg
    }

    /**
     * Begin the watcher service.
     */
    fun setScanning(enabled: Boolean) {
        if (enabled && scanner != null && !scanner!!.isDone) {
            LBL_SCANNING.isVisible = true
            MI_LOADESPS.isEnabled = false
        } else {
            LBL_SCANNING.isVisible = false
            MI_LOADESPS.isEnabled = save != null && analysis == null
        }
    }

    /**
     *
     */
    private fun showLog() {
        val dialog = JDialog(this, "Log")
        dialog.contentPane = LOGWINDOW
        dialog.modalityType = Dialog.ModalityType.MODELESS
        dialog.defaultCloseOperation = DISPOSE_ON_CLOSE
        dialog.preferredSize = Dimension(600, 400)
        dialog.setLocationRelativeTo(null)
        dialog.pack()
        dialog.isVisible = true
    }

    /**
     *
     */
    private fun setChangeFlagFilter() {
        var pair = MI_CHANGEFILTER.getValue()
        if (null == pair) {
            pair = make(0, 0)
        }
        val dlg = ChangeFlagDialog(this, pair.A, pair.B) { m: Int, f: Int ->
            val newPair = make(m, f)
            MI_CHANGEFILTER.setValue(newPair)
            updateFilters(false)
        }
        dlg.pack()
        dlg.setLocationRelativeTo(this)
        dlg.isVisible = true
    }

    /**
     *
     */
    private fun setChangeFormFlagFilter() {
        var pair = MI_CHANGEFORMFILTER!!.getValue()
        if (null == pair) {
            pair = make(0, 0)
        }
        val dlg = ChangeFlagDialog(this, pair.A, pair.B) { m: Int, f: Int ->
            val newPair = make(m, f)
            MI_CHANGEFORMFILTER.setValue(newPair)
            updateFilters(false)
        }
        dlg.pack()
        dlg.setLocationRelativeTo(this)
        dlg.isVisible = true
    }

    /**
     * Selects an `Element` in the `FilterTree`.
     *
     * @param element The `Element` to find.
     * @param index The index of the data table for `Element` to
     * select.
     */
    fun findElement(element: Element?, index: Int) {
        this.findElement(element)
        TABLE.scrollSelectionToVisible(index)
    }

    /**
     * Selects an `Element` in the `FilterTree`.
     *
     * @param element The `Element` to select.
     */
    fun findElement(element: Element?) {
        if (null == element) {
            return
        }
        val path = TREE.findPath(element)
        if (null == path) {
            JOptionPane.showMessageDialog(this, "The element was not found.", "Not Found", JOptionPane.ERROR_MESSAGE)
            return
        }
        TREE.updateUI()
        TREE.scrollPathToVisible(path)
        TREE.selectionPath = path
    }

    /**
     * Selects the element stored in a reference variable or array variable.
     *
     * @param var The `Variable` whose contents should be found.
     */
    private fun findElement(`var`: Variable?) {
        when {
            `var` == null -> {
            }
            `var` is VarArray -> {
                val ID = `var`.arrayID
                if (ID.isZero) {
                    return
                }
                val array = save!!.papyrus!!.arrays[ID]
                assert(array == `var`.array)
                if (null == array) {
                    JOptionPane.showMessageDialog(
                        this,
                        "The array with that ID was not found.",
                        "Not Found",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return
                }
                this.findElement(array)
            }
            `var`.hasRef() -> {
                val ID = `var`.ref
                if (ID!!.isZero) {
                    return
                }
                if (null == `var`.referent) {
                    JOptionPane.showMessageDialog(
                        this,
                        "The element with that ID was not found.",
                        "Not Found",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return
                }
                this.findElement(`var`.referent)
            }
        }
    }

    /**
     * Deletes plugins' script instances and forms.
     *
     * @param plugins The list of plugins to purge.
     * @param purgeForms A flag indicating to purge changeforms.
     * @param purgeScripts A flag indicating to purge script instances.
     * @return The count of instances and changeforms removed.
     */
    private fun purgePlugins(plugins: MutableList<Plugin?>, purgeScripts: Boolean, purgeForms: Boolean) {
        val NUM_FORMS: Int
        val NUM_INSTANCES: Int
        if (purgeScripts) {
            val INSTANCES = plugins
                .filterNotNull()
                .flatMap { it.getInstances(save) }
                .toSet()
            NUM_INSTANCES = INSTANCES.size
            val REMOVED = save!!.papyrus!!.removeElements(INSTANCES)
            assert(REMOVED.size == NUM_INSTANCES) {
                String.format(
                    "Deleted %d/%d instances.",
                    REMOVED.size,
                    NUM_INSTANCES
                )
            }
            if (REMOVED.isNotEmpty()) {
                deleteNodesFor(REMOVED)
                setModified()
            }
        } else {
            NUM_INSTANCES = 0
        }
        if (purgeForms) {
            val FORMS = plugins
                .filterNotNull()
                .flatMap { it.getChangeForms(save) }
                .toSet()
            NUM_FORMS = FORMS.size
            val REMOVED = save!!.removeChangeForms(FORMS)
            assert(REMOVED.size == NUM_INSTANCES) { String.format("Deleted %d/%d forms.", REMOVED.size, NUM_FORMS) }
            if (REMOVED.isNotEmpty()) {
                deleteNodesFor(REMOVED)
                setModified()
            }
        } else {
            NUM_FORMS = 0
        }
        val TITLE = "Plugin Purge"
        if (NUM_INSTANCES > 0 && NUM_FORMS == 0) {
            val FORMAT = "Deleted %d script instances from %d plugins."
            val MSG = String.format(FORMAT, NUM_INSTANCES, plugins.size)
            JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
        } else if (NUM_INSTANCES == 0 && NUM_FORMS > 0) {
            val FORMAT = "Deleted %d changeforms from %d plugins."
            val MSG = String.format(FORMAT, NUM_FORMS, plugins.size)
            JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
        } else if (NUM_INSTANCES > 0 && NUM_FORMS > 0) {
            val FORMAT = "Deleted %d script instances and %d changeforms from %d plugins."
            val MSG = String.format(FORMAT, NUM_INSTANCES, NUM_FORMS, plugins.size)
            JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
        } else {
            val MSG = "There was nothing to delete."
            JOptionPane.showMessageDialog(this, MSG, TITLE, JOptionPane.INFORMATION_MESSAGE)
        }
    }

    /**
     * Zero a thread, terminating it.
     *
     * @param threads An `Element` `List` that will be
     * terminated.
     */
    private fun zeroThreads(threads: List<ActiveScript>) {
        if (threads.isEmpty()) {
            return
        }
        val QUESTION = if (threads.size > 1) String.format(
            "Are you sure you want to terminate these %d threads?",
            threads.size
        ) else "Are you sure you want to terminate this thread?"
        val TITLE = "Thread Termination"
        val result = JOptionPane.showConfirmDialog(this, QUESTION, TITLE, JOptionPane.YES_NO_OPTION)
        if (result == JOptionPane.NO_OPTION) {
            return
        }
        setModified()
        for (thread in threads) {
            thread.zero()
        }
        refreshTree()
        val MSG = if (threads.size > 1) "Thread terminated and zeroed." else "Threads terminated and zeroed."
        logger.info{MSG}
        JOptionPane.showMessageDialog(this, MSG, "Thread Termination", JOptionPane.INFORMATION_MESSAGE)
    }

    /**
     * Deletes selected elements of the tree.
     *
     * @param elements The selections to delete.
     */
    fun deletePaths(elements: Map<Element?, FilterTreeModel.Node?>) {
        deleteElements(elements.keys)
    }

    /**
     * Deletes selected elements of the tree.
     *
     * @param elements The selections to delete.
     */
    fun deleteElements(elements: Set<Element?>?) {
        if (null == save || null == elements || elements.isEmpty()) {
            return
        }

        // Save the selected row so that we can select it again after this is done.
        val ROW = TREE.selectionModel.maxSelectionRow
        if (elements.size == 1) {
            val ELEMENT = elements.iterator().next()
            val WARNING: String = when {
                ESS.THREAD.test(ELEMENT) -> {
                    "Element \"${ELEMENT.toString()}\" is a Papyrus thread. Deleting it could make your savefile impossible to load. Are you sure you want to proceed?"
                }
                ESS.DELETABLE.test(ELEMENT) -> {
                    "Are you sure you want to delete this element?\n$ELEMENT"
                }
                ELEMENT is SuspendedStack -> {
                    "Element \"$ELEMENT\" is a Suspended Stack. Deleting it could make your savefile impossible to load. Are you sure you want to proceed?"
                }
                else -> {
                    return
                }
            }
            val TITLE = "Warning"
            val result = JOptionPane.showConfirmDialog(this, WARNING, TITLE, JOptionPane.OK_CANCEL_OPTION)
            if (result == JOptionPane.CANCEL_OPTION) {
                return
            }
            val REMOVED = save!!.removeElements(elements)
            deleteNodesFor(REMOVED)
            if (REMOVED.containsAll(elements)) {
                val MSG = "Element Deleted:\n$ELEMENT"
                JOptionPane.showMessageDialog(this, MSG, "Element Deleted", JOptionPane.INFORMATION_MESSAGE)
                logger.info{MSG}
            } else {
                val MSG = "Couldn't delete element:\n$ELEMENT"
                JOptionPane.showMessageDialog(this, MSG, "Error", JOptionPane.ERROR_MESSAGE)
                logger.warn{MSG}
            }
        } else {
            val DELETABLE = elements
                .filter { ESS.DELETABLE.test(it) && it !is ActiveScript && it !is SuspendedStack }
                .toSet()
            val STACKS = elements
                .filterIsInstance<SuspendedStack>()
                .toSet()
            val THREADS = elements
                .filterIsInstance<ActiveScript>()
                .toSet()
            var deleteStacks = false
            if (STACKS.isNotEmpty()) {
                val WARN =
                    "Deleting Suspended Stacks could make your savefile impossible to load.\nAre you sure you want to delete the Suspended Stacks?\nIf you select \"No\" then they will be skipped instead of deleted."
                val TITLE = "Warning"
                val result = JOptionPane.showConfirmDialog(this, WARN, TITLE, JOptionPane.YES_NO_CANCEL_OPTION)
                if (result == JOptionPane.CANCEL_OPTION) {
                    return
                }
                deleteStacks = result == JOptionPane.YES_OPTION
            }
            var deleteThreads = false
            if (THREADS.isNotEmpty()) {
                val WARN =
                    "Deleting Active Scripts could make your savefile impossible to load.\nAre you sure you want to delete the Active Scripts?\nIf you select \"No\" then they will be terminated instead of deleted."
                val TITLE = "Warning"
                val result = JOptionPane.showConfirmDialog(this, WARN, TITLE, JOptionPane.YES_NO_CANCEL_OPTION)
                if (result == JOptionPane.CANCEL_OPTION) {
                    return
                }
                deleteThreads = result == JOptionPane.YES_OPTION
            }
            var count = DELETABLE.size
            count += if (deleteStacks) STACKS.size else 0
            count += if (deleteThreads) THREADS.size else 0
            if (DELETABLE.isEmpty() && STACKS.isEmpty() && THREADS.isEmpty()) {
                return
            }
            val QUESTION: String = if (DELETABLE.isEmpty() && THREADS.isEmpty()) {
                return
            } else if (count == 0 && THREADS.isNotEmpty()) {
                String.format("Are you sure you want to terminate these %d Active Scripts?", THREADS.size)
            } else if (deleteThreads || count > 0 && THREADS.isEmpty()) {
                String.format("Are you sure you want to delete these %d elements and their dependents?", count)
            } else {
                String.format(
                    "Are you sure you want to terminate these %d Active Scripts and delete these %d elements and their dependents?",
                    THREADS.size,
                    count
                )
            }
            val result = JOptionPane.showConfirmDialog(this, QUESTION, "Delete Elements", JOptionPane.YES_NO_OPTION)
            if (result == JOptionPane.NO_OPTION) {
                return
            }
            val REMOVED = save!!.removeElements(DELETABLE)
            for (THREAD in THREADS) {
                THREAD.zero()
            }
            if (deleteThreads) {
                REMOVED.addAll(save!!.papyrus!!.removeElements(THREADS))
            }
            if (deleteStacks) {
                REMOVED.addAll(save!!.papyrus!!.removeElements(STACKS))
            }
            deleteNodesFor(REMOVED)
            val BUF = StringBuilder()
            BUF.append(REMOVED.size).append(" elements deleted.")
            if (THREADS.isNotEmpty()) {
                BUF.append("\n").append(THREADS.size)
                BUF.append(if (deleteThreads) " threads terminated and deleted." else " threads terminated.")
            }
            val MSG = BUF.toString()
            logger.info{MSG}
            JOptionPane.showMessageDialog(this, MSG, "Elements Deleted", JOptionPane.INFORMATION_MESSAGE)
        }

        // Select the next row.
        TREE.setSelectionRow(ROW)
        setModified()
    }

    /**
     *
     * @param newCompressionType
     */
    fun setCompressionType(newCompressionType: CompressionType?) {
        if (save != null && save!!.supportsCompression() && newCompressionType != null) {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to change the compression type?",
                "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (result == JOptionPane.YES_OPTION) {
                save!!.header.setCompression(newCompressionType)
                setModified()
            }
        }
    }

    /**
     * Deletes nodes from the tree.
     *
     * @param removed
     */
    fun deleteNodesFor(removed: Set<Element?>) {
        TREE.model!!.deleteElements(removed)
        refreshTree()
    }

    /**
     * Edits an element. Currently just globalvariables.
     *
     * @param element
     */
    private fun editElement(element: Element) {
        if (element is GlobalVariable) {
            val response = JOptionPane.showInputDialog(this, "Input new value:", element.value)
            if (null != response) {
                try {
                    val newVal = response.toFloat()
                    element.value = newVal
                    JOptionPane.showMessageDialog(this, "GlobalVariable updated.", "Success", JOptionPane.PLAIN_MESSAGE)
                } catch (ex: NumberFormatException) {
                    JOptionPane.showMessageDialog(this, "Invalid number.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    /**
     * Updates infopane data.
     *
     */
    private fun updateContextInformation() {
        val PATH = TREE.selectionPath
        if (null == PATH) {
            clearContextInformation()
            return
        }
        val OBJ = PATH.lastPathComponent
        if (OBJ !is FilterTreeModel.Node) {
            clearContextInformation()
            return
        }
        if (OBJ.hasElement()) {
            showContextInformation(OBJ.element)
        } else {
            clearContextInformation()
        }
    }

    /**
     * Clears infopane data.
     *
     */
    private fun clearContextInformation() {
        INFOPANE.text = ""
        TABLE.clearTable()
    }

    /**
     *
     * @param element
     */
    private fun showContextInformation(element: Element?) {
        clearContextInformation()
        if (element is ESS) {
            RIGHTSPLITTER!!.resizeWeight = 1.0
            RIGHTSPLITTER.setDividerLocation(1.0)
            INFOPANE.text = save!!.getInfo(analysis) + WORRIER.message
            try {
                val DOC = INFOPANE.document
                val ICONWIDTH = INFOPANE.width * 95 / 100
                val IMAGE = save!!.header.getImage(ICONWIDTH)
                if (null != IMAGE) {
                    val STYLE = StyleContext().getStyle(StyleContext.DEFAULT_STYLE)
                    StyleConstants.setComponent(STYLE, JLabel(save!!.header.getImage(ICONWIDTH)))
                    DOC.insertString(DOC.length, "Ignored", STYLE)
                }
            } catch (ex: BadLocationException) {
                logger.warn(ex){"Error displaying ESS context information."}
            }
        } else if (element is AnalyzableElement) {
            INFOPANE.text = element.getInfo(analysis, save)!!
            if (TABLE.isSupported(element)) {
                RIGHTSPLITTER!!.resizeWeight = 0.66
                RIGHTSPLITTER.setDividerLocation(0.66)
                TABLE.displayElement(element, save!!.papyrus!!.context)
            } else {
                RIGHTSPLITTER!!.resizeWeight = 1.0
                RIGHTSPLITTER.setDividerLocation(1.0)
                TABLE.clearTable()
            }
        } else if (element is GeneralElement) {
            INFOPANE.text = element.getInfo(analysis, save)
        }
    }

    /**
     * Resolves a URL.
     *
     * @param event The `HyperLinkEvent` to handle.
     * @see HyperlinkListener.hyperlinkUpdate
     */
    fun hyperlinkUpdate(event: HyperlinkEvent) {
        if (event.eventType == HyperlinkEvent.EventType.ENTERED) {
            if (event.source === INFOPANE) {
                val component = event.source as JComponent
                component.toolTipText = event.description
            }
            return
        } else if (event.eventType == HyperlinkEvent.EventType.EXITED) {
            if (event.source === INFOPANE) {
                val component = event.source as JComponent
                component.toolTipText = null
            }
            return
        }
        val URL = event.description
        logger.info{"Resolving URL: $URL"}
        val MATCHER = URLPATTERN.matcher(URL)
        if (!MATCHER.find()) {
            logger.warn("URL could not be resolved: $URL")
            return
        }
        val TYPE = MATCHER.group("type")
        val ADDRESS = MATCHER.group("address")
        var index1: Int? = null
        try {
            index1 = MATCHER.group("target1")?.toInt()
        } catch (ex: NumberFormatException) {
        } catch (ex: NullPointerException) {
        }
        var index2: Int? = null
        try {
            index2 = MATCHER.group("target2")?.toInt()
        } catch (ex: NumberFormatException) {
        } catch (ex: NullPointerException) {
        }
        val CONTEXT = save!!.papyrus!!.context
        try {
            when (TYPE) {
                "string" -> {
                    val stringIndex = ADDRESS.toInt()
                    this.findElement(CONTEXT.getTString(stringIndex))
                }
                "plugin" -> save!!.pluginInfo.stream()
                    .filter { v: Plugin -> v.NAME.equals(ADDRESS, ignoreCase = true) }
                    .findAny()
                    .ifPresent { element: Plugin? -> this.findElement(element) }
                "refid" -> {
                    val REFID = CONTEXT.makeRefID(ADDRESS.toInt(16))
                    this.findElement(CONTEXT.getChangeForm(REFID))
                }
                "script" -> {
                    val NAME = save!!.papyrus!!.stringTable.resolve(ADDRESS)
                    if (index1 != null) {
                        this.findElement(CONTEXT.findScript(NAME), index1)
                    } else {
                        this.findElement(CONTEXT.findScript(NAME))
                    }
                }
                "struct" -> {
                    val NAME = save!!.papyrus!!.stringTable.resolve(ADDRESS)
                    if (index1 != null) {
                        this.findElement(CONTEXT.findStruct(NAME), index1)
                    } else {
                        this.findElement(CONTEXT.findStruct(NAME))
                    }
                }
                "scriptinstance" -> {
                    val ID = CONTEXT.makeEID(ADDRESS.toULong(16))
                    if (index1 != null) {
                        this.findElement(CONTEXT.findScriptInstance(ID), index1)
                    } else {
                        this.findElement(CONTEXT.findScriptInstance(ID))
                    }
                }
                "structinstance" -> {
                    val ID = CONTEXT.makeEID(ADDRESS.toULong(16))
                    if (index1 != null) {
                        this.findElement(CONTEXT.findStructInstance(ID), index1)
                    } else {
                        this.findElement(CONTEXT.findStructInstance(ID))
                    }
                }
                "reference" -> {
                    val ID = CONTEXT.makeEID(ADDRESS.toULong(16))
                    if (index1 != null) {
                        this.findElement(CONTEXT.findReference(ID), index1)
                    } else {
                        this.findElement(CONTEXT.findReference(ID))
                    }
                }
                "array" -> {
                    val ID = CONTEXT.makeEID(ADDRESS.toULong(16))
                    if (index1 != null) {
                        this.findElement(CONTEXT.findArray(ID), index1)
                    } else {
                        this.findElement(CONTEXT.findArray(ID))
                    }
                }
                "thread" -> {
                    val ID = CONTEXT.makeEID32(UtilityFunctions.parseUnsignedInt(ADDRESS, 16))
                    if (index1 != null) {
                        this.findElement(CONTEXT.findActiveScript(ID), index1)
                    } else {
                        this.findElement(CONTEXT.findActiveScript(ID))
                    }
                }
                "suspended" -> {
                    val ID = CONTEXT.makeEID32(UtilityFunctions.parseUnsignedInt(ADDRESS, 16))
                    val STACK = save!!.papyrus!!.suspendedStacks[ID]
                    if (index1 != null) {
                        this.findElement(STACK, index1)
                    } else {
                        this.findElement(STACK)
                    }
                }
                "unbind" -> {
                    val ID = CONTEXT.makeEID(ADDRESS.toULong(16))
                    val UNBIND = save!!.papyrus!!.unbinds[ID]
                    if (index1 != null) {
                        this.findElement(UNBIND, index1)
                    } else {
                        this.findElement(UNBIND)
                    }
                }
                "message" -> {
                    val ID = CONTEXT.makeEID32(UtilityFunctions.parseUnsignedInt(ADDRESS, 16))
                    val MESSAGE: FunctionMessage? = save!!.papyrus!!.functionMessages.firstOrNull { it?.iD!! == ID }
                    if (index1 != null) {
                        this.findElement(MESSAGE, index1)
                    } else {
                        this.findElement(MESSAGE)
                    }
                }
                "frame" -> {
                    val ID = CONTEXT.makeEID32(UtilityFunctions.parseUnsignedInt(ADDRESS, 16))
                    val THREAD = CONTEXT.findActiveScript(ID)
                    if (THREAD != null && index1 != null) {
                        val FRAME = THREAD.stackFrames[index1]
                        if (index2 != null) {
                            this.findElement(FRAME, index2)
                        } else {
                            this.findElement(FRAME)
                        }
                    }
                }
            }
        } catch (ex: NumberFormatException) {
            logger.warn{"Invalid address: $URL"}
        } catch (ex: IndexOutOfBoundsException) {
            logger.warn{"Invalid address: $URL"}
        }
    }

    /**
     * Try to initialize JavaFX.
     *
     * @return An uncast `JFXPanel` object, or null if JavaFX could
     * not be found.
     */
    fun initializeJavaFX(): Any? {
        return try {
            val CLASS_JFXPANEL = Class.forName("javafx.embed.swing.JFXPanel")
            val CONSTRUCTORS = CLASS_JFXPANEL.constructors
            for (constructor in CONSTRUCTORS) {
                if (constructor.parameterCount == 0) {
                    return constructor.newInstance()
                }
            }
            null
        } catch (ex: ReflectiveOperationException) {
            logger.warn(ex){"Error initializing JavaFX."}
            null
        }
    }

    /**
     * Try to termiante JavaFX.
     *
     */
    fun terminateJavaFX() {
        try {
            if (JFXPANEL != null) {
                val CLASS_PLATFORM = Class.forName("javafx.application.Platform")
                val METHOD_EXIT = CLASS_PLATFORM.getMethod("exit")
                METHOD_EXIT.invoke(null)
            }
        } catch (ex: ReflectiveOperationException) {
            logger.warn(ex) {"Error terminating JavaFX."}
        } catch (ex: NullPointerException) {
            logger.warn(ex) { "Error terminating JavaFX."}
        }
    }

    /**
     * @return Indicates whether JavaFX was found or not.
     */
    val isJavaFXAvailable: Boolean
        get() = JFXPANEL != null && PREFS.getBoolean("settings.javafx", false)

    /**
     * Used to render cells.
     */
    private inner class ModListCellRenderer : ListCellRenderer<Mod?> {
        override fun getListCellRendererComponent(
            list: JList<out Mod>?,
            value: Mod?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            return if (null == value) {
                RENDERER.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus)
            } else RENDERER.getListCellRendererComponent(
                list,
                value.getName(),
                index,
                isSelected,
                cellHasFocus
            )
        }

        private val RENDERER = BasicComboBoxRenderer()
    }

    /**
     * Used to render cells.
     */
    private inner class PluginListCellRenderer : ListCellRenderer<Plugin?> {
        override fun getListCellRendererComponent(
            list: JList<out Plugin>?,
            value: Plugin?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            return if (null == value) {
                RENDERER.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus)
            } else RENDERER.getListCellRendererComponent(list, value.NAME, index, isSelected, cellHasFocus)
        }

        private val RENDERER = BasicComboBoxRenderer()
    }

    /**
     * Listener for tree selection events.
     */
    private var save: ESS?
    private var analysis: Analysis?
    private var modified = false
    private var filter: Predicate<FilterTreeModel.Node>?
    private var scanner: Scanner?
    private val LBL_MEMORY: MemoryLabel
    private val LBL_WATCHING: JLabel
    private val LBL_SCANNING: JLabel
    private val TREE: FilterTree
    private val TABLE: VariableTable
    private val INFOPANE: InfoPane
    private val BTN_CLEAR_FILTER: JButton
    private val TREESCROLLER: JScrollPane
    private val DATASCROLLER: JScrollPane
    private val INFOSCROLLER: JScrollPane
    private val MAINSPLITTER: JSplitPane
    private val RIGHTSPLITTER: JSplitPane?
    private val MAINPANEL: JPanel
    private val MODPANEL: JPanel
    private val MODCOMBO: JComboBox<Mod?>
    private val PLUGINCOMBO: JComboBox<Plugin>
    private val MODLABEL: JLabel
    private val FILTERPANEL: JPanel
    private val FILTERFIELD: JTreeFilterField
    private val TOPPANEL: JPanel
    private val STATUSPANEL: JPanel
    private val TREEHISTORY: JTreeHistory
    private val PROGRESSPANEL: JPanel

    /**
     * Makes the `ProgressIndicator` component available to subtasks.
     *
     * @return
     */
    val progressIndicator: ProgressIndicator
    private val MENUBAR: JMenuBar
    private val FILEMENU: JMenu
    private val DATAMENU: JMenu
    private val CLEANMENU: JMenu
    private val OPTIONSMENU: JMenu
    private val HELPMENU: JMenu
    private val MI_LOAD: JMenuItem
    private val MI_SAVE: JMenuItem
    private val MI_SAVEAS: JMenuItem
    private val MI_EXIT: JMenuItem
    private val MI_LOADESPS: JMenuItem
    private val MI_LOOKUPID: JMenuItem
    private val MI_LOOKUPBASE: JMenuItem
    private val MI_REMOVEUNATTACHED: JMenuItem
    private val MI_REMOVEUNDEFINED: JMenuItem
    private val MI_RESETHAVOK: JMenuItem
    private val MI_CLEANSEFORMLISTS: JMenuItem
    private val MI_REMOVENONEXISTENT: JMenuItem
    private val MI_BATCHCLEAN: JMenuItem
    private val MI_KILL: JMenuItem
    private val MI_SHOWLONGSTRINGS: JMenuItem
    private val MI_ANALYZE_ARRAYS: JMenuItem
    private val MI_COMPARETO: JMenuItem
    private val MI_USEMO2: JCheckBoxMenuItem
    private val MI_SHOWMODS: JCheckBoxMenuItem
    private val MI_WATCHSAVES: JCheckBoxMenuItem
    private val MI_SHOWLOG: JMenuItem
    private val MI_ABOUT: JMenuItem
    private val MI_EXPORTPLUGINS: JMenuItem
    private val MI_SETTINGS: JMenuItem
    private val MI_SHOWUNATTACHED: JCheckBoxMenuItem
    private val MI_SHOWUNDEFINED: JCheckBoxMenuItem
    private val MI_SHOWMEMBERLESS: JCheckBoxMenuItem
    private val MI_SHOWCANARIES: JCheckBoxMenuItem
    private val MI_SHOWNULLREFS: JCheckBoxMenuItem
    private val MI_SHOWNONEXISTENTCREATED: JCheckBoxMenuItem
    private val MI_SHOWDELETED: JCheckBoxMenuItem
    private val MI_SHOWEMPTY: JCheckBoxMenuItem
    private val MI_CHANGEFILTER: JValueMenuItem<Duad<Int>?>
    private val MI_CHANGEFORMFILTER: JValueMenuItem<Duad<Int>?>?
    private val LOGWINDOW: LogWindow

    /**
     *
     */
    val watcher: Watcher
    private val WORRIER: Worrier
    private val TIMER: Timer
    private val JFXPANEL: Any?

    companion object:KLoggable {
        private val PREFS = Preferences.userNodeForPackage(ReSaver::class.java)
        private val LOG = Logger.getLogger(SaveWindow::class.java.canonicalName)
        private val URLPATTERN =
            Pattern.compile("(?<type>[a-z]+)://(?<address>[^\\[\\]]+)(?:\\[(?<target1>\\d+)])?(?:\\[(?<target2>\\d+)])?$")
        override val logger: KLogger
            get() = logger()
    }

    /**
     * Create a new `SaveWindow` with a `Path`. If the
     * `Path` is a savefile, it will be opened.
     *
     * @param path The `Path` to open.
     * @param autoParse Automatically parse the specified savefile.
     */
    init {
        super.setExtendedState(PREFS.getInt("settings.extendedState", MAXIMIZED_BOTH))
        JFXPANEL = if (PREFS.getBoolean("settings.javafx", false)) initializeJavaFX() else null
        TIMER = Timer("SaveWindow timer")
        TIMER.start()
        logger.info{"Created timer."}
        save = null
        analysis = null
        filter = Predicate { true }
        scanner = null
        TREE = FilterTree()
        TREESCROLLER = JScrollPane(TREE)
        TOPPANEL = JPanel()
        MODPANEL = JPanel(FlowLayout(FlowLayout.LEADING))
        MODLABEL = JLabel("Mod Filter:")
        MODCOMBO = JComboBox()
        PLUGINCOMBO = JComboBox()
        FILTERFIELD = JTreeFilterField({ updateFilters(false) }, PREFS["settings.regex", ""])
        FILTERPANEL = JPanel(FlowLayout(FlowLayout.LEADING))
        MAINPANEL = JPanel(BorderLayout())
        PROGRESSPANEL = JPanel()
        progressIndicator = ProgressIndicator()
        STATUSPANEL = JPanel(BorderLayout())
        TREEHISTORY = JTreeHistory(TREE)
        TABLE = VariableTable(this)
        INFOPANE = InfoPane(null) { event: HyperlinkEvent -> hyperlinkUpdate(event) }
        DATASCROLLER = JScrollPane(TABLE)
        INFOSCROLLER = JScrollPane(INFOPANE)
        RIGHTSPLITTER = JSplitPane(JSplitPane.VERTICAL_SPLIT, INFOSCROLLER, DATASCROLLER)
        MAINSPLITTER = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, MAINPANEL, RIGHTSPLITTER)
        MENUBAR = JMenuBar()
        FILEMENU = JMenu("File")
        CLEANMENU = JMenu("Clean")
        OPTIONSMENU = JMenu("Options")
        DATAMENU = JMenu("Data")
        HELPMENU = JMenu("Help")
        MI_EXIT = JMenuItem("Exit", KeyEvent.VK_E)
        MI_LOAD = JMenuItem("Open", KeyEvent.VK_O)
        MI_LOADESPS = JMenuItem("Parse ESP/ESMs.", KeyEvent.VK_P)
        MI_SAVE = JMenuItem("Save", KeyEvent.VK_S)
        MI_SAVEAS = JMenuItem("Save As", KeyEvent.VK_A)
        MI_EXPORTPLUGINS = JMenuItem("Export plugin list", KeyEvent.VK_X)
        MI_SETTINGS = JMenuItem("Settings")
        MI_WATCHSAVES = JCheckBoxMenuItem("Watch Savefile Directory", PREFS.getBoolean("settings.watch", false))
        MI_USEMO2 = JCheckBoxMenuItem("Mod Organizer 2 integration", PREFS.getBoolean("settings.useMO2", false))
        MI_SHOWUNATTACHED = JCheckBoxMenuItem("Show unattached instances", false)
        MI_SHOWUNDEFINED = JCheckBoxMenuItem("Show undefined elements", false)
        MI_SHOWMEMBERLESS = JCheckBoxMenuItem("Show memberless instances", false)
        MI_SHOWCANARIES = JCheckBoxMenuItem("Show zeroed canaries", false)
        MI_SHOWNULLREFS = JCheckBoxMenuItem("Show Formlists containg nullrefs", false)
        MI_SHOWNONEXISTENTCREATED = JCheckBoxMenuItem("Show non-existent-form instances", false)
        MI_SHOWLONGSTRINGS = JCheckBoxMenuItem("Show long strings (512ch or more)", false)
        MI_SHOWDELETED = JCheckBoxMenuItem("Show cell(-1) changeforms", false)
        MI_SHOWEMPTY = JCheckBoxMenuItem("Show empty REFR", false)
        MI_CHANGEFILTER = JValueMenuItem("ChangeFlag filter (%s)", null)
        MI_CHANGEFORMFILTER = JValueMenuItem("ChangeFormFlag filter (%s)", null)
        MI_REMOVEUNATTACHED = JMenuItem("Remove unattached instances", KeyEvent.VK_1)
        MI_REMOVEUNDEFINED = JMenuItem("Remove undefined elements", KeyEvent.VK_2)
        MI_RESETHAVOK = JMenuItem("Reset Havok", KeyEvent.VK_3)
        MI_CLEANSEFORMLISTS = JMenuItem("Purify FormLists", KeyEvent.VK_4)
        MI_REMOVENONEXISTENT = JMenuItem("Remove non-existent form instances", KeyEvent.VK_5)
        MI_BATCHCLEAN = JMenuItem("Batch Clean", KeyEvent.VK_6)
        MI_KILL = JMenuItem("Kill Listed")
        MI_SHOWMODS = JCheckBoxMenuItem("Show Mod Filter box", PREFS.getBoolean("settings.showMods", false))
        MI_LOOKUPID = JMenuItem("Lookup ID by name")
        MI_LOOKUPBASE = JMenuItem("Lookup base object/npc")
        MI_ANALYZE_ARRAYS = JMenuItem("Analyze Arrays Block")
        MI_COMPARETO = JMenuItem("Compare To")
        MI_SHOWLOG = JMenuItem("Show Log", KeyEvent.VK_S)
        MI_ABOUT = JMenuItem("About", KeyEvent.VK_A)
        BTN_CLEAR_FILTER = JButton("Clear Filters")
        LOGWINDOW = LogWindow()
        LBL_MEMORY = MemoryLabel()
        LBL_WATCHING = JLabel("WATCHING")
        LBL_SCANNING = JLabel("SCANNING")
        WORRIER = Worrier()
        watcher = Watcher(this, WORRIER)
        initComponents(path, autoParse)
        TIMER.stop()
        logger.info{"Version: $version"}
        logger.info{"SaveWindow constructed; took ${TIMER.formattedTime}."}
    }
}