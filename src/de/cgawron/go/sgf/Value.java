/*
 *
 * $Id$
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

import de.cgawron.go.Symmetry;
import java.io.PrintWriter;

/**
 * A value of a {@link Property}
 */
public interface Value extends Cloneable
{
    void write(PrintWriter out);
    
    String getString();

    public interface Transformable
    {
	void transform(Symmetry symmetry);
    }

    public interface Void extends Value 
    {
    }

    public interface Point extends Value, Transformable 
    {
	de.cgawron.go.Point getPoint();
    }

    public interface PointList extends Value, Transformable, java.util.Collection<de.cgawron.go.Point> 
    {
	short getMinX();
	short getMinY();
	short getMaxX();
	short getMaxY();
    }

    public interface Label extends Value, Transformable 
    {
	de.cgawron.go.Point getPoint();
    }

    public interface Text extends Value {}

    public interface Result extends Value {}

    public interface Number extends Value 
    {
	int intValue();
    }

    public interface ValueList extends Value, java.util.List<Value> 
    {
    }

    public Value clone();
}
