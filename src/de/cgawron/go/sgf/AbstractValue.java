/**
 *
 * (C) 2010 Christian Gawron. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package de.cgawron.go.sgf;

import de.cgawron.go.MutablePoint;
import de.cgawron.go.Symmetry;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract base class for implementations of {@link Value}.
 */
public abstract class AbstractValue implements Value
{
    private static Logger logger = Logger.getLogger(AbstractValue.class.getName());

    /**
     * Write the SGF representation of this Value to an PrintWriter.
     * @param out PrintWriter to write on.
     */
    public abstract void write(PrintWriter out);


    /**
     * A factory class for Values.
     */
    protected static class Factory
    {
	/**
	 * Create a Value from an Object.
	 * Depending on the class of the Object, an appropriate sub-class is returned. 
	 * @param o an Object to wrap into an Value.
	 * @return A Value representing the Object.
	 */
        public Value createValue(Object o)
        {
            if (o instanceof String)
                return new AbstractValue.Text((String)o);
            else if (o instanceof Integer)
                return new AbstractValue.Number((Integer)o);
            else if (o == null)
                return new AbstractValue.Void();
            else
                return new AbstractValue.Text(o.toString());
        }

	/**
	 * Create a {@link Value.Point} from a String.
	 * @param text the SGF representation of a {@link de.cgawon.go.Point}.
	 * @return A {@link Value.Point} representing the String.
	 */
        public Value createPoint(String text)
        {
            return new AbstractValue.Point(text);
        }

	/**
	 * Create a {@link Value.Point} from a {@link de.cgawron.go.Point}.
	 * @param point the Point to wrap in a {@link de.cgawon.go.Point}.
	 * @return A {@link Value.Point} representing the Point.
	 */
        public Value createPoint(de.cgawron.go.Point point)
        {
            return new AbstractValue.Point(point);
        }

	/**
	 * Create a {@link Value.Label}.
	 * @param pt the SGF representation of a {@link de.cgawon.go.Point}.
	 * @param text the label text.
	 * @return A {@link Value.Label}.
	 */
        public Value createLabel(String pt, String text)
        {
            return new AbstractValue.Label(pt, text);
        }

	/**
	 * Create an empty {@link Value.PointList}.
	 * @return An empty {@link Value.PointList}.
	 */
        public Value createPointList()
        {
            return new AbstractValue.PointList();
        }

	/**
	 * Create a {@link Value.PointList} from a String.
	 * @param text the SGF representation of a {@link de.cgawon.go.PointList}.
	 * @return A {@link Value.PointList} representing the String.
	 */
        public Value createPointList(String text)
        {
            return new AbstractValue.PointList(text);
        }

	/**
	 * Create an empty {@link Value.ValueList}.
	 * @return An empty {@link Value.ValueList}.
	 */
	public static Value.ValueList createValueList()
	{
	    return new AbstractValue.ValueList();
	}
	
	/**
	 * Create a {@link Value.ValueList} consisting of a single Value.
	 * @param value the value to wrap in a {@link Value.ValueList}
	 * @return A {@link Value.ValueList} consisting of value.
	 */
	public static Value.ValueList createValueList(Value value)
	{
	    return new AbstractValue.ValueList(value);
	}

	public static Value parseResult(Value value)
	{
	    return new AbstractValue.Result(value);
	}
    }


    private static Factory factory = null;

    /**
     * Get a {@link Factory} to create Values.
     * @return a Factory instance.
     */
    protected static Factory getFactory()
    {
        if (factory == null)
            factory = new Factory();
        return factory;
    }

    /**
     * Creates and returns a copy of this Value.
     * @return A copy of this Value.
     */
    public Value clone() 
    {
	try {
	    return (Value) super.clone();
	}
	catch (CloneNotSupportedException ex) {
	    throw new RuntimeException(ex);
	}
    } 

    /**
     * Create a Value from an Object.
     * Depending on the class of the Object, an appropriate sub-class is returned. 
     * @param o an Object to wrap into an Value.
     * @return A Value representing the Object.
     */
    public static Value createValue(Object o)
    {
        return getFactory().createValue(o);
    }

