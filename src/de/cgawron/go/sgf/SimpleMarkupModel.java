/*
 *
 * $Id: SimpleMarkupModel.java 369 2006-04-14 17:04:02Z cgawron $
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

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.GobanEvent;
import de.cgawron.go.GobanListener;
import de.cgawron.go.Point;
import de.cgawron.go.SimpleGoban;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * A simple implementation of the MarkupModel interface based on <code>SimpleGoban</code>
 * @version $Id: SimpleMarkupModel.java 369 2006-04-14 17:04:02Z cgawron $
 */
public class SimpleMarkupModel extends SimpleGoban implements MarkupModel, PropertyChangeListener
{
    private static Logger logger = Logger.getLogger(SimpleMarkupModel.class.getName());
    protected MarkupModel.Markup[][] markup;
    private Region region;
    private SortedSet<Conflict> conflicts = new TreeSet<Conflict>();
    private Map toolTipMap = new TreeMap();
    private int moveNo = 1;


    /** SimpleMarkupModel constructor comment. */
    public SimpleMarkupModel()
    {
        super();
    }

    /** SimpleGoban constructor comment. */
    public SimpleMarkupModel(short size)
    {
        super(size);
    }

    /**
     * Insert the method's description here. Creation date: (04/09/00 18:29:33)
     * @param m gawron.go.goban.Goban
     */
    public SimpleMarkupModel(Goban m)
    {
        super(m);
        if (m instanceof MarkupModel)
        {
            MarkupModel mm = (MarkupModel) m;
            setRegion(mm.getRegion());
            conflicts = (SortedSet<Conflict>)((TreeSet<Conflict>)mm.getConflicts()).clone();
        }
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        logger.info("SimpleMarkupModel.propertyChange: " + event);
        if (event.getSource() instanceof Region)
        {
            firePropertyChange(null, null, null);
            fireRegionChanged();
            fireModelChanged();
        }
    }

    /**
     * Insert the method's description here. Creation date: (04/09/00 18:33:15)
     * @param m gawron.go.goban.Goban
     */
    @Override
    public void copy(Goban m)
    {
        super.copy(m);
	if (m instanceof SimpleMarkupModel)
	{
	    SimpleMarkupModel smp = (SimpleMarkupModel) m;
	    for (int i=0; i<size; i++) 
		System.arraycopy(smp.markup[i], 0, markup[i], 0, size); 

	    try 
	    {
		if (smp.region != null)
		    this.region = (Region) smp.region.clone();
	    }
	    catch (CloneNotSupportedException ex)
	    {
		throw new RuntimeException(ex);
	    }
	}
        else if (m instanceof MarkupModel)
        {
            MarkupModel mm = (MarkupModel) m;
            short i;
            short j;
            for (i = 0; i < size; i++)
                for (j = 0; j < size; j++)
                    setMarkup(i, j, mm.getMarkup(i, j));
            setRegion(mm.getRegion());
        }
	else
	{
            short i;
            short j;
	    BoardType color;
            for (i = 0; i < size; i++)
                for (j = 0; j < size; j++)
		    if ((color = m.getStone(i, j)) != BoardType.EMPTY)
			setMarkup(i, j, new Stone(color));
	}
	fireModelChanged();
    }

    public void resetMarkup()
    {
	logger.info("reset markup: " + this);
        short i;
        short j;

        for (i = 0; i < size; i++)
            for (j = 0; j < size; j++)
                if (getStone(i, j) != BoardType.EMPTY)
		{
		    if (logger.isDebugEnabled())
			logger.debug("reset markup: [" + i + ", " + j + "]: " + markup[i][j]);
                    markup[i][j] = new Stone(getStone(i, j));
		}
                else
                    markup[i][j] = null;
        setRegion(null);
        conflicts.clear();
        fireModelChanged();
    }

    /** getStone method comment. */
    public MarkupModel.Markup getMarkup(Point p)
    {
        return markup[p.getX()][p.getY()];
    }

    /** getStone method comment. */
    public MarkupModel.Markup getMarkup(short x, short y)
    {
        return markup[x][y];
    }

    /** setBoardSize method comment. */
    @Override
    public void setBoardSize(int s)
    {
	if (size != s)
	{
	    super.setBoardSize(s);
	    short i, j;
	    size = s;
	    markup = new MarkupModel.Markup[size][size];
	}
    }

    /** setBoardSize method comment. */
    @Override
    protected void setBoardSizeNoInit(int s)
    {
	if (size != s)
	{
	    super.setBoardSizeNoInit(s);
	    size = s;
	    markup = new MarkupModel.Markup[size][size];
	}
    }

    /** setMarkup method comment. */
    public void setMarkup(Point p, MarkupModel.Markup m)
    {
        setMarkup(p.getX(), p.getY(), m);
    }

