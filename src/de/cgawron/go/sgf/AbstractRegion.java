/*
 *
 * $Id: AbstractRegion.java 154 2004-12-17 23:51:02Z cgawron $
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


/**
 * An abstract region providing property change support
 * @author Christian Gawron
 * @version $Revision: 154 $
 */
public abstract class AbstractRegion implements Region
{
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is registered for all properties.
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered for all properties.
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener for a specific property. The listener will be invoked only when a call on firePropertyChange
     * names that specific property.
     * @param propertyName The name of the property to listen on
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property.
     * @param propertyName The name of the property that was listened on
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * This is merely a convenience wrapper around the more general firePropertyChange method that takes Object values.
     * No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, int oldValue, int newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if old and new are equal and non-null.
     * This is merely a convenience wrapper around the more general firePropertyChange method that takes Object values.
     * No event is fired if old and new are equal and non-null.
     * @param propertyName The programmatic name of the property that was changed
     * @param oldValue The old value of the property
     * @param newValue The new value of the property.
     */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Fires an existing PropertyChangeEvent to any registered listeners.
     * No event is fired if the given event's old and new values are equal and non-null.
     * @param evt The PropertyChangeEvent object.
     */
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        pcs.firePropertyChange(evt);
    }

    /**
     * Checks if there are any listeners for a specific property.
     * @param propertyName The name of the property.
     * @return <code>true</code>if there are one or more listeners for the given property
     */
    public boolean hasListeners(String propertyName)
    {
        return pcs.hasListeners(propertyName);
    }

    public AbstractRegion()
    {
    }

    @Override
	public Object clone() throws CloneNotSupportedException
    {
	return super.clone();
    }
}
