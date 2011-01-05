/*
 *
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

import java.io.Serializable;
import java.util.Collection;
import java.util.EventListener;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple implementation of a GobanModel, using a two-dimensional array to represent the board.
 * Creation date: (03/23/00 21:36:30)
 */
public class SimpleGoban extends AbstractGoban implements Serializable
{
    protected int size = 0;
    protected BoardType[][] boardRep;
    private int[][] tmpBoard;
    private int[] _hash;

    private int visited;
    int numStones = 0;
    private Point lastMove;
    private int whiteCaptured;
    private int blackCaptured;

    protected Collection<GobanListener> listeners = new java.util.ArrayList<GobanListener>();

    private static Logger logger = Logger.getLogger(SimpleGoban.class.getName());

    /** Create a SimpleGoban with default board size of 19x19. */
    public SimpleGoban()
    {
        this(19);
    }

    /** Create a SimpleGoban with a given board size. */
    public SimpleGoban(int size)
    {
        super();
	numStones = 0;
        setBoardSize((int)size);
    }

    /**
     * Create a SimpleGoban which copies the board of another Goban
     * @param m gawron.go.goban.Goban
     */
    public SimpleGoban(Goban m)
    {
        super();
	numStones = 0;
        if (m != null)
            copy(m);
        else
            setBoardSize((int)19);
    }

    /** addGobanListener method comment. */
    public void addGobanListener(GobanListener l)
    {
        listeners.add(l);
    }

    /**
     * Insert the method's description here. Creation date: (04/09/00 18:33:15)
     * @param m gawron.go.goban.Goban
     */
    public void copy(Goban m)
    {
        int i;
        int j;
	if (m instanceof SimpleGoban) {
	    SimpleGoban sm = (SimpleGoban) m;
	    setBoardSizeNoInit(sm.size);

	    for (i=0; i<size; i++) 
		System.arraycopy(sm.boardRep[i], 0, boardRep[i], 0, size); 

	    System.arraycopy(sm._hash, 0, _hash, 0, 16);
	    whiteCaptured = sm.whiteCaptured;
	    blackCaptured = sm.blackCaptured;
	    numStones = sm.numStones;
	}
	else {
	    setBoardSize(m.getBoardSize());
	    for (i = 0; i < size; i++)
		for (j = 0; j < size; j++)
		    setStone(i, j, m.getStone(i, j));
	    
	    whiteCaptured = m.getWhiteCaptured();
	    blackCaptured = m.getBlackCaptured();
	}
	fireModelChanged();
    }

    public void clear()
    {
        int i;
        int j;
        for (i = 0; i < size; i++)
            for (j = 0; j < size; j++)
                setStone(i, j, BoardType.EMPTY);
	numStones = 0;
	fireModelChanged();
    }

    /**
     * Insert the method's description here. Creation date: (03/26/00 17:12:54)
     * @return int
     * @param p goban.Point
     */
    public int countLiberties(Point p)
    {
        tmpBoard[p.getX()][p.getY()] = visited;
        int liberties = (int)0;

        NeighborhoodEnumeration ne = new NeighborhoodEnumeration(this, p);
        while (ne.hasMoreElements())
        {
            Point q = (Point)ne.nextElement();
            if (tmpBoard[q.getX()][q.getY()] != visited)
            {
                tmpBoard[q.getX()][q.getY()] = visited;
                if (getStone(q) == BoardType.EMPTY)
                    liberties++;
                else if (getStone(q) == getStone(p))
                    liberties += countLiberties(q);
            }
        }
        return liberties;
    }

    /**
     * Insert the method's description here. Creation date: (03/25/00 16:07:59)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    protected void fireStonesRemoved(Vector<Point> removed)
    {
        // Guaranteed to return a non-null array
        GobanEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (GobanListener listener : listeners)
        {
	    // Lazily create the event:
	    if (e == null)
		e = new GobanEvent(this, removed);
	    listener.stonesRemoved(e);
        }
    }

    /**
     * Insert the method's description here. Creation date: (04/18/00 23:51:16)
     * @return int
     */
    public int getBlackCaptured()
    {
        return blackCaptured;
    }

    /** getStone method comment. */
    public BoardType getStone(Point p)
    {
        return boardRep[p.getX()][p.getY()];
    }

    /**
     * Insert the method's description here. Creation date: (04/18/00 23:50:17)
     * @return int
     */
    public int getWhiteCaptured()
    {
        return whiteCaptured;
    }

    /** move method comment. */
    public void move(Point p, BoardType color)
    {
        move(p.getX(), p.getY(), color);
    }

    public void move(Point p, BoardType color, int moveNo)
    {
        move(p.getX(), p.getY(), color, moveNo);
    }


