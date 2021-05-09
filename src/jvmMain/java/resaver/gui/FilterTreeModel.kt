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

import java.util.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import ess.Element;
import ess.ESS;
import ess.papyrus.*;

import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.swing.tree.TreePath;
import ess.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A <code>TreeModel</code> that supports filtering.
 *
 * @author Mark Fairchild
 */
final public class FilterTreeModel implements TreeModel {

    /**
     *
     */
    public FilterTreeModel() {
        this.LISTENERS = new java.util.LinkedList<>();
        this.root = null;
    }

    /**
     *
     * @param elements
     */
    public void deleteElements(@NotNull Set<? extends Element> elements) {
        this.deleteElements(this.root, elements);
    }

    /**
     *
     * @param node
     * @param elements
     */
    private void deleteElements(@NotNull Node node, @NotNull Set<? extends Element> elements) {
        assert !node.isLeaf();

        if (!node.isLeaf()) {
            Iterator<Node> iterator = node.getChildren().iterator();
            while (iterator.hasNext()) {
                Node child = iterator.next();
                if (child.hasElement() && elements.contains(child.getElement())) {
                    TreePath path = this.getPath(child);
                    iterator.remove();
                    node.countLeaves();
                    this.fireTreeNodesRemoved(new TreeModelEvent(this, path));
                    LOG.info(String.format("Deleting treepath: %s", path));
                } else if (!child.isLeaf()) {
                    this.deleteElements(child, elements);
                }
            }
        }
    }

    /**
     * Transforms a <code>TreePath</code> array into a map of elements and their
     * corresponding nodes.
     *
     * @param paths
     * @return
     */
    @NotNull
    public Map<Element, Node> parsePaths(@NotNull TreePath[] paths) {
        Objects.requireNonNull(paths);
        final Map<Element, Node> ELEMENTS = new LinkedHashMap<>(paths.length);

        for (TreePath path : paths) {
            if (null == path) {
                continue;
            }

            final Node NODE = (Node) path.getLastPathComponent();

            if (NODE.hasElement()) {
                ELEMENTS.put(NODE.getElement(), NODE);
            } else {
                ELEMENTS.putAll(this.parsePath(NODE));
            }
        }

        return ELEMENTS;
    }

    /**
     * Searches for the <code>Node</code> that represents a specified
     * <code>Element</code> and returns it.
     *
     * @param element The <code>Element</code> to find.
     * @return The corresponding <code>Node</code> or null if the
     * <code>Element</code> was not found.
     */
    @Nullable
    public TreePath findPath(Element element) {
        if (null == this.root) {
            return null;
        } else if (this.root.hasElement(element)) {
            return getPath(this.root);
        }

        TreePath path = this.findPath(this.root, element);
        if (null != path) {
            return path;
        }

        path = this.findPathUnfiltered(this.root, element);
        if (null != path) {
            //this.root.defilter(path, 0);
            this.defilter(path);
            return path;
        }

        return null;
    }

    /**
     * Finds a path from a <code>Node</code> down to an <code>Element</code>.
     *
     * @param node The <code>Node</code> to search from.
     * @param element The <code>Element</code> for which to search.
     * @return A <code>TreePath</code> to the <code>Element</code>, or
     * <code>null</code> if it is not a leaf of this node.
     */
    @Nullable
    private TreePath findPath(@NotNull Node node, Element element) {
        if (node == this.root) {
            for (Node c : node.getChildren()) {
                if (c.isVisible()) {
                    TreePath path = findPath(c, element);
                    if (path != null) {
                        return path;
                    }
                }
            }
            return null;

        } else if (!node.isLeaf()) {
            for (Node child : node.getChildren()) {
                if (child.isVisible()) {
                    if (child.hasElement(element)) {
                        return getPath(child);
                    }
                    TreePath path = this.findPath(child, element);
                    if (path != null) {
                        return path;
                    }
                }
            }
            return null;
        }

        return null;
    }

