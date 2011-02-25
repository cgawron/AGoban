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

import de.cgawron.go.*;
import java.beans.PropertyChangeListener;

/**
 * A Region represents a region, i.e. a (usually connected) set of points, on a
 * Goban
 */
public interface Region extends Cloneable {
	/**
	 * Returns a <code>Shape</code> describing the region. The coordinates of
	 * the shape are board coordinates, i.e. values between 0 and the board
	 * size.
	 * 
	 * @return the <code>Shape</code>
	 */
	// Shape getShape();

	Value.PointList getPointList();

	/**
	 * Set the region to the rectangle described by the parameters.
	 * 
	 * @param xMin
	 *            the minimal x coordinate of the rectangle
	 * @param yMin
	 *            the minimal y coordinate of the rectangle
	 * @param xMax
	 *            the maximal x coordinate of the rectangle
	 * @param yMax
	 *            the maximal y coordinate of the rectangle
	 */
	void set(short xMin, short yMin, short xMax, short yMax);

	/**
	 * Adds a PropertyChangeListener to the listener list. The listener is
	 * registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	void addPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Removes a PropertyChangeListener from the listener list. This removes a
	 * PropertyChangeListener that was registered for all properties.
	 * 
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	void removePropertyChangeListener(PropertyChangeListener listener);

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
	void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener);

	/**
	 * Removes a PropertyChangeListener for a specific property.
	 * 
	 * @param propertyName
	 *            The name of the property that was listened on
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener);

	public Object clone() throws CloneNotSupportedException;
}
