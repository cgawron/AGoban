/*
 *
 * $Id: GobanModelEvent.java 97 2004-08-21 18:35:50Z  $
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
 * Insert the type's description here. Creation date: (03/23/00 23:13:28)
 * @author: Administrator
 */

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import java.util.EventObject;
import java.util.Vector;

public class GobanEvent extends EventObject {
	private static final long serialVersionUID = -3305394382897241209L;

	protected Vector<Point> points;
	protected BoardType color;

	/** GobanEvent constructor comment. */
	public GobanEvent(Goban source) {
		super(source);
		points = new Vector<Point>();
	}

	/** GobanEvent constructor comment. */
	public GobanEvent(Goban source, Point point) {
		super(source);
		points = new Vector<Point>();
		addPoint(point);
	}

	/** GobanEvent constructor comment. */
	public GobanEvent(Goban source, Vector<Point> points) {
		super(source);
		this.points = points;
	}

	/**
	 * Insert the method's description here. Creation date: (03/23/00 23:24:00)
	 * 
	 * @param point
	 *            goban.Point
	 */
	public void addPoint(Point point) {
		points.addElement(point);
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 16:14:16)
	 * 
	 * @return goban.BoardType
	 */
	public BoardType getColor() {
		return color;
	}

	/**
	 * Insert the method's description here. Creation date: (04/12/00 21:23:04)
	 * 
	 * @return gawron.go.goban.Goban
	 */
	public Goban getModel() {
		return (Goban) source;
	}

	/**
	 * Insert the method's description here. Creation date: (03/23/00 23:22:53)
	 * 
	 * @return java.util.Vector
	 */
	public Vector<Point> getPoints() {
		return points;
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 16:14:16)
	 * 
	 * @param newColor
	 *            goban.BoardType
	 */
	public void setColor(BoardType newColor) {
		color = newColor;
	}

	/**
	 * Insert the method's description here. Creation date: (03/23/00 23:22:53)
	 * 
	 * @param newPoints
	 *            java.util.Vector
	 */
	/*
	 * public void setPoints(Vector<Point> newPoints) { points = newPoints; }
	 */

	/** GobanEvent constructor comment. */
	public GobanEvent(Goban source, int x, int y, BoardType c) {
		super(source);
		points = new Vector<Point>();
		addPoint(new Point(x, y));
		color = c;
	}
}