    /**
     * Finds a path from a <code>Node</code> to an <code>Element</code>,
     * ignoring filtering.
     *
     * @param node The <code>Node</code> to search from.
     * @param element The <code>Element</code> for which to search.
     * @return A <code>TreePath</code> to the <code>Element</code>, or
     * <code>null</code> if it is not a leaf of this node.
     */
    @Nullable
    private TreePath findPathUnfiltered(@NotNull Node node, Element element) {
        if (node == this.root) {
            for (Node v : node.getChildren()) {
                TreePath pathUnfiltered = findPathUnfiltered(v, element);
                if (pathUnfiltered != null) {
                    return pathUnfiltered;
                }
            }
            return null;

        } else if (!node.isLeaf()) {
            for (Node child : node.getChildren()) {
                if (child.hasElement(element)) {
                    return getPath(child);
                }
                TreePath path = this.findPathUnfiltered(child, element);
                if (path != null) {
                    return path;
                }
            }
            return null;
        }

        return null;
    }

    /**
     * Generates a <code>TreePath</code> from the root to a specified
     * <code>Node</code>.
     *
     * @param node The <code>Node</code> to search from.
     * @return
     */
    public TreePath getPath(@NotNull Node node) {
        if (node.getParent() == null) {
            return new TreePath(node);
        } else {
            return this.getPath(node.getParent()).pathByAddingChild(node);
        }
    }

    /**
     * Retrieves the node's element and all the elements of its children.
     *
     * @return
     */
    @NotNull
    public List<Element> getElements() {
        if (null == this.root) {
            return new LinkedList<>();
        } else {
            return this.getElements(this.root);
        }
    }

    /**
     * Retrieves the node's element and all the elements of its children.
     *
     * @return
     */
    @NotNull
    private List<Element> getElements(@NotNull Node node) {
        List<Element> collected = new LinkedList<>();

        if (node.hasElement() && node.isVisible()) {
            collected.add(node.getElement());
        }

        if (!node.isLeaf()) {
            for (Node n : node.getChildren()) {
                if (n.isVisible()) {
                    if (n.hasElement() && n.isLeaf()) {
                        collected.add(n.getElement());
                    } else {
                        collected.addAll(this.getElements(n));
                    }
                }
            }
        }

        return collected;
    }

    /**
     * Refreshes names.
     *
     */
    public void refresh() {
        if (null == this.root) {
            return;
        }
        this.root.countLeaves();
        this.fireTreeNodesChanged(new TreeModelEvent(this.root, this.getPath(this.root)));
    }

    /**
     * Removes all filtering.
     *
     */
    public void removeFilter() {
        if (null == this.root) {
            return;
        }

        this.removeFilter(this.root);
        this.fireTreeNodesChanged(new TreeModelEvent(this.root, this.getPath(this.root)));
    }

    /**
     * Removes all filtering on a node and its children.
     *
     * @param
     */
    private void removeFilter(@NotNull Node node) {
        node.setVisible(true);
        if (!node.isLeaf()) {
            node.getChildren().forEach(this::removeFilter);
            node.countLeaves();
        }
    }

    /**
     * Filters the model and its contents.
     *
     * @param filter The setFilter that determines which nodes to keep.
     */
    public void setFilter(@NotNull Predicate<Node> filter) {
        Objects.requireNonNull(filter);

        if (null == this.root) {
            return;
        }

        for (Node node : this.root.getChildren()) {
            this.setFilter(node, filter);
        }
        this.root.countLeaves();

        this.LISTENERS.forEach(l -> l.treeStructureChanged(new TreeModelEvent(this.root, this.getPath(this.root))));
    }

