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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.GobanEvent;
import de.cgawron.go.Point;
import de.cgawron.util.Memento;
import de.cgawron.util.MementoOriginator;

/**
 * An instance of this class represents a node in a sgf game tree.
 * 
 * @author Christian Gawron
 */
public class Node extends PropertyList implements MarkupModelListener,
		TreeNode, Comparable<Node>, MementoOriginator// , PropertyChangeListener
{
	private PropertyList inheritedProperties = null;

	/**
	 * The children of this node.
	 */
	protected List<Node> children = new LinkedList<Node>();
	protected GameTree gameTree = null;

	private Node parent = null;
	private Goban goban = null;
	private int moveNo = 0;
	private int depth = -1;
	private final int id;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	private static int lastId = 0;
	private static Logger logger = Logger.getLogger(Node.class.getName());

	/**
	 * Compares two Nodes or a Node and a Goban.
	 * 
	 * This Comparator is <strong>not</strong> compatible with
	 * {@link Node#compareTo} or {@link Node#equals}.
	 */
	static class BoardComparator implements Comparator<Goban>
	{
		/**
		 * Compare two Nodes by comparing the values of the Zobrist hash of
		 * their Gobans.
		 * 
		 * @see de.cgawron.go.goban.Goban#zobristHash
		 * @see "Comparator in the Java API documentation."
		 */
		public int compare(Goban o1, Goban o2)
		{
			Goban m1;
			Goban m2;
			if (o1 instanceof Node)
				m1 = ((Node) o1).getGoban();
			else
				m1 = o1;
			if (o2 instanceof Node)
				m2 = ((Node) o2).getGoban();
			else
				m2 = o2;

			int h1 = m1.zobristHash();
			int h2 = m2.zobristHash();
			if (h1 < h2)
				return -1;
			else if (h1 > h2)
				return 1;
			else
				return 0;
		}
	}

	public class SiblingsIterator implements Iterator<Node>
	{
		private int i = 0;

		public SiblingsIterator()
		{
			while (i < parent.children.size()
					&& parent.children.get(i) == Node.this)
				i++;
		}

		public boolean hasNext()
		{
			return i < parent.children.size();
		}

		public Node next()
		{
			Node node = parent.children.get(i++);
			while (i < parent.children.size()
					&& parent.children.get(i) == Node.this)
				i++;
			return node;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	public class SiblingsList extends AbstractList<Node>
	{
		@Override
		public Iterator<Node> iterator()
		{
			return new SiblingsIterator();
		}

		@Override
		public int size()
		{
			return parent.children.size() - 1;
		}

		@Override
		public Node get(int index)
		{
			Iterator<Node> it = iterator();
			Node n = it.next();

			for (int i = 0; i < index; i++)
				n = it.next();

			return n;
		}
	}

	/**
	 * The VariationPath of a Node consists of all nodes of the TreePath of a
	 * node which are not the first child of their parent.
	 */
	public static class VariationPath extends LinkedList<Node>
	{
		VariationPath(Node node)
		{
			super();
			Node parent = node.getParent();
			while (parent != null) {
				if (parent.getIndex(node) != 0)
					add(0, node);
				node = parent;
				parent = node.getParent();
			}
		}

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			for (Node n : this) {
				if (sb.length() != 0)
					sb.append(", ");
				sb.append(n.getParent().getMoveNo());
			}
			return sb.toString();
		}
	}

	public VariationPath getVariationPath()
	{
		return new VariationPath(this);
	}

	/**
	 * This method is called if the {@link Goban} of this node is changed. The
	 * change is signalled to all registered <code>PropertyChangeListener</code>
	 */
	public void modelChanged(GobanEvent e)
	{
		firePropertyChange(null, null, null);
	}

	/**
	 * This method is called if a stone is added to the Goban of this node.
	 * 
	 * @param event
	 *            The {@link GobanEvent}.
	 */
	public void stoneAdded(GobanEvent event)
	{
		// throw new
		// RuntimeException("Hey, someone just added a stone! This should not happen this way");
	}

	/**
	 * This method is called if stones are removed from the Goban of this node.
	 * 
	 * @param event
	 *            The {@link GobanEvent}.
	 */
	public void stonesRemoved(GobanEvent event)
	{
		// throw new
		// RuntimeException("Hey, someone just removed a stone! This should not happen this way");
	}

	/**
	 * This method is called if the selected {@link Region} of the Goban of this
	 * node is changed.
	 * 
	 * @param event
	 *            The {@link GobanEvent}.
	 */
	public void regionChanged(GobanEvent event)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("Node.regionChanged: " + id);
		Property oldView = get(Property.VIEW);
		MarkupModel mm = (MarkupModel) getGoban();
		Property newView = Property.createProperty(Property.VIEW);
		Value.ValueList value = AbstractValue.createValueList();
		if (mm.getRegion() != null && oldView != newView) {
			value.add(mm.getRegion().getPointList());
			newView.setValue(value);
			put(newView);
			firePropertyChange("view", oldView, newView);
		} else {
			logger.warning("regionChanged: region is null");
			remove(newView.getKey());
			firePropertyChange("view", oldView, null);
		}
	}

	/**
	 * Adds a PropertyChangeListener to the listener list. The listener is
	 * registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		assert listener != this;
		logger.fine("Node.addPropertyChangeListener: " + listener);
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Removes a PropertyChangeListener from the listener list. This removes a
	 * PropertyChangeListener that was registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		logger.fine("Node.removePropertyChangeListener: " + listener);
		pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Adds a PropertyChangeListener for a specific property. The listener will
	 * be invoked only when a call on firePropertyChange names that specific
	 * property.
	 * 
	 * @param propertyName
	 *            The name of the property to listen on
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener)
	{
		logger.fine("Node.addPropertyChangeListener: " + listener + ", "
				+ propertyName);
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Removes a PropertyChangeListener for a specific property.
	 * 
	 * @param propertyName
	 *            The name of the property that was listened on
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener)
	{
		logger.fine("Node.removePropertyChangeListener: " + listener + ", "
				+ propertyName);
		pcs.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * Reports a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null.
	 * 
	 * @param propertyName
	 *            The programmatic name of the property that was changed
	 * @param oldValue
	 *            The old value of the property
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue)
	{
		pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Reports a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null.
	 * 
	 * @param propertyName
	 *            The programmatic name of the property that was changed
	 * @param oldValue
	 *            The old value of the property
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, boolean oldValue,
			boolean newValue)
	{
		pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Fires an existing PropertyChangeEvent to any registered listeners. No
	 * event is fired if the given event's old and new values are equal and
	 * non-null.
	 * 
	 * @param evt
	 *            The PropertyChangeEvent object.
	 */
	public void firePropertyChange(PropertyChangeEvent evt)
	{
		pcs.firePropertyChange(evt);
	}

	/**
	 * Checks if there are any listeners for a specific property.
	 * 
	 * @param evt
	 *            The PropertyChangeEvent object.
	 * @return <code>true</code>if there are one or more listeners for the given
	 *         property
	 */
	public boolean hasListeners(String propertyName)
	{
		return pcs.hasListeners(propertyName);
	}

	/**
	 * Get the TreePath of this Node.
	 * 
	 * @return the TreePath of this Node.
	 */
	TreePath getPath()
	{
		TreeNode n = this;
		List<TreeNode> l = new LinkedList<TreeNode>();
		for (; n != null; n = n.getParent()) {
			l.add(0, n);
		}
		return new TreePath(l.toArray());
	}

	public int compareTo(Node node)
	{
		// logger.info("Comparing " + this + " and " + o);
		if (node == null)
			return -1;
		else if (node.id == id)
			return 0;
		else {
			if (isMainVariation()) {
				if (node.isMainVariation()) {
					TreePath myPath = getPath();
					TreePath theirPath = node.getPath();
					return myPath.getPathCount() < theirPath.getPathCount() ? -1
							: 1;
				} else
					return -1;
			} else {
				if (node.isMainVariation())
					return 1;
				else {
					TreePath myPath = getPath();
					TreePath theirPath = node.getPath();
					Node parent = (Node) myPath.getPathComponent(0);
					for (int i = 0; i < myPath.getPathCount()
							&& i < theirPath.getPathCount(); i++) {
						Node myNode = (Node) myPath.getPathComponent(i);
						Node theirNode = (Node) theirPath.getPathComponent(i);
						if (myNode != theirNode) {
							return (parent.getIndex(myNode) < parent
									.getIndex(theirNode) ? 1 : -1);
						}
						parent = myNode;
					}
					return myPath.getPathCount() < theirPath.getPathCount() ? -1
							: 1;
				}
			}
		}
	}

	/**
	 * Create a node without any properties.
	 */
	public Node(GameTree gameTree)
	{
		super();
		this.gameTree = gameTree;
		id = ++lastId;
		pcs = new PropertyChangeSupport(this);
	}

	/**
	 * Create a new node which inherits the properties of node n.
	 * 
	 * @param n
	 *            A node from which the properties are copied.
	 */
	protected Node(Node n)
	{
		super(n);
		initInheritedProperties(n);
		this.gameTree = n.gameTree;
		id = ++lastId;
		pcs = new PropertyChangeSupport(this);
	}

	Node(PropertyList pl)
	{
		super(pl);
		initInheritedProperties(pl);
		id = ++lastId;
		pcs = new PropertyChangeSupport(this);
	}

	/**
	 * Set the <code>GameTree</code> of this node. Each node has a reference to
	 * the {@link GameTree} it belongs to.
	 * 
	 * @param gameTree
	 *            The new GameTree.
	 */
	public void setGameTree(GameTree gameTree)
	{
		this.gameTree = gameTree;
		// This initialization seems to be quite inefficient as it creates a lot
		// of new Objects.
		/*
		 * Iterator<Node> it = children.iterator(); while (it.hasNext()) {
		 * it.next().setGameTree(gameTree); }
		 */
	}

	/**
	 * Get the GameTree associated with this Node. Use lazy initialization to
	 * reduce overhead during parsing. We can assume that the RootNode always
	 * has a GameTree (the constructors ensure that).
	 * 
	 * @returns the GameTree associated with this Node.
	 */
	public GameTree getGameTree()
	{
		Node node = this;
		while (node.gameTree == null) {
			if (node instanceof Sequence) {
				node = ((Sequence) node).getFirst().getParent();
			} else
				node = node.getParent();
		}
		if (node != this)
			gameTree = node.gameTree;

		return gameTree;
	}

	/**
	 * Get the board size. The size is determined by the <code>SiZe</code>
	 * property and defaults to 19.
	 * 
	 * @return The size of the board.
	 */
	public short getBoardSize()
	{
		short boardsize = 19;
		if (contains(Property.SIZE)) {
			Value.Number size = (Value.Number) (get(Property.SIZE)).getValue();
			boardsize = (short) size.intValue();
		}
		if (logger.isLoggable(Level.FINE))
			logger.fine("Setting boardsize to " + boardsize);
		return boardsize;
	}

	public Node getParent()
	{
		assert this != parent;
		return parent;
	}

	public RootNode getRoot()
	{
		if (gameTree == null)
			if (parent == null)
				return (RootNode) this;
			else
				return parent.getRoot();
		else
			return gameTree.getRoot();
	}

	public List<Node> getChildren()
	{
		return children;
	}

	public Enumeration children()
	{
		return Collections.enumeration(children);
	}

	public boolean getAllowsChildren()
	{
		return true;
	}

	public Node getChildAt(int i)
	{
		try {
			return children.get(i);
		} catch (IndexOutOfBoundsException ex) {
			return null;
		}
	}

	public int getChildCount()
	{
		return children.size();
	}

	public Node getChild(Point p)
	{
		Property.Key key = Property.BLACK;
		if (this.contains(Property.BLACK))
			key = Property.WHITE;
		for (Node child : children) {
			if (p == child.getPoint(key))
				return child;
		}
		return null;
	}

	public List<Node> getSiblings()
	{
		return new SiblingsList();
	}

	public int getSiblingCount()
	{
		if (parent != null) {
			return parent.children.size() - 1;
		} else
			return 0;
	}

	public boolean isFirstChild()
	{
		if (parent == null)
			return false;

		if (parent.children.get(0) == this)
			return true;
		else
			return false;
	}

	public boolean isLastChild()
	{
		if (parent == null)
			return false;

		if (parent.children.get(parent.children.size() - 1) == this)
			return true;
		else
			return false;
	}

	/*
	 * public Node getSiblingAt(int n) { return (Node) parent.getChildAt(n +
	 * parent.getIndex(this) + 1); }
	 */

	public int getIndex(Node n)
	{
		return children.indexOf(n);
	}

	public int getIndex(TreeNode n)
	{
		if (n instanceof Node)
			return getIndex((Node) n);
		else
			return -1;
	}

	public boolean isLeaf()
	{
		return children.size() == 0;
	}

	public boolean isMove()
	{
		return contains(Property.BLACK) || contains(Property.WHITE);
	}

	public boolean isBoardSetup()
	{
		return contains(Property.ADD_BLACK) || contains(Property.ADD_WHITE)
				|| contains(Property.ADD_EMPTY);
	}

	public boolean isRoot()
	{
		return this == getRoot();
	}

	public boolean isDiagram()
	{
		// The root node should not be a figure if the board is empty
		// SmartGo marks the root node as the start of a new diagram
		if (isRoot() && !isBoardSetup() && !isMove())
			return false;

		if (logger.isLoggable(Level.FINE))
			logger.fine("isDiagram() called for " + this);
		return contains(Property.FIGURE);
	}

	public void setDiagram(boolean newValue)
	{
		// it makes no sense to set the diagram property on an empty root node
		if (isRoot() && !isMove() && !isBoardSetup())
			return;

		logger.info("setDiagram(" + newValue + ") called for " + this);
		boolean oldValue = isDiagram();
		if (newValue) {
			put(Property.createProperty(Property.FIGURE));
			if (getRoot() != null) {
				TreeVisitor<GameTree, Node> visitor = new TreeVisitor<GameTree, Node>(
						getRoot().getGameTree(), this) {
					@Override
					protected void visitNode(Node n)
					{
						Node p = n.getParent();
						Goban goban;
						if (p == null) {
							goban = gameTree.getGoban(n.getBoardSize());
							n.setMoveNo(n.isMove() ? 1 : 0);
						} else {
							goban = p.getGoban();
							if (goban != null) {
								try {
									goban = goban.clone();
								} catch (CloneNotSupportedException ex) {
									throw new RuntimeException(
											"goban should support clone() but doesn't",
											ex);
								}

								if (n.isBeginOfVariation() || p.isDiagram()) {
									if (goban instanceof MarkupModel) {
										// logger.info("ResetMarkup on move " +
										// p.getMoveNo());
										// ((MarkupModel) goban).resetMarkup();
										// doMarkup();
									} else
										logger.warning("ResetMarkup not called, no MarkupModel");
								}

								if (p.getIndex(n) != 0)
									n.setMoveNo(1);
							}
							// else
							// n.setMoveNo(p.getMoveNo() + (n.isMove() ? 1 :
							// 0));

							n.setGoban(null, false);
							n.setGoban(goban, true);
						}
					}
				};
				visitor.visit();
			}
		} else {
			remove(Property.FIGURE);
		}
		firePropertyChange("diagram", oldValue, newValue);
	}

	public String getComment()
	{
		Property p = get(Property.COMMENT);
		if (p == null)
			return "";
		Value comment = p.getValue();
		if (comment != null)
			return comment.toString();
		else
			return "";
	}

	public void setComment(String c)
	{
		Object oldValue = get(Property.COMMENT).getValue();
		Object newValue = c;
		logger.info("Setting comment to " + c);
		if (c.length() > 0) {
			if (!newValue.equals(oldValue.toString())) {
				Property p = Property.createProperty(Property.COMMENT);
				p.setValue(AbstractValue.createValue(c));
				put(p);
				firePropertyChange("comment", oldValue, newValue);
			}
		} else {
			remove(Property.COMMENT);
			firePropertyChange("comment", oldValue, newValue);
		}
	}

	public void setValue(Property.Key key, String value)
	{
		Property oldValue = get(key);
		Property newValue = oldValue;
		if (oldValue != null)
			oldValue = oldValue.clone();
		if (newValue == null) {
			newValue = Property.createProperty(key);
			add(newValue);
		}

		logger.info("Setting " + key + " to " + value);
		if (value.length() > 0) {
			newValue.setValue(value);
			firePropertyChange("SGFProperty", oldValue, newValue);
		} else {
			remove(key);
			firePropertyChange("SGFProperty", oldValue, null);
		}
	}

	public void setProperty(Property property)
	{
		Property oldValue = get(property.getKey());
		Property newValue = property;
		if (oldValue != null)
			oldValue = oldValue.clone();
		add(property);

		logger.info("Setting " + property.getKey() + " to " + property.getValue());
		firePropertyChange("SGFProperty", oldValue, newValue);
	}


	public Goban getGoban()
	{
		if (goban == null) {
			Node node = this;
			Stack<Node> nodes = new Stack<Node>();
			while (node != null && node.goban == null) {
				nodes.push(node);
				node = node.parent;
			}
			while (!nodes.empty()) {
				nodes.pop().initGoban();
			}
		}
		return goban;
	}

	public int getId()
	{
		return id;
	}

	public void initGoban()
	{
		if (gameTree == null)
			gameTree = parent.getGameTree();

		if (parent == null) {
			setGoban(gameTree.getGoban(getBoardSize()));
		} else {
			setGoban(gameTree.getGoban(parent.getGoban()));
		}

		if (contains(Property.MOVE_NO)) {
			Value.Number no = null;

			Value value = (get(Property.MOVE_NO)).getValue();
			logger.info("value is " + value + " " + value.getClass());
			if (value instanceof Value.ValueList)
				no = (Value.Number) ((Value.ValueList) value).get(0);
			else
				no = (Value.Number) value;

			if (logger.isLoggable(Level.FINE))
				logger.fine("Setting moveNo on node " + this + " to "
						+ no.intValue());
			setMoveNo(no.intValue());
		} else if (parent == null) {
			setMoveNo(isMove() ? 1 : 0);
		} else if (parent.getIndex(this) != 0) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("Setting moveNo on node " + this + " to 1");
			setMoveNo(1);
		} else {
			if (logger.isLoggable(Level.FINE))
				logger.fine("Setting moveNo on node " + this + " to "
						+ parent.getMoveNo() + " + " + (isMove() ? 1 : 0));
			setMoveNo(parent.getMoveNo() + (isMove() ? 1 : 0));
		}
	}

	private void doMarkup()
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("doMarkup: enter");
		if (contains(Property.SIZE)) {
			Value.Number size = (Value.Number) (get(Property.SIZE)).getValue();
			goban.setBoardSize((short) size.intValue());
		}
		if (contains(Property.BLACK)) {
			Point pt = getPoint(Property.BLACK);
			if (pt != null) {
				if (logger.isLoggable(Level.FINE))
					logger.fine("Node " + this + ": Black move at " + pt);
				goban.move(pt, BoardType.BLACK, moveNo);
			}
		} else if (contains(Property.WHITE)) {
			Point pt = getPoint(Property.WHITE);
			if (pt != null) {
				if (logger.isLoggable(Level.FINE))
					logger.fine("Node " + this + ": White move at " + pt);
				goban.move(pt, BoardType.WHITE, moveNo);
			}
		}
		if (contains(Property.ADD_BLACK)) {
			Value.PointList pointList = getPointList(Property.ADD_BLACK);
			Iterator it = pointList.iterator();
			Point pt;
			while (it.hasNext()) {
				pt = (Point) it.next();
				if (logger.isLoggable(Level.FINE))
					logger.fine("Node " + this + ": AddBlack at " + pt);
				goban.putStone(pt, BoardType.BLACK);
			}
		}
		if (contains(Property.ADD_WHITE)) {
			Value.PointList pointList = getPointList(Property.ADD_WHITE);
			Iterator it = pointList.iterator();
			Point pt;
			while (it.hasNext()) {
				pt = (Point) it.next();
				if (logger.isLoggable(Level.FINE))
					logger.fine("Node " + this + ": AddWhite at " + pt);
				goban.putStone(pt, BoardType.WHITE);
			}
		}
		if (contains(Property.ADD_EMPTY)) {
			Value.PointList pointList = getPointList(Property.ADD_EMPTY);
			Iterator it = pointList.iterator();
			Point pt;
			while (it.hasNext()) {
				pt = (Point) it.next();
				if (logger.isLoggable(Level.FINE))
					logger.fine("Node " + this + ": AddEmpty at " + pt);
				goban.putStone(pt, BoardType.EMPTY);
			}
		}

		if (goban instanceof MarkupModel) {
			MarkupModel markup = (MarkupModel) goban;

			if (isBeginOfVariation() || parent.isDiagram())
				markup.resetMarkup();

			Iterator propIt = values().iterator();
			while (propIt.hasNext()) {
				Property property = (Property) propIt.next();
				{
					if (property.getKey().equals(Property.VIEW)) {
						logger.fine("VIEW found");
						Value view = getValue(Property.VIEW);
						if (view instanceof Value.ValueList) {
							Value.ValueList vl = (Value.ValueList) view;
							if (logger.isLoggable(Level.FINE))
								logger.fine("ValueList: size=" + vl.size());
							view = vl.get(0);
						}
						markup.setRegion(new SimpleRegion(
								(Value.PointList) view));
					}

					if (property instanceof Property.Move) {
						Point pt = getPoint(property.getKey());
						BoardType color = ((Property.Move) property).getColor();
						if (pt != null) {
							if (logger.isLoggable(Level.FINE))
								logger.fine("setMarkup: " + pt + ", " + color
										+ ", " + moveNo);
							markup.setMarkup(pt, new MarkupModel.Move(color,
									moveNo));
						}
					}

					if (property instanceof Property.AddStones) {
						BoardType color = ((Property.AddStones) property)
								.getColor();
						Value.PointList pointList = getPointList(property
								.getKey());
						Iterator it = pointList.iterator();
						Point pt;
						while (it.hasNext()) {
							pt = (Point) it.next();
							if (logger.isLoggable(Level.FINE))
								logger.fine("setMarkup: " + pt + ", " + color);
							markup.setMarkup(pt, new MarkupModel.Stone(color));
						}
					}
					if (property instanceof Property.Markup) {
						if (property instanceof Property.Label) {
							Value vl = property.getValue();
							if (vl instanceof Value.ValueList) {
								Iterator vi = ((Value.ValueList) vl).iterator();
								while (vi.hasNext()) {
									Value v = (Value) vi.next();
									if (v instanceof Value.Label) {
										Point point = ((Value.Label) v)
												.getPoint();
										String text = ((Value.Label) v)
												.toString();
										if (logger.isLoggable(Level.FINE))
											logger.fine("setMarkup: " + point
													+ ", " + text);
										markup.setMarkup(point,
												new MarkupModel.Text(text));
									}
								}
							} else if (logger.isLoggable(Level.FINE))
								logger.fine("Label has a value of "
										+ vl.getClass().getName());
						} else {
							Value.PointList pointList = getPointList(property
									.getKey());
							Iterator it = pointList.iterator();
							Point pt;
							while (it.hasNext()) {
								pt = (Point) it.next();
								if (logger.isLoggable(Level.FINE))
									logger.fine("Node " + this + ": "
											+ property.getKey() + " at " + pt);
								if (property.getKey().equals(Property.TRIANGLE)) {
									markup.setMarkup(pt,
											new MarkupModel.Triangle());
								} else if (property.getKey().equals(
										Property.SQUARE)) {
									markup.setMarkup(pt,
											new MarkupModel.Square());
								} else if (property.getKey().equals(
										Property.CIRCLE)) {
									markup.setMarkup(pt,
											new MarkupModel.Circle());
								} else if (property.getKey().equals(
										Property.MARK)) {
									markup.setMarkup(pt, new MarkupModel.Mark());
								} else if (property.getKey().equals(
										Property.TERRITORY_WHITE)) {
									markup.setMarkup(pt,
											new MarkupModel.WhiteTerritory());
								} else if (property.getKey().equals(
										Property.TERRITORY_BLACK)) {
									markup.setMarkup(pt,
											new MarkupModel.BlackTerritory());
								}
							}
						}
					}
				}
			}
		}
		// if (logger.isLoggable(Level.FINE))
		// logger.fine("Node: goban is " + goban);
		if (logger.isLoggable(Level.FINE))
			logger.fine("doMarkup: leave");
	}

	public void setGoban(Goban newGoban)
	{
		setGoban(newGoban, true);
	}

	public void setGoban(Goban newGoban, boolean doMarkup)
	{
		Goban oldGoban = goban;
		goban = newGoban;

		// logger.info("Node " + this + ": Setting goban to " + newGoban);

		if (oldGoban != null)
			oldGoban.removeGobanListener(this);
		if (doMarkup && newGoban != null)
			doMarkup();
		firePropertyChange("goban", oldGoban, newGoban);
		if (newGoban != null)
			newGoban.addGobanListener(this);
	}

	@Override
	public void add(Property p)
	{
		super.add(p);
		if (p instanceof Property.Inheritable) {
			logger.info("inherited: " + p);
			if (inheritedProperties == null) {
				inheritedProperties = new PropertyList();
			} else if (parent != null
					&& parent.inheritedProperties == inheritedProperties) {
				inheritedProperties = new PropertyList(inheritedProperties);
			}
			inheritedProperties.put(p.getKey(), p);
			propagateInheritedProperties();
		}
	}

    boolean add(Node n)
	{
		n.setParent(this);
		if (n.depth < 0) {
			Node p = n;
			while (p.parent != null) {
				p = p.parent;
				p.depth = -1;
			}
		} else {
			Node p = n;
			while (p.parent != null && p.parent.depth <= p.depth) {
				p.parent.depth = p.depth + 1;
				p = p.parent;
			}
		}
		return children.add(n);
	}

	public void setParent(Node n)
	{
		assert n != this;
		if (logger.isLoggable(Level.FINE))
			logger.fine("Node " + this + ": setting parent to " + n);
		parent = n;
		if (n != null && n.inheritedProperties != null) {
			if (inheritedProperties == null)
				inheritedProperties = n.inheritedProperties;
			else {
				inheritedProperties.addAll(n.inheritedProperties);
			}
		}
	}

	public BoardType getColor()
	{
		if (contains(Property.WHITE))
			return BoardType.WHITE;
		else if (contains(Property.BLACK))
			return BoardType.BLACK;
		else
			return BoardType.EMPTY;
	}

	@Override
	public boolean contains(Object o)
	{
		assert o instanceof Property.Key;
		if (super.containsKey(o))
			return true;
		else if (inheritedProperties != null)
			return inheritedProperties.containsKey(o);
		else
			return false;
	}

	@Override
	public Property get(Object key)
	{
		Property p;
		p = super.get(key);
		if (p == null && inheritedProperties != null)
			p = inheritedProperties.get(key);
		return p;
	}

	@Override
	public Value getValue(Property.Key k)
	{
		return super.getValue(k);
	}

	public String getString(Property.Key k)
	{
		Value value = super.getValue(k);
		if (value != null)
			return value.getString();
		else
			return "";
	}

	public Point getPoint()
	{
		if (contains(Property.BLACK))
			return getPoint(Property.BLACK);
		else if (contains(Property.WHITE))
			return getPoint(Property.WHITE);
		else
			return null;
	}

	@Override
	public Point getPoint(Property.Key k)
	{
		return super.getPoint(k);
	}

	public String getGameName()
	{
		assert getRoot() != null;

		if (this != getRoot())
			return getRoot().getGameName();
		else if (contains(Property.GAME_NAME)) {
			return getValue(Property.GAME_NAME).toString();
		} else if (contains(Property.PLAYER_BLACK)
				&& contains(Property.PLAYER_WHITE)) {
			Value black = getValue(Property.PLAYER_BLACK);
			Value white = getValue(Property.PLAYER_WHITE);
			assert black != null;
			assert white != null;
			return black.toString() + " - " + white.toString();
		} else
			return "";
	}

	public String getName()
	{
		if (contains(Property.NAME)) {
			return getValue(Property.NAME).toString();
		} else if (contains(Property.BLACK))
			return "Black " + getMoveNo();
		else if (contains(Property.WHITE))
			return "White " + getMoveNo();
		else if (isBoardSetup())
			return "position setup";
		else
			return "<Node>";
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer(16).append("Node ").append(
				getId());
		if (contains(Property.NAME))
			buffer.append(" ").append(getValue(Property.NAME).toString());

		return buffer.toString();
	}

	public int getMoveNo()
	{
		Node n = this;
		while (n != null && n.getParent() != null && !n.isMove()) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("getMoveNo: not a move, get parent node");
			n = n.getParent();
		}
		return n.moveNo;
	}

	/**
	 * Play a move at point p.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the is already a move or setup property at this node
	 * @param Point
	 *            p the point where to add a stone
	 */
	public void move(Point p) throws UnsupportedOperationException
	{
		if (isMove() || isBoardSetup() || this instanceof RootNode)
			throw new UnsupportedOperationException();
		if (contains(Property.MOVE_NO)) {
			Value.Number no = (Value.Number) (get(Property.MOVE_NO)).getValue();
			setMoveNo(no.intValue());
		} else if (parent != null)
			setMoveNo(parent.getMoveNo() + 1);

		Node parentMove = parent;
		while (!parentMove.isMove() && parentMove.parent != null)
			parentMove = parentMove.parent;
		Property.Key key = Property.BLACK;
		if (parentMove != null)
			if (parentMove.contains(Property.BLACK))
				key = Property.WHITE;
		Value point = AbstractValue.createPoint(p);
		Goban parentModel = gameTree.getGoban(parent.getGoban());
		assert parentModel != null;
		put(Property.createProperty(key, point));
		setGoban(parentModel);
	}

	/**
	 * Add a stone of color c at point p. This method ensures that the AW, AB
	 * and AE properties of this node are kept in a consistent state, i.e. that
	 * each point occurs at most in one of the pointlists of these properties.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the node is a move node
	 * @param Point
	 *            p the point where to add a stone
	 * @param BoardType
	 *            c the color of the stone
	 */
	public void addStone(Point p, BoardType c)
			throws UnsupportedOperationException
	{
		if (isMove())
			throw new UnsupportedOperationException();
		Iterator it = Property.addStoneProperties.iterator();
		Property.Key key = null;
		while (it.hasNext()) {
			key = (Property.Key) it.next();
			if (contains(key)) {
				getPointList(key).remove(p);
			}
		}

		Goban model = getGoban();
		if (model == null)
			if (parent == null)
				model = gameTree.getGoban(getBoardSize());
			else {
				try {
					model = parent.getGoban().clone();
				} catch (CloneNotSupportedException ex) {
					throw new RuntimeException("Implementation error", ex);
				}
			}

		if (!model.getStone(p).equals(c)) {
			if (c == BoardType.WHITE)
				key = Property.ADD_WHITE;
			else if (c == BoardType.BLACK)
				key = Property.ADD_BLACK;
			else if (c == BoardType.EMPTY)
				key = Property.ADD_EMPTY;
			else
				assert (false);

			Value.PointList pl;
			if (!contains(key)) {
				pl = (Value.PointList) AbstractValue.createPointList();
				put(Property.createProperty(key, pl));
			} else
				pl = getPointList(key);
			pl.add(p);
		}
		setGoban(model);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Node)
			return id == ((Node) o).id;
		else
			return false;
	}

	@Override
	public int hashCode()
	{
		return id;
	}

	public boolean isMainVariation()
	{
		Node n = this;
		while (n.parent != null && !(n instanceof RootNode)) {
			if (n.parent.children.indexOf(n) != 0)
				return false;
			else
				n = n.parent;
		}
		return true;
	}

	public boolean isBeginOfVariation()
	{
		Node n = this;
		if (parent == null)
			return true;
		else
			return n.parent.children.indexOf(n) != 0;
	}

	public Node getBeginOfVariation()
	{
		Node n = this;
		while (n != null && !n.isBeginOfVariation())
			n = n.getParent();
		return n;
	}

	public Iterator iterator()
	{
		return children.iterator();
	}

	@Override
	public void write(PrintWriter out)
	{
		super.write(out);
		/*
		 * if (getChildCount() > 1) { Iterator it = getChildren().iterator();
		 * while (it.hasNext()) { Node node = (Node)it.next(); out.println("(");
		 * node.write(out); out.println(")"); } } else if (getChildCount() == 1)
		 * { Node node = (Node)getChildAt(0); node.write(out); }
		 */
	}

	public void setProperty(Property.Key key, Object newValue)
	{
		assert newValue != null;
		if (!contains(key))
			put(Property.createProperty(key, newValue));
		else {
			Object oldValue = (get(key)).getValue();
			if (!newValue.equals(oldValue))
				logger.severe("Value already set! old: " + oldValue + " new: "
						+ newValue);
		}
	}

	public void setFileFormat(int fileFormat)
	{
		setProperty(Property.FILE_FORMAT, new Integer(fileFormat));
	}

	public void setGame(int game)
	{
		setProperty(Property.GAME, new Integer(game));
	}

	/**
	 * Set the move number of this node.
	 * 
	 * @param moveNo
	 *            The move number to set.
	 */
	public void setMoveNo(int moveNo)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("Node " + this + ": Setting moveNo to " + moveNo);
		this.moveNo = moveNo;
	}

	public Memento createMemento()
	{
		return new NodeMemento(this);
	}

	public void setMemento(Memento memento)
	{
		NodeMemento nm = (NodeMemento) memento;
		logger.info("Node: setMemento: " + nm);
		parent = nm.getParent();
		children = nm.getChildren();
		logger.info("Setting properties to " + nm.getProperties());
		clear();
		putAll(nm.getProperties());
		Goban parentModel;
		if (parent == null)
			parentModel = gameTree.getGoban(getBoardSize());
		else
			parentModel = parent.getGoban();
		setGoban(parentModel);
		firePropertyChange(null, null, null);
	}

	public void join(Node n)
	{
		SortedMap<Goban, Node> childMap = new TreeMap<Goban, Node>(
				new BoardComparator());
		Iterator it;
		it = getChildren().iterator();
		while (it.hasNext()) {
			Node c = (Node) it.next();
			childMap.put(c.getGoban(), c);
		}
		it = n.getChildren().iterator();
		while (it.hasNext()) {
			Node theirChild = (Node) it.next();
			Node myChild = childMap.get(theirChild.getGoban());
			if (myChild == null)
				add(theirChild);
			else
				myChild.join(theirChild);
		}
	}

	/**
	 * Calculate the depth of the sub-tree rooted at this node by recursion over
	 * all children. The result is cached for later use.
	 * 
	 * @returns the depth of the sub-tree rooted at this node.
	 */
	public int getDepth()
	{
		if (depth < 0) {
			Queue<Node> subRoots = new LinkedList<Node>();
			Node n = this;

			while (n != null) {
				if (n.children.size() == 0) {
					// found a leaf; go up
					n.depth = 0;
					while (n.parent != null && n.depth >= n.parent.depth) {
						n.parent.depth = n.depth + 1;
						n = n.parent;
					}
					n = subRoots.poll();
				} else if (n.children.size() == 1) {
					n = n.children.get(0);
				} else {
					subRoots.addAll(n.children);
					n = subRoots.poll();
				}
			}
		}
		return depth;
	}

	void initInheritedProperties(PropertyList p)
	{
		Map<Property.Key, Property> inheritable = null;

		for (Property.Key k : p.keySet()) {
			if (get(k) instanceof Property.Inheritable) {
				if (logger.isLoggable(Level.FINE))
					logger.fine("inherited: " + get(k));
				if (inheritable == null)
					inheritable = new TreeMap<Property.Key, Property>();
				inheritable.put(k, get(k));
			}
		}

		if (inheritable != null) {
			inheritedProperties = new PropertyList();
			inheritedProperties.putAll(inheritable);
		}
		propagateInheritedProperties();
	}

	void propagateInheritedProperties()
	{
		for (Node n : children) {
			if (n.inheritedProperties != inheritedProperties) {
				if (n.inheritedProperties == null) {
					n.inheritedProperties = inheritedProperties;
				} else {
					n.inheritedProperties.addAll(inheritedProperties);
				}
				n.propagateInheritedProperties();
			}
		}
	}
}