    /**
     * Create a {@link Value.Point} from a String.
     * @param text the SGF representation of a {@link de.cgawon.go.Point}.
     * @return A {@link Value.Point} representing the String.
     */    public static Value createPoint(String text)
    {
        return getFactory().createPoint(text);
    }

    /**
     * Create a {@link Value.Point} from a {@link de.cgawron.go.Point}.
     * @param point the Point to wrap in a {@link de.cgawon.go.Point}.
     * @return A {@link Value.Point} representing the Point.
     */
    public static Value createPoint(de.cgawron.go.Point point)
    {
        return getFactory().createPoint(point);
    }


    /**
     * Create a {@link Value.Label}.
     * @param pt the SGF representation of a {@link de.cgawon.go.Point}.
     * @param text the label text.
     * @return A {@link Value.Label}.
     */
    public static Value createLabel(String pt, String text)
    {
        return getFactory().createLabel(pt, text);
    }

    /**
     * Create an empty {@link Value.PointList}.
     * @return An empty {@link Value.PointList}.
     */
    public static Value createPointList()
    {
        return getFactory().createPointList();
    }

    /**
     * Create a {@link Value.PointList} from a String.
     * @param text the SGF representation of a {@link de.cgawon.go.PointList}.
     * @return A {@link Value.PointList} representing the String.
     */
    public static Value createPointList(String text)
    {
        return getFactory().createPointList(text);
    }

    /**
     * Create an empty {@link Value.ValueList}.
     * @return An empty {@link Value.ValueList}.
     */
    public static Value.ValueList createValueList()
    {
        return getFactory().createValueList();
    }

    /**
     * Create a {@link Value.ValueList} consisting of a single Value.
     * @param value the value to wrap in a {@link Value.ValueList}
     * @return A {@link Value.ValueList} consisting of value.
     */
    public static Value.ValueList createValueList(Value value)
    {
        return getFactory().createValueList(value);
    }

    public static Value parseResult(Value text)
    {
        return getFactory().parseResult(text);
    }

    public String getString()
    {
	return toString();
    }

    public boolean equals(Object o)
    {
	return toString().equals(o.toString());
    }

    private static class Void extends AbstractValue implements Value.Void
    {
        private Void()
        {
        }
	
        public String toString()
        {
            return "<null>";
        }

        public void write(PrintWriter out)
        {
            out.print("[");
            out.print("]");
        }
    }


    private static class Point extends AbstractValue implements Value.Point
    {
        de.cgawron.go.Point point;

        public Point(String text)
        {
            point = new de.cgawron.go.Point(text);
        }

        public Point(de.cgawron.go.Point p)
        {
            point = new de.cgawron.go.Point(p);
        }

        public Point(short x, short y)
        {
            point = new de.cgawron.go.Point(x, y);
        }

        public de.cgawron.go.Point getPoint()
        {
            return point;
        }

        public short getX()
        {
            return point.getX();
        }

        public short getY()
        {
            return point.getY();
        }

        public void write(PrintWriter out)
        {
            out.print("[");
            out.print(point.sgfString());
            out.print("]");
        }

	public void transform(Symmetry s)
	{
	    //HACK! A point value should know about the board size ...
	    point = s.transform(point, (short) 19);
	    logger.info(toString() + ": transform(" + s + ")");
	}
    }


    private static class Label extends AbstractValue implements Value.Label
    {
        de.cgawron.go.Point point;
        String text;

        public Label(String pt, String text)
        {
            point = new de.cgawron.go.Point(pt);
            this.text = text;
	    logger.fine("Value.Label " + text);
        }

	public void transform(Symmetry s)
	{
	    //HACK! A point value should know about the board size ...
	    point = s.transform(point, (short) 19);
	    logger.info(toString() + ": transform(" + s + ")");
	}

        public de.cgawron.go.Point getPoint()
        {
            return point;
        }

        public String toString()
        {
            return text;
        }

        public void write(PrintWriter out)
        {
            out.print("[");
            out.print(getPoint().sgfString());
            out.print(":");
            out.print(text);
            out.print("]");
        }

    }

    private static class PointList extends java.util.AbstractCollection<de.cgawron.go.Point> implements Value.PointList
    {
        short lx = 255;
        short ly = 255;
        short ux = -1;
        short uy = -1;
        SortedSet<de.cgawron.go.Point> points = new TreeSet<de.cgawron.go.Point>();

