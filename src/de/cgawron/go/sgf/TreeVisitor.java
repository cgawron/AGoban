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
        return new TreeIterator.PreorderIterator<N>((N) model.getRoot());
    }

    /**
     * This method is called for every node visited.
     * @param o - current node visited.
     */
    protected abstract void visitNode(N o) throws Exception;

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
