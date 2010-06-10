/*
 *
 * $Id: Point.java 289 2005-08-15 09:44:31Z cgawron $
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

/**
 * A point on a Goban.
 * @author Christian Gawron
 */
public class Point implements Comparable<Point>
{
    /** The x coordinate of the point. */
    protected short x;

    /** The y coordinate of the point. */
    protected short y;

    /**
     * Default constructor. 
     * The constructed point is initialized with 0 as x and y coordinates. 
     */
    protected Point()
    {
	x = 0;
	y = 0;
    }

    /**
     * Create a Point from a textual representation in SGF notation.
     * @param s - the String representing the Point. 
     */
    public Point(String s) 
    {
	x = (short)(s.charAt(0) - 'a');
	y = (short)(s.charAt(1) - 'a');
    }

    /**
     * Copy a Point.
     * @param p - the Point to copy.
     */
    public Point(Point p) 
    {
	x = p.x;
	y = p.y;
    }

    /**
     * Construct a point by specifying the coordinates.
     * @param x - the x coordinate
     * @param y - the y coordinate
     */
    public Point(int x, int y) 
    {
	this.x = (short) x;
	this.y = (short) y;
    }

    /**
     * Construct a point by specifying the coordinates.
     * @param _x - the x coordinate
     * @param _y - the y coordinate
     */
    public Point(short _x, short _y) 
    {
	super();
	x = _x;
	y = _y;
    }

    /**
     * Compares this Point to another Object.
     * @see <code>Comparable</code>
     * @throws ClassCastException if the argument is not an instance of Point.
     * @param p - the <code>Point</code> to compare to
     * @return -1 if o is smaller (i.e. left or below) this point, 0 if o and this point are equal and +1 if o is bigger.
     */
    public int compareTo(Point p) throws ClassCastException
    {
        if (p.x > x) return -1;
	else if (p.x < x) return 1;
	else if (p.y > y) return -1;
	else if (p.y < y) return 1;
	else return 0;
    }

    /**
     * Compares this Point to another Object.  
     * @param o - object to compare to.
     * @return true if o represents the same point on a goban.
     */
    public boolean equals(Object o) 
    {
	if (!(o instanceof Point))
	    return false;
	else {
	    Point p = (Point) o;
	    return (x == p.x) && (y == p.y);
	}
    }


    /*
     * Insert the method's description here.
     * @return java.util.Enumeration repr
    public java.util.Enumeration neighbors() 
    {
	return null;
    }
    */


    /**
     * Get the SGF representation of this point.
     * @return a String containing the SGF representation of this point.
     */
    public java.lang.String sgfString() 
    {
	StringBuffer sgf = new StringBuffer(2);
	sgf.append((char) ('a' + x)).append((char) ('a' + y));
	return sgf.toString();
    }

    /**
     * Get the string representation of this point.
     * @return a string representation of this point.
     */
    public String toString() 
    {
	return "[" + x + ", " + y + "]";
    }


    /**
     * Get the x coodinate of this point.
     * @return the x coordinate.
     */
    public final short getX() {
	return x;
    }

    /**
     * Get the y coodinate of this point.
     * @return the y coordinate.
     */
    public final short getY() {
	return y;
    }

    /**
     * Append the String representation of this point to a StringBuffer.
     * @param b - the StringBuffer to which the String is appended.
     * @param p - the point to append.
     * @return the StringBuffer b.
     */
    public static StringBuffer append(StringBuffer b, Point p)
    {
	return b.append(p.x).append(' ').append(p.y);
    }

    /** 
     * An Iterator traversing all points on a goban.
     */
    public static class BoardIterator implements java.util.Iterator
    {
	private Point p = new Point();
	private int boardSize;

	/**
	 * Create a BoardIterator for a board of given size.
	 * @param boardSize - the size of the board.
	 */
	public BoardIterator(int boardSize)
	{
	    this.boardSize = boardSize;
	    p.x = -1;
	    p.y = 0;
	}
	
	public boolean hasNext()
	{
	    return (p.y < boardSize - 1) || (p.y == boardSize - 1 && p.x < boardSize - 1);
	}
	
	public Object next()
	{
	    p.x++;
	    if (p.x == boardSize) {
		p.x = 0;
		p.y++;
	    }
	    
	    return p;
	}

	public void remove()
	{
	    throw new UnsupportedOperationException("remove not supported");
	}

    }


}