        public short getMinX() { return lx; }

        public short getMinY() { return ly; }

        public short getMaxX() { return ux; }

        public short getMaxY() { return uy; }

        public PointList()
        {
        }

        public PointList(String text)
        {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("PointList(" + text + ")");
            if (text.length() == 0)
	    {
		// PointList is empty
		lx=0;
		ly=0;
		ux=18;
		uy=18;
	    }
            else if (text.length() == 2)
            {
                de.cgawron.go.Point p = new de.cgawron.go.Point(text);
                add(p);
		lx = p.getX();
		ux = p.getX();
		ly = p.getY();
		uy = p.getY();
            }
            else if (text.length() == 5)
            {
                de.cgawron.go.Point p1 = new de.cgawron.go.Point(text.substring(0, 2));
                de.cgawron.go.Point p2 = new de.cgawron.go.Point(text.substring(3, 5));

                short x, y;
                short xMin, xMax;
                short yMin, yMax;
                if (p1.getX() < p2.getX())
                {
                    xMin = p1.getX();
                    xMax = p2.getX();
                }
                else
                {
                    xMin = p2.getX();
                    xMax = p1.getX();
                }
                if (p1.getY() < p2.getY())
                {
                    yMin = p1.getY();
                    yMax = p2.getY();
                }
                else
                {
                    yMin = p2.getY();
                    yMax = p1.getY();
                }
                if (xMin < lx) lx = xMin;
                if (xMax > ux) ux = xMax;
                if (yMin < ly) ly = yMin;
                if (yMax > uy) uy = yMax;
                for (x = xMin; x <= xMax; x++)
                    for (y = yMin; y <= yMax; y++)
                        points.add(new de.cgawron.go.Point(x, y));

		if (logger.isLoggable(Level.FINE))
		    logger.fine("PointList: " + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ", " + this);
            }
            else
                throw new ClassCastException(text + " is not a PointList");
        }

	public Value clone()
	{
	    PointList list = new AbstractValue.PointList();
	    for (de.cgawron.go.Point p : points)
		list.add(p);
	    return list;
	}

	public int size()
	{
	    return points.size();
	}

        public boolean add(Value o)
        {
            if (o instanceof Value.Point)
                return this.add(((Value.Point)o).getPoint());
            else if (o instanceof de.cgawron.go.Point)
                return this.add((de.cgawron.go.Point) o);
            else if (o instanceof Value.PointList)
            {
                Value.PointList pl = (Value.PointList) o;
                Iterator it = pl.iterator();
                while (it.hasNext())
                    this.add((de.cgawron.go.Point) it.next());
                return true;
            }
            else
                throw new ClassCastException("Could not add an object of class " + o.getClass().getName());
        }

        public boolean add(de.cgawron.go.Point p)
        {
            if (p.getX() < lx) lx = p.getX();
            if (p.getX() > ux) ux = p.getX();
            if (p.getY() < ly) ly = p.getY();
            if (p.getY() > uy) uy = p.getY();
            return points.add(p);
        }

        public boolean remove(de.cgawron.go.Point p)
        {
            boolean b = points.remove(p);
	    revalidate();
	    return b;
        }

	private void revalidate()
	{
	    lx = 255;
	    ly = 255;
	    ux = -1;
	    uy = -1;
	    Iterator it = points.iterator();
	    de.cgawron.go.Point p;
	    while (it.hasNext()) {
		p = (de.cgawron.go.Point) it.next();
		if (p.getX() < lx) lx = p.getX();
		if (p.getX() > ux) ux = p.getX();
		if (p.getY() < ly) ly = p.getY();
		if (p.getY() > uy) uy = p.getY();
	    }		
	}

        public Iterator<de.cgawron.go.Point> iterator()
        {
            return points.iterator();
        }

	public void transform(Symmetry s)
	{
	    logger.info(toString() + ": transform(" + s + ")");
	    Iterator it = iterator();
	    while (it.hasNext()) {
		Object o = it.next();
		if (o instanceof Transformable) {
		    ((Transformable) o).transform(s);
		}
	    }
	}

