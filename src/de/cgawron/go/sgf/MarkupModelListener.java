/*
 *
 * $Id: MarkupModelListener.java 15 2003-03-15 23:25:52Z cgawron $
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
import de.cgawron.go.GobanEvent;
import de.cgawron.go.GobanListener;

/** A MarkupModelListener gets informed when the model changes, i.e. when a region is set or a letter is added. */
public interface MarkupModelListener extends GobanListener
{
    /**
     * The region (or VieW in SGF speak) of the model has changed.
     * @param e gawron.go.GobanEvent
     */
    void regionChanged(GobanEvent e);
}
