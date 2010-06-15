/*
 *
 * $Id: AbstractGameTreeVisitor.java 99 2004-08-28 22:22:18Z  $
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
 * This class provides an abstract base class for the {@link GameTreeVisitor} interface.
 * A child class only has to implement the {@link #visitNode} method. 
 */
public abstract class AbstractGameTreeVisitor 
    extends TreeVisitor<GameTree, Node> 
    implements GameTreeVisitor, GameTreeCommand 
{
    /**
     * Constructs a AbstractGameTreeVisitor without an underlying {@link GameTree}.
     */
    public AbstractGameTreeVisitor()
    {
    }
    
    /**
     * Constructs a AbstractGameTreeVisitor and sets the underlying {@link GameTree}.
     * @param gameTree - the {@link GameTree} to set.
     */
    public AbstractGameTreeVisitor(GameTree gameTree)
    {
	setModel(gameTree);
    }

    /**
     * Runs the {@link visit} on the gameTree.
     * @param gameTree - {@link GameTree} to vist.
     * @throws Exception - if any {@link visitNode} throws an exception, it will be reported here.
     */
    public void run(GameTree gameTree) throws Exception
    {
	setModel(gameTree);
	visit();
    }

    /**
     * {@inheritDoc}.
     * @throws Exception - an implementing method may throw any exception. 
     */
    public abstract void visitNode(Object o) throws Exception;
}
