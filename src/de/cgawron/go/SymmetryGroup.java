/*
 *
 * $Id$
 *
 * (c) 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go;

import java.util.logging.Logger;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.MutablePoint;


/**
 * This class describes the symmetry group of a position on a goban.
 * For example, the empty goban is invariant under every {@link Symmetry}, 
 * while the symmetry group of a goban with a single stone
 * on one of the hoshi contains only the identity and the reflection
 * along the diagonal through the hoshi.
 */
public class SymmetryGroup
{
    private static Logger logger = Logger.getLogger(SymmetryGroup.class.getName());
    private int symmetries;

    private SymmetryGroup(int symmetries)
    {
	this.symmetries = symmetries;
    }

    public final static SymmetryGroup allSymmetries = new SymmetryGroup(0xffff);

    public SymmetryGroup(Goban model)
    {
	symmetries = 1;
	//logger.info("SymmetryGroup for \n" + model);

	int h = ((AbstractGoban) model)._hash(new Symmetry(0));
	int i;

	for (i=0; i<8; i++) {
	    Symmetry s = new Symmetry(i);
	    int _h = ((AbstractGoban) model)._hash(s);
	    //logger.info("SymmetryGroup: " + s + ": " + h + ", " + _h);
	    if (_h == h)
		symmetries |= (1 << i);
	}
    }

    public class Iterator extends Symmetry.Iterator
    {
	private int i=0;
	
	public boolean hasNext()
	{
	    return i < 8;
	}

	public Object next()
	{
	    Symmetry s = new Symmetry(i);
	    for (i++; i<16 && (symmetries & (1<<i)) == 0; i++);
	    logger.fine("SymmetryGroup.Iterator.next(): returning " + s);

	    return s;
	}

	public void remove()
	{
	    throw new UnsupportedOperationException("remove not supported");
	}
    }

    public Iterator iterator()
    {
	return new Iterator();
    }

}

