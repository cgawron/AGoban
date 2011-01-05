/*
 * Copyright (C) 2010 Christian Gawron
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

package de.cgawron.go.sgf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Symmetry;
import de.cgawron.util.Memento;
import de.cgawron.util.MementoOriginator;
import de.cgawron.util.MiscEncodingReader;

/**
 * This class represents an SGF game tree.
 */
public class GameTree implements TreeModel, PropertyChangeListener, MementoOriginator
{
    private static Logger logger = Logger.getLogger(GameTree.class.getName());

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private File file = null;
    private String name;
    private RootNode root;
    //TODO: EventListenerList
    private final Collection<EventListener> listeners;

    private boolean modified = false;
    private int noOfDiagrams = -1;
    private int noOfFigures = -1;

    private boolean collection = false;
    private final boolean rootOnly = false;
    
    interface GobanFactory<M extends Goban>
    {
    	M getGoban(short boardsize);
    	M getGoban(Goban m);
    }

    GobanFactory factory;

    abstract class NodeCount extends TreeVisitor<GameTree, Node>
    {
        private int count;

        NodeCount()
        {
            super(GameTree.this);
            count = 0;
            visit();
        }

        public int getCount()
        {
            return count;
        }

        public abstract boolean predicate(Node n);

        @Override
	public void visitNode(Object o)
        {
            if (predicate((Node)o))
                count++;
        }
    }

    private void init(GameTree tree)
    {
	logger.info("init: this=" + this + ", tree=" + tree);
	this.root = tree.root;

	root.setGameTree(this);
        root.setDefaultRootProperties();
        setModified(false);
    }

    private void init(Node root)
    {
	logger.info("init: this=" + this + ", root=" + root);
	if (root instanceof RootNode)
	    setRoot((RootNode) root);
	else
	    setRoot(new RootNode(root));

	root.setGameTree(this);
        root.setDefaultRootProperties();
        setModified(false);

	/*
	if (root instanceof RootNode)
	    init((RootNode) root);
	else if (root instanceof CollectionRoot)
	    init((CollectionRoot) root);
	else if (root instanceof Sequence) {
	    init((Sequence) root);
	}
	else {
	    init(new RootNode(root));
	    throw new RuntimeException("init: " + root);
	}
	*/
    }

    /**
     * Constructs an empty GameTree, consisting only of a <code>RootNode</code>
     */
    public GameTree()
    {
        listeners = new HashSet<EventListener>();
	Node root = new RootNode(this);
	root.setGameTree(this);
        name = "<empty>";
	init(root);
    }

    /**
     * Constructs a GameTree with a given <code>Node</code> as root.
     * @param o the <code>Node</code> used as root. If o is not an instace of <code>RootNode</code>, it will be wrapped appropriately.
     */
    public GameTree(Node o)
    {
        listeners = new HashSet<EventListener>();
        name = "<created from node>";
        init(o);
    }

    /**
     * Constructs a GameTree from an SGF file.
     * @param file the SGF file used to construct the <code>GameTree</code>.
     */
    public GameTree(File file) throws Exception
    {
        this(new FileInputStream(file));
	this.file = file;
        name = file.getPath();
    }

    /**
     * Constructs a GameTree from an SGF file.
     * @param file the SGF file used to construct the <code>GameTree</code>.
     */
    public GameTree(final RandomAccessFile file) throws Exception
    {
        this(new InputStream() { 
		@Override public int read() throws IOException {return file.read();}});
    }

    /**
     * Constructs a GameTree from an SGF stream.
     * @param in the SGF stream used to construct the <code>GameTree</code>.
     */
    public GameTree(InputStream in) throws Exception
    {
	this(new MiscEncodingReader(in));
    }

