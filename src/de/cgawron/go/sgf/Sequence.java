/*
 *
 * $Id: Sequence.java 92 2004-08-07 12:32:15Z  $
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

import java.util.Collection;
import java.util.Iterator;

public class Sequence extends Node
{
    Node first = null;
    Node last = null;
    int len = 0;

    public Sequence(Node n)
    {
        super(n);
        first = this;
        last = this;
        len = 1;
    }

    public boolean addAll(Collection c)
    {
        Iterator i = c.iterator();
        while (i.hasNext())
        {
            last.add((Node)i.next());
        }
        return true;
    }

    public boolean append(Node o)
    {
	boolean b = false;
	if (last == this)
	    b = super.add(o);
	else
	    b = last.add(o);
	last = (Node)o;
	len++;
	return b;
    }

    public int size()
    {
        return len;
    }

    public Node getFirst()
    {
	return first;
    }

    public String toString()
    {
        String s = "Sequence: length " + size() + ": ";
        s += super.toString();
        return s;
    }
}