        void getMaximumRectangle(SortedSet<de.cgawron.go.Point> ts, SortedSet<de.cgawron.go.Point> r, de.cgawron.go.Point p)
        {
            short width = 0;
            short height = 0;

            MutablePoint q = new MutablePoint(p);

            while (ts.contains(q.setX((short)(p.getX() + width))))
            {
                width++;
                r.add(new de.cgawron.go.Point(q));
            }
            boolean b = true;
            while (b)
            {
                for (int i = 0; i < width; i++)
                {
                    q.setX((short)(p.getX() + i));
                    q.setY((short)(p.getY() + height + 1));
                    if (!ts.contains(q)) b = false;
                }
                if (b)
                {
                    for (int i = 0; i < width; i++)
                    {
                        q.setX((short)(p.getX() + i));
                        q.setY((short)(p.getY() + height + 1));
                        r.add(new de.cgawron.go.Point(q));
                    }
                    height++;
                }
            }
        }

        public void write(PrintWriter out)
        {
            if (true)
            {
                // write a compressed PointList
                TreeSet<de.cgawron.go.Point> ts = new TreeSet<de.cgawron.go.Point>();
                TreeSet<de.cgawron.go.Point> r = new TreeSet<de.cgawron.go.Point>();
                ts.addAll(points);

                while (!ts.isEmpty())
                {
                    int size1 = ts.size();
                    de.cgawron.go.Point p = (de.cgawron.go.Point) ts.first();
                    assert ts.contains(p);
                    //logger.info("PointList.write: " + ts.size() + " points to go, current is " + p);
                    r.clear();
                    getMaximumRectangle(ts, r, p);
                    if (r.size() > 1)
                    {
                        de.cgawron.go.Point m = (de.cgawron.go.Point) r.first();
                        de.cgawron.go.Point M = (de.cgawron.go.Point) r.last();
                        out.print("[");
                        out.print(m.sgfString());
                        out.print(":");
                        out.print(M.sgfString());
                        out.print("]");
                    }
                    else
                    {
                        de.cgawron.go.Point m = (de.cgawron.go.Point) r.first();
                        out.print("[");
                        out.print(m.sgfString());
                        out.print("]");
                    }
                    //logger.info("ts=" + ts);
                    //logger.info("r=" + r);
                    boolean b = ts.removeAll(r);
                    assert b : "ts.removeAll(r) didn't return true";
                    assert r.size() > 0 : "r.size() > 0";
                    assert ts.size() == size1 - r.size() : "ts.size() == size1 - r.size()";
                }
            }
            else
            {
                Iterator it = iterator();
                while (it.hasNext())
                {
                    de.cgawron.go.Point p = (de.cgawron.go.Point) it.next();
                    out.print("[");
                    out.print(p.sgfString());
                    out.print("]");
                }
            }
        }

        public String getString()
	{
	    return toString();
	}

        public String toString()
        {
            StringBuffer s = new StringBuffer();
            Iterator it = points.iterator();
            while (it.hasNext())
            {
                s.append((de.cgawron.go.Point) it.next()).toString();
            }
            return s.toString();
        }
    }


    private static class Text extends AbstractValue implements Value.Text
    {
        String text;

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
	    return toString().equals(o.toString());
	}

