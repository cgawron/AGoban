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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * An instance of this class represents a node in a sgf game tree.
 * @author Christian Gawron
 */
public class RootNode extends Node implements PropertyChangeListener
{
    private static Logger logger = Logger.getLogger(RootNode.class.getName());
   
    RootNode(GameTree gameTree)
    {
        super(gameTree);
    }

    protected RootNode(Node n /*, GameTree gameTree */)
    {
        super(n);
	Iterator it = n.getChildren().iterator();
	while (it.hasNext())
	    add((Node) it.next());
    }

    public void write(PrintWriter out)
    {
	out.println("(");
	super.write(out);
	out.println(")");
    }


    /**
     * This method is called when a property in some child node changes. 
     * It just calls {@link Node#firePropertyChange} to signal the change to PropertyChangeListeners registered to this RootNode.
     * @param e Describes the change.
     */
    public void propertyChange(PropertyChangeEvent e)
    {
        Object name = e.getPropertyName();
	if (logger.isLoggable(Level.FINE))
	    logger.fine("property changed: " + (e != null ? e.toString() : "<null>"));
        firePropertyChange(e);
    }

    /** Check if the game rooted at this node represents a game.
     * How can problems be differentiated from games? The most obvious approach is to check wether the first node
     * is a move or a board setup. However, this fails for handicap and ancient chinese games. Just looking for game info 
     * would also fail if there is none. We use the following approach:
     * A tree is <bf>not</bf> a game if it starts with a board setup node containing more than 4 stones and the tree has a 
     * depth of less than 70.
     * @return true if this tree represents a game 
     */
    public boolean isGame()
    {
	Node node = this;
	while (!node.isBoardSetup() && !node.isMove()) {
	    if (node.getChildCount() == 0)
		return false;
	    else
		node = (Node) node.getChildAt(0);
	}

	if (node.isMove())
	    return true;
	else
	{
	    Value.PointList ab = node.getPointList(Property.ADD_BLACK);
	    Value.PointList aw = node.getPointList(Property.ADD_WHITE);
	    int added = 0;
	    if (ab != null)
		added += ab.size();
	    if (aw != null)
		added += aw.size();
	
	    if (added > 4) 
	    {
		int i = 0;
		while (node.getChildCount() > 0) {
		    i++;
		    node = (Node) node.getChildAt(0);
		}
		if (i >= 70)
		    return true;
		else 
		    return false;
	    }
	    else
		return true;
	}
    }

    /** Get game signature of the game tree starting at this node. 
     * If the game tree does not represent a game but e.g. a problem, the Zobrist hash of the first node 
     * with a board setup is returned instead.
     * @return game id 
     */
    public String getSignature()
    {
	Node node = this;
	while (!node.isBoardSetup() && !node.isMove()) {
	    if (node.getChildCount() == 0)
		return null;
	    else
		node = (Node) node.getChildAt(0);
	}
	if (!isGame())
	    return Integer.toString(node.getGoban().zobristHash());
	else {
	    while (!node.isMove()) {
		if (node.getChildCount() == 0)
		    return null;
		else
		    node = (Node) node.getChildAt(0);
	    }
	    int i = 1;
	    char[] sig = "------_------".toCharArray();
	    while (i<71) {
		Point pt;

		if (node.getChildCount() == 0)
		    break;
		else
		    node = (Node) node.getChildAt(0);
		if (node.isMove()) 
		    i++;
		
		switch (i) {
		case 20:
		case 40:
		case 60:
		    pt = node.getPoint(Property.WHITE);
		    if (pt == null)
			pt = node.getPoint(Property.BLACK);
		    if (pt != null) {
			sig[i/10-2] = (char) ('a' + pt.getX());
			sig[i/10-1] = (char) ('a' + pt.getY());
		    }
		    break;
		case 31:
		case 51:
		case 71:
		    pt = node.getPoint(Property.BLACK);
		    if (pt == null)
			pt = node.getPoint(Property.WHITE);
		    if (pt != null) {
			sig[i/10+4] = (char) ('a' + pt.getX());
			sig[i/10+5] = (char) ('a' + pt.getY());
		    }
		    break;
		}
	    }
	    return new String(sig);
	}
    }
}
