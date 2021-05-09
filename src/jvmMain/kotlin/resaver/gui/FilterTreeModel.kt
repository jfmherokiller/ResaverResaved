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

import ess.ESS
import ess.Element
import ess.Plugin
import ess.papyrus.ActiveScript
import ess.papyrus.FunctionMessage
import ess.papyrus.HasID
import ess.papyrus.SuspendedStack
import java.util.function.Predicate
import java.util.logging.Logger
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

/**
 * A `TreeModel` that supports filtering.
 *
 * @author Mark Fairchild
 */
class FilterTreeModel : TreeModel {
    /**
     *
     * @param elements
     */
    fun deleteElements(elements: Set<Element?>) {
        this.deleteElements(this.root!!, elements)
    }

    /**
     *
     * @param node
     * @param elements
     */
    private fun deleteElements(node: Node, elements: Set<Element?>) {
        assert(!node.isLeaf)
        if (!node.isLeaf) {
            val iterator:MutableIterator<Node?> = node.children!!.iterator()
            while (iterator.hasNext()) {
                val child = iterator.next()!!
                if (child.hasElement() && elements.contains(child.element)) {
                    val path = getPath(child)
                    iterator.remove()
                    node.countLeaves()
                    fireTreeNodesRemoved(TreeModelEvent(this, path))
                    LOG.info(String.format("Deleting treepath: %s", path))
                } else if (!child.isLeaf) {
                    this.deleteElements(child, elements)
                }
            }
        }
    }

    /**
     * Transforms a `TreePath` array into a map of elements and their
     * corresponding nodes.
     *
     * @param paths
     * @return
     */
    fun parsePaths(paths: Array<TreePath>): Map<Element, Node> {
        val ELEMENTS: MutableMap<Element, Node> = mutableMapOf()
        for (path in paths) {
            val NODE = path.lastPathComponent as Node
            if (NODE.hasElement()) {
                if(NODE.element != null) {
                    ELEMENTS[NODE.element!!] = NODE
                }
            } else {
                ELEMENTS.putAll(parsePath(NODE))
            }
        }
        return ELEMENTS
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
        if (null == this.root) {
            return null
        } else if (this.root!!.hasElement(element)) {
            return getPath(this.root!!)
        }
        var path = this.findPath(this.root!!, element)
        if (null != path) {
            return path
        }
        path = findPathUnfiltered(this.root!!, element)
        if (null != path) {
            //this.root.defilter(path, 0);
            defilter(path)
            return path
        }
        return null
    }

    /**
     * Finds a path from a `Node` down to an `Element`.
     *
     * @param node The `Node` to search from.
     * @param element The `Element` for which to search.
     * @return A `TreePath` to the `Element`, or
     * `null` if it is not a leaf of this node.
     */
    private fun findPath(node: Node, element: Element?): TreePath? {
        if (node === this.root) {
            for (c in node.children!!) {
                if (c?.isVisible == true) {
                    val path = findPath(c, element)
                    if (path != null) {
                        return path
                    }
                }
            }
            return null
        } else if (!node.isLeaf) {
            for (child in node.children!!) {
                if (child?.isVisible == true) {
                    if (child.hasElement(element)) {
                        return getPath(child)
                    }
                    val path = this.findPath(child, element)
                    if (path != null) {
                        return path
                    }
                }
            }
            return null
        }
        return null
    }

    /**
     * Finds a path from a `Node` to an `Element`,
     * ignoring filtering.
     *
     * @param node The `Node` to search from.
     * @param element The `Element` for which to search.
     * @return A `TreePath` to the `Element`, or
     * `null` if it is not a leaf of this node.
     */
    private fun findPathUnfiltered(node: Node, element: Element?): TreePath? {
        if (node === this.root) {
            for (v in node.children!!) {
                val pathUnfiltered = v?.let { findPathUnfiltered(it, element) }
                if (pathUnfiltered != null) {
                    return pathUnfiltered
                }
            }
            return null
        } else if (!node.isLeaf) {
            for (child in node.children!!) {
                if (child?.hasElement(element) == true) {
                    return getPath(child)
                }
                val path = child?.let { findPathUnfiltered(it, element) }
                if (path != null) {
                    return path
                }
            }
            return null
        }
        return null
    }

