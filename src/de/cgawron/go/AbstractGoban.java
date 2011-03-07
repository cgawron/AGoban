/*
 * Copyright (C) 2010 Christian Gawron
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
 */

package de.cgawron.go;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A {@link Goban} providing <code>PropertyChangeSupport</code>.
 * 
 * @author Christian Gawron
 * @version $Id: AbstractGoban.java 155 2005-01-04 10:47:49Z cgawron $
 * @see PropetrtyChangeSupport
 */
public abstract class AbstractGoban implements Goban
{
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * Adds a PropertyChangeListener to the listener list. The listener is
	 * registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Removes a PropertyChangeListener from the listener list. This removes a
	 * PropertyChangeListener that was registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Adds a PropertyChangeListener for a specific property. The listener will
	 * be invoked only when a call on firePropertyChange names that specific
	 * property.
	 * 
	 * @param propertyName
	 *            The name of the property to listen on
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener)
	{
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Removes a PropertyChangeListener for a specific property.
	 * 
	 * @param propertyName
	 *            The name of the property that was listened on
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener)
	{
		pcs.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * Reports a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null.
	 * 
	 * @param propertyName
	 *            The programmatic name of the property that was changed
	 * @param oldValue
	 *            The old value of the property
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue)
	{
		pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Reports a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null. This is merely a convenience
	 * wrapper around the more general firePropertyChange method that takes
	 * Object values. No event is fired if old and new are equal and non-null.
	 * 
	 * @param propertyName
	 *            The programmatic name of the property that was changed
	 * @param oldValue
	 *            The old value of the property
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, int oldValue,
			int newValue)
	{
		pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Reports a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null. This is merely a convenience
	 * wrapper around the more general firePropertyChange method that takes
	 * Object values. No event is fired if old and new are equal and non-null.
	 * 
	 * @param propertyName
	 *            The programmatic name of the property that was changed
	 * @param oldValue
	 *            The old value of the property
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, boolean oldValue,
			boolean newValue)
	{
		pcs.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Fires an existing PropertyChangeEvent to any registered listeners. No
	 * event is fired if the given event's old and new values are equal and
	 * non-null.
	 * 
	 * @param evt
	 *            The PropertyChangeEvent object.
	 */
	public void firePropertyChange(PropertyChangeEvent evt)
	{
		pcs.firePropertyChange(evt);
	}

	/**
	 * Checks if there are any listeners for a specific property.
	 * 
	 * @param propertyName
	 *            The name of the property
	 * @return <code>true</code>if there are one or more listeners for the given
	 *         property
	 */
	public boolean hasListeners(String propertyName)
	{
		return pcs.hasListeners(propertyName);
	}

	/**
	 * This method should be considered private. It is only part of this
	 * interface for implementation reasons.
	 * 
	 * @todo Maybe it would be better to move this to AbstractGoban.
	 */
	abstract int _hash(Symmetry s);

	@Override
	abstract public Goban clone() throws CloneNotSupportedException;
}