    /**
     * Insert the method's description here. Creation date: (03/26/00 17:13:44)
     * @return int
     */
    public int removeChain(Point p, Vector<Point> removed)
    {
        removed.addElement(p);
        BoardType c = getStone(p);
        setStone(p, BoardType.EMPTY);
        int r = 1;
        NeighborhoodEnumeration ne = new NeighborhoodEnumeration(this, p);

        while (ne.hasMoreElements())
        {
            Point q = (Point)ne.nextElement();
            if (getStone(q) == c)
                r += removeChain(q, removed);
        }

        if (c == BoardType.BLACK)
            whiteCaptured += r;
        else
            blackCaptured += r;

        return r;
    }

    /** addGobanListener method comment. */
    public void removeGobanListener(GobanListener l)
    {
        listeners.remove(l);
    }

    /**
     * Insert the method's description here. Creation date: (04/18/00 23:51:16)
     * @param newBlackCaptured int
     */
    protected void setBlackCaptured(int newBlackCaptured)
    {
        blackCaptured = newBlackCaptured;
    }

    /**
     * Insert the method's description here. Creation date: (04/18/00 23:50:17)
     * @param newWhiteCaptured int
     */
    protected void setWhiteCaptured(int newWhiteCaptured)
    {
        whiteCaptured = newWhiteCaptured;
    }

    public String toString()
    {
        StringBuffer s = new StringBuffer(512);
        int i, j;
        BoardType p;
        for (i = 0; i < size; i++)
        {
            for (j = 0; j < size; j++)
            {
                p = boardRep[i] [j];
                if (p == BoardType.WHITE)
                    s.append('O');
                else if (p == BoardType.BLACK)
                    s.append('X');
                else
                    s.append('.');
            }
            s.append('\n');
        }
        return s.toString();
    }

    /**
     * Insert the method's description here. Creation date: (03/25/00 16:07:59)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    protected void fireStoneAdded(int x, int y, BoardType c)
    {
        GobanEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (GobanListener listener : listeners)
        {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("Notifying listener ...");
	    // Lazily create the event:
	    if (e == null)
		e = new GobanEvent(this, x, y, c);
	    listener.stoneAdded(e);
        }
    }

    /**
     * Insert the method's description here. Creation date: (03/25/00 16:07:59)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    protected void fireModelChanged()
    {
        GobanEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (GobanListener listener : listeners)
        {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("Notifying listener ...");
	    // Lazily create the event:
	    if (e == null)
		e = new GobanEvent(this);
	    listener.modelChanged(e);
        }
    }

    /** getBoardSize method comment. */
    final public int getBoardSize()
    {
        return size;
    }

    /** getStone method comment. */
    public final BoardType getStone(int x, int y)
    {
        return boardRep[x][y];
    }

    /** move method comment. */
    public void move(int x, int y, BoardType color, int moveNo)
    {
	move(x, y, color);
    }

    public void move(int x, int y, BoardType color)
    {
        if (x < 0 || y < 0 || x >= getBoardSize() || y >= getBoardSize())
            return;

        int captured = 0;
        BoardType enemy = color.opposite();
        if (getStone(x, y) != BoardType.EMPTY)
            return;

        setStone(x, y, color);

        Point p = new Point(x, y);
	lastMove = p;
        NeighborhoodEnumeration ne = new NeighborhoodEnumeration(this, p);
        Vector removed = new Vector();
        while (ne.hasMoreElements())
        {
            p = (Point)ne.nextElement();
            visited++;
            if (getStone(p) == enemy)
                if (countLiberties(p) == 0)
                    captured += removeChain(p, removed);
        }
        visited++;
        p = new Point(x, y);
        if (countLiberties(p) == 0)
        {
            removeChain(p, removed);
            // throw new IllegalMove();
        }
        fireStonesRemoved(removed);
        fireStoneAdded(x, y, color);
    }

    public Point getLastMove() {
	return lastMove;
    }

    /** putStone method comment. */
    public void putStone(int x, int y, BoardType color)
    {
        setStone(x, y, color);
        fireStoneAdded(x, y, color);
    }

    /** putStone method comment. */
    public void putStone(Point p, BoardType color)
    {
        putStone(p.getX(), p.getY(), color);
    }

    /** setBoardSize method comment. */
    public void setBoardSize(int s)
    {
        if (size != s)
        {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("setBoardSize: " + s);
            size = s;
            boardRep = new BoardType[size][size];
            tmpBoard = new int[size][size];
	    _hash = new int[16];

            int i, j;
            for (i=0; i<size; i++)
	    {
		java.util.Arrays.fill(boardRep[i], BoardType.EMPTY);
		java.util.Arrays.fill(tmpBoard[i], 0);
	    }
	    
	    for (i=0; i<16; i++)
		_hash[i] = 0;

	    numStones = 0;
        }
    }

    /** setBoardSize method comment. */
    protected void setBoardSizeNoInit(int s)
    {
        if (size != s)
        {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("setBoardSize: " + s);
            size = s;
            boardRep = new BoardType[size][size];
            tmpBoard = new int[size][size];
	    _hash = new int[16];

            int i;
            for (i=0; i<size; i++)
	    {
		java.util.Arrays.fill(tmpBoard[i], 0);
	    }
        }
    }