    /**
     * Generates a `TreePath` from the root to a specified
     * `Node`.
     *
     * @param node The `Node` to search from.
     * @return
     */
    fun getPath(node: Node): TreePath {
        return if (node.parent == null) {
            TreePath(node)
        } else {
            getPath(node.parent!!).pathByAddingChild(node)
        }
    }

    /**
     * Retrieves the node's element and all the elements of its children.
     *
     * @return
     */
    val elements: List<Element?>
        get() = if (null == this.root) {
            listOf()
        } else {
            getElements(this.root!!)
        }

    /**
     * Retrieves the node's element and all the elements of its children.
     *
     * @return
     */
    private fun getElements(node: Node): List<Element?> {
        val collected: MutableList<Element?> = mutableListOf()
        if (node.hasElement() && node.isVisible) {
            collected.add(node.element)
        }
        if (!node.isLeaf) {
            for (n in node.children!!) {
                if (n?.isVisible == true) {
                    if (n.hasElement() && n.isLeaf) {
                        collected.add(n.element)
                    } else {
                        collected.addAll(getElements(n))
                    }
                }
            }
        }
        return collected
    }

    /**
     * Refreshes names.
     *
     */
    fun refresh() {
        if (null == this.root) {
            return
        }
        this.root!!.countLeaves()
        fireTreeNodesChanged(TreeModelEvent(this.root, getPath(this.root!!)))
    }

    /**
     * Removes all filtering.
     *
     */
    fun removeFilter() {
        if (null == this.root) {
            return
        }
        this.removeFilter(this.root!!)
        fireTreeNodesChanged(TreeModelEvent(this.root, getPath(this.root!!)))
    }

    /**
     * Removes all filtering on a node and its children.
     *
     * @param
     */
    private fun removeFilter(node: Node) {
        node.isVisible = true
        if (!node.isLeaf) {
            node.children!!.forEach { node: Node? ->
                if (node != null) {
                    this.removeFilter(node)
                }
            }
            node.countLeaves()
        }
    }

    /**
     * Filters the model and its contents.
     *
     * @param filter The setFilter that determines which nodes to keep.
     */
    fun setFilter(filter: Predicate<Node>) {
        if (null == this.root) {
            return
        }
        for (node in this.root!!.children!!) {
            if (node != null) {
                this.setFilter(node, filter)
            }
        }
        this.root!!.countLeaves()
        LISTENERS.forEach { l: TreeModelListener ->
            l.treeStructureChanged(
                TreeModelEvent(
                    this.root, getPath(
                        this.root!!
                    )
                )
            )
        }
    }

    /**
     * Filters the node and its contents. NEVER CALL THIS ON THE ROOT DIRECTLY.
     *
     * @param node The `Node` to search.
     * @param filter The setFilter that determines which nodes to keep.
     * @return True if the node is still visible.
     */
    private fun setFilter(node: Node, filter: Predicate<Node>) {

        // Determine if the node itself would be filtered out.
        // Never setFilter the root!
        val nodeVisible = filter.test(node)
        if (node.isLeaf) {
            // If there are no children, finish up.
            node.isVisible = nodeVisible
        } else if (node.hasElement() && nodeVisible) {
            // For Elements that contain other elements, don't setFilter
            // children at all unless the Element itself is filtered.
            // Don't apply this to the root!
            node.isVisible = true
        } else {
            // For folders, determine which children to setFilter.
            for (child in node.children!!) {
                if (child != null) {
                    this.setFilter(child, filter)
                }
            }
            var hasVisibleChildren = false
            for (node1 in node.children!!) {
                if (node1!!.isVisible) {
                    hasVisibleChildren = true
                    break
                }
            }
            node.isVisible = nodeVisible || hasVisibleChildren
        }
    }

