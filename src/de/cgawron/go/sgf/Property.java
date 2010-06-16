/*
 *
 * $Id: Property.java 342 2005-10-16 12:09:00Z cgawron $
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
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * This class represents an SGF property.
 * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
 */
public class Property implements Cloneable
{
    
    public interface Joinable
    {
	public void join(Property p);
    }

    /**
     * Instances of this class identify SGF properties.
     */
    public static class Key implements Comparable
    {
        String k;
        Integer priority;
	String userFriendlyName;

	/**
	 * Construct a key from a given string.
	 * Only uppercase characters are relevant according to the SGF spec.
	 * @param name the name of the Key
	 */
        Key(String name)
        {
            StringBuffer shortName = new StringBuffer(2);
            for (int i = 0; i < name.length(); i++)
            {
                char c = name.charAt(i);
                if (Character.isUpperCase(c)) shortName.append(c);
            }
            k = shortName.toString();
            PropertyDescriptor d = getDescriptor(this);
            priority = new Integer(d != null ? d.getPriority() : 0);
	    if (d != null)
		userFriendlyName = d.getUserFriendlyName(); 
	    if (userFriendlyName == null)
		userFriendlyName = shortName.toString();
        }

	/**
	 * Return the SGF short name of the key.
	 * @return the short name of the Key
	 */
        public String toString()
        {
            return k;
        }

	public String getUserFriendlyName()
	{
	    return userFriendlyName;
	}

	/**
	 * Compare this key to another key.
	 * @param o the object to be compared
	 * @return a negative integer, zero, or a positive integer as this key is less than, equal to, 
	 * or greater than the specified object.
	 * @throws ClassCastException if the specified object's type prevents it from being compared to this Object.
	 */
        public int compareTo(Object o) throws ClassCastException
        {
            Key key = (Key) o;
	    int c = priority.compareTo(key.priority);
	    if (c == 0)
                return k.compareTo(key.k);
            else
                return -c;
        }