    /**
     * Filters the node and its contents. NEVER CALL THIS ON THE ROOT DIRECTLY.
     *
     * @param node The <code>Node</code> to search.
     * @param filter The setFilter that determines which nodes to keep.
     * @return True if the node is still visible.
     */
    private void setFilter(@NotNull Node node, @NotNull Predicate<Node> filter) {
        Objects.requireNonNull(filter);

        // Determine if the node itself would be filtered out.
        // Never setFilter the root!
        boolean nodeVisible = filter.test(node);

        if (node.isLeaf()) {
            // If there are no children, finish up.
            node.setVisible(nodeVisible);

        } else if (node.hasElement() && nodeVisible) {
            // For Elements that contain other elements, don't setFilter
            // children at all unless the Element itself is filtered.
            // Don't apply this to the root!
            node.setVisible(true);

        } else {
            // For folders, determine which children to setFilter.
            for (Node child : node.getChildren()) {
                this.setFilter(child, filter);
            }
            boolean hasVisibleChildren = false;
            for (Node node1 : node.getChildren()) {
                if (node1.isVisible()) {
                    hasVisibleChildren = true;
                    break;
                }
            }
            node.setVisible(nodeVisible || hasVisibleChildren);
        }
    }

    /**
     * After rebuilding the treemodel, paths become invalid. This corrects them.
     *
     * @param path
     * @return
     */
    @Nullable
    public TreePath rebuildPath(@NotNull TreePath path) {
        Objects.requireNonNull(path);
        if (path.getPathCount() < 1) {
            return null;
        }

        TreePath newPath = new TreePath(this.root);
        Node newNode = this.root;

        for (int i = 1; i < path.getPathCount(); i++) {
            Node originalNode = (Node) path.getPathComponent(i);
            Optional<Node> child = Optional.empty();
            for (Node node : newNode.getChildren()) {
                if (node.getName().equals(originalNode.getName()) || (node.hasElement(originalNode.getElement()))) {
                    child = Optional.of(node);
                    break;
                }
            }

            if (!child.isPresent()) {
                if (originalNode.hasElement() && originalNode.getElement() instanceof HasID) {
                    HasID original = (HasID) originalNode.getElement();
                    Optional<Node> found = Optional.empty();
                    for (Node n : newNode.getChildren()) {
                        if (n.hasElement() && n.getElement() instanceof HasID) {
                            if (((HasID) n.getElement()).getID() == original.getID()) {
                                found = Optional.of(n);
                                break;
                            }
                        }
                    }
                    child = found;
                }
            }

            if (!child.isPresent()) {
                return newPath;
            }

            newNode = child.get();
            newPath = newPath.pathByAddingChild(newNode);
        }

        return newPath;
    }

    /**
     * Defilters along a path.
     *
     * @param path
     */
    public void defilter(@NotNull TreePath path) {
        final TreePath PARENT = path.getParentPath();
        if (PARENT != null) {
            defilter(PARENT);
        }

        final Node NODE = (Node) path.getLastPathComponent();
        if (!NODE.isVisible()) {
            NODE.setVisible(true);
            fireTreeNodesInserted(new TreeModelEvent(this, path));
        }
    }

    /**
     * @param node The <code>Node</code> to search.
     * @return A <code>List</code> of every <code>Element</code> contained by
     * the descendents of the <code>Node</code> (not including the node itself).
     */
    @NotNull
    private Map<Element, Node> parsePath(@NotNull Node node) {
        final Map<Element, Node> ELEMENTS = new LinkedHashMap<>();

        if (!node.isLeaf()) {
            for (Node child : node.getChildren()) {
                if (child.isVisible()) {
                    if (child.hasElement()) {
                        ELEMENTS.put(child.getElement(), child);
                    }

                    ELEMENTS.putAll(this.parsePath(child));
                }
            }
        }

        return ELEMENTS;
    }

    @Nullable
    @Override
    public Node getRoot() {
        return this.root;
    }

    public void setRoot(Node newRoot) {
        this.root = Objects.requireNonNull(newRoot);
    }

