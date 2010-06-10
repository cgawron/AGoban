/*
 *
 * $Id: MarkupModel.java 369 2006-04-14 17:04:02Z cgawron $
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
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import java.util.logging.Logger;

/**
 * Represents markup on a goban - move numbers, letters, views.
 * @see GobanModel
 * @see Goban
 * @version $Id: MarkupModel.java 369 2006-04-14 17:04:02Z cgawron $
 */
public interface MarkupModel extends Goban
{

    /** A marker interface for all classes which represent a certain type of markups */
    public interface Markup extends Comparable<Markup>
    {
    }

    public abstract class AbstractMarkup implements Markup
    {
	public int compareTo(Markup m)
	{
	    return toString().compareTo(m.toString());
	}   
    }

    abstract static class IntegerMarkup extends AbstractMarkup
    {
	public abstract Integer toInteger();
 
	public int compareTo(Markup o)
	{
	    if (o instanceof IntegerMarkup)
		return toInteger().compareTo(((IntegerMarkup) o).toInteger());
	    else
		return super.compareTo(o);
	}
    }

    /** A markup for move numbers. */
    public static class Move extends IntegerMarkup
    {
        private int moveNo;
        private BoardType color;

        public Move(BoardType c, int moveNo)
        {
            this.moveNo = moveNo;
            this.color = c;
        }

        public int getMoveNo()
        {
            return moveNo;
        }

        public BoardType getColor()
        {
            return color;
        }

        public Integer toInteger()
        {
            return new Integer(moveNo);
        }

        public String toString()
        {
            return Integer.toString(moveNo);
        }

	public boolean equals(Object o)
        {
            if (o == this)
                return true;
            else if (o instanceof Move)
            {
                return color == ((Move) o).color && moveNo == ((Move) o).moveNo;
            }
            else
                return false;
        }
    }


    /**
     * A markup for stones which are not really present on the board. This is used for diagrams and figures when captured
     * stones are to be shown.
     */
    public static class Stone extends AbstractMarkup
    {
        private BoardType color;

        public Stone(BoardType c)
        {
            this.color = c;
        }

        public Stone(Stone s)
        {
            this.color = s.color;
        }

        public BoardType getColor()
        {
            return color;
        }

        public String toString()
        {
            return color.toString();
        }

	public boolean equals(Object o)
        {
            if (o == this)
                return true;
            else if (o instanceof Stone)
            {
                return color == ((Stone) o).color;
            }
            else
                return false;

        }

    }

    public static class ConflictMark extends Stone
    {
	private String text;

        public ConflictMark(Stone s, char c)
        {
	    super(s);
	    char[] b = new char[1];
	    b[0] = c;
            this.text = new String(b);
        }

        public ConflictMark(Stone s, String text)
        {
	    super(s);
            this.text = text;
        }

        public String toString()
        {
            return text;
        }

	public boolean equals(Object o)
        {
            if (o == this)
                return true;
            else if (o instanceof ConflictMark)
            {
                return getColor() == ((ConflictMark) o).getColor() && text.equals(((ConflictMark) o).text);
            }
            else
                return false;
        }
    }


    /**
     * Arbitrary text to be rendered on the Goban.
     * For the display of move numbers, {@link de.cgawron.go.goban.MarkupModel.Move} should be used.
     * @see de.cgawron.go.goban.MarkupModel.Move
     */
    public static class Text extends AbstractMarkup
    {
        private String text;

        public Text(String text)
        {
            this.text = text;
        }

        public String toString()
        {
            return text;
        }

	public boolean equals(Object o)
	{
	    if (o == this)
		return true;
	    else
		return toString().equals(o.toString());
	}
    }

    /**
     * A triangle mark.
     */
    public static class Triangle extends AbstractMarkup
    {

        public Triangle()
        {
        }

        public String toString()
        {
            return "triangle";
        }
    }

    /**
     * A square mark.
     */
    public static class Square extends AbstractMarkup
    {

        public Square()
        {
        }

        public String toString()
        {
            return "square";
        }
    }

    /**
     * A circle mark.
     */
    public static class Circle extends AbstractMarkup
    {

        public Circle()
        {
        }

        public String toString()
        {
            return "circle";
        }
    }

    /**
     * A mark ('X').
     */
    public static class Mark extends AbstractMarkup
    {

        public Mark()
        {
        }

        public String toString()
        {
            return "mark";
        }
    }

    /**
     * White territory.
     */
    public static class WhiteTerritory extends AbstractMarkup
    {

        public WhiteTerritory()
        {
        }

        public String toString()
        {
            return "white territory";
        }
    }

    /**
     * Black territory.
     */
    public static class BlackTerritory extends AbstractMarkup
    {

        public BlackTerritory()
        {
        }

        public String toString()
        {
            return "black territory";
        }
    }

    public static class Conflict implements Comparable<Conflict>
    {
	public Markup first;
	public Markup second;

	public Conflict(Markup first, Markup second)
	{
	    this.first = first;
	    this.second = second;
	}

	public int compareTo(Conflict c) throws ClassCastException
	{
	    int i1 = first.compareTo(c.first);
	    if (i1 == 0)
		return second.compareTo(c.second);
	    else
		return i1;
	}

	public String toString()
	{
	    return second.toString() + " on " + first.toString();
	}
    }


    SortedSet<Conflict> getConflicts();

    Markup getMarkup(Point p);

    void setMarkup(Point p, Markup m);

    void resetMarkup();

    Markup getMarkup(short x, short y);

    void setMarkup(short x, short y, Markup m);

    // Region getRegion();

    // void setRegion(Region region);

    String getToolTipText(Point p);

    void setToolTipText(Point p, String s);

}
