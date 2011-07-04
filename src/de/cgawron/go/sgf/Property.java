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

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents an SGF property.
 * 
 * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
 */
public class Property implements Cloneable
{
	@Retention(value = RUNTIME)
	@Target(value = ElementType.FIELD)
	public @interface SGFProperty {
		Class propertyClass() default GameInfo.class;

		String name() default "";

		int priority() default 1000;
	}

	static Class[] argt = new Class[1];

	static {
		try {
			argt[0] = Class.forName("de.cgawron.go.sgf.Property$Key");
		} catch (ClassNotFoundException ex) {
			// logger.severe("Class Key missing, installation is corrupt: " +
			// ex.getMessage());
			throw new RuntimeException(
					"Class Key missing, installation is corrupt", ex);
		}
	}

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
		String userFriendlyName;
		int priority = -1;

		/**
		 * Construct a key from a given string. Only uppercase characters are
		 * relevant according to the SGF spec.
		 * 
		 * @param name
		 *            the name of the Key
		 */
		public Key(String name)
		{
			StringBuffer shortName = new StringBuffer(2);
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (Character.isUpperCase(c))
					shortName.append(c);
			}
			k = shortName.toString();
			if (userFriendlyName == null)
				userFriendlyName = shortName.toString();
		}

		/**
		 * Return the SGF short name of the key.
		 * 
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

		public int getPriority()
		{
			if (priority < 0) {
				priority = getDescriptor(this) != null ? getDescriptor(this)
						.getPriority() : 0;
			}
			return priority;
		}

		/**
		 * Compare this key to another key.
		 * 
		 * @param o
		 *            the object to be compared
		 * @return a negative integer, zero, or a positive integer as this key
		 *         is less than, equal to, or greater than the specified object.
		 * @throws ClassCastException
		 *             if the specified object's type prevents it from being
		 *             compared to this Object.
		 */
		public int compareTo(Object o) throws ClassCastException
		{
			Key key = (Key) o;
			int myPrio = getPriority();
			int theirPrio = key.getPriority();

			if (myPrio == theirPrio)
				return k.compareTo(key.k);
			else if (myPrio < theirPrio)
				return -1;
			else
				return 1;
		}

		/**
		 * Compare this key to another one for equality.
		 * 
		 * @param o
		 *            the object to be compare
		 * @return true if o is equal to this key, false otherwise.
		 */
		public boolean equals(Object o)
		{
			if (o == this)
				return true;
			else if (o instanceof Key) {
				return k.equals(((Key) o).k);
			} else
				return false;

		}
	}

	static class PropertyDescriptor
	{
		Class myClass;
		Constructor myConstructor;
		int priority = 0;
		String userFriendlyName;

		PropertyDescriptor(SGFProperty annotation)
		{
			myClass = annotation.propertyClass();
			try {
				myConstructor = myClass.getConstructor(argt);
			} catch (Exception ex) {
				myConstructor = null;
			}
			priority = annotation.priority();
			userFriendlyName = annotation.name();
		}

		PropertyDescriptor(String s)
		{
			StringTokenizer tokens = new StringTokenizer(s, ",");
			String name = "";

			name = tokens.nextToken();
			try {
				myClass = Class.forName(name);
				if (myClass != null)
					myConstructor = myClass.getConstructor(argt);
			} catch (Throwable e) {
				myConstructor = null;
			}
			priority = Integer.parseInt(tokens.nextToken());
			if (tokens.hasMoreElements())
				userFriendlyName = tokens.nextToken();
			else
				userFriendlyName = null;
		}

		Integer getPriority()
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

		Constructor getConstructor()
		{
			return myConstructor;
		}
	}

	private static class Factory
	{
		private static HashMap<String, PropertyDescriptor> propertyMap = null;
		Map<Key, Constructor> constructorMap = new HashMap<Key, Constructor>();

		Factory()
		{
			if (propertyMap == null)
				propertyMap = new HashMap<String, PropertyDescriptor>();

			logger.fine("Factory()");
			Properties properties = new Properties();

			Field[] fields = Property.class.getFields();
			for (Field field : fields) {
				try {
					logger.fine("field: " + field.getName() + " "
							+ field.get(null));
					SGFProperty annotation = field
							.getAnnotation(SGFProperty.class);
					if (annotation != null) {
						Key key = (Key) field.get(null);
						logger.fine("key: " + key);
						propertyMap.put(key.toString(), new PropertyDescriptor(
								annotation));
					}
				} catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		static Object[] argv = new Object[1];
		static Class[] argt = new Class[1];

		static {
			try {
				argt[0] = Class.forName("de.cgawron.go.sgf.Property$Key");
			} catch (ClassNotFoundException ex) {
				logger.severe("Class Key missing, installation is corrupt: "
						+ ex.getMessage());
				throw new RuntimeException(
						"Class Key missing, installation is corrupt", ex);
			}
		}

		PropertyDescriptor getDescriptor(Key key)
		{
			return propertyMap.get(key.toString());
		}

		/*
		 * Property createProperty(String s) { return createProperty(new
		 * Key(s)); }
		 */

		private Property createProperty(Key key)
		{
			String className = "";
			Class propertyClass = null;
			try {
				argv[0] = key;
				propertyClass = getDescriptor(key).getPropertyClass();
				Constructor c = getDescriptor(key).getConstructor();
				if (c == null)
					return new Property(key);
				return (Property) c.newInstance(argv);
			} catch (Exception e) {
				if (propertyClass != null) {
					logger.warning("Couldn't create a "
							+ propertyClass.getName() + " for " + key + ": "
							+ e);
					if (e instanceof InvocationTargetException) {
						logger.warning("Cause was: "
								+ ((InvocationTargetException) e).getCause());
					}
				} else
					logger.warning("No class known for " + key);

				return new Property(key);
			}
		}

		static Object[] argv2 = new Object[2];
		static Class[] argt2 = new Class[2];
		static {
			try {
				argt2[0] = Class.forName("de.cgawron.go.sgf.Property$Key");
				argt2[1] = Class.forName("java.lang.String");
			} catch (ClassNotFoundException ex) {
				logger.severe("Class missing, installation is corrupt: "
						+ ex.getMessage());
				throw new RuntimeException(
						"Class missing, installation is corrupt", ex);
			}
		}

		private Property createProperty(Key key, String s)
		{
			String className = "";
			Class propertyClass = null;
			try {
				argv2[0] = key;
				argv2[1] = s.substring(1, s.length() - 1);
				propertyClass = getDescriptor(key).getPropertyClass();
				logger.fine("Creating property for key " + argv[0] + " "
						+ argt[0] + " " + propertyClass);
				Constructor c = propertyClass.getConstructor(argt2);
				return (Property) c.newInstance(argv2);
			} catch (Exception e) {
				if (propertyClass != null) {
					logger.warning("Couldn't create a "
							+ propertyClass.getName() + " for " + key + ": "
							+ e);
					if (e instanceof InvocationTargetException) {
						logger.warning("Cause was: "
								+ ((InvocationTargetException) e).getCause());
					}
				} else
					logger.warning("No class known for " + key);

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
	 * 
	 * @param name
	 *            - the name of the SGF property (either in short or long
	 *            notation).
	 */
	public static Property createProperty(String name)
	{
		return createProperty(new Key(name));
	}

	/**
	 * Create a Property with a given {@link Key}.
	 * 
	 * @param key
	 *            - the key identifying the Property
	 */
	public static Property createProperty(Key key)
	{
		return getFactory().createProperty(key);
	}

	/**
	 * Create a Property from an SGF name and a string representation of the
	 * value.
	 * 
	 * @param name
	 *            - the name of the SGF property (either in short or long
	 *            notation).
	 * @param value
	 *            - the value of the property (in SGF notation).
	 */
	public static Property createProperty(String name, String value)
	{
		return getFactory().createProperty(new Key(name), value);
	}

	/**
	 * Create a Property with a given {@link Key} and a string representation of
	 * the value.
	 * 
	 * @param key
	 *            - the name of the SGF property (either in short or long
	 *            notation).
	 * @param value
	 *            - the value of the property (in SGF notation).
	 */
	public static Property createProperty(Key key, String value)
	{
		return getFactory().createProperty(key, value);
	}

	/**
	 * Create a Property with a given {@link Key} and value.
	 * 
	 * @param key
	 *            - the name of the SGF property (either in short or long
	 *            notation).
	 * @param value
	 *            - the value of the property.
	 */
	public static Property createProperty(Key key, Object value)
	{
		Value v = AbstractValue.createValue(value);
		return createProperty(key, v);
	}

	/**
	 * Create a Property with a given {@link Key} and value.
	 * 
	 * @param key
	 *            - the name of the SGF property (either in short or long
	 *            notation).
	 * @param value
	 *            - the value of the property.
	 */
	public static Property createProperty(Key key, Value value)
	{
		Property prop = getFactory().createProperty(key);
		prop.setValue(value);
		return prop;
	}

	/**
	 * A move property. Either a black or white move property.
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
			else if (v instanceof Value.ValueList) {
				Value.ValueList vl = (Value.ValueList) v;
				if (vl.size() == 1) {
					setValue((Value) vl.get(0));
				} else
					logger.warning("Move can't have a ValueList of size "
							+ vl.size());
			} else
				logger.warning("Class is " + v.getClass().getName());
		}

		/**
		 * Get the {@link Point} of the move.
		 * 
		 * @return The point where the move is played.
		 */
		public Point getPoint()
		{
			return point;
		}

		/**
		 * Get the color of the move.
		 * 
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
			} else {
				Value.ValueList vl = AbstractValue.createValueList();
				vl.add(value);
				vl.add(p.getValue());
				setValue(vl);
			}
		}

		/**
		 * Get the color of the stones added.
		 * 
		 * @return The color of the stones added.
		 */
		public BoardType getColor()
		{
			logger.fine("AddStones.getColor: " + this);
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

	public static class Markup extends Property implements MarkupModel.Markup,
			Joinable
	{
		protected MarkupModel.Type type;

		/** See {@link Property#Property(Property.Key)}. */
		public Markup(Key key)
		{
			super(key);

			if (key.equals(TRIANGLE))
				type = MarkupModel.Type.TRIANGLE;
			else if (key.equals(SQUARE))
				type = MarkupModel.Type.SQUARE;
			else if (key.equals(CIRCLE))
				type = MarkupModel.Type.CIRCLE;
			else if (key.equals(MARK))
				type = MarkupModel.Type.MARK;
			else if (key.equals(TERRITORY_WHITE))
				type = MarkupModel.Type.TERRITORY_WHITE;
			else if (key.equals(TERRITORY_BLACK))
				type = MarkupModel.Type.TERRITORY_BLACK;
			else
				type = MarkupModel.Type.UNKNOWN;

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
			} else {
				Value.ValueList vl = AbstractValue.createValueList();
				vl.add(value);
				vl.add(p.getValue());
				setValue(vl);
			}
		}

		public MarkupModel.Type getType()
		{
			return type;
		}
	}

	public static class Label extends Markup
	{
		/** See {@link Property#Property(Property.Key)}. */
		public Label(Key key)
		{
			super(key);
			this.type = MarkupModel.Type.LABEL;
		}

		public void setValue(Value vl)
		{
			super.setValue(vl);
		}
	}

	/**
	 * A marker interface for ineritable properties. Certain properties are
	 * "inherited" by child nodes according to the SGF spec - this interface
	 * markes these properties.
	 * 
	 * @see <a href="http://www.red-bean.com/sgf/">SGF Specification</a>
	 */
	public interface Inheritable
	{
	}

	/**
	 * A base class for game info properties.
	 * 
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
	 * 
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
	 * 
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
			//gameTree.setCharset(vl.toString());
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
			if (v instanceof Value.ValueList) {
				Value.ValueList vl = (Value.ValueList) v;
				if (vl.size() == 1) {
					setValue((Value.Number) vl.get(0));
				} else
					throw new IllegalArgumentException(
							"Cannot set value to a ValueList with length != 1");
			} else
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
			if (v instanceof Value.ValueList) {
				Value.ValueList vl = (Value.ValueList) v;
				if (vl.size() == 1) {
					setValue((Value.Number) vl.get(0));
				} else
					throw new IllegalArgumentException(
							"Cannot set value to a ValueList with length != 1");
			} else
				super.setValue((Value.Number) v);
		}
	}

	/** The SGF Property AddBlack. */
	@SGFProperty(propertyClass = AddStones.class, priority = 62)
	public static final Key ADD_BLACK = new Key("AB");

	/** The SGF Property AddEmpty. */
	@SGFProperty(propertyClass = AddStones.class, priority = 61)
	public final static Key ADD_EMPTY = new Key("AE");

	/** The SGF Property AddWhite. */
	@SGFProperty(propertyClass = AddStones.class, priority = 62)
	public final static Key ADD_WHITE = new Key("AW");

	/** The SGF Property APplication. */
	@SGFProperty(propertyClass = Root.class, priority = 91)
	public final static Key APPLICATION = new Key("AP");

	/** The SGF Property Black. */
	@SGFProperty(propertyClass = Move.class, priority = 60)
	public final static Key BLACK = new Key("B");

	/** The SGF Property BlackRank. */
	@SGFProperty(propertyClass = GameInfo.class, priority = 85)
	public final static Key BLACK_RANK = new Key("BR");

	/** The SGF Property ChAracterset. */
	@SGFProperty(propertyClass = Charset.class, priority = 0)
	public final static Key CHARACTER_SET = new Key("CA");

	/** The SGF Property CiRcle. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key CIRCLE = new Key("CR");

	/** The SGF Property Comment. */
	@SGFProperty(propertyClass = Text.class)
	public final static Key COMMENT = new Key("C");

	/** The SGF Property DAte. */
	@SGFProperty(propertyClass = GameInfo.class, priority = 77)
	public final static Key DATE = new Key("DT");

	/** The SGF Property EVent. */
	@SGFProperty(propertyClass = GameInfo.class, priority = 88)
	public final static Key EVENT = new Key("EV");

	/** The SGF Property PlaCe. */
	@SGFProperty(propertyClass = GameInfo.class, priority = 89)
	public final static Key PLACE = new Key("PC");

	/** The SGF Property FiGure. */
	@SGFProperty(propertyClass = Property.class)
	public final static Key FIGURE = new Key("FG");

	/** The SGF Property FileFormat. */
	@SGFProperty(propertyClass = RootNumber.class, priority = 1)
	public final static Key FILE_FORMAT = new Key("FF");

	/** The SGF Property GaMe. */
	@SGFProperty(propertyClass = Root.class, priority = 2)
	public final static Key GAME = new Key("GM");

	/** The SGF Property GameName. */
	@SGFProperty(propertyClass = GameInfo.class)
	public final static Key GAME_NAME = new Key("GN");

	/** The SGF Property LaBel. */
	@SGFProperty(propertyClass = Label.class)
	public final static Key LABEL = new Key("LB");

	/** The SGF Property MArk. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key MARK = new Key("MA");

	/** The SGF Property MoveNumber. */
	@SGFProperty(propertyClass = SimpleNumber.class)
	public final static Key MOVE_NO = new Key("MN");

	/** The SGF Property Name. */
	@SGFProperty(propertyClass = Text.class)
	public final static Key NAME = new Key("N");

	/** The SGF Property PlayerBlack. */
	@SGFProperty(propertyClass = GameInfo.class)
	public final static Key PLAYER_BLACK = new Key("PB");

	/** The SGF Property PlayerWhite. */
	@SGFProperty(propertyClass = GameInfo.class)
	public final static Key PLAYER_WHITE = new Key("PW");

	/** The SGF Property REsult. */
	@SGFProperty(propertyClass = GameInfo.class, priority=75)
	public final static Key RESULT = new Key("RE");

	/** The SGF Property KoMi. */
	@SGFProperty(propertyClass = GameInfo.class, priority=78)
	public final static Key KOMI = new Key("KM");

	/** The SGF Property SiZe. */
	@SGFProperty(propertyClass = RootNumber.class)
	public final static Key SIZE = new Key("SZ");

	/** The SGF Property SQuare. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key SQUARE = new Key("SQ");

	/** The SGF Property TerritoryWhite. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key TERRITORY_WHITE = new Key("TW");

	/** The SGF Property TerritoryBlack. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key TERRITORY_BLACK = new Key("TB");

	/** The SGF Property TRiangle. */
	@SGFProperty(propertyClass = Markup.class)
	public final static Key TRIANGLE = new Key("TR");

	/** The SGF Property USer. */
	@SGFProperty(propertyClass = GameInfo.class)
	public final static Key USER = new Key("US");

	/** The SGF Property VieW. */
	@SGFProperty(propertyClass = View.class)
	public final static Key VIEW = new Key("VW");

	/** The SGF Property White. */
	@SGFProperty(propertyClass = Move.class, priority = 60)
	public final static Key WHITE = new Key("W");

	/** The SGF Property WhiteRank. */
	@SGFProperty(propertyClass = GameInfo.class)
	public final static Key WHITE_RANK = new Key("WR");

	private final static Key[] addStoneKeys = { ADD_BLACK, ADD_WHITE, ADD_EMPTY };

	/**
	 * The properties AddWhite, AddBlack and AddEmpty.
	 */
	public static final List addStoneProperties = Arrays.asList(addStoneKeys);

	static Logger logger = Logger.getLogger(Property.class.getName());

	static Set InheritableProperties = new TreeSet();

	private Value value = null;
	private Key key = null;

	/**
	 * Create a property with a given key. This constructor is protected, use
	 * 
	 * @link{Property.createProperty(Key) . This constructor should only be used
	 *                                    by the {@link Factory} class.
	 * 
	 * @param key
	 *            {@link Key} identifying the property.
	 */
	Property(Key key)
	{
		this.key = key;
	}

	/**
	 * Set the {@link Value} of the property.
	 * 
	 * @param value
	 *            the value to set
	 */
	public void setValue(Value value)
	{
		this.value = value;
	}

	/**
	 * Set the {@link Value} of the property to String <code>newValue</code>.
	 * This operation is optional, i.e. not all sub-classes might support it.
	 * The default implemantion in this class just throws an
	 * UnsupportedOperationException.
	 * 
	 * @throws java.lang.UnsupportedOperationException
	 * @param value
	 *            the value to set
	 */
	public void setValue(String newValue)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the value of this proprty.
	 * 
	 * @return the Value of this property.
	 */
	public Value getValue()
	{
		if (value == null) {
			logger.warning("Property " + key + ": no value");
		}
		return value;
	}

	/**
	 * Get the key of this proprty.
	 * 
	 * @return the Key of this property.
	 */
	public Key getKey()
	{
		return key;
	}

	/**
	 * Get the name, i.e. the string representation of the key of this proprty.
	 * 
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

	public Value.PointList getPointList()
	{
		Value value = getValue();
		// logger.finest("getPointList: " + value + ": " +
		// value.getClass().getName());
		if (value instanceof Value.ValueList) {
			Value.ValueList list = (Value.ValueList) value;
			// logger.finest("getPointList: " + list.size());
			assert list.size() == 1;
			return (Value.PointList) list.get(0);
		}
		return (Value.PointList) value;
	}

	public static String formatResult(Object o)
	{
		return formatResult((Property) o);
	}

	final static Pattern pattern = Pattern
			.compile("([wW]|[bB])\\+(resign|R|time|forfeit|(\\d+(?:[.,]\\d*)))|([Jj]igo)");

	public static String formatResult(Property p)
	{
		if (!p.getKey().equals(Property.RESULT))
			throw new IllegalArgumentException("Argument is no result");
		else {
			Matcher m = pattern.matcher(p.getValue().toString());

			if (!m.matches()) {
				logger.warning("Unrecognized result value: "
						+ p.getValue().toString());
				return p.getValue().toString();
			} else {
				if (m.group(4) != null)
					return "Jigo";
				else {
					String winner = m.group(1);
					String looser = null;
					String special = m.group(2);
					String margin = m.group(3);
					if (winner.equals("w") || winner.equals("W")) {
						winner = "Wei\u00df";
						looser = "Schwarz";
					} else if (winner.equals("b") || winner.equals("B")) {
						winner = "Schwarz";
						looser = "Wei\u00df";
					} else
						assert false;

					if (margin == null) {
						if (special.equals("resign") || special.equals("R"))
							return winner + " gewinnt durch Aufgabe";
						else if (special.equals("time"))
							return looser
									+ " verliert durch Zeit\u00fcberschreitung";
						else
							return winner + " gewinnt";
					} else
						return winner + " gewinnt mit " + margin + " Punkten";
				}
			}
		}

	}
}