        public void write(PrintWriter out)
        {
            out.print("[");
	    for (int i=0; i<text.length(); i++)
	    {
		char c=text.charAt(i); 
		switch (c)
		{
		case ']':
		    out.print("\\]");
		    break;
		default:
		    out.print(c);
		    break;
		}
	    }
            out.print("]");
        }
    }

    private static class Result extends AbstractValue implements Value.Result
    {
        String text;
	Pattern win = Pattern.compile("(B|W)\\+([0-9.]+|R|T|F)");

        public Result(Value value)
        {
            this.text = value.toString();
	    Matcher matcher = win.matcher(text);
	    if (matcher.matches())
	    {
		logger.info("Winner is " + matcher.group(1));
		logger.info("Win by " + matcher.group(2));
	    }
        }

        public String toString()
        {
            return text;
        }

        public void write(PrintWriter out)
        {
            out.print("[");
	    for (int i=0; i<text.length(); i++)
	    {
		char c=text.charAt(i); 
		switch (c)
		{
		case ']':
		    out.print("\\]");
		    break;
		default:
		    out.print(c);
		    break;
		}
	    }
            out.print("]");
        }
    }

    private static class Number extends AbstractValue implements Value.Number
    {
        Integer number;

        public Number(String text)
        {
            this.number = Integer.getInteger(text);
        }

        public Number(Integer number)
        {
            this.number = number;
        }

	public int intValue()
	{
	    return number.intValue();
	}

        public String toString()
        {
            return number.toString();
        }

	public boolean equals(Object o)
	{
	    if (o instanceof Value.Number)
	    {
		return intValue() == ((Value.Number) o).intValue();
	    }
	    else return false;
	}

        public void write(PrintWriter out)
        {
            out.print("[");
            out.print(number);
            out.print("]");
        }
    }


    private static class ValueList extends AbstractValue implements Value.ValueList
    {
        List<Value> values = new ArrayList<Value>();

        public ValueList()
        {
        }

        public ValueList(Value v)
        {
            values.add(v);
        }

        public boolean add(Value v)
        {
            if (!(v instanceof Value))
                throw new ClassCastException("Only a Value can be added to a ValueList");
            else if ((v instanceof Value.PointList) && values.size() > 0 && values.get(0) instanceof Value.PointList)
            {
                Value.PointList pl = (Value.PointList) values.get(0);
                return pl.addAll((Collection<de.cgawron.go.Point>) v);
            }
            else
                return values.add(v);
        }

        public int size()
        {
            return values.size();
        }

        public boolean isEmpty()
        {
            return values.isEmpty();
        }

        public boolean contains(Object o)
        {
            return values.contains(o);
        }

        public Iterator<Value> iterator()
        {
            return values.iterator();
        }

	public void transform(Symmetry s)
	{
	    logger.info(toString() + ": transform(" + s + ")");
	    Iterator it = iterator();
	    while (it.hasNext()) {
		Object o = it.next();
		if (o instanceof Transformable) {
		    ((Transformable) o).transform(s);
		}
	    }
	}

        public Object[] toArray()
        {
            return values.toArray();
        }

        public <T> T[] toArray(T a[])
        {
            return values.toArray(a);
        }

        public boolean remove(Object o)
        {
            return values.remove(o);
        }

        public boolean containsAll(Collection<?> c)
        {
            return values.containsAll(c);
        }

        public boolean addAll(Collection<? extends Value> c)
        {
            return values.addAll(c);
        }

        public boolean addAll(int index, Collection<? extends Value> c)
        {
            return values.addAll(index, c);
        }

        public boolean removeAll(Collection<?> c)
        {
            return values.removeAll(c);
        }

        public boolean retainAll(Collection<?> c)
        {
            return values.retainAll(c);
        }

        public void clear()
        {
            values.clear();
        }

        public boolean equals(Object o)
        {
            return values.equals(o);
        }

        public int hashCode()
        {
            return values.hashCode();
        }

        public Value get(int index)
        {
            return values.get(index);
        }

        public Value set(int index, Value element) throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        public void add(int index, Value element) throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        public Value remove(int index)
        {
            return values.remove(index);
        }

        public int indexOf(Object o)
        {
            return values.indexOf(o);
        }

        public int lastIndexOf(Object o)
        {
            return values.lastIndexOf(o);
        }

        public ListIterator<Value> listIterator()
        {
            return values.listIterator();
        }

        public ListIterator<Value> listIterator(int index)
        {
            return values.listIterator(index);
        }

        public List<Value> subList(int fromIndex, int toIndex)
        {
            return values.subList(fromIndex, toIndex);
        }

        public String toString()
        {
            String s = "";
            Iterator i = iterator();
            while (i.hasNext())
            {
                Value p = (Value)i.next();
                s += p.toString();
            }
            return s;
        }

        public void write(PrintWriter out)
        {
            Iterator it = iterator();
            while (it.hasNext())
            {
                Value p = (Value)it.next();
                p.write(out);
            }
        }

	public Value clone()
	{
	    Value.ValueList list = new AbstractValue.ValueList();
	    for (Value v : values)
	    {
		list.add((Value) (v.clone()));
	    }
	    return list;
	}
    }
}