    /**
     * After rebuilding the treemodel, paths become invalid. This corrects them.
     *
     * @param path
     * @return
     */
    fun rebuildPath(path: TreePath): TreePath? {
        if (path.pathCount < 1) {
            return null
        }
        var newPath = TreePath(this.root!!)
        var newNode = this.root
        for (i in 1 until path.pathCount) {
            val originalNode = path.getPathComponent(i) as Node
            var child: Node? = null
            for (node in newNode!!.children!!) {
                if (node!!.name == originalNode.name || node.hasElement(originalNode.element)) {
                    child = node
                    break
                }
            }
            if (child == null) {
                if (originalNode.hasElement() && originalNode.element is HasID) {
                    val original = originalNode.element as HasID?
                    var found: Node? = null
                    for (n in newNode.children!!) {
                        if (n!!.hasElement() && n.element is HasID) {
                            if ((n.element as HasID?)!!.iD === original!!.iD) {
                                found = n
                                break
                            }
                        }
                    }
                    child = found
                }
            }
            if (child == null) {
                return newPath
            }
            newNode = child
            newPath = newPath.pathByAddingChild(newNode)
        }
        return newPath
    }

    /**
     * Defilters along a path.
     *
     * @param path
     */
    fun defilter(path: TreePath) {
        val PARENT = path.parentPath
        PARENT?.let { defilter(it) }
        val NODE = path.lastPathComponent as Node
        if (!NODE.isVisible) {
            NODE.isVisible = true
            fireTreeNodesInserted(TreeModelEvent(this, path))
        }
    }

    /**
     * @param node The `Node` to search.
     * @return A `List` of every `Element` contained by
     * the descendents of the `Node` (not including the node itself).
     */
    private fun parsePath(node: Node): Map<Element, Node> {
        val ELEMENTS: MutableMap<Element, Node> = mutableMapOf()
        if (!node.isLeaf) {
            for (child in node.children!!) {
                if (child?.isVisible == true) {
                    if (child.hasElement()) {
                        if(child.element != null) {
                            ELEMENTS[child.element!!] = child
                        }
                    }
                    ELEMENTS.putAll(parsePath(child))
                }
            }
        }
        return ELEMENTS
    }

    override fun getRoot(): Node? {
        return this.root
    }

    fun setRoot(newRoot: Node?) {
        this.root = newRoot
    }

    /**
     * Retrieves the child at the specified index.
     *
     * @param parent The parent `Node`.
     * @param index The index of the child to retrieve.
     * @return The child at the specified index, or `null` if the
     * node is a leaf or the index is invalid.
     */
    override fun getChild(parent: Any, index: Int): Any? {
        assert(parent is Node)
        val NODE = parent as Node
        check(!NODE.isLeaf) { "Leaves don't have children!!" }
        var i = 0
        for (child in NODE.children!!) {
            if (child!!.isVisible) {
                if (i == index) {
                    return child
                }
                i++
            }
        }
        return null
    }

    override fun getChildCount(parent: Any): Int {
        assert(parent is Node)
        val NODE = parent as Node
        return if (NODE.isLeaf) {
            0
        } else {
            var count = 0
            for (child in NODE.children!!) {
                if (child!!.isVisible) {
                    count++
                }
            }
            count
        }
    }

    override fun getIndexOfChild(parent: Any, target: Any): Int {
        val PARENT = parent as Node
        if (!PARENT.isLeaf) {
            var i = 0
            for (child in PARENT.children!!) {
                if (child!!.isVisible) {
                    if (child === target) {
                        return i
                    }
                    i++
                }
            }
        }
        return -1
    }

    override fun isLeaf(node: Any): Boolean {
        assert(node is Node)
        val NODE = node as Node
        return NODE.isLeaf
    }

    override fun valueForPathChanged(path: TreePath, newValue: Any) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun addTreeModelListener(l: TreeModelListener) {
        LISTENERS.add(l)
    }

    override fun removeTreeModelListener(l: TreeModelListener) {
        LISTENERS.remove(l)
    }

    /**
     * @see TreeModelListener.treeNodesChanged
     * @param event
     */
    private fun fireTreeNodesChanged(event: TreeModelEvent) {
        LISTENERS.forEach { listener: TreeModelListener -> listener.treeNodesChanged(event) }
    }

    /**
     * @see  TreeModelListener.treeNodesInserted
     * @param event
     */
    private fun fireTreeNodesInserted(event: TreeModelEvent) {
        LISTENERS.forEach { listener: TreeModelListener -> listener.treeNodesInserted(event) }
    }

