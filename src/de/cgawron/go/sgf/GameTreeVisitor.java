/*
 *
 * $Id: GameTreeVisitor.java 38 2003-09-12 19:22:24Z cgawron $
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

/** 
 * A GameTreeVisitor calls {@link #visitNode} for all nodes of a {@link GameTree}.
 * @author Christian Gawron
 */
public interface GameTreeVisitor 
{
    void visitNode(Object o) throws Exception;
}
