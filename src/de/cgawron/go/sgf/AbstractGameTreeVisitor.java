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
