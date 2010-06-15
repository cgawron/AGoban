/*
 *
 * $Id: NodeMemento.java 15 2003-03-15 23:25:52Z cgawron $
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

class GameTreeMemento implements Memento
{
    private static Logger logger = Logger.getLogger(NodeMemento.class.getName());
    private Map<Node, Memento> nodeMementos = new HashMap<Node, Memento>(); 
    
    public GameTreeMemento(GameTree gameTree)
    {
	Iterator it = new TreeIterator(gameTree);
	while (it.hasNext())
	{
	    Node n = (Node) it.next();
	    nodeMementos.put(n, n.createMemento());
	}
    }

    void setMemento(Node n)
    {
	logger.info("GameTreeMement: setMemento: " + n);
	Memento m = (Memento) nodeMementos.get(n);
	if (m != null)
	    n.setMemento(m);
	else
	    logger.error("No Memento stored for Node " + n);
	Iterator it = n.getChildren().iterator();
	while (it.hasNext())
	{
	    setMemento((Node) it.next());
	}
    }
    
    public String toString()
    {
	return "[GameTreeMemento nodeMementos=" + nodeMementos + "]";
    }
}
