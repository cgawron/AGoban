/*
 *
 * $Id: CollectionRoot.java 369 2006-04-14 17:04:02Z cgawron $
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

import java.util.Iterator;
import java.io.PrintWriter;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * An instance of this class represents a node in a sgf game tree.
 * @author Christian Gawron
 */
public class CollectionRoot extends RootNode
{
    public CollectionRoot(GameTree gameTree)
    {
        super(gameTree);
    }

    public void write(PrintWriter out)
    {
	Iterator it = getChildren().iterator();
	while (it.hasNext())
	{
	    Node node = (Node)it.next();
	    node.write(out);
	}
    }

}
