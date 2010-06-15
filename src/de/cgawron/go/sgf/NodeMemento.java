/*
 *
 * $Id: NodeMemento.java 342 2005-10-16 12:09:00Z cgawron $
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

import de.cgawron.util.Memento;

import java.util.*;

import org.apache.log4j.Logger;

class NodeMemento implements Memento
{
    private static Logger logger = Logger.getLogger(NodeMemento.class);
    private Map<Property.Key, Property> properties;
    private Node parent;
    private List<Node> children;
    
    public NodeMemento(Node node)
    {
	if (node != null)
	{
	    this.parent = (Node) node.getParent();
	    this.children = null;
	    if (node.getChildren() != null)
		this.children = new LinkedList<Node>(node.getChildren());
	    properties = new TreeMap<Property.Key, Property>();
	    Iterator it = node.keySet().iterator();
	    while (it.hasNext()) {
		Property.Key key = (Property.Key) it.next();
		Property prop = (Property) node.get(key);
		properties.put(key, (Property) prop.clone());
	    }
	    logger.info("NodeMemento: Setting properties to " + properties);
	}
    }
    
    public Map<Property.Key, Property> getProperties()
    {
	return Collections.unmodifiableMap(properties);
    }

    public Node getParent()
    {
	return parent;
    }

    public List<Node> getChildren()
    {
	return children;
    }
    
    public String toString()
    {
	return "NodeMemento: properies=" + properties.toString() + " " + super.toString() + 
	       ", children=" + children + ", parent=" + parent;
    }
}
