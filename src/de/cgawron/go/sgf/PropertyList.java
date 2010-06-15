/*
 *
 * $Id: PropertyList.java 342 2005-10-16 12:09:00Z cgawron $
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

import de.cgawron.go.Point;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class PropertyList extends TreeMap<Property.Key, Property>
{
    private static Logger logger = Logger.getLogger(PropertyList.class.getName());

    public PropertyList()
    {
    }

    public PropertyList(Property p)
    {
        put(p.getKey(), p);
    }

    public PropertyList(PropertyList pl)
    {
        super();
	Iterator<Map.Entry<Property.Key, Property>> it = pl.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry<Property.Key, Property> entry = it.next();
	    put(entry.getKey(), entry.getValue());
	}
    }

    public Value getValue(Property.Key s)
    {
        Property prop = (Property)get(s);
        if (prop == null)
        {
            logger.info(s + " not found");
            return null;
        }
        else
            return prop.getValue();
    }

    public Point getPoint(Property.Key s)
    {
        Property prop = (Property)get(s);
        if (prop == null)
        {
            return null;
        }
        else
        {
            if (prop instanceof Property.Move)
                return ((Property.Move) prop).getPoint();
            else
                return null;
        }
    }

    public Value.PointList getPointList(Property.Key s)
    {
        Property prop = (Property)get(s);
        if (prop == null)
        {
            return null;
        }
        else
        {
            Value value = prop.getValue();
            // logger.finest("getPointList: " + value + ": " + value.getClass().getName());
            if (value instanceof Value.ValueList)
            {
                Value.ValueList list = (Value.ValueList) value;
                // logger.finest("getPointList: " + list.size());
                assert list.size() == 1;
                return (Value.PointList) list.get(0);
            }
            return (Value.PointList) value;
        }
    }

    public boolean contains(Object o)
    {
        return containsKey(o);
    }

    public void add(Property p)
    {
        put(p);
    }

    public Property put(Property.Key k, Property v)
    {
	if (contains(k)) {
	    Property p = get(k);
	    if (p instanceof Property.Joinable)
	    {
		((Property.Joinable) p).join(v);
		return p;
	    }
	    else if (p.getValue() == null)
	    {
		p.setValue(v.getValue());
		return p;
	    }
	    else if (v.getValue() == null)
		return p;
	    else if (p.getValue().equals(v.getValue()))
		return p;
	    else
		throw new RuntimeException("Can not add " + v);
	}
	else
	    return super.put(k, v);
    }

    public void put(Property p)
    {
        put(p.getKey(), p);
    }

    public void write(PrintWriter out)
    {
        out.print(";");
        Iterator it = values().iterator();
        while (it.hasNext())
        {
            Property prop = (Property)it.next();
            prop.write(out);
        }
        out.println();
    }
}