    /**
     * @see TreeModelListener.treeNodesRemoved
     * @param event
     */
    private fun fireTreeNodesRemoved(event: TreeModelEvent) {
        LISTENERS.forEach { listener: TreeModelListener -> listener.treeNodesRemoved(event) }
    }

    private var root: Node?
    val LISTENERS: MutableList<TreeModelListener> = mutableListOf()

    /**
     * A node class that wraps an `Element` or string provides
     * filtering.
     *
     */
    abstract class Node : Comparable<Node> {
        /**
         * @return The `Collection` of children, or null if the
         * `Node` is a leaf.
         */
        abstract val children: MutableCollection<Node?>?

        /**
         * @return A flag indicating if the `Node` has an element.
         */
        abstract fun hasElement(): Boolean

        /**
         * @return The element, if any.
         */
        abstract val element: Element?

        /**
         * @param <T>
         * @param cls
         * @return The element.
        </T> */
        fun <T> getAs(cls: Class<T>): T? {
            return if (this.hasElement(cls)) {
                cls.cast(element)
            } else {
                null
            }
        }

        /**
         * @param check The `Element` to check for.
         * @return The element.
         */
        fun hasElement(check: Element?): Boolean {
            return this.hasElement() && element === check
        }

        /**
         * @param check The `Element` to check for.
         * @return The element.
         */
        fun hasElement(check: Class<*>): Boolean {
            return this.hasElement() && check.isInstance(element)
        }

        /**
         * @return A flag indicating if the `Node` is a leaf.
         */
        abstract val isLeaf: Boolean

        /**
         * @return The name of the `Node`.
         */
        abstract val name: String

        /**
         * Keeps the leaf and labels up to date.
         *
         * @return The leaf count for the `Node`.
         */
        abstract fun countLeaves(): Int
        override fun compareTo(other: Node): Int {
            return this.toString().compareTo(other.toString())
        }
        /**
         * @return The parent of the `Node`.
         */
        /**
         * @param newParent The new parent `Node`.
         */
        var parent: Node? = null
        /**
         * @return True if the node will be visible, false if it is filtered.
         */
        /**
         * Sets the `Node` to be visible or filtered.
         *
         * @param visible The visibility status of the `Node`.
         */
        var isVisible = true
    }

    /**
     * A node class that wraps an `Element` or string provides
     * filtering.
     *
     */
    open class ContainerNode : Node {
        /**
         * Creates a new container `Node`.
         *
         * @param name The name of the container.
         */
        constructor(name: CharSequence) {
            this.name = name.toString()
            CHILDREN = ArrayList()
            countLeaves()
        }

        /**
         * Creates a new container `Node`.
         *
         * @param name The name of the container.
         * @param elements A list of elements with which to populate the
         * `Node`.
         */
        constructor(name: CharSequence, elements: Collection<Element?>) {
            this.name = name.toString()
            val list: MutableList<Node?> = mutableListOf()
            for (element in elements) {
                val elementNode: ElementNode<out Element?> = ElementNode(element)
                list.add(elementNode)
            }
            CHILDREN = list
            CHILDREN.forEach { child: Node? -> child!!.parent = this }
            countLeaves()
        }

        /**
         * Adds a `Collection` of children.
         *
         * @param children The children to add.
         * @return The `Node` itself, to allow for chaining.
         */
        fun addAll(children: Collection<Node?>): ContainerNode {
            if (children.contains(null)) {
                throw NullPointerException()
            }
            for (child in children) {
                child!!.parent = this
            }
            CHILDREN.addAll(children)
            countLeaves()
            return this
        }

        /**
         * Sorts the children of the node.
         *
         * @return The `Node` itself, to allow for chaining.
         */
        fun sort(): ContainerNode {
            CHILDREN.sortWith { n1: Node?, n2: Node? ->
                n1.toString().compareTo(n2.toString(), ignoreCase = true)
            }
            return this
        }

        override val children: MutableCollection<Node?>?
            get() = CHILDREN

        override fun hasElement(): Boolean {
            return false
        }

        override val element: Element?
            get() = null
        override val isLeaf: Boolean
            get() = false

