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

class NodeMemento implements Memento {
	private static Logger logger = Logger
			.getLogger(NodeMemento.class.getName());
	private Map<Property.Key, Property> properties;
	private Node parent;
	private List<Node> children;

	public NodeMemento(Node node) {
		if (node != null) {
			this.parent = (Node) node.getParent();
			this.children = null;
			if (node.getChildren() != null)
				this.children = new LinkedList<Node>(node.getChildren());
			properties = new TreeMap<Property.Key, Property>();
			Iterator it = node.keySet().iterator();
			while (it.hasNext()) {
				Property.Key key = (Property.Key) it.next();
				Property prop = (Property) node.get(key);
				properties.put(key, (Property) prop.clone());
			}
			logger.info("NodeMemento: Setting properties to " + properties);
		}
	}

	public Map<Property.Key, Property> getProperties() {
		return Collections.unmodifiableMap(properties);
	}

	public Node getParent() {
		return parent;
	}

	public List<Node> getChildren() {
		return children;
	}

	public String toString() {
		return "NodeMemento: properies=" + properties.toString() + " "
				+ super.toString() + ", children=" + children + ", parent="
				+ parent;
	}
}