    /**
     * Retrieves the child at the specified index.
     *
     * @param parent The parent <code>Node</code>.
     * @param index The index of the child to retrieve.
     * @return The child at the specified index, or <code>null</code> if the
     * node is a leaf or the index is invalid.
     */
    @Nullable
    @Override
    public Object getChild(Object parent, int index) {
        assert parent instanceof Node;
        final Node NODE = (Node) parent;

        if (NODE.isLeaf()) {
            throw new IllegalStateException("Leaves don't have children!!");
        }

        int i = 0;
        for (Node child : NODE.getChildren()) {
            if (child.isVisible()) {
                if (i == index) {
                    return child;
                }
                i++;
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        assert parent instanceof Node;
        final Node NODE = (Node) parent;

        if (NODE.isLeaf()) {
            return 0;
        } else {
            int count = 0;
            for (Node child : NODE.getChildren()) {
                if (child.isVisible()) {
                    count++;
                }
            }
            return count;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object target) {
        final Node PARENT = (Node) parent;

        if (!PARENT.isLeaf()) {
            int i = 0;
            for (Node child : PARENT.getChildren()) {
                if (child.isVisible()) {
                    if (child == target) {
                        return i;
                    }
                    i++;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean isLeaf(Object node) {
        assert node instanceof Node;
        final Node NODE = (Node) node;
        return NODE.isLeaf();
    }

    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        this.LISTENERS.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        this.LISTENERS.remove(l);
    }

    /**
     * @see TreeModelListener#treeNodesChanged(javax.swing.event.TreeModelEvent)
     * @param event
     */
    private void fireTreeNodesChanged(TreeModelEvent event) {
        this.LISTENERS.forEach(listener -> listener.treeNodesChanged(event));
    }

    /**
     * @see
     * TreeModelListener#treeNodesInserted(javax.swing.event.TreeModelEvent)
     * @param event
     */
    private void fireTreeNodesInserted(TreeModelEvent event) {
        this.LISTENERS.forEach(listener -> listener.treeNodesInserted(event));
    }

    /**
     * @see TreeModelListener#treeNodesRemoved(javax.swing.event.TreeModelEvent)
     * @param event
     */
    private void fireTreeNodesRemoved(TreeModelEvent event) {
        this.LISTENERS.forEach(listener -> listener.treeNodesRemoved(event));
    }

    @Nullable
    private Node root;
    @NotNull
    final List<TreeModelListener> LISTENERS;

    /**
     * A node class that wraps an <code>Element</code> or string provides
     * filtering.
     *
     */
    abstract static public class Node implements Comparable<Node> {

        /**
         * @return The <code>Collection</code> of children, or null if the
         * <code>Node</code> is a leaf.
         */
        @Nullable
        abstract public Collection<Node> getChildren();

        /**
         * @return A flag indicating if the <code>Node</code> has an element.
         */
        abstract public boolean hasElement();

        /**
         * @return The element, if any.
         */
        @Nullable
        abstract public Element getElement();

        /**
         * @param <T>
         * @param cls
         * @return The element.
         */
        @Nullable
        public <T> T getAs(@NotNull Class<T> cls) {
            if (this.hasElement(cls)) {
                return cls.cast(this.getElement());
            } else {
                return null;
            }
        }

        /**
         * @param check The <code>Element</code> to check for.
         * @return The element.
         */
        public boolean hasElement(Element check) {
            return this.hasElement() && this.getElement() == check;
        }

        /**
         * @param check The <code>Element</code> to check for.
         * @return The element.
         */
        public boolean hasElement(@NotNull Class<?> check) {
            return this.hasElement() && check.isInstance(this.getElement());
        }

        /**
         * @return A flag indicating if the <code>Node</code> is a leaf.
         */
        abstract public boolean isLeaf();

        /**
         * @return The name of the <code>Node</code>.
         */
        abstract public String getName();

        /**
         * Keeps the leaf and labels up to date.
         *
         * @return The leaf count for the <code>Node</code>.
         */
        abstract public int countLeaves();

        /**
         * @return The parent of the <code>Node</code>.
         */
        @Nullable
        final public Node getParent() {
            return this.parent;
        }

        /**
         * @param newParent The new parent <code>Node</code>.
         */
        final public void setParent(Node newParent) {
            this.parent = Objects.requireNonNull(newParent);
        }

        /**
         * @return True if the node will be visible, false if it is filtered.
         */
        final public boolean isVisible() {
            return this.isVisible;
        }

        /**
         * Sets the <code>Node</code> to be visible or filtered.
         *
         * @param visible The visibility status of the <code>Node</code>.
         */
        final public void setVisible(boolean visible) {
            this.isVisible = visible;
        }

        @Override
        final public int compareTo(@NotNull Node o) {
            return this.toString().compareTo(o.toString());
        }

        @Nullable
        private Node parent = null;
        private boolean isVisible = true;
    }

    /**
     * A node class that wraps an <code>Element</code> or string provides
     * filtering.
     *
     */
    static public class ContainerNode extends Node {

        /**
         * Creates a new container <code>Node</code>.
         *
         * @param name The name of the container.
         */
        public ContainerNode(CharSequence name) {
            this.NAME = Objects.requireNonNull(name).toString();
            this.CHILDREN = new ArrayList<>();
            this.countLeaves();
        }

        /**
         * Creates a new container <code>Node</code>.
         *
         * @param name The name of the container.
         * @param elements A list of elements with which to populate the
         * <code>Node</code>.
         */
        public ContainerNode(CharSequence name, @NotNull Collection<? extends Element> elements) {
            this.NAME = Objects.requireNonNull(name).toString();
            List<Node> list = new ArrayList<>();
            for (Element element : elements) {
                ElementNode<? extends Element> elementNode = new ElementNode(element);
                list.add(elementNode);
            }
            this.CHILDREN = list;
            this.CHILDREN.forEach(child -> child.setParent(this));
            this.countLeaves();
        }

        /**
         * Adds a <code>Collection</code> of children.
         *
         * @param children The children to add.
         * @return The <code>Node</code> itself, to allow for chaining.
         */
        @NotNull
        public ContainerNode addAll(@NotNull Collection<Node> children) {
            Objects.requireNonNull(children);
            if (children.contains(null)) {
                throw new NullPointerException();
            }

            for (Node child : children) {
                child.setParent(this);
            }
            this.CHILDREN.addAll(children);
            this.countLeaves();
            return this;
        }

        /**
         * Sorts the children of the node.
         *
         * @return The <code>Node</code> itself, to allow for chaining.
         *
         */
        @NotNull
        public ContainerNode sort() {
            this.CHILDREN.sort((n1, n2) -> n1.toString().compareToIgnoreCase(n2.toString()));
            return this;
        }

        @NotNull
        @Override
        public String getName() {
            return this.NAME;
        }

        @NotNull
        @Override
        public Collection<Node> getChildren() {
            return this.CHILDREN;
        }

        @Override
        public boolean hasElement() {
            return false;
        }

        @Nullable
        @Override
        public Element getElement() {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        final public int countLeaves() {
            int leafCount = 0;
            for (Node node : this.getChildren()) {
                if (node.isVisible()) {
                    int countLeaves = node.countLeaves();
                    leafCount += countLeaves;
                }
            }

            this.label = this.isLeaf()
                    ? this.getName()
                    : this.getName() + " (" + leafCount + ")";

            return leafCount;
        }

        @Override
        public String toString() {
            return this.label;
        }

        @NotNull
        final private String NAME;
        @NotNull
        final private List<Node> CHILDREN;
        private String label;

    }

    /**
     * A node class that wraps an <code>Element</code> or string provides
     * filtering.
     *
     * @param <T>
     */
    static public class ElementNode<T extends Element> extends Node {

        /**
         * Creates a new <code>Node</code> to wrap the specified element.
         *
         * @param element The element that the node will contain.
         *
         */
        private ElementNode(T element) {
            this.ELEMENT = Objects.requireNonNull(element);
            this.label = null;
        }

        @Override
        public T getElement() {
            return this.ELEMENT;
        }

        @Nullable
        @Override
        public Collection<Node> getChildren() {
            return null;
        }

        @Override
        public boolean hasElement() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Nullable
        @Override
        public String toString() {
            if (null == this.label) {
                this.label = this.getName();
            }
            return this.label;
        }

        @Override
        public String getName() {
            return this.ELEMENT.toString();
        }

        @Override
        public int countLeaves() {
            this.label = this.getName();
            return 1;
        }

        final private T ELEMENT;
        @Nullable
        protected String label;

    }

    /**
     * A node class that wraps a <code>Plugin</code>.
     *
     */
    static public class PluginNode extends ElementNode<Plugin> {

        /**
         * Creates a new <code>Node</code> to wrap the specified element.
         *
         * @param element The element that the node will contain.
         *
         */
        public PluginNode(Plugin element) {
            super(element);
        }

        @NotNull
        @Override
        public String getName() {
            return this.getElement().indexName();
        }

    }

    /**
     * A node class that wraps an <code>Element</code> or string provides
     * filtering.
     *
     */
    static public class RootNode extends ContainerNode {

        public RootNode(@NotNull ESS root, @NotNull Collection<Node> children) {
            super(root.toString());
            this.ROOT = Objects.requireNonNull(root);
            super.addAll(children);
        }

        @Override
        public ESS getElement() {
            return this.ROOT;
        }

        @Override
        public boolean hasElement() {
            return true;
        }

        final private ESS ROOT;
    }

    /**
     * A node class that wraps an <code>ActiveScript</code>.
     *
     */
    static public class ActiveScriptNode extends ElementNode<ActiveScript> {

        public ActiveScriptNode(@NotNull ActiveScript element) {
            super(element);
            if (element.hasStack()) {
                List<Node> list = new ArrayList<>();
                for (StackFrame stackFrame : element.getStackFrames()) {
                    ElementNode<StackFrame> stackFrameElementNode = new ElementNode<>(stackFrame);
                    list.add(stackFrameElementNode);
                }
                this.CHILDREN = list;
               this.CHILDREN.forEach(child -> child.setParent(this));
            } else {
                this.CHILDREN = null;
            }
        }

        @Override
        public int countLeaves() {
            return 1;
        }

        @Override
        public boolean isLeaf() {
            return this.CHILDREN == null;
        }

        @Nullable
        @Override
        public Collection<Node> getChildren() {
            return this.CHILDREN;
        }

        @Nullable
        final private List<Node> CHILDREN;
    }

    /**
     * A node class that wraps an <code>SuspendedStack</code>.
     *
     */
    static public class SuspendedStackNode extends ElementNode<SuspendedStack> {

        public SuspendedStackNode(@NotNull SuspendedStack element) {
            super(element);
            if (element.hasMessage()) {
                final ElementNode<FunctionMessageData> CHILD = new ElementNode<>(element.getMessage());
                CHILD.setParent(this);
                this.CHILDREN = Collections.singletonList(CHILD);
            } else {
                this.CHILDREN = null;
            }
        }

        @Override
        public int countLeaves() {
            return 1;
        }

        @Override
        public boolean isLeaf() {
            return this.CHILDREN == null;
        }

        @Nullable
        @Override
        public Collection<Node> getChildren() {
            return this.CHILDREN;
        }

        @Nullable
        final private List<Node> CHILDREN;
    }

    /**
     * A node class that wraps an <code>FunctionMessage</code>.
     *
     */
    static public class FunctionMessageNode extends ElementNode<FunctionMessage> {

        public FunctionMessageNode(@NotNull FunctionMessage element) {
            super(element);
            if (element.hasMessage()) {
                final ElementNode<FunctionMessageData> CHILD = new ElementNode<>(element.getMessage());
                CHILD.setParent(this);
                this.CHILDREN = Collections.singletonList(CHILD);
            } else {
                this.CHILDREN = null;
            }
        }

        @Override
        public int countLeaves() {
            return 1;
        }

        @Override
        public boolean isLeaf() {
            return this.CHILDREN == null;
        }

        @Nullable
        @Override
        public Collection<Node> getChildren() {
            return this.CHILDREN;
        }

        @Nullable
        final private List<Node> CHILDREN;
    }

    static final private Logger LOG = Logger.getLogger(FilterTreeModel.class.getCanonicalName());

}