    /**
     * Constructs a GameTree from a <code>Reader</code> which reads an SGF stream.
     * @param reader the reader from which the SGF is read to construct the <code>GameTree</code>.
     */
    public GameTree(Reader reader) throws Exception
    {
	root = new CollectionRoot(this);
        name = "<unknown input>";
        listeners = new HashSet<EventListener>();
	Yylex lexer = new Yylex(reader);
        Parser parser = new Parser(lexer);
	logger.info("parsing " + reader + " ...");
        List roots = (List) parser.debug_parse().value;
	logger.info("parsing done");
        if (roots.size() > 1) {
	    init(new CollectionRoot(this));
	    Iterator it = roots.iterator();
	    while (it.hasNext()) {
		GameTree tree = (GameTree)it.next();
		Node n = tree.getRoot();
		logger.info("GameTree(stream): " + root + ", " + n);
		if (n != null) {
		    root.add(n);
		    n.setGameTree(this);
		}
	    }
	    collection = true;
	}
        else {
	    GameTree tree = (GameTree) roots.get(0);
	    init(tree);
	    //Node node = tree.getRoot();
	    //node.setRoot((Node)getRoot());
	    //node.setRoot(node);
	    //init(node);
	}
        reader.close();
        setModified(false);
    }
    

    /**
     * Add <strong>canonical</strong> diagrams to a <code>GameTree</code>.
     * A diagram is added at every leaf node and every <code>movesPerFigure</code> moves.
     * @param movesPerFigure Number of moves per figure.
     */
    public void addCanonicalDiagrams(int movesPerFigure)
    {
	logger.info("adding canonical diagrams");
	List leafs = getLeafs();
	Iterator it = leafs.iterator();
	while (it.hasNext()) {
	    Node leaf = (Node) it.next();
	    leaf.setDiagram(true);
	}

	if (movesPerFigure > 0)
	{
	    int j;
	    for (j=movesPerFigure; j<getNoOfMoves(); j+=movesPerFigure)
		getMove(j).setDiagram(true);
	}
    }

    /**
     * Add <strong>canonical</strong> diagrams to a <code>GameTree</code>.
     * A diagram is added at every leaf node and - if this property is set - every <code>godiagram.movesPerDiagram</code> moves.
     */
    public void addCanonicalDiagrams()
    {
	int movesPerFigure = Integer.getInteger("godiagram.movesPerDiagram", -1);
	addCanonicalDiagrams(movesPerFigure);
    }


    Goban getGoban(short boardSize)
    {
	if (factory != null)
	    return factory.getGoban(boardSize);
	else
	    return new SimpleMarkupModel(boardSize);
    }

    Goban getGoban(Goban m)
    {
	if (factory != null)
	    return factory.getGoban(m);
	else
	    return new SimpleMarkupModel(m);
    }

    public Memento createMemento()
    {
 	return new GameTreeMemento(this);
    }
    
    public void setMemento(Memento memento)
    {
	logger.info("GameTree: setMemento: " + memento);
 	GameTreeMemento m = (GameTreeMemento) memento;
	m.setMemento(root);
        TreeModelEvent ev = new TreeModelEvent(this, new TreePath(root));
        fireTreeStructureChanged(ev);
    }

    public boolean isCollection()
    {
	return collection;
    }

    public Collection<Node> getRoots()
    {
	Collection<Node> c = new ArrayList<Node>();
	if (isCollection()) 
	    c.addAll(getRoot().getChildren());
	else
	    c.add(getRoot());
	return c;
    }

    public String getBaseName()
    {
	if (file == null)
	    return getName();
	else {
	    String n = file.getName();
	    int idx = n.lastIndexOf(".");
	    if (idx < 0)
		return n;
	    else
		return n.substring(0, idx);
	}
    }

    public File getFile()
    {
	return file;
    } 
    public void save() throws IOException
    {
	save(getFile());
    }

    public void save(File file) throws IOException
    {
        logger.info("Saving gametree in " + file);
        if (file.exists()) {
	    if (!file.renameTo(new File(file.toString() + "~")))
		logger.warning("Could not create backup file " + file.toString() + "~");
	}
	
        save(new FileOutputStream(file));
    }