    /**
     * Insert the method's description here. Creation date: (03/25/00 18:52:08)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    protected void setStone(Point p, BoardType c)
    {
	setStone(p.getX(), p.getY(), c);
    }

    /**
     * Insert the method's description here. Creation date: (03/25/00 18:52:08)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    protected void setStone(int x, int y, BoardType c)
    {
	BoardType oc = boardRep[x][y];
	if (oc != c) {
	    boardRep[x][y] = c;

	    if (oc != BoardType.EMPTY) 
		numStones--;
	    if (c != BoardType.EMPTY) 
		numStones++;

	    Symmetry.Iterator it = new Symmetry.Iterator();
	    while (it.hasNext()) {
		Symmetry s = (Symmetry) it.next();
		int si = s.toInt();
		Point pt = s.transform(x, y, size);
		if (oc != BoardType.EMPTY) {
		    if (s.transform(oc) == BoardType.BLACK) 
			_hash[si] ^= zobrist[pt.getY()*size+pt.getX()];
		    else 
			_hash[si] ^= ~zobrist[pt.getY()*size+pt.getX()];
		}
		if (c != BoardType.EMPTY) {
		    if (s.transform(c) == BoardType.BLACK) 
			_hash[si] ^= zobrist[pt.getY()*size+pt.getX()];
		    else 
			_hash[si] ^= ~zobrist[pt.getY()*size+pt.getX()];
		}
	    }
	}
    }

    public int _hash(Symmetry s)
    {
	//int h = (_hash[s.toInt()] & 0x01ffffff) | ((numStones & 0xfe) << (32-7));
	//assert h == __hash(s) : "hashes differ: " + this + ": " + numStones + ": " + h + " " + __hash(s);
	//return h;
	return __hash(s);
    }

    public int __hash(Symmetry s)
    {
	Point.BoardIterator it = new Point.BoardIterator(size);
	int h = 0;
	int n = 0;
	
	while (it.hasNext()) {
	    Point p = (Point) it.next();
	    BoardType stone = getStone(p);
	    if (stone != BoardType.EMPTY) {
		n++;
		Point pt = s.transform(p, size);
		int z = zobrist[pt.getY()*size+pt.getX()];
		//logger.info("__hash: " + p + " " + pt + ": " + stone + " " + n + " " + z + " " + (-z));
		if (s.transform(stone) == BoardType.BLACK) 
		    h += z;
		else 
		    h -= z;
	    }
	}
	// if (sym & 8 != 0) h ^= 0xffffffff;
	h = (h & 0x01ffffff) | ((n & 0xfe) << (32-7));
	return h;
    }

    public int hashCode()
    {
	Symmetry.Iterator it = new Symmetry.Iterator();
	int h = 0;

	while (it.hasNext()) {
	    Symmetry s = (Symmetry) it.next();
	    int _h = _hash(s);
	    if (h == 0 || _h > h) {
		h = _h;
	    }
	}
	
	return h;
    }

    public int zobristHash()
    {
	return hashCode();
    }

    public boolean equals(Object o, Symmetry s)
    {
	if (o instanceof Goban) {
	    Goban goban = (Goban) o;
	    Point.BoardIterator it = new Point.BoardIterator(size);
	    
	    while (it.hasNext()) {
		Point p = (Point) it.next();
		Point pt = s.transform(p, size);
		BoardType stone = getStone(p);
		if (goban.getStone(p) != s.transform(getStone(pt)))
		    return false;
	    }
	    return true;
	}
	return false;
    }

    public boolean equals(Object o)
    {
	if (o == this)
	    return true;
	else if (o instanceof SimpleGoban) {
	    SimpleGoban goban = (SimpleGoban) o;
	    int i, j;
	    for (i=0; i<size; i++)
		for (j=0; j<size; j++)
		    if (boardRep[i][j] != goban.boardRep[i][j])
			return false;
	    return true;
	}
	else if (o instanceof Goban) {
	    Goban goban = (Goban) o;
	    /*
	    Point.BoardIterator it = new Point.BoardIterator(size);
	    
	    while (it.hasNext()) {
		Point p = (Point) it.next();
		if (getStone(p) != goban.getStone(p))
		    return false;
	    }
	    */
	    int i, j;
	    for (i=0; i<size; i++)
		for (j=0; j<size; j++)
		    if (getStone(i, j) != goban.getStone(i, j))
			return false;
	    return true;
	}
	return false;
    }

    public Goban transform(Symmetry s)
    {
	int size = getBoardSize();
	Goban m = newInstance();


	Point.BoardIterator it = new Point.BoardIterator(size);
	
	while (it.hasNext()) {
	    Point p = (Point) it.next();
	    BoardType stone = getStone(p);
	    if (stone != BoardType.EMPTY) {
		Point pt = s.transform(p, size);
		m.putStone(pt, s.transform(stone));
	    }
	}
	return m;
    }
    
    public Goban clone() throws CloneNotSupportedException
    {
	Goban model = new SimpleGoban();
	model.copy(this);
	return model;
    }

    public Goban newInstance()
    {
	return new SimpleGoban(getBoardSize());
    }
}
