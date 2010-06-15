/*
 *
 * $Id: TreeVisitor.java 98 2004-08-26 21:46:15Z  $
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

import java.util.Iterator;

/**
 * Visits all nodes of a TreeModel.
 */
public abstract class TreeVisitor<T extends TreeModel, N extends TreeNode>
{
    protected T model = null;
    protected Iterator<N> iterator = null;

    /**
     * This default constructor does not set a TreeModel. 
     */
    public TreeVisitor()
    {
    }

    /**
     * Construct a visitor for a given TreeModel.
     * @param model - the TreeModel to visit
     */
    public TreeVisitor(T model)
    {
	setModel(model);
    }

    /**
     * Construct a visitor for the subtree of <code>model</code> rooted at <code>subRoot</code>. 
     * @param model - the TreeModel 
     * @param subRoot - root node of the subtree to visit 
     */
    public TreeVisitor(T model, N subRoot)
    {
        this.model = model;
        iterator = new TreeIterator.BreadthFirstIterator<N>(subRoot);
    }

    /**
     * Set a new TreeModel for the visitor. 
     * This method also reinitializes the {@link #iterator}.
     * @param model the new TreeModel
     */
    public void setModel(T model)
    {
        this.model = model;
        iterator = getIterator();
    }

    protected TreeIterator<N> getIterator()
    {
        return new TreeIterator.DepthFirstIterator<N>((N) model.getRoot());
    }

    /**
     * This method is called for every node visited.
     * @param o - current node visited.
     */
    protected abstract void visitNode(Object o) throws Exception;

    protected void initialize()
    {
    }

    /**
     * Visit the nodes of the {@link #model}.
     */
    public void visit()
    {
        initialize();
        while (iterator.hasNext())
        {
	    try {
		visitNode(iterator.next());
	    }
	    catch (Exception ex)
	    {
		throw new RuntimeException(ex);
	    }
        }
    }
}
