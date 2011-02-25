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

import de.cgawron.util.Memento;

import java.util.*;
import java.util.logging.Logger;

class GameTreeMemento implements Memento {
	private static Logger logger = Logger
			.getLogger(NodeMemento.class.getName());
	private Map<Node, Memento> nodeMementos = new HashMap<Node, Memento>();

	public GameTreeMemento(GameTree gameTree) {
		Iterator it = new TreeIterator(gameTree);
		while (it.hasNext()) {
			Node n = (Node) it.next();
			nodeMementos.put(n, n.createMemento());
		}
	}

	void setMemento(Node n) {
		logger.info("GameTreeMement: setMemento: " + n);
		Memento m = (Memento) nodeMementos.get(n);
		if (m != null)
			n.setMemento(m);
		else
			logger.severe("No Memento stored for Node " + n);
		Iterator it = n.getChildren().iterator();
		while (it.hasNext()) {
			setMemento((Node) it.next());
		}
	}

	public String toString() {
		return "[GameTreeMemento nodeMementos=" + nodeMementos + "]";
	}
}
