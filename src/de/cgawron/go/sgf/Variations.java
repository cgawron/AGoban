/*
 *
 * $Id: Variations.java 15 2003-03-15 23:25:52Z cgawron $
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
import java.util.LinkedList;

public class Variations extends LinkedList
{
    public Variations()
    {
        super();
    }

    public String toString()
    {
        String s = "\nVariations (" + size() + " ):\n";
        Iterator i = iterator();
        while (i.hasNext())
        {
            Object o = i.next();
            if (o != null)
                s += o.toString();
            else
                s += "<null>";
        }
        return s;
    }
}
