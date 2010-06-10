/*
 *
 * $Id: NeighborhoodEnumeration.java 15 2003-03-15 23:25:52Z cgawron $
 *
 * (c) 2010 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go;

/**
 * Insert the type's description here. Creation date: (03/25/00 18:58:27)
 * @author: Administrator
 */
import de.cgawron.go.Point;
import java.util.Enumeration;

public class NeighborhoodEnumeration implements Enumeration
{
    protected int direction;
    protected int size;
    protected Point point;
    private Point nextPoint;

    /** NeighborhoodEnumeration constructor comment. */
    public NeighborhoodEnumeration(Goban goban, Point p)
    {
        super();
        point = p;
        size = (int) goban.getBoardSize();
        direction = 0;
        calcNext();
    }

    /** Insert the method's description here. Creation date: (03/25/00 19:05:28) */
    private void calcNext()
    {
        nextPoint = null;
        while (nextPoint == null && direction < 4)
        {
            switch (direction)
            {
                case 0:
                    if (point.getX() + 1 < size)
                        nextPoint = new Point((short)(point.getX() + 1), point.getY());
                    break;
                case 1:
                    if (point.getY() + 1 < size)
                        nextPoint = new Point(point.getX(), (short)(point.getY() + 1));
                    break;
                case 2:
                    if (point.getX() > 0)
                        nextPoint = new Point((short)(point.getX() - 1), point.getY());
                    break;
                case 3:
                    if (point.getY() > 0)
                        nextPoint = new Point(point.getX(), (short)(point.getY() - 1));
                    break;
            }
            direction++;
        }
    }

    /** hasMoreElements method comment. */
    public boolean hasMoreElements()
    {
        return nextPoint != null;
    }

    /** nextElement method comment. */
    public Object nextElement()
    {
        Point p = nextPoint;
        calcNext();
        return p;
    }
}