        final override fun countLeaves(): Int {
            var leafCount = 0
            for (node in children!!) {
                if (node!!.isVisible) {
                    val countLeaves = node.countLeaves()
                    leafCount += countLeaves
                }
            }
            label = if (isLeaf) name else "$name ($leafCount)"
            return leafCount
        }

        override fun toString(): String {
            return label!!
        }

        final override val name: String
        private val CHILDREN: MutableList<Node?>
        private var label: String? = null
    }

    /**
     * A node class that wraps an `Element` or string provides
     * filtering.
     *
     * @param <T>
    </T> */
    open class ElementNode<T : Element?>(element: T) : Node() {
        override val element: Element?
            get() = ELEMENT
        override val children: MutableCollection<Node?>?
            get() = null

        override fun hasElement(): Boolean {
            return true
        }

        override val isLeaf: Boolean
            get() = true

        override fun toString(): String {
            if (null == label) {
                label = name
            }
            return label!!
        }

        override val name: String
            get() = ELEMENT.toString()

        override fun countLeaves(): Int {
            label = name
            return 1
        }

        private val ELEMENT: T = element
        protected var label: String?

        /**
         * Creates a new `Node` to wrap the specified element.
         *
         * @param element The element that the node will contain.
         */
        init {
            label = null
        }
    }

    /**
     * A node class that wraps a `Plugin`.
     *
     */
    class PluginNode
    /**
     * Creates a new `Node` to wrap the specified element.
     *
     * @param element The element that the node will contain.
     */
        (element: Plugin) : ElementNode<Plugin?>(element) {
        override val name: String
            get() = (element as Plugin).indexName()
    }

    /**
     * A node class that wraps an `Element` or string provides
     * filtering.
     *
     */
    class RootNode(root: ESS, children: Collection<Node?>) : ContainerNode(root.toString()) {
        override val element: Element
            get() = ROOT

        override fun hasElement(): Boolean {
            return true
        }

        private val ROOT: ESS = root

        init {
            super.addAll(children)
        }
    }

    /**
     * A node class that wraps an `ActiveScript`.
     *
     */
    class ActiveScriptNode(element: ActiveScript) : ElementNode<ActiveScript?>(element) {
        override fun countLeaves(): Int {
            return 1
        }

        override val isLeaf: Boolean
            get() = CHILDREN == null
        override val children: MutableCollection<Node?>?
            get() = CHILDREN?.toMutableList()
        private var CHILDREN: List<Node?>? = null

        init {
            if (element.hasStack()) {
                val list: MutableList<Node?> = mutableListOf()
                for (stackFrame in element.stackFrames) {
                    val stackFrameElementNode = ElementNode(stackFrame)
                    list.add(stackFrameElementNode)
                }
                CHILDREN = list
                CHILDREN?.forEach { child: Node? -> child!!.parent = this }
            } else {
                CHILDREN = null
            }
        }
    }

    /**
     * A node class that wraps an `SuspendedStack`.
     *
     */
    class SuspendedStackNode(element: SuspendedStack) : ElementNode<SuspendedStack?>(element) {
        override fun countLeaves(): Int {
            return 1
        }

        override val isLeaf: Boolean
            get() = CHILDREN == null
        override val children: MutableCollection<Node?>?
            get() = CHILDREN?.toMutableList()
        private var CHILDREN: List<Node?>? = null

        init {
            if (element.hasMessage()) {
                val CHILD = ElementNode(element.message)
                CHILD.parent = this
                CHILDREN = listOf(CHILD)
            } else {
                CHILDREN = null
            }
        }
    }

    /**
     * A node class that wraps an `FunctionMessage`.
     *
     */
    class FunctionMessageNode(element: FunctionMessage) : ElementNode<FunctionMessage?>(element) {
        override fun countLeaves(): Int {
            return 1
        }

        override val isLeaf: Boolean
            get() = CHILDREN == null
        override val children: MutableCollection<Node?>?
            get() = CHILDREN?.toMutableList()
        private var CHILDREN: List<Node?>? = null

        init {
            if (element.hasMessage()) {
                val CHILD = ElementNode(element.message)
                CHILD.parent = this
                CHILDREN = listOf(CHILD)
            } else {
                CHILDREN = null
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(FilterTreeModel::class.java.canonicalName)
    }

    /**
     *
     */
    init {
        this.root = null
    }
}