    /** setMarkup method comment. */
    public void setMarkup(short x, short y, MarkupModel.Markup m)
    {
        if (x < 0 || y < 0 || x >= getBoardSize() || y >= getBoardSize())
            return;

        if (markup[x][y] == null || m == null 
	    /* ohne den zweiten Teil funktionieren Label auf mit AB[] bzw. AW[] hinzugefügten Steinen nicht! */
	    || (markup[x][y] instanceof Stone && !(m instanceof Stone || m instanceof Move))
	    || (markup[x][y] instanceof Stone && m instanceof Move && ((Stone)markup[x][y]).getColor().equals(((Move) m).getColor()))
	    ) 
	{
            markup[x][y] = m;
	    if (m == null)
		assert getStone(x, y) == BoardType.EMPTY : "Setting null Markup on non-empty field"; 
	    fireModelChanged();
	}
	else if (!m.equals(markup[x][y])) 
	{
	    if (markup[x][y] instanceof MarkupModel.Stone && !(markup[x][y] instanceof MarkupModel.ConflictMark))
		markup[x][y] = getConflictLabel((MarkupModel.Stone) markup[x][y]);
	    if (logger.isDebugEnabled())
		logger.debug("Markup conflict at (" + x + ", " + y + "): " + m + ", " + markup[x][y]);
	    conflicts.add(new MarkupModel.Conflict(markup[x][y], m));
	    if (logger.isDebugEnabled())
		logger.debug("Markup conflicts: " + conflicts);
	}
    }
    
    MarkupModel.Markup getConflictLabel(MarkupModel.Stone s)
    {
	Iterator<Conflict> it = conflicts.iterator();

	short n=0;
	while (it.hasNext())
	{
	    if (it.next().first instanceof ConflictMark)
		n++;
	}
	return new MarkupModel.ConflictMark(s, (char) ('a' + n));
    }
    
    /** putStone method comment. */
    public void putStone(short x, short y, BoardType color)
    {
	logger.debug("putStone: " + x + ", " + y + ", " + color);
        super.putStone(x, y, color);
	setMarkup(x, y, new Stone(color));
    }

    protected void setStone(short x, short y, BoardType color)
    {
	logger.debug("setStone: " + x + ", " + y + ", " + color);
        super.setStone(x, y, color);
    }

    /** move method comment. */
    public void move(short x, short y, BoardType color)
    {
        super.move(x, y, color);
	setMarkup(x, y, new Move(color, moveNo++));
    }

    /** move method comment. */
    public void move(short x, short y, BoardType color, int moveNo)
    {
	this.moveNo = moveNo;
        super.move(x, y, color, moveNo);
	setMarkup(x, y, new Move(color, moveNo++));
    }

    public Region getRegion()
    {
        return region;
    }

    public void setRegion(Region newRegion)
    {
        Region oldRegion = region;
        logger.debug("Setting region to " + newRegion);
        if (oldRegion != null)
            oldRegion.removePropertyChangeListener(this);
        region = newRegion;
        if (oldRegion != newRegion)
        {
            firePropertyChange("region", oldRegion, newRegion);
            fireRegionChanged();
            fireModelChanged();
        }
        if (newRegion != null)
            newRegion.addPropertyChangeListener(this);
    }

    public SortedSet<Conflict> getConflicts()
    {
        logger.debug("Conflicts: " + conflicts);
        return conflicts;
    }

    /** addGobanListener method comment. */
    public void addGobanListener(GobanListener l)
    {
	listeners.add(l);
    }

    /** addGobanListener method comment. */
    public void removeGobanListener(GobanListener l)
    {
	listeners.remove(l);
    }


    protected void fireRegionChanged()
    {
	if (logger.isDebugEnabled())
	    logger.debug("SimpleMarkupModel.fireRegionChanged");

	GobanEvent e = new GobanEvent(this);
        // Guaranteed to return a non-null array
        for (GobanListener listener : listeners)
        {
	    //listener.regionChanged(e);
        }
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
		if (markup[i][j] == null)
		    s.append('.');
		else if (markup[i][j] instanceof Stone)
		{
		    if (p == BoardType.WHITE)
			s.append('O');
		    else if (p == BoardType.BLACK)
			s.append('X');
		    else
			s.append('!');
		}
		else
		{
		    if (p == BoardType.WHITE)
			s.append('o');
		    else if (p == BoardType.BLACK)
			s.append('x');
		    else
			s.append('?');
		}
            }
            s.append('\n');
        }
        return s.toString();
    }

    public String getToolTipText(Point p)
    {
	return (String) toolTipMap.get(p);
    }

    public void setToolTipText(Point p, String s)
    {
	toolTipMap.put(p, s);
    }

    public Goban clone() throws CloneNotSupportedException
    {
	Goban model = new SimpleMarkupModel(this);
	return model;
    }

}
