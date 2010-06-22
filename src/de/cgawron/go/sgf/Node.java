/*
 *
 * $Id: Node.java 369 2006-04-14 17:04:02Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go.sgf;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.GobanEvent;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.MarkupModelListener;
import de.cgawron.util.Memento;
import de.cgawron.util.MementoOriginator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * An instance of this class represents a node in a sgf game tree.
 * @author Christian Gawron
 * @version $Id: Node.java 369 2006-04-14 17:04:02Z cgawron $
 */
public class Node 
    extends PropertyList 
    implements MarkupModelListener, TreeNode, Comparable<Node>, MementoOriginator//, PropertyChangeListener
{
    private PropertyList inheritedProperties = new PropertyList();

    /**
     * The children of this node.
     */
    protected List<Node> children = new LinkedList<Node>();

    private Node parent = null;
    private GameTree gameTree = null;
    private Goban goban = null;
    private int moveNo = 0;
    private int id;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private static int lastId = 0;
    private static Logger logger = Logger.getLogger(Node.class.getName());

    /**
     * Compares two Nodes or a Node and a Goban.
     * 
     * This Comparator is <strong>not</strong> compatible with {@link Node#compareTo} or {@link Node#equals}.
     */
    static class BoardComparator implements Comparator<Goban> 
    {
	/**
	 * Compare two Nodes by comparing the values of the Zobrist hash of their Gobans.
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
		m1 = (Goban) o1;
	    if (o2 instanceof Node)
		m2 = ((Node) o2).getGoban();
	    else 
		m2 = (Goban) o2;
	    
	    int h1 = m1.zobristHash();
	    int h2 = m2.zobristHash();
	    if (h1 < h2) 
		return -1;
	    else if (h1 > h2) 
		return 1;
		else return 0;
	}
    }

    /**
     * The VariationPath of a Node consists of all nodes of the TreePath of a node which are not the first child of their parent.
     */
    public static class VariationPath extends LinkedList<Node>
    {
	VariationPath(Node node)
	{
	    super();
	    Node parent = node.getParent();
	    while (parent != null)
	    {
		if (parent.getIndex(node) != 0)
		    add(0, node);
		node = parent;
		parent = node.getParent();
	    }
	}
	
	public String toString()
	{
	    StringBuffer sb = new StringBuffer();
	    for (Node n : this)
	    {
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
     * This method is called if the {@link Goban} of this node is changed.
     * The change is signalled to all registered <code>PropertyChangeListener</code>
     */
    public void modelChanged(GobanEvent e)
    {
        firePropertyChange(null, null, null);
    }

    /**
     * This method is called if a stone is added to the Goban of this node.
     * @param event The {@link GobanEvent}.
     */
    public void stoneAdded(GobanEvent event)
    {
        // throw new RuntimeException("Hey, someone just added a stone! This should not happen this way");
    }

    /**
     * This method is called if stones are removed from the Goban of this node.
     * @param event The {@link GobanEvent}.
     */
    public void stonesRemoved(GobanEvent event)
    {
        // throw new RuntimeException("Hey, someone just removed a stone! This should not happen this way");
    }

    /**
     * This method is called if the selected {@link Region} of the Goban of this node is changed.
     * @param event The {@link GobanEvent}.
     */
    public void regionChanged(GobanEvent event)
    {
	if (logger.isDebugEnabled())
	    logger.debug("Node.regionChanged: " + id);
        Property oldView = (Property)get(Property.VIEW);
        MarkupModel mm = (MarkupModel) getGoban();
        Property newView = Property.createProperty(Property.VIEW);
        Value.ValueList value = (Value.ValueList) AbstractValue.createValueList();
        if (mm.getRegion() != null && oldView != newView)
        {
            value.add(mm.getRegion().getPointList());
            newView.setValue(value);
            put(newView);
            firePropertyChange("view", oldView, newView);
        }
        else
        {
            logger.warn("regionChanged: region is null");
            remove(newView.getKey());
            firePropertyChange("view", oldView, null);
        }
    }

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is registered for all properties.
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        assert listener != this;
        logger.debug("Node.addPropertyChangeListener: " + listener);
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered for all properties.
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        logger.debug("Node.removePropertyChangeListener: " + listener);
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
        logger.debug("Node.addPropertyChangeListener: " + listener + ", " + propertyName);
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property.
     * @param propertyName The name of the property that was listened on
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        logger.debug("Node.removePropertyChangeListener: " + listener + ", " + propertyName);
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


    /**
     * Get the TreePath of this Node.
     * @return the TreePath of this Node.
     */
    TreePath getPath()
    {
        TreeNode n = this;
        List<TreeNode> l = new LinkedList<TreeNode>();
        for (; n != null; n = n.getParent())
        {
            l.add(0, n);
        }
        return new TreePath(l.toArray());
    }


    public int compareTo(Node node)
    {
	//logger.info("Comparing " + this + " and " + o);
	if (node == null)
	    return -1;
	else if (node.id == id)
	    return 0;
	else 
	    if (isMainVariation())
	    {
		if (node.isMainVariation())
		{
		    TreePath myPath = getPath();
		    TreePath theirPath = node.getPath();
		    return myPath.getPathCount() < theirPath.getPathCount() ? -1 : 1;
		}
		else
		    return -11;
	    }
	    else
	    {
		if (node.isMainVariation())
		    return 1;
		else
		{
		    TreePath myPath = getPath();
		    TreePath theirPath = node.getPath();
		    Node parent = (Node) myPath.getPathComponent(0);
		    for (int i = 0; i < myPath.getPathCount() && i < theirPath.getPathCount(); i++)
		    {
			Node myNode = (Node) myPath.getPathComponent(i);
			Node theirNode = (Node) theirPath.getPathComponent(i);
			if (myNode != theirNode)
			{
			    return (parent.getIndex(myNode) < parent.getIndex(theirNode) ? 1 : -1);
			}
			parent = myNode;
		    }
		    return myPath.getPathCount() < theirPath.getPathCount() ? -1 : 1;
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
     * @param n A node from which the properties are copied.
     */
    protected Node(Node n)
    {
	super((PropertyList) n);
	setInheritedProperties((PropertyList) n);
	this.gameTree = n.gameTree;
        id = ++lastId;
        pcs = new PropertyChangeSupport(this);
    }

    Node(PropertyList pl)
    {
        super(pl);
	setInheritedProperties(pl);
        id = ++lastId;
        pcs = new PropertyChangeSupport(this);
    }



    /**
     * Set the <code>GameTree</code> of this node.
     * Each node has a reference to the {@link GameTree} it belongs to.
     * @param gameTree The new GameTree.
     */
    public void setGameTree(GameTree gameTree)
    {
	this.gameTree = gameTree;
        Iterator<Node> it = children.iterator();
	while (it.hasNext())
	{
	    it.next().setGameTree(gameTree);
	}
    }

    public GameTree getGameTree()
    {
	return gameTree;
    }

    /**
     * Get the board size.
     * The size is determined by the <code>SiZe</code> property and defaults to 19.
     * @return The size of the board.
     */
    public short getBoardSize()
    {
	short boardsize = 19;
	if (contains(Property.SIZE)) {
	    Value.Number size = (Value.Number) ((Property) get(Property.SIZE)).getValue();
	    boardsize = (short) size.intValue();
	}
	if (logger.isDebugEnabled())
	    logger.debug("Setting boardsize to " + boardsize);
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

    public TreeNode getChildAt(int i)
    {
        try
        {
            return (Node)children.get(i);
        }
        catch (IndexOutOfBoundsException ex)
        {
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
	for (Node child : children)
	{
	    if (p == child.getPoint(key))
		return child;
	}
	return null;
    }
    
    public int getSiblingCount()
    {
	if (logger.isDebugEnabled())
	    logger.debug("getSiblingCount()");
	if (parent != null)
	{
	    if (logger.isDebugEnabled())
	    {
		logger.debug("childCount: " + parent.getChildCount());
		logger.debug("index: " + parent.getIndex(this));
	    }
	    return parent.getChildCount() - parent.getIndex(this) - 1;
	}
	else 
	    return 0;
    }

    public Node getSiblingAt(int n)
    {
	return (Node) parent.getChildAt(n + parent.getIndex(this) + 1);
    }

    public int getIndex(Node n)
    {
        return children.indexOf(n);
    }

    public int getIndex(TreeNode n)
    {
        if (n instanceof Node)
            return getIndex((Node)n);
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
        return contains(Property.ADD_BLACK) || contains(Property.ADD_WHITE) || contains(Property.ADD_EMPTY);
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

	if (logger.isDebugEnabled())
	    logger.debug("isDiagram() called for " + this);
        return contains(Property.FIGURE);
    }

    public void setDiagram(boolean newValue)
    {
	// it makes no sense to set the diagram property on an empty root node
	if (isRoot() && !isMove() && !isBoardSetup())
	    return;

	logger.info("setDiagram(" + newValue + ") called for " + this);
        boolean oldValue = isDiagram();
        if (newValue)
        {
            put(Property.createProperty(Property.FIGURE));
	    if (getRoot() != null)
	    {
		TreeVisitor<GameTree, Node> visitor = 
		    new TreeVisitor<GameTree, Node>(getRoot().getGameTree(), this) 
		    {
			protected void visitNode(Object o)
			{
			    Node n = (Node) o;
			    
			    Node p = (Node) n.getParent();
			    Goban goban;
			    if (p == null) 
			    {
				goban = gameTree.getGoban(n.getBoardSize());
				n.setMoveNo(n.isMove() ? 1 : 0);
			    }
			    else 
			    {
				goban = (Goban) p.getGoban();
				if (goban != null)
				{
				    try {
					goban = goban.clone();
				    }
				    catch (CloneNotSupportedException ex) {
					throw new RuntimeException("goban should support clone() but doesn't", ex);
				    }
				    
				    if (n.isBeginOfVariation() || p.isDiagram()) {
					if (goban instanceof MarkupModel)
					{ 
					    //logger.info("ResetMarkup on move " + p.getMoveNo());
					    //((MarkupModel) goban).resetMarkup();
					    //doMarkup();
					}
					else
					    logger.warn("ResetMarkup not called, no MarkupModel");
				    }
				    
				    if (p.getIndex(n) != 0) 
					n.setMoveNo(1);
				}
				//else 
				//    n.setMoveNo(p.getMoveNo() + (n.isMove() ? 1 : 0));

				n.setGoban(null, false);
				n.setGoban(goban, true);
			    }
			}
		    };
		visitor.visit();
	    }
	}
        else
        {
            remove(Property.FIGURE);
        }
        firePropertyChange("diagram", oldValue, newValue);
    }

    public void setComment(String c)
    {
	Object oldValue = get(Property.COMMENT).getValue();
	Object newValue = c;
	logger.info("Setting comment to " + c);
	if (c.length() > 0)
	{
	    if (!newValue.equals(oldValue.toString()))
	    {
		Property p = Property.createProperty(Property.COMMENT);
		p.setValue(AbstractValue.createValue(c));
		put(p);
		firePropertyChange("comment", oldValue, newValue);
	    }
	}
	else
	{
	    remove(Property.COMMENT);
	    firePropertyChange("comment", oldValue, newValue);
	}
    }

    public void setValue(Property.Key key, String value)
    {
	Property oldValue = get(key);
	Property newValue = oldValue;
	if (oldValue != null) oldValue = oldValue.clone();
	if (newValue == null) 
	{
	    newValue = Property.createProperty(key);
	    add(newValue);
	}
	
	logger.info("Setting " + key + " to " + value);
	if (value.length() > 0)
	{
	    newValue.setValue(value);
	    firePropertyChange("SGFProperty", oldValue, newValue);
	}
	else
	{
	    remove(key);
	    firePropertyChange("SGFProperty", oldValue, null);
	}
    }

    public Goban getGoban()
    {
        return goban;
    }

    public int getId()
    {
        return id;
    }

    private void doMarkup()
    {
	logger.info("doMarkup: enter");
	if (contains(Property.SIZE)) {
	    Value.Number size = (Value.Number) ((Property) get(Property.SIZE)).getValue();
	    goban.setBoardSize((short) size.intValue());
	}
        if (contains(Property.BLACK))
        {
            Point pt = getPoint(Property.BLACK);
            if (pt != null) {
		if (logger.isDebugEnabled())
		    logger.debug("Node " + this + ": Black move at " + pt);
                goban.move(pt, BoardType.BLACK, moveNo);
	    }
        }
        else if (contains(Property.WHITE))
        {
            Point pt = getPoint(Property.WHITE);
            if (pt != null) {
		if (logger.isDebugEnabled())
		    logger.debug("Node " + this + ": White move at " + pt);
                goban.move(pt, BoardType.WHITE, moveNo);
	    }
        }
        if (contains(Property.ADD_BLACK))
        {
            Value.PointList pointList = getPointList(Property.ADD_BLACK);
            Iterator it = pointList.iterator();
	    Point pt;
            while (it.hasNext()) {
		pt = (Point) it.next();
		if (logger.isDebugEnabled())
		    logger.debug("Node " + this + ": AddBlack at " + pt);
                goban.putStone(pt, BoardType.BLACK);
	    }
        }
        if (contains(Property.ADD_WHITE))
        {
            Value.PointList pointList = getPointList(Property.ADD_WHITE);
            Iterator it = pointList.iterator();
	    Point pt;
            while (it.hasNext()) {
		pt = (Point) it.next();
		if (logger.isDebugEnabled())
		    logger.debug("Node " + this + ": AddWhite at " + pt);
                goban.putStone(pt, BoardType.WHITE);
	    }
        }
        if (contains(Property.ADD_EMPTY))
        {
            Value.PointList pointList = getPointList(Property.ADD_EMPTY);
            Iterator it = pointList.iterator();
	    Point pt;
            while (it.hasNext()) {
		pt = (Point) it.next();
		if (logger.isDebugEnabled())
		    logger.debug("Node " + this + ": AddEmpty at " + pt);
                goban.putStone(pt, BoardType.EMPTY);
	    }
        }

        if (goban instanceof MarkupModel)
        {
            MarkupModel markup = (MarkupModel)goban;

	    if (isBeginOfVariation() || parent.isDiagram())
		markup.resetMarkup();

	    Iterator propIt = values().iterator();
	    while (propIt.hasNext()) {
		Property property = (Property) propIt.next();
		{
		    if (property.getKey().equals(Property.VIEW)) {
			logger.debug("VIEW found");
			Value view = getValue(Property.VIEW);
			if (view instanceof Value.ValueList) {
			    Value.ValueList vl = (Value.ValueList) view;
			    if (logger.isDebugEnabled())
				logger.debug("ValueList: size=" + vl.size());
			    view = (Value)vl.get(0);
			}
			markup.setRegion(new SimpleRegion((Value.PointList) view));
		    }
		   
		    if (property instanceof Property.Move) {
			Point pt = getPoint(property.getKey());
			BoardType color = ((Property.Move) property).getColor();
			if (pt != null)
			{
			    logger.debug("setMarkup: " + pt + ", " + color + ", " + moveNo);
			    markup.setMarkup(pt, new MarkupModel.Move(color, moveNo));
			}
		    }
		    
		    if (property instanceof Property.AddStones) {
			BoardType color = ((Property.AddStones) property).getColor();
			Value.PointList pointList = getPointList(property.getKey());
			Iterator it = pointList.iterator();
			Point pt;
			while (it.hasNext()) {
			    pt = (Point) it.next();
			    if (logger.isDebugEnabled())
				logger.debug("setMarkup: " + pt + ", " + color);
			    markup.setMarkup(pt, new MarkupModel.Stone(color));
			}
		    }
		    if (property instanceof Property.Markup) {
			if (property instanceof Property.Label) {
			    Value vl = property.getValue();
			    if (vl instanceof Value.ValueList) {
				Iterator vi = ((Value.ValueList) vl).iterator();
				while (vi.hasNext()) {
				    Value v = (Value)vi.next();
				    if (v instanceof Value.Label) {
					Point point = ((Value.Label) v).getPoint();
					String text = ((Value.Label) v).toString();
					if (logger.isDebugEnabled())
					    logger.debug("setMarkup: " + point + ", " + text);
					markup.setMarkup(point, new MarkupModel.Text(text));
				    }
				}
			    }
			    else
				if (logger.isDebugEnabled())
				    logger.debug("Label has a value of " + vl.getClass().getName());
			}
			else {
			    Value.PointList pointList = getPointList(property.getKey());
			    Iterator it = pointList.iterator();
			    Point pt;
			    while (it.hasNext()) {
				pt = (Point) it.next();
				if (logger.isDebugEnabled())
				    logger.debug("Node " + this + ": " + property.getKey() + " at " + pt);
				if (property.getKey().equals(Property.TRIANGLE)) {
				    markup.setMarkup(pt, new MarkupModel.Triangle());
				}
				else if (property.getKey().equals(Property.SQUARE)) {
				    markup.setMarkup(pt, new MarkupModel.Square());
				}
				else if (property.getKey().equals(Property.CIRCLE)) {
				    markup.setMarkup(pt, new MarkupModel.Circle());
				}
				else if (property.getKey().equals(Property.MARK)) {
				    markup.setMarkup(pt, new MarkupModel.Mark());
				}
				else if (property.getKey().equals(Property.TERRITORY_WHITE)) {
				    markup.setMarkup(pt, new MarkupModel.WhiteTerritory());
				}
				else if (property.getKey().equals(Property.TERRITORY_BLACK)) {
				    markup.setMarkup(pt, new MarkupModel.BlackTerritory());
				}
			    }
			}
		    }
		}
	    }
	}
	if (logger.isDebugEnabled())
	    logger.debug("Node: goban is " + goban);
	logger.info("doMarkup: leave");
    }

    public void setGoban(Goban newGoban)
    {
	setGoban(newGoban, true);
    }

    public void setGoban(Goban newGoban, boolean doMarkup)
    {
        Goban oldGoban = goban;
        goban = newGoban;

        logger.info("Node " + this + ": Setting goban to " + newGoban);

        if (oldGoban != null)
            oldGoban.removeGobanListener(this);
	if (doMarkup && newGoban != null)
	    doMarkup();
        firePropertyChange("goban", oldGoban, newGoban);
	if (newGoban != null)
	    newGoban.addGobanListener(this);
    }

    public void add(Property p)
    {
	super.add(p);
	if (p instanceof Property.Inheritable)
	{
	    logger.info("inherited: " + p);
	    inheritedProperties.put(p.getKey(), p);
	    setInheritedProperties(inheritedProperties);
	}
    }

    public boolean add(Node o)
    {
        Node n = (Node)o;
        n.setParent(this);
        return children.add(n);
    }

    public void setParent(Node n)
    {
	assert n != this;
	if (logger.isDebugEnabled())
	    logger.debug("Node " + this + ": setting parent to " + n);
        parent = n;
	if (n != null && n.inheritedProperties != null)
	    setInheritedProperties(n.inheritedProperties);
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

    public Property get(Object key)
    {
	Property p;
	p = super.get(key);
	if (p == null && inheritedProperties != null)
	    p = inheritedProperties.get(key);
	return p;
    }

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

    public Point getPoint(Property.Key k)
    {
        return super.getPoint(k);
    }

    public String getGameName()
    {
        assert getRoot() != null;

        if (this != getRoot())
            return getRoot().getGameName();
        else if (contains(Property.GAME_NAME))
        {
            return getValue(Property.GAME_NAME).toString();
        }
        else if (contains(Property.PLAYER_BLACK) && contains(Property.PLAYER_WHITE))
        {
            Value black = getValue(Property.PLAYER_BLACK);
            Value white = getValue(Property.PLAYER_WHITE);
            assert black != null;
            assert white != null;
            return black.toString() + " - " + white.toString();
        }
        else
            return "";
    }

    public String getName()
    {
        if (contains(Property.NAME))
        {
            return getValue(Property.NAME).toString();
        }
        else if (contains(Property.BLACK))
            return "Schwarz " + getMoveNo();
        else if (contains(Property.WHITE))
            return "Weiﬂ " + getMoveNo();
        else if (isBoardSetup())
            return "position setup";
        else
            return "<Node>";
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer(16).append("Node ").append(getId());
        if (contains(Property.NAME))
            buffer.append(" ").append(getValue(Property.NAME).toString());
	
        return buffer.toString();
    }

    public int getMoveNo()
    {
        Node n = this;
        while (n != null && n.getParent() != null && !n.isMove())
        {
	    if (logger.isDebugEnabled())
		logger.debug("getMoveNo: not a move, get parent node");
            n = (Node)n.getParent();
        }
        return n.moveNo;
    }

    /** Play a move at point p.
     *  @throws UnsupportedOperationException if the is already a move or setup property at this node
     *  @param Point p the point where to add a stone
     */
    public void move(Point p) throws UnsupportedOperationException
    {
	if (isMove() || isBoardSetup() || this instanceof RootNode)
	    throw new UnsupportedOperationException();
	if (contains(Property.MOVE_NO)) {
	    Value.Number no = (Value.Number) ((Property) get(Property.MOVE_NO)).getValue();
	    setMoveNo(no.intValue());
	}
	else if (parent != null)
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

    /** Add a stone of color c at point p.
     *  This method ensures that the AW, AB and AE properties of this node are kept in a consistent state, 
     *  i.e. that each point occurs at most in one of the pointlists of these properties.
     *  @throws UnsupportedOperationException if the node is a move node
     *  @param Point p the point where to add a stone
     *  @param BoardType c the color of the stone
     */
    public void addStone(Point p, BoardType c) throws UnsupportedOperationException
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
	    else
	    {
		try {
		    model = parent.getGoban().clone();
		}
		catch (CloneNotSupportedException ex)
		{
		    throw new RuntimeException("This exception should not occur!", ex);
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
		assert(false);
	    
	    Value.PointList pl;
	    if (!contains(key)) {
		pl = (Value.PointList) AbstractValue.createPointList();
		put(Property.createProperty(key, pl));
	    }
	    else pl = getPointList(key);
	    pl.add(p);
	}
	setGoban(model);
    }

    public boolean equals(Object o)
    {
        if (o instanceof Node)
            return id == ((Node)o).id;
        else
            return false;
    }

    public int hashCode()
    {
	return id;
    }

    public boolean isMainVariation()
    {
        Node n = this;
        while (n.parent != null && !(n instanceof RootNode) )
        {
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
	while (n!=null && !n.isBeginOfVariation())
	    n = (Node) n.getParent();
	return n;
    }

    public Iterator iterator()
    {
        return children.iterator();
    }

    public void write(PrintWriter out)
    {
        super.write(out);
        if (getChildCount() > 1)
        {
            Iterator it = getChildren().iterator();
            while (it.hasNext())
            {
                Node node = (Node)it.next();
                out.println("(");
                node.write(out);
                out.println(")");
            }
        }
        else if (getChildCount() == 1)
        {
            Node node = (Node)getChildAt(0);
            node.write(out);
        }
    }

    public void setDefaultRootProperties()
    {
	setFileFormat(4);
	setGame(1);
	//setProperty(Property.APPLICATION, "GoDiagram:" + GoDiagram.getVersion());
	/*
	Charset defaultCharset = Charset.forName(new OutputStreamWriter(System.out).getEncoding());
	if (defaultCharset == null)
	    defaultCharset = Charset.forName("UTF-8");
	setProperty(Property.CHARACTER_SET, defaultCharset.name());
	*/
    }

    public void setProperty(Property.Key key, Object newValue)
    {
	assert newValue != null;
	if (!contains(key))
	    put(Property.createProperty(key, newValue));
	else {
	    Object oldValue = ((Property) get(key)).getValue();
	    if (!newValue.equals(oldValue))
		logger.error("Value already set! old: " + oldValue + " new: " + newValue);
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
     * @param moveNo The move number to set.
     */
    public void setMoveNo(int moveNo)
    {
	if (logger.isDebugEnabled())
	    logger.debug("Node " + this + ": Setting moveNo to " + moveNo);
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
	SortedMap<Goban, Node> childMap = new TreeMap<Goban, Node>(new BoardComparator());
	Iterator it;
	it = getChildren().iterator();
	while (it.hasNext()) {
	    Node c = (Node) it.next();
	    childMap.put(c.getGoban(), c);
	}
	it = n.getChildren().iterator();
	while (it.hasNext()) {
	    Node theirChild = (Node) it.next();
	    Node myChild = (Node) childMap.get(theirChild.getGoban());
	    if (myChild == null)
		add(theirChild);
	    else
		myChild.join(theirChild);
	}
    }

    void setInheritedProperties(PropertyList p)
    {
        Map<Property.Key, Property> inheritable = null;

        Iterator<Property.Key> it = keySet().iterator();
        while (it.hasNext())
        {
            Property.Key k = it.next();
            if (get(k) instanceof Property.Inheritable)
            {
		logger.info("inherited: " + get(k));
                if (inheritable == null) inheritable = new TreeMap<Property.Key, Property>();
                inheritable.put(k, get(k));
            }
        }
        it = p.keySet().iterator();
        while (it.hasNext())
        {
            Property.Key k = it.next();
            if (get(k) instanceof Property.Inheritable)
            {
		logger.info("inherited: " + get(k));
                if (inheritable == null) inheritable = new TreeMap<Property.Key, Property>();
                inheritable.put(k, get(k));
            }
        }

        if (inheritable != null)
        {
            inheritedProperties.putAll(inheritable);
        }

	for (Node n : children)
	{
            n.setInheritedProperties(inheritedProperties);
        }
    }
}
