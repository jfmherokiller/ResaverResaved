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

import resaver.ess.*
import resaver.ess.papyrus.ActiveScript
import resaver.ess.papyrus.ArrayInfo
import resaver.ess.papyrus.StackFrame
import resaver.ess.papyrus.Variable
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
import javax.swing.*
import javax.swing.tree.TreePath

/**
 * A JTree that supports filtering.
 *
 * @author Mark Fairchild
 */
class FilterTree : JTree(FilterTreeModel()) {
    /**
     * Initialize the swing and AWT components.
     */
    private fun initComponents() {
        this.isLargeModel = true
        this.isRootVisible = true
        setShowsRootHandles(true)
        TREE_POPUP_MENU.add(MI_DELETE)
        TREE_POPUP_MENU.add(MI_ZERO_THREAD)
        TREE_POPUP_MENU.add(MI_FIND_OWNER)
        TREE_POPUP_MENU.add(MI_CLEANSE_FLST)
        TREE_POPUP_MENU.add(MI_PURGES)
        PLUGIN_POPUP_MENU.add(MI_PURGE)
        PLUGIN_POPUP_MENU.add(MI_FILTER)
        PLUGIN_POPUP_MENU.add(MI_DELETE_FORMS)
        PLUGIN_POPUP_MENU.add(MI_DELETE_INSTANCES)
        COMPRESSION_POPUP_MENU.add(MI_COMPRESS_UNCOMPRESSED)
        COMPRESSION_POPUP_MENU.add(MI_COMPRESS_ZLIB)
        COMPRESSION_POPUP_MENU.add(MI_COMPRESS_LZ4)
        COMPRESSION_GROUP.add(MI_COMPRESS_UNCOMPRESSED)
        COMPRESSION_GROUP.add(MI_COMPRESS_ZLIB)
        COMPRESSION_GROUP.add(MI_COMPRESS_LZ4)
        this.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected")
        actionMap.put("deleteSelected", object : AbstractAction() {
            override fun actionPerformed(evt: ActionEvent) {
                deleteNodes()
            }
        })
        MI_DELETE.addActionListener { e: ActionEvent? -> deleteNodes() }
        MI_FILTER.addActionListener { e: ActionEvent? ->
            if (null != pluginFilterHandler) {
                val plugin = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element as Plugin
                pluginFilterHandler!!.accept(plugin)
            }
        }
        MI_PURGE.addActionListener { e: ActionEvent? ->
            val plugin = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element as Plugin
            if (null != purgeHandler) {
                purgeHandler!!.accept(setOf(plugin))
            }
        }
        MI_PURGES.addActionListener { e: ActionEvent? ->
            if (null != purgeHandler) {
                val PATHS = selectionPaths
                if (null == PATHS || PATHS.isEmpty()) {
                    return@addActionListener
                }
                val ELEMENTS = model?.parsePaths(PATHS)
                val PLUGINS: MutableList<Plugin> = ArrayList()
                ELEMENTS?.keys?.forEach { v ->
                    if (v is Plugin) {
                        PLUGINS.add(v)
                    }
                }
                purgeHandler!!.accept(PLUGINS)
            }
        }
        MI_DELETE_FORMS.addActionListener { e: ActionEvent? ->
            val plugin = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element as Plugin
            if (null != deleteFormsHandler) {
                deleteFormsHandler!!.accept(plugin)
            }
        }
        MI_DELETE_INSTANCES.addActionListener { e: ActionEvent? ->
            val plugin = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element as Plugin
            if (null != deleteInstancesHandler) {
                deleteInstancesHandler!!.accept(plugin)
            }
        }
        MI_ZERO_THREAD.addActionListener { e: ActionEvent? ->
            if (null != zeroThreadHandler) {
                val PATHS = selectionPaths
                if (null == PATHS || PATHS.isEmpty()) {
                    return@addActionListener
                }
                val ELEMENTS = model?.parsePaths(PATHS)
                val THREADS: MutableList<ActiveScript> = ELEMENTS?.keys
                    ?.asSequence()
                    ?.filter { ESS.THREAD.test(it) }
                    ?.map { it as ActiveScript }
                    ?.toMutableList()?.toList()!!.toMutableList()
                zeroThreadHandler!!.accept(THREADS)
            }
        }
        MI_FIND_OWNER.addActionListener { e: ActionEvent? ->
            val element = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element
            if (null != findHandler) {
                if (element is ActiveScript) {
                    if (null != element.instance) {
                        findHandler!!.accept(element.instance!!)
                    }
                } else if (element is StackFrame) {
                    val owner = element.owner
                    if (null != owner && owner is Variable.Ref) {
                        val ref = element.owner as Variable.Ref?
                        findHandler!!.accept(ref!!.referent)
                    }
                } else if (element is ArrayInfo) {
                    if (null != element.holder) {
                        findHandler!!.accept(element.holder!!)
                    }
                }
            }
        }
        MI_CLEANSE_FLST.addActionListener { e: ActionEvent? ->
            val form = (selectionPath?.lastPathComponent as FilterTreeModel.Node).element as ChangeForm
        }
        MI_COMPRESS_UNCOMPRESSED.addActionListener { e: ActionEvent? ->
            if (compressionHandler != null) {
                compressionHandler!!.accept(CompressionType.UNCOMPRESSED)
            }
        }
        MI_COMPRESS_ZLIB.addActionListener { e: ActionEvent? ->
            if (compressionHandler != null) {
                compressionHandler!!.accept(CompressionType.ZLIB)
            }
        }
        MI_COMPRESS_LZ4.addActionListener { e: ActionEvent? ->
            if (compressionHandler != null) {
                compressionHandler!!.accept(CompressionType.LZ4)
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(evt: MouseEvent) {
                if (evt.isPopupTrigger) {
                    val x = evt.point.x
                    val y = evt.point.y
                    val path = getClosestPathForLocation(x, y)
                    var paths = selectionPaths
                    if (!listOf(*paths!!).contains(path)) {
                        selectionPath = path
                        paths = selectionPaths
                    }
                    val ELEMENTS = model?.parsePaths(paths)
                    if (ELEMENTS?.size == 1) {
                        val ELEMENT = ELEMENTS.keys.iterator().next()
                        if (ELEMENT is ESS) {
                            val ESS = ELEMENT
                            if (ESS.supportsCompression()) {
                                when (ESS.header.getCompression()) {
                                    CompressionType.UNCOMPRESSED -> MI_COMPRESS_UNCOMPRESSED.isSelected = true
                                    CompressionType.ZLIB -> MI_COMPRESS_ZLIB.isSelected = true
                                    CompressionType.LZ4 -> MI_COMPRESS_LZ4.isSelected = true
                                }
                                COMPRESSION_POPUP_MENU.show(evt.component, evt.x, evt.y)
                            }
                        } else if (ELEMENT is GlobalVariable) {
                            if (null != editHandler) {
                                editHandler!!.accept(ELEMENT)
                            }
                        } else if (ELEMENT is Plugin) {
                            PLUGIN_POPUP_MENU.show(evt.component, evt.x, evt.y)
                        } else if (ESS.DELETABLE.test(ELEMENT) || ESS.THREAD.test(ELEMENT) || ESS.OWNABLE.test(ELEMENT)) {
                            MI_DELETE.text = "Delete (1 element)"
                            MI_DELETE.isVisible = ESS.DELETABLE.test(ELEMENT)
                            MI_ZERO_THREAD.isVisible = ESS.THREAD.test(ELEMENT)
                            MI_FIND_OWNER.isVisible = ESS.OWNABLE.test(ELEMENT)
                            //MI_CLEANSE_FLST.setVisible(ELEMENT instanceof ChangeForm && ((ChangeForm) ELEMENT).getData() instanceof ChangeFormFLST);
                            MI_CLEANSE_FLST.isVisible = false
                            TREE_POPUP_MENU.show(evt.component, evt.x, evt.y)
                        }
                    } else if (ELEMENTS?.size!! > 1) {
                        MI_FIND_OWNER.isVisible = false
                        var count = 0L
                        ELEMENTS.keys
                            .asSequence()
                            .filter { ESS.PURGEABLE.test(it) }
                            .forEach { _ -> count++ }
                        val purgeable = count.toInt()
                        MI_PURGES.isEnabled = purgeable > 0
                        MI_PURGES.text = "Purge ($purgeable plugins)"
                        var result = 0L
                        ELEMENTS.keys
                            .asSequence()
                            .filter { ESS.DELETABLE.test(it) }
                            .forEach { _ -> result++ }
                        val deletable = result.toInt()
                        MI_DELETE.isEnabled = deletable > 0
                        MI_DELETE.text = String.format("Delete (%d elements)", deletable)
                        var count1 = 0L
                        ELEMENTS.keys
                            .asSequence()
                            .filter { ESS.THREAD.test(it) }
                            .forEach { _ -> count1++ }
                        val threads = count1.toInt()
                        MI_ZERO_THREAD.isVisible = threads > 0
                        TREE_POPUP_MENU.show(evt.component, evt.x, evt.y)
                    }
                }
            }
        })
    }

