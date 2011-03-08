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

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.MutablePoint;

public class Symmetry
{
	/**
	 * The symmetry is stored as follows: bit 0: exchange x and y bit 1:
	 * reflection (x-axis) bit 2: reflection (y-axis) bit 3: color exchange
	 */
	private int symmetry;

	private static Symmetry _inverse[] = {
			new Symmetry(0), // I
			new Symmetry(1), // X
			new Symmetry(2), // Y
			new Symmetry(3), // XY
			new Symmetry(4), // D
			new Symmetry(6), // DX
			new Symmetry(5), // DY
			new Symmetry(7), // DXY
			new Symmetry(8), new Symmetry(9), new Symmetry(10),
			new Symmetry(11), new Symmetry(12), new Symmetry(14),
			new Symmetry(13), new Symmetry(15) };

	/*
	 * private static Symmetry cache[] = new Symmetry[16]; static { int i; for
	 * (i=0; i<16; i++) cache[i] = new Symmetry(i); }
	 */

	public static class Iterator implements java.util.Iterator
	{
		private int i = -1;

		public boolean hasNext()
		{
			return i < 7;
		}

		public Object next()
		{
			return new Symmetry(++i);
		}

		public void remove()
		{
			throw new UnsupportedOperationException("remove not supported");
		}
	}

	public static class SpatialIterator implements java.util.Iterator
	{
		private int i = -1;

		public boolean hasNext()
		{
			return i < 7;
		}

		public Object next()
		{
			return new Symmetry(++i);
		}

		public void remove()
		{
			throw new UnsupportedOperationException("remove not supported");
		}
	}

	public Symmetry()
	{
		this(0);
	}

	public Symmetry(Symmetry s)
	{
		this(s.symmetry);
	}

	Symmetry(int symmetry)
	{
		this.symmetry = symmetry;
	}

	public int toInt()
	{
		return symmetry;
	}

	final public void transform(MutablePoint p, int boardSize)
	{
		int x = -1;
		int y = -1;

		if ((symmetry & 4) == 0) {
			x = p.getX();
			y = p.getY();
		} else {
			y = p.getX();
			x = p.getY();
		}

		if ((symmetry & 1) != 0)
			x = (boardSize - x - 1);

		if ((symmetry & 2) != 0)
			y = (boardSize - y - 1);

		p.setX(x);
		p.setY(y);
	}

	final public Point transform(Point p, int boardSize)
	{
		MutablePoint mp = new MutablePoint(p);
		transform(mp, boardSize);
		return mp;
	}

	final public Point transform(int x, int y, int boardSize)
	{
		MutablePoint mp = new MutablePoint(x, y);
		transform(mp, boardSize);
		return mp;
	}

	final public BoardType transform(BoardType c)
	{
		if (c == BoardType.EMPTY || (symmetry & 8) == 0)
			return c;
		else if (c == BoardType.WHITE)
			return BoardType.BLACK;
		else
			return BoardType.WHITE;
	}

	public Symmetry inverse()
	{
		return new Symmetry(_inverse[symmetry]);
	}

	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		if ((symmetry & 1) != 0)
			buffer.append('X');
		if ((symmetry & 2) != 0)
			buffer.append('Y');
		if ((symmetry & 4) != 0)
			buffer.append('D');
		if ((symmetry & 8) != 0)
			buffer.append('C');
		if (buffer.length() == 0)
			return "I";
		else
			return buffer.toString();
	}

	public int zobristHash(Goban g)
	{
		return zobristHash(g, SymmetryGroup.allSymmetries);
	}

	public int zobristHash(Goban g, SymmetryGroup group)
	{
		Symmetry.Iterator it = group.iterator();
		int h = Integer.MIN_VALUE;

		while (it.hasNext()) {
			Symmetry s = (Symmetry) it.next();
			int _h = ((AbstractGoban) g)._hash(s);
			if (_h > h) {
				h = _h;
				this.symmetry = s.symmetry;
			}
		}

		return h;
	}

}
