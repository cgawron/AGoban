/*
 *
 * $Id: MutablePoint.java 38 2003-09-12 19:22:24Z cgawron $
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
 * A point on a Goban. This class adds methods to set the x and y components to
 * the immutable class Point.
 * 
 * @author Christian Gawron
 */
public class MutablePoint extends Point {
	/**
	 * MutablePoint constructor comment.
	 */
	public MutablePoint() {
		super();
	}

	/**
	 * MutablePoint constructor comment.
	 * 
	 * @param p
	 *            - Point from which to initialize the coordinates.
	 */
	public MutablePoint(Point p) {
		super(p);
	}

	/**
	 * Point constructor comment.
	 * 
	 * @param x
	 *            - the x coordinate
	 * @param y
	 *            - the y coordinate
	 */
	public MutablePoint(int x, int y) {
		super(x, y);
	}

	/**
	 * Set the x coordinate.
	 * 
	 * @param x
	 *            - the new x coordinate
	 * @return the modified MutablePoint
	 */
	public final MutablePoint setX(int x) {
		this.x = (short) x;
		return this;
	}

	/**
	 * Set the y coordinate.
	 * 
	 * @param y
	 *            - the new y coordinate
	 * @return the modified MutablePoint
	 */
	public final MutablePoint setY(int y) {
		this.y = (short) y;
		return this;
	}

	/**
	 * Copy the fields from a Point.
	 * 
	 * @param p
	 *            - a Point from which the coordinates are copied
	 * @return the modified MutablePoint
	 */
	public MutablePoint set(Point p) {
		this.x = p.x;
		this.y = p.y;
		return this;
	}
}