    /**
     * Clears the `ESS`.
     */
    fun clearESS() {
        this.model = FilterTreeModel()
    }

    /**
     * Uses an `ESS` to create the tree's data model.
     *
     * @param ess The `ESS`.
     * @param model A `FilterTreeModel`.
     * @param filter An optional setFilter.
     */
    fun setESS(ess: ESS?, model: FilterTreeModel?, filter: Predicate<FilterTreeModel.Node>?) {
        val PATHS = this.selectionPaths
        if (null != filter) {
            model!!.setFilter(filter)
        }
        if (null != model) {
            this.model = model
        }
        if (null != PATHS) {
            for (i in PATHS.indices) {
                PATHS[i] = model?.rebuildPath(PATHS[i])
            }
            this.selectionPaths = PATHS
        }
    }

    /**
     * Searches for the `Node` that represents a specified
     * `Element` and returns it.
     *
     * @param element The `Element` to find.
     * @return The corresponding `Node` or null if the
     * `Element` was not found.
     */
    fun findPath(element: Element?): TreePath? {
        return this.model?.findPath(element)!!
    }

    /**
     * Sets the delete handler.
     *
     * @param newHandler The new delete handler.
     */
    fun setDeleteHandler(newHandler: Consumer<Map<Element, FilterTreeModel.Node>>?) {
        deleteHandler = newHandler
    }

