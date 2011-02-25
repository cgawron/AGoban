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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;

/**
 * An instance of this class represents a node in a sgf game tree.
 * 
 * @author Christian Gawron
 */
public class CollectionRoot extends RootNode {
	private static Logger logger = Logger.getLogger(CollectionRoot.class
			.getName());

	public CollectionRoot(GameTree gameTree) {
		super(gameTree);
		add(gameTree.getRoot());
	}

	public void write(PrintWriter out) {
		logger.info("CollectionRoot.write: " + this);
		for (Node node : getChildren()) {
			logger.info("CollectionRoot.write:" + node);
			node.write(out);
		}
	}

}
