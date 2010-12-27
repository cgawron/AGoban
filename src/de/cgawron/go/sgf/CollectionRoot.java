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
import java.io.PrintWriter;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * An instance of this class represents a node in a sgf game tree.
 * @author Christian Gawron
 */
public class CollectionRoot extends RootNode
{
    public CollectionRoot(GameTree gameTree)
    {
        super(gameTree);
    }

    public void write(PrintWriter out)
    {
	Iterator it = getChildren().iterator();
	while (it.hasNext())
	{
	    Node node = (Node)it.next();
	    node.write(out);
	}
    }

}