	/**
	 * Compare this key to another one for equality.
	 * @param o the object to be compare
	 * @return true if o is equal to this key, false otherwise.
	 */
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            else if (o instanceof Key)
            {
                return k.equals(((Key)o).k);
            }
            else
                return false;

        }
    }

    /** The SGF Property AddBlack. */
    public static final Key ADD_BLACK = new Key("AB");

    /** The SGF Property AddEmpty. */
    public final static Key ADD_EMPTY = new Key("AE");

    /** The SGF Property AddWhite. */
    public final static Key ADD_WHITE = new Key("AW");

    /** The SGF Property APplication. */
    public final static Key APPLICATION = new Key("AP");

    /** The SGF Property Black. */
    public final static Key BLACK = new Key("B");

    /** The SGF Property BlackRank. */
    public final static Key BLACK_RANK = new Key("BR");

    /** The SGF Property ChAracterset. */
    public final static Key CHARACTER_SET = new Key("CA");

    /** The SGF Property CiRcle. */
    public final static Key CIRCLE = new Key("CR");

    /** The SGF Property Comment. */
    public final static Key COMMENT = new Key("C");

    /** The SGF Property DAte. */
    public final static Key DATE = new Key("DT");

    /** The SGF Property EVent. */
    public final static Key EVENT = new Key("EV");

    /** The SGF Property FiGure. */
    public final static Key FIGURE = new Key("FG");

    /** The SGF Property FileFormat. */
    public final static Key FILE_FORMAT = new Key("FF");

    /** The SGF Property GaMe. */
    public final static Key GAME = new Key("GM");

    /** The SGF Property GameName. */
    public final static Key GAME_NAME = new Key("GN");

    /** The SGF Property LaBel. */
    public final static Key LABEL = new Key("LB");

    /** The SGF Property MArk. */
    public final static Key MARK = new Key("MA");

    /** The SGF Property MoveNumber. */
    public final static Key MOVE_NO = new Key("MN");

    /** The SGF Property Name. */
    public final static Key NAME = new Key("N");

    /** The SGF Property PlayerBlack. */
    public final static Key PLAYER_BLACK = new Key("PB");

    /** The SGF Property PlayerWhite. */
    public final static Key PLAYER_WHITE = new Key("PW");

    /** The SGF Property REsult. */
    public final static Key RESULT = new Key("RE");

    /** The SGF Property SiZe. */
    public final static Key SIZE = new Key("SZ");

    /** The SGF Property SQuare. */
    public final static Key SQUARE = new Key("SQ");

    /** The SGF Property TerritoryWhite. */
    public final static Key TERRITORY_WHITE = new Key("TW");

    /** The SGF Property TerritoryBlack. */
    public final static Key TERRITORY_BLACK = new Key("TB");

    /** The SGF Property TRiangle. */
    public final static Key TRIANGLE = new Key("TR");

    /** The SGF Property USer. */
    public final static Key USER = new Key("US");

    /** The SGF Property VieW. */
    public final static Key VIEW = new Key("VW");

    /** The SGF Property White. */
    public final static Key WHITE = new Key("W");

    /** The SGF Property WhiteRank. */
    public final static Key WHITE_RANK = new Key("WR");


    private final static Key[] addStoneKeys = {ADD_BLACK, ADD_WHITE, ADD_EMPTY};

    /**
     * The properties AddWhite, AddBlack and AddEmpty.
     */
    public static final List addStoneProperties = 
	Arrays.asList(addStoneKeys);

    static Logger logger = Logger.getLogger(Property.class.getName());

    static Set InheritableProperties = new TreeSet();

    static class PropertyDescriptor
    {
        Class myClass;
        int priority = 0;
	String userFriendlyName;

        PropertyDescriptor(String s)
        {
            StringTokenizer tokens = new StringTokenizer(s, ",");
            String name = "";

            name = tokens.nextToken();
            try
            {
                myClass = Class.forName(name);
            }
            catch (Throwable e)
            {
                throw new MissingResourceException("Exception while loading " + name + ": " + e.getMessage(), 
						   getClass().getName(), name);
            }
            priority = Integer.parseInt(tokens.nextToken());
	    if (tokens.hasMoreElements())
		userFriendlyName = tokens.nextToken();
	    else
		userFriendlyName = null;
        }

        int getPriority()
        {
            return priority;
        }

	String getUserFriendlyName()
	{
	    return userFriendlyName;
	}

        Class getPropertyClass()
        {
            return myClass;
        }
    }


    private static class Factory
    {
        private HashMap<String, PropertyDescriptor> propertyMap = new HashMap<String, PropertyDescriptor>();

        Factory()
        {
            Properties properties = new Properties();
            try
            {
                //properties.load(getClass().getResourceAsStream("sgf.properties"));
                properties.load(de.cgawron.agoban.EditSGF.resources.openRawResource(de.cgawron.agoban.R.raw.sgf));
            }
            catch (Exception e)
            {
                throw new MissingResourceException(e.getMessage(), getClass().getName(), "sgf.properties");
            }
            Iterator it = properties.keySet().iterator();
            while (it.hasNext())
            {
                String key = (String)it.next();
                String value = (String)properties.get(key);
                if (value != null)
                    propertyMap.put(key, new PropertyDescriptor(value));
                else
                    logger.warn("key " + key + " without value");
            }
        }

        static Object[] argv = new Object[1];
        static Class[] argt = new Class[1];

        static
        {
            try
            {
                argt[0] = Class.forName("de.cgawron.go.sgf.Property$Key");
            }
            catch (ClassNotFoundException ex)
            {
                logger.fatal("Class Key missing, installation is corrupt: " + ex.getMessage());
                throw new RuntimeException("Class Key missing, installation is corrupt", ex);
            }
        }

        PropertyDescriptor getDescriptor(Key key)
        {
            return (PropertyDescriptor)propertyMap.get(key.toString());
        }

	/*
	Property createProperty(String s)
	{
	    return createProperty(new Key(s));
	}
	*/

        private Property createProperty(Key key)
        {
            String className = "";
            Class propertyClass = null;
            try
            {
                argv[0] = key;
		// logger.debug("Creating property for key " + argv[0] + " " + argt[0]);
                propertyClass = getDescriptor(key).getPropertyClass();
                Constructor c = propertyClass.getConstructor(argt);
                return (Property)c.newInstance(argv);
            }
            catch (Exception e)
            {
                if (propertyClass != null) {
                    logger.warn("Couldn't create a " + propertyClass.getName() + " for " + key + ": " + e);
		    if (e instanceof InvocationTargetException) {
			logger.warn("Cause was: " + ((InvocationTargetException) e).getCause());
		    } 
		}
                else
                    logger.warn("No class known for " + key);

                return new Property(key);
            }
        }

        static Object[] argv2 = new Object[2];
        static Class[] argt2 = new Class[2];
        static
        {
            try
            {
                argt2[0] = Class.forName("de.cgawron.go.sgf.Property$Key");
                argt2[1] = Class.forName("java.lang.String");
            }
            catch (ClassNotFoundException ex)
            {
                logger.fatal("Class missing, installation is corrupt: " + ex.getMessage());
                throw new RuntimeException("Class missing, installation is corrupt", ex);
            }
        }

        private Property createProperty(Key key, String s)
        {
            String className = "";
            Class propertyClass = null;
            try
            {
                argv2[0] = key;
                argv2[1] = s.substring(1, s.length()-1);
		// logger.debug("Creating property for key " + argv2[0] + " " + argv2[1]);
                propertyClass = getDescriptor(key).getPropertyClass();
                Constructor c = propertyClass.getConstructor(argt2);
                return (Property)c.newInstance(argv2);
            }
            catch (Exception e)
            {
                if (propertyClass != null) {
                    logger.warn("Couldn't create a " + propertyClass.getName() + " for " + key + ": " + e);
		    if (e instanceof InvocationTargetException) {
			logger.warn("Cause was: " + ((InvocationTargetException) e).getCause());
		    } 
		}
                else
                    logger.warn("No class known for " + key);

                return new Property(key);
            }
        }
    }


    static Factory factory = null;

    private static Factory getFactory()
    {
        if (factory == null)
            factory = new Factory();
        return factory;
    }

    static PropertyDescriptor getDescriptor(Key key)
    {
        return getFactory().getDescriptor(key);
    }

    /** 
     * Create a Property from an SGF name.
     * @param name - the name of the SGF property (either in short or long notation).
     */
    public static Property createProperty(String name)
    {
        return createProperty(new Key(name));
    }

    /** 
     * Create a Property with a given {@link Key}.
     * @param key - the key identifying the Property
     */
    public static Property createProperty(Key key)
    {
        return getFactory().createProperty(key);
    }

    /** 
     * Create a Property from an SGF name and a string representation of the value.
     * @param name - the name of the SGF property (either in short or long notation).
     * @param value - the value of the property (in SGF notation).
     */
    public static Property createProperty(String name, String value)
    {
        return getFactory().createProperty(new Key(name), value);
    }

    /** 
     * Create a Property with a given {@link Key} and a string representation of the value.
     * @param key - the name of the SGF property (either in short or long notation).
     * @param value - the value of the property (in SGF notation).
     */
    public static Property createProperty(Key key, String value)
    {
        return getFactory().createProperty(key, value);
    }

    /** 
     * Create a Property with a given {@link Key} and value.
     * @param key - the name of the SGF property (either in short or long notation).
     * @param value - the value of the property.
     */
    public static Property createProperty(Key key, Object value)
    {
        Value v = AbstractValue.createValue(value);
	return createProperty(key, v);
    }

    /** 
     * Create a Property with a given {@link Key} and value.
     * @param key - the name of the SGF property (either in short or long notation).
     * @param value - the value of the property.
     */
    public static Property createProperty(Key key, Value value)
    {
        Property prop = getFactory().createProperty(key);
        prop.setValue(value);
        return prop;
    }

    /**
     * A move property.
     * Either a black or white move property.
     */
    public static class Move extends Property
    {
        Point point = null;

	/** See {@link Property#Property(Property.Key)}. */
        public Move(Key key)
        {
            super(key);
        }


	/** {@inheritDoc}. */
        public void setValue(Value v)
        {
            super.setValue(v);
            if (v instanceof Value.Point)
                point = ((Value.Point) v).getPoint();
            else if (v instanceof Value.Void)
                point = null;
            else if (v instanceof Value.ValueList)
            {
                Value.ValueList vl = (Value.ValueList) v;
                if (vl.size() == 1)
                {
                    setValue((Value)vl.get(0));
                }
                else
                    logger.warn("Move can't have a ValueList of size " + vl.size());
            }
            else
                logger.warn("Class is " + v.getClass().getName());
        }
	
	/**
	 * Get the {@link Point} of the move.
	 * @return The point where the move is played.
	 */
        public Point getPoint()
        {
            return point;
        }

	/**
	 * Get the color of the move.
	 * @return The color of the move.
	 */
        public BoardType getColor()
        {
            if (getKey().equals(Property.BLACK))
		return BoardType.BLACK;
            else if (getKey().equals(Property.WHITE))
		return BoardType.WHITE;
	    else {
		assert false : "Move has no color: " + this;
		return null;
	    }
        }
    }

    /**
     * A common class for AddWhite, AddBlack and AddEmpty.
     */
    public static class AddStones extends Property implements Joinable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public AddStones(Key key)
        {
            super(key);
        }

	public void join(Property p)
	{
	    Value value = getValue();
	    if (value instanceof Value.ValueList) {
		((Value.ValueList) value).add(p.getValue());
	    }
	    else {
		Value.ValueList vl = AbstractValue.createValueList();
		vl.add(value);
		vl.add(p.getValue());
		setValue(vl);
	    }
	}

	/**
	 * Get the color of the stones added.
	 * @return The color of the stones added.
	 */
	public BoardType getColor()
	{
	    logger.debug("AddStones.getColor: " + this);
	    if (getKey().equals(Property.ADD_WHITE))
		return BoardType.WHITE;
	    else if (getKey().equals(Property.ADD_BLACK))
		return BoardType.BLACK;
	    else
		return BoardType.EMPTY;
	}
    }

    
    public static class Annotation extends Property
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Annotation(Key key)
        {
            super(key);
        }
    }


    public static class Markup extends Property implements MarkupModel.Markup, Joinable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Markup(Key key)
        {
            super(key);
        }

	public int compareTo(MarkupModel.Markup m)
	{
	    return toString().compareTo(m.toString());
	}

	public void join(Property p)
	{
	    Value value = getValue();
	    if (value instanceof Value.ValueList) {
		((Value.ValueList) value).add(p.getValue());
	    }
	    else {
		Value.ValueList vl = AbstractValue.createValueList();
		vl.add(value);
		vl.add(p.getValue());
		setValue(vl);
	    }
	}
    }


    public static class Label extends Markup
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Label(Key key)
        {
            super(key);
            logger.warn("Creating label " + key);
        }

	public void setValue(Value vl)
	{
	    logger.info("Setting label text to " + vl.toString());
	    super.setValue(vl);
	}
    }

    /**
     * A marker interface for ineritable properties.
     * Certain properties are "inherited" by child nodes according to the SGF spec - 
     * this interface markes these properties.
     * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
     */
    public interface Inheritable
    {
    }


    /**
     * A base class for game info properties.
     * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
     */
    public static class GameInfo extends Property implements Inheritable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public GameInfo(Key key)
        {
            super(key);
        }

        public GameInfo(Key key, String s)
        {
            super(key);
	    setValue(AbstractValue.createValue(s)); 
        }

	public void setValue(String value)
	{
	    setValue(AbstractValue.createValue(value));
	}
    }

    /**
     * A base class for game info properties.
     * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
     */
    public static class Result extends GameInfo implements Inheritable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Result(Key key)
        {
            super(key);
        }

	public void setValue(Value value)
	{
	    Value result = AbstractValue.parseResult(value);
	    logger.info("Setting result to " + result);
	    super.setValue(result);
	}
    }

    /**
     * A base class for root properties.
     * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
     */
    public static class Root extends Property implements Inheritable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Root(Key key)
        {
            super(key);
        }
    }

    public static class Charset extends Root
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Charset(Key key)
        {
            super(key);
        }

	public void setValue(Value vl)
	{
	    logger.info("Setting Charset to " + vl.toString());
	    Yylex.setCharset(vl.toString());
	    super.setValue(vl);
	}
    }


    public static class View extends Property implements Inheritable
    {
	/** See {@link Property#Property(Property.Key)}. */
        public View(Key key)
        {
            super(key);
        }

        public View(Key key, String s)
        {
            super(key);
	    setValue(AbstractValue.createPointList(s)); 
        }
    }


    public static class Text extends Property
    {
	/** See {@link Property#Property(Property.Key)}. */
        public Text(Key key)
        {
            super(key);
        }

        public Text(Key key, String s)
        {
            super(key);
	    setValue(AbstractValue.createValue(s)); 
        }
    }

    public static interface Number
    {
    }

    public static class SimpleNumber extends Property implements Number
    {
	/** See {@link Property#Property(Property.Key)}. */
        public SimpleNumber(Key key)
        {
            super(key);
        }

	public void setValue(Value v)
	{
	    logger.info("Setting value to " + v);
	    if (v instanceof Value.ValueList) {
                Value.ValueList vl = (Value.ValueList) v;
                if (vl.size() == 1) {
		    setValue((Value.Number) vl.get(0));
                }
                else
		    throw new IllegalArgumentException("Cannot set value to a ValueList with length != 1");
	    }
	    else
		super.setValue((Value.Number) v);
	}
    }

    public static class RootNumber extends Root implements Number
    {
	/** See {@link Property#Property(Property.Key)}. */
        public RootNumber(Key key)
        {
            super(key);
        }

	public void setValue(Value v)
	{
	    logger.info("Setting value to " + v);
	    if (v instanceof Value.ValueList) {
                Value.ValueList vl = (Value.ValueList) v;
                if (vl.size() == 1) {
		    setValue((Value.Number) vl.get(0));
                }
                else
		    throw new IllegalArgumentException("Cannot set value to a ValueList with length != 1");
	    }
	    else
		super.setValue((Value.Number) v);
	}
    }


    private Value value = null;
    private Key key = null;

    /**
     * Create a property with a given key.
     * This constructor is protected, use @link{Property.createProperty(Key)}. This constructor should only be used by the {@link Factory} class.
     * @param key {@link Key} identifying the property.
     */
    Property(Key key)
    {
        this.key = key;
    }

    /**
     * Set the {@link Value} of the property.
     * @param value the value to set 
     */
    public void setValue(Value value)
    {
        this.value = value;
    }

    /**
     * Set the {@link Value} of the property to String <code>newValue</code>.
     * This operation is optional, i.e. not all sub-classes might support it. The default implemantion in this class just throws an UnsupportedOperationException.
     * @throws java.lang.UnsupportedOperationException 
     * @param value the value to set 
     */
    public void setValue(String newValue)
    {
	throw new UnsupportedOperationException();
    }
    

    /**
     * Get the value of this proprty.
     * @return the Value of this property.
     */
    public Value getValue()
    {
        if (value == null)
        {
            logger.warn("Property " + key + ": no value");
        }
        return value;
    }

    /**
     * Get the key of this proprty.
     * @return the Key of this property.
     */
    public Key getKey()
    {
        return key;
    }

    /**
     * Get the name, i.e. the string representation of the key of this proprty.
     * @return the Value of this property.
     */
    public String getName()
    {
        return key.toString();
    }

    public String toString()
    {
        String s = key + (value != null ? value.toString() : "<null>");
        return s;
    }

    public void write(PrintWriter out)
    {
        out.print(key.toString());
        if (value != null)
            value.write(out);
        else
            out.print("[]");
    }

    public Property clone()
    {
	Property p = createProperty(key);
	if (value != null)
	    p.setValue(value.clone());
	return p;
    }
    
    public static String formatResult(Object o)
    {
	return formatResult((Property) o);
    } 

    final static Pattern pattern = Pattern.compile("([wW]|[bB])\\+(resign|R|time|forfeit|(\\d+(?:[.,]\\d*)))|([Jj]igo)");

    public static String formatResult(Property p)
    {
	if (!p.getKey().equals(Property.RESULT))
	    throw new IllegalArgumentException("Argument is no result");
	else {
	    Matcher m = pattern.matcher(p.getValue().toString());
	    
	    if (!m.matches()) {
		logger.warn("Unrecognized result value: " + p.getValue().toString());
		return p.getValue().toString();
	    }
	    else {
		if (m.group(4) != null)
		    return "Jigo";
		else {
		    String winner = m.group(1);
		    String looser = null;
		    String special = m.group(2);
		    String margin = m.group(3);
		    if (winner.equals("w") || winner.equals("W")) {
			winner = "Weiß";
			looser = "Schwarz"; 
		    }
		    else if (winner.equals("b") || winner.equals("B")) {
			winner = "Schwarz"; 
			looser = "Weiß";
		    }
		    else
			assert false;
		    
		    if (margin == null) {
			if (special.equals("resign") || special.equals("R"))
			    return winner + " gewinnt durch Aufgabe";
			else if (special.equals("time"))
			    return looser + " verliert durch Zeitüberschreitung";
			else 
			    return winner + " gewinnt";
		    }
		    else
			return winner + " gewinnt mit " + margin + " Punkten";
		}
	    }
	}

    }
}