    /**
     * Sets the edit handler.
     *
     * @param newHandler The new edit handler.
     */
    fun setEditHandler(newHandler: Consumer<Element>?) {
        editHandler = newHandler
    }

    /**
     * Sets the setFilter plugin handler.
     *
     * @param newHandler The new delete handler.
     */
    fun setFilterPluginsHandler(newHandler: Consumer<Plugin>?) {
        pluginFilterHandler = newHandler
    }

    /**
     * Sets the purge plugins handler.
     *
     * @param newHandler The new delete handler.
     */
    fun setPurgeHandler(newHandler: Consumer<Collection<Plugin>>?) {
        purgeHandler = newHandler
    }

    /**
     * Sets the delete plugin forms handler.
     *
     * @param newHandler The new delete handler.
     */
    fun setDeleteFormsHandler(newHandler: Consumer<Plugin>?) {
        deleteFormsHandler = newHandler
    }

    /**
     * Sets the delete plugin instances handler.
     *
     * @param newHandler The new delete handler.
     */
    fun setDeleteInstancesHandler(newHandler: Consumer<Plugin>?) {
        deleteInstancesHandler = newHandler
    }

    /**
     * Sets the zero active script handler.
     *
     * @param newHandler The new handler.
     */
    fun setZeroThreadHandler(newHandler: Consumer<List<ActiveScript>>?) {
        zeroThreadHandler = newHandler
    }

