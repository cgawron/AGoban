/*
 *
 * $Id: MementoOriginator.java 15 2003-03-15 23:25:52Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.util;

public interface MementoOriginator
{
    class InvalidMementoException extends RuntimeException
    {
    }

    void setMemento(Memento memento) throws InvalidMementoException;
    Memento createMemento();
}