    public void save(OutputStream stream) throws IOException
    {
        Node root = getRoot();
	String charset = null;
	if (root.get(Property.CHARACTER_SET) != null)
	    (root.get(Property.CHARACTER_SET)).getValue().getString();
	if (charset == null) charset = "utf8";
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(stream), charset));
        root.write(out);
        out.close();
        setModified(false);
    }

    /**
     * Set the Charset of the GameTree.
     * This method sets the CA[] Property of the root node. Subsequents call of @link{#save()} will use this charset.
     * @param charset Charset to set. 
     */
    public void setCharset(Charset charset)
    {
        Node root = getRoot();
	Property ca = Property.createProperty(Property.CHARACTER_SET, charset.name());
	root.add(ca);
    }

    /**
     * Get the value of name.
     * @return value of name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the value of name.
     * @param v  Value to assign to name.
     */
    public void setName(String v)
    {
        this.name = v;
    }

    /**
     * Get the value of modified.
     * @return value of modified.
     */
    public boolean isModified()
    {
        return modified;
    }

    /**
     * Set the value of modified.
     * @param v  Value to assign to modified.
     */
    public void setModified(boolean newValue)
    {
        boolean oldValue = modified;
	    logger.fine("GameTree.setModified: " + oldValue + ", " + newValue);
        if (newValue != modified) {
	    modified = newValue;
	    firePropertyChange("modified", oldValue, newValue);
	}
    }

    @Override
	public String toString()
    {
        return "GameTree " + name;
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        logger.info("GameTree.propertyChange: " + event + " " + event.getPropertyName());
        noOfDiagrams = -1;
        noOfFigures = -1;
	if (event.getSource() instanceof Node)
	    fireTreeNodeChanged(event);
        setModified(true);
    }

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     * @see     #removeTreeModelListener
     * @param   l       the listener to add
     */
    public void addTreeModelListener(TreeModelListener l)
    {
        logger.fine("Adding listener " + l);
        listeners.add(l);
    }

    /**
     * Returns the child of <I>parent</I> at index <I>index</I> in the parent's
     * child array.  <I>parent</I> must be a node previously obtained from
     * this data source. This should not return null if <i>index</i>
     * is a valid index for <i>parent</i> (that is <i>index</i> >= 0 && <i>index</i> < getChildCount(<i>parent</i>)).
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <I>parent</I> at index <I>index</I>
     */
    public TreeNode getChild(TreeNode parent, int index)
    {
        if (parent instanceof TreeNode) {
	    TreeNode node = parent;
	    return node.getChildAt(index);
	}
        else {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("GameTreeModel.getChild: " + parent + " [" + parent.getClass() + "]");
	    return null;
	}
    }


    /**
     * Returns the number of children of <I>parent</I>.  Returns 0 if the node
     * is a leaf or if it has no children.  <I>parent</I> must be a node previously obtained from this data source.
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <I>parent</I>
     */
    public int getChildCount(Object parent)
    {
        TreeNode node = (TreeNode)parent;
        if (node == null)
            return -1;
        else
            return node.getChildCount();
    }

    /** Returns the index of child in parent. */
    public int getIndexOfChild(Object parent, Object child)
    {
        TreeNode node = (TreeNode)parent;
        if (node == null)
            return -1;
        else
            return node.getIndex((TreeNode)child);
    }


    public String getSignature()
    {
      return (getRoot()).getSignature();
    }

    /**
     * Returns the root of the tree.  Returns null only if the tree has no nodes.
     * @return  the root of the tree
     */
    public RootNode getRoot()
    {
        return root;
    }

    public void setRoot(Object o)
    {
        if (o instanceof RootNode)
            setRoot((RootNode)o);
        else if (o instanceof GameTree)
            setRoot(((GameTree)o).getRoot());
        else
            logger.warning("What shall I do with a " + o.getClass().getName() + "?");
    }

    public void setRoot(final RootNode newRoot)
    {
        Node oldRoot = root;
        if (oldRoot != null) {
	    if (oldRoot.equals(newRoot))
		return;
            oldRoot.removePropertyChangeListener(this);
	}

        logger.info("Setting root: " + newRoot);
        root = newRoot;
	root.setGameTree(this);

	if (false && !rootOnly) {
	    TreeVisitor<GameTree, Node> visitor = 
		new TreeVisitor<GameTree, Node>(this) 
		{
		    @Override
		    protected void visitNode(Object o)
		    {
			Node n = (Node) o;
			Goban goban = null;
			
			if (logger.isLoggable(Level.FINE))
			    logger.fine("setRoot(" + newRoot + "): visiting " + n);
			Node p = n.getParent();
			
			if (p == null) {
			    goban = getGoban(n.getBoardSize());
			    n.setMoveNo(n.isMove() ? 1 : 0);
			}
			else {
			    if (logger.isLoggable(Level.FINE))
				logger.fine(n.toString() + ": inheriting Board from " + p.toString());
			    goban = getGoban(p.getGoban());
			    
			    
			    if (n.contains(Property.MOVE_NO)) {
				Value.Number no = null;
				try {
				    Value value = (n.get(Property.MOVE_NO)).getValue();
				    logger.info("value is " + value + " " + value.getClass());
				    if (value instanceof Value.ValueList)
					no = (Value.Number) ((Value.ValueList) value).get(0);
				    else
					no = (Value.Number) value;
				    
				    if (logger.isLoggable(Level.FINE))
					logger.fine("Setting moveNo on node " + n + " to " + no.intValue());
				    n.setMoveNo(no.intValue());
				}
				catch (Throwable e) {
				    logger.info("value is " + (n.get(Property.MOVE_NO)).getValue());
				    //throw new RuntimeException(e);
				}
			    }
			    else if (p.getIndex(n) != 0) {
				if (logger.isLoggable(Level.FINE))
				    logger.fine("Setting moveNo on node " + n + " to 1");
				n.setMoveNo(1);
			    }
			    else {
				if (logger.isLoggable(Level.FINE))
				    logger.fine("Setting moveNo on node " + n + " to " + p.getMoveNo() + " + " + (n.isMove() ? 1 : 0));
				n.setMoveNo(p.getMoveNo() + (n.isMove() ? 1 : 0));
			    }
			}
			n.setGoban(goban);
		    }
		};
	    visitor.visit();
	}

        TreeModelEvent ev = new TreeModelEvent(this, new TreePath(root));
        fireTreeStructureChanged(ev);
        firePropertyChange("root", oldRoot, newRoot);
        newRoot.addPropertyChangeListener(this);
    }
    
    /**
     * Returns the node specified by <code>nodeSpec</code>.
     * Allowed node specifications are
     * <dl>
     * <dt>root</dt>
     * <dd>specifies the root node</dd>
     * </dl>
     * @param   nodeSpec specifies the node to return
     * @return  the specified node
     */
    public Node getNode(String nodeSpec) throws Exception
    {
	if ("root".equals(nodeSpec))
	    return getRoot();
	else
	    throw new Exception("node specification " + nodeSpec + " not recognized");
    }
    
    public String getGameName()
    {
        return ((Node)getRoot()).getGameName();
    }
    
    public Node appendNode(Node currentNode)
    {
	Node newNode = new Node(this);
	logger.info("appending node " + newNode + " at " + currentNode);
	int newIndex = currentNode.getChildCount();
	currentNode.add(newNode);
	try {
	    newNode.setGoban(currentNode.getGoban().clone());
	}
	catch (CloneNotSupportedException ex)
	{
	    throw new RuntimeException(ex);
	}
	if (newNode.getGoban() == currentNode.getGoban())
	    throw new NullPointerException();
	setModified(true);
	int[] childIndices = new int[1];
	Object[] newChildren = new Object[1];
	childIndices[0] = newIndex;
	newChildren[0] = newNode;
        TreeModelEvent ev = new TreeModelEvent(this, new TreePath(currentNode), childIndices, newChildren);
        fireTreeNodesInserted(ev);
	return newNode;
    }
    
    public int getNoOfMoves()
    {
	int moves = 0;
	Node node = getRoot();
	while (node != null) {
	    if (node.isMove()) moves++;
	    if (node.getChildCount() > 0)
		node = (Node) node.getChildAt(0);
	    else
		node = null;
	}
	return moves;
    }
    
    public int getNoOfDiagrams()
    {
        if (noOfDiagrams < 0) {
	    NodeCount counter = 
		new NodeCount()
		{
		    @Override
			public boolean predicate(Node node)
		    {
			return !node.isMainVariation() && node.isDiagram();
		    }
		};
	    noOfDiagrams = counter.getCount();
	}
        return noOfDiagrams;
    }
    
    public int getNoOfFigures()
    {
        if (noOfFigures < 0) {
	    NodeCount counter = 
		new NodeCount()
		{
		    @Override
			public boolean predicate(Node node)
		    {
			return node.isMainVariation() && node.isDiagram();
		    }
		};
	    noOfFigures = counter.getCount();
	}
        return noOfFigures;
    }

    /**
     * Returns true if <I>node</I> is a leaf.  It is possible for this method
     * to return false even if <I>node</I> has no children.  A directory in a
     * filesystem, for example, may contain no files; the node representing
     * the directory is not a leaf, but it also has no children.
     * @param   node    a node in the tree, obtained from this data source
     * @return  true if <I>node</I> is a leaf
     */
    public boolean isLeaf(Object node)
    {
        return getChildCount(node) == 0;
    }

    /**
     * Get all leaf nodes
     * @return A List containing all leaf nodes
     */
    public List<Node> getLeafs()
    {
	final List<Node> leafs = new ArrayList<Node>();
	TreeVisitor<GameTree, Node> visitor = 
	    new TreeVisitor<GameTree, Node>(this)
	    {
		@Override
		protected void visitNode(Object o)
		{
		    if (logger.isLoggable(Level.FINE))
			logger.fine("getLeafs: visiting " + o);
		    Node node = (Node) o;
		    if (node.isLeaf())
			leafs.add(node);
		}
	    };
	visitor.visit();
        return leafs;
    }


    /** 
     * get the node representing the move with number <code>moveNo</code> 
     * @param  moveNo the number of the move to get
     * @return the node containing the specified move
     */
    public Node getMove(int moveNo)
    {
	Node node = root;
	int currentMoveNo = 0;
	while (node != null && currentMoveNo != moveNo) {
	    if (node.isMove()) {
		currentMoveNo++;
	    }
	    node = (Node) node.getChildAt(0);
	}
	return node;
    }

    /**
     * Removes a listener previously added with <B>addTreeModelListener()</B>.
     * @see     #addTreeModelListener
     * @param   l the listener to remove
     */
    public void removeTreeModelListener(TreeModelListener l)
    {
        listeners.remove(l);
    }

    void fireTreeStructureChanged(TreeModelEvent ev)
    {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
	    TreeModelListener l = (TreeModelListener)it.next();
	    l.treeStructureChanged(ev);
	}
    }

    void fireTreeNodesInserted(TreeModelEvent ev)
    {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
	    TreeModelListener l = (TreeModelListener)it.next();
	    l.treeNodesInserted(ev);
	}
    }

    public class NodePropertyChangedEvent extends TreeModelEvent
    {
	PropertyChangeEvent event;

	NodePropertyChangedEvent(PropertyChangeEvent e)
	{
	    super(GameTree.this, new TreePath(e.getSource()));
	    this.event = e;
	}

	public 	PropertyChangeEvent getPropertyChangeEvent()
	{
	    return this.event;
	}
    }

    public void fireTreeNodeChanged(PropertyChangeEvent event)
    {
	setModified(true);
        TreeModelEvent ev = new NodePropertyChangedEvent(event);
        fireTreeNodesChanged(ev);
    }
    
    public void fireTreeNodeChanged(Node n)
    {
	setModified(true);
        TreeModelEvent ev = new TreeModelEvent(this, new TreePath(n));
        fireTreeNodesChanged(ev);
    }

    void fireTreeNodesChanged(TreeModelEvent e)
    {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
	    TreeModelListener l = (TreeModelListener)it.next();
	    l.treeNodesChanged(e);
	}
    }

    /**
     * Messaged when the user has altered the value for the item identified
     * by <I>path</I> to <I>newValue</I>.  If <I>newValue</I> signifies
     * a truly new value the model should post a treeNodesChanged event.
     * @param path path to the node that the user has altered.
     * @param newValue the new value from the TreeCellEditor.
     */
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        if (path != null)
            fireTreeNodesChanged(new TreeModelEvent(newValue, path));
    }

    /** An enumeration that is always empty. This is used when an enumeration of a leaf node's children is requested. */
    public static final Iterator EMPTY_ITERATOR = 
	new Iterator()
	{
	    public boolean hasNext() { return false; }
	    public Object next()
	    {
		throw new NoSuchElementException("No more elements");
	    }
	    public void remove()
	    {
		throw new UnsupportedOperationException();
	    }
	};

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is registered for all properties.
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        logger.info("GameTree.addPropertyChangeListener: " + listener);
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered for all properties.
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        logger.info("GameTree.removePropertyChangeListener: " + listener);
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener for a specific property. The listener will be invoked only when a call on firePropertyChange
     * names that specific property.
     * @param propertyName The name of the property to listen on
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        logger.info("GameTree.addPropertyChangeListener: " + listener + ", " + propertyName);
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property.
     * @param propertyName The name of the property that was listened on
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        logger.info("GameTree.removePropertyChangeListener: " + listener + ", " + propertyName);
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * This is merely a convenience wrapper around the more general firePropertyChange method that takes Object values.
     * No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, int oldValue, int newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * This is merely a convenience wrapper around the more general firePropertyChange method that takes Object values.
     * No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Fires an existing PropertyChangeEvent to any registered listeners.
     * No event is fired if the given event's old and new values are equal and non-null.
     * @param evt The PropertyChangeEvent object.
     */
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        pcs.firePropertyChange(evt);
    }

    /**
     * Checks if there are any listeners for a specific property.
     * @param evt The PropertyChangeEvent object.
     * @return <code>true</code>if there are one or more listeners for the given property
     */
    public boolean hasListeners(String propertyName)
    {
        return pcs.hasListeners(propertyName);
    }
    
    public void join(GameTree tree, Node theirSetup)
    {
	Node mySetup = getRoot();
	while (mySetup != null && !mySetup.isBoardSetup())
	    mySetup = (Node) mySetup.getChildAt(0);
	while (theirSetup != null && !theirSetup.isBoardSetup())
	    theirSetup = (Node) theirSetup.getChildAt(0);
	
	Goban myModel = mySetup.getGoban();
	Goban theirModel = theirSetup.getGoban();
	logger.info("join: my position: " + myModel);
	logger.info("join: their position: " + theirModel);

	Symmetry symmetry = null;
	Symmetry.Iterator it = new Symmetry.Iterator();
	while (it.hasNext()) {
	    Symmetry s = (Symmetry) it.next();
	    if (myModel.equals(theirModel, s)) {
		symmetry = s;
		break;
	    }
	}
	logger.info("Symmetry is " + symmetry);
	tree.transform(symmetry);    
	
	((Node) getRoot()).join(tree.getRoot());
    }

    public void transform(final Symmetry s)
    {
        TreeVisitor<GameTree, Node> visitor = 
	    new TreeVisitor<GameTree, Node>(this) 
	    {
		@Override
		protected void visitNode(Object o)
		{
		    Node n = (Node) o;
		    Iterator it = n.entrySet().iterator();
		    while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			logger.info("Entry: " + entry);
		        Property p = (Property) entry.getValue();
			Value v = p.getValue();
			if (v instanceof Value.Transformable) {
			    ((Value.Transformable) v).transform(s);
			}
		    }
		    if (n.getGoban() != null)
			n.setGoban(n.getGoban().transform(s), false);
		}
	    };
        visitor.visit();
    }

    public static class GameTreeTest
    {
	MarkupModel.Stone blackStone;
	MarkupModel.Stone whiteStone;
	MarkupModel.Move move1;
	MarkupModel.Move move5;

	public GameTreeTest()
	{
	}

	@Before public void setUp() 
	{
	    blackStone = new MarkupModel.Stone(BoardType.BLACK);
	    whiteStone = new MarkupModel.Stone(BoardType.WHITE);
	    move1 = new MarkupModel.Move(BoardType.BLACK, 1);
	    move5 = new MarkupModel.Move(BoardType.BLACK, 5);
	}

	@Test public void testInheritedProperties() throws Exception 
	{
	    List<Node> leafs;
	    GameTree gameTree = new GameTree(new StringReader("(;GM[1]FF[4]AP[CGoban:2]ST[2]VW[aa:dd]FG[];B[aa];W[bb];B[cc];W[dd]FG[];B[ee];W[ff];B[gg];W[hh]FG[])"));
	    leafs = gameTree.getLeafs();
	    assertEquals("There should be one leaf", 1, leafs.size());
	    Node leaf = leafs.get(0);
	    assertTrue("The leaf should have a view set", leaf.contains(Property.VIEW));
	    assertTrue("Move 2 should not have FG set", !gameTree.getMove(3).contains(Property.FIGURE));
	}
	

	@Test public void testExplicitDiagrams() throws Exception 
	{
	    List<Node> leafs;
	    MarkupModel goban;
	    
	    GameTree gameTree = new GameTree(new StringReader("(;GM[1]FF[4]AP[CGoban:2]ST[2]FG[];B[aa];W[bb];B[cc];W[dd]FG[];B[ee];W[ff];B[gg];W[hh]FG[])"));
	    
	    // Der Root-Node darf trotz FG-Property kein Diagram sein
	    assertTrue("Root node should not be a diagram", !gameTree.getRoot().isDiagram());
	    assertTrue("Move 1 should not be a diagram", !gameTree.getMove(1).isDiagram());
	    assertTrue("Move 3 should not be a diagram", !gameTree.getMove(3).isDiagram());
	    assertTrue("Move 4 should be a diagram", !gameTree.getMove(4).isDiagram());
	    assertTrue("Move 5 should not be a diagram", !gameTree.getMove(5).isDiagram());

	    // Der erste Zug im neuen Diagramm muss eine Zugnummer tragen
	    goban = (MarkupModel) gameTree.getMove(1).getGoban();
	    assertEquals("Check if first move in dia 1 has number 1", move1, goban.getMarkup((short) 0, (short) 0));
	    goban = (MarkupModel) gameTree.getMove(5).getGoban();
	    assertEquals("Check if first move in dia 2 has number 5", move5, goban.getMarkup((short) 4, (short) 4));
	    // Der letzte Zug des ersten Diagrammes darf in Diagram 2 keine Nummer tragen
	    assertEquals("Check if last move in dia 1 is not numbered in dia 2", whiteStone, goban.getMarkup((short) 3, (short) 3));
	}

	@Test public void testDefaultDiagrams() throws Exception
	{
	    System.setProperty("godiagram.movesPerDiagram", "50");
	    List<Node> leafs;
	    MarkupModel goban;
	    
	    GameTree gameTree = new GameTree(new StringReader("(;FF[4]DT[2004-12-06]EV[Nikolaus Meschede]HA[0]KM[1,5]RE[B+5]RO[3]SZ[9]GM[1]FF[4];B[cc];W[gf];B[dg];W[ce];B[de];W[df];B[cf];W[ef];B[dd];W[cg];B[bf];W[bg];B[be];W[ec];B[ee];W[fe];B[dc];W[eb];B[db];W[ed];B[eg];W[ff];B[fg];W[gh];B[gg];W[hg];B[fh];W[hh];B[ch];W[fi];B[ei];W[dh];B[gi];W[hi];B[di];W[bh];B[eh];W[fi];B[bi];W[da];B[ca];W[ea];B[hc];W[gc];B[gb];W[gd];B[he];W[hd];B[ic];W[id];B[ha];W[ib];B[fc];W[fd];B[fb];W[hb];B[gi];W[ai];B[ah];W[fi];B[];W[gi])"));
	    // Test vor addCanonicalDiagrams: Der letzte Knoten muss volles Markup haben.
	    leafs = gameTree.getLeafs();
	    assertEquals("There should be one leaf", 1, leafs.size());
	    Node leaf = leafs.get(0);
	    goban = (MarkupModel) leaf.getGoban();
	    assertEquals("Check if black 1 worked before addCanonicalDiagrams()", move1, goban.getMarkup((short) 2, (short) 2));
	    
	    gameTree.addCanonicalDiagrams();
	    assertTrue("At move 50 should be a figure", gameTree.getMove(50).isDiagram());
	    goban = (MarkupModel) gameTree.getMove(51).getGoban();
	    assertEquals("Check if black 1 worked", blackStone, goban.getMarkup((short) 2, (short) 2));
	    
	    leafs = gameTree.getLeafs();
	    goban = (MarkupModel) leaf.getGoban();
	    assertEquals("Check if black 1 worked after addCanonicalDiagrams()", blackStone, goban.getMarkup((short) 2, (short) 2));
	}
    }

    /*
    public static junit.framework.Test suite() {
	return new JUnit4TestAdapter(GameTreeTest.class);
    }
    */
}