    /**
     * Sets the find element handler.
     *
     * @param newHandler The new handler.
     */
    fun setFindHandler(newHandler: Consumer<Element>?) {
        findHandler = newHandler
    }

    /**
     * Sets the cleanse formlist handler.
     *
     * @param newHandler The new handler.
     */
    fun setCleanseFLSTHandler(newHandler: Consumer<ChangeFormFLST>?) {
        cleanFLSTHandler = newHandler
    }

    /**
     * Sets the compression type handler.
     *
     * @param newHandler The new compression type handler.
     */
    fun setCompressionHandler(newHandler: Consumer<CompressionType>?) {
        compressionHandler = newHandler
    }

    /**
     * Deletes a node by submitting it back to the app.
     */
    private fun deleteNodes() {
        if (null == deleteHandler) {
            return
        }
        val PATHS = selectionPaths
        if (null == PATHS || PATHS.isEmpty()) {
            return
        }
        val ELEMENTS = model?.parsePaths(PATHS)
        deleteHandler!!.accept(ELEMENTS!!)
    }

    override fun getModel(): FilterTreeModel? {
        val model = super.getModel()
        if(model != null) {
            return model as FilterTreeModel
        }
        return null
    }

    private val MI_PURGE: JMenuItem = JMenuItem("Purge (1 plugin)", KeyEvent.VK_P)
    private val MI_PURGES: JMenuItem = JMenuItem("Purge (%d plugins)", KeyEvent.VK_P)
    private val MI_DELETE: JMenuItem = JMenuItem("Delete", KeyEvent.VK_D)
    private val MI_FILTER: JMenuItem = JMenuItem("Set filter for this plugin", KeyEvent.VK_F)
    private val MI_DELETE_FORMS: JMenuItem = JMenuItem("Delete plugin changeforms", KeyEvent.VK_C)
    private val MI_DELETE_INSTANCES: JMenuItem = JMenuItem("Delete plugin script instances", KeyEvent.VK_S)
    private val MI_ZERO_THREAD: JMenuItem = JMenuItem("Terminate", KeyEvent.VK_Z)
    private val MI_FIND_OWNER: JMenuItem = JMenuItem("Find owner", KeyEvent.VK_F)
    private val MI_CLEANSE_FLST: JMenuItem = JMenuItem("Cleanse Formlist", KeyEvent.VK_C)
    private val MI_COMPRESS_UNCOMPRESSED: JRadioButtonMenuItem = JRadioButtonMenuItem("No compression")
    private val MI_COMPRESS_ZLIB: JRadioButtonMenuItem = JRadioButtonMenuItem("ZLib compression")
    private val MI_COMPRESS_LZ4: JRadioButtonMenuItem = JRadioButtonMenuItem("LZ4 compression")
    val COMPRESSION_GROUP: ButtonGroup = ButtonGroup()
    private val TREE_POPUP_MENU: JPopupMenu = JPopupMenu()
    private val PLUGIN_POPUP_MENU: JPopupMenu = JPopupMenu()
    private val COMPRESSION_POPUP_MENU: JPopupMenu = JPopupMenu()
    private var deleteHandler: Consumer<Map<Element, FilterTreeModel.Node>>? =
        null
    private var zeroThreadHandler: Consumer<List<ActiveScript>>? = null
    private var editHandler: Consumer<Element>? = null
    private var pluginFilterHandler: Consumer<Plugin>? = null
    private var deleteFormsHandler: Consumer<Plugin>? = null
    private var deleteInstancesHandler: Consumer<Plugin>? = null
    private var purgeHandler: Consumer<Collection<Plugin>>? = null
    private var findHandler: Consumer<Element>? = null
    private var cleanFLSTHandler: Consumer<ChangeFormFLST>? = null
    private var compressionHandler: Consumer<CompressionType>? = null

    /**
     * Creates a new `FilterTree`.
     */
    init {
        initComponents()
    }
}