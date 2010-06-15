/*
 *
 * $Id: GobanModelListener.java 15 2003-03-15 23:25:52Z cgawron $
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

import java.util.EventListener;

/** A GobanListener gets informed when the model changes, i.e. when a stone is added or removed. */
public interface GobanListener extends EventListener
{
    /**
     * Insert the method's description here. Creation date: (04/11/00 10:18:51)
     * @param event GobanEvent
     */
    void modelChanged(GobanEvent event);

    /**
     * Insert the method's description here. Creation date: (03/23/00 23:40:43)
     * @param event GobanEvent
     */
    void stoneAdded(GobanEvent event);

    /**
     * Insert the method's description here. Creation date: (03/23/00 23:40:43)
     * @param event GobanEvent
     */
    void stonesRemoved(GobanEvent event);
}
