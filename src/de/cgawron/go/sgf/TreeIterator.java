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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TreeIterator<N extends TreeNode> implements Iterator<N> {
	private static Logger logger = Logger.getLogger(TreeIterator.class
			.getName());
	private Iterator<N> iterator;

	private TreeIterator() {
	}

	public TreeIterator(N node) {
		iterator = new PreorderIterator<N>(node);
	}

	public TreeIterator(TreeModel model) {
		this((N) model.getRoot());
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public N next() {
		return iterator.next();
	}

	public void remove() {
		iterator.remove();
	}

	static class BreadthFirstIterator<N extends TreeNode> extends
			TreeIterator<N> {
		protected Queue<Iterator<N>> queue;

		public BreadthFirstIterator(N node) {
			queue = new LinkedList<Iterator<N>>();
			/* queue.enqueue(new EnumIterator(node.children())); */
			Collection<N> c = new ArrayList<N>();
			c.add(node);
			queue.add(c.iterator());
		}

		public boolean hasNext() {
			return (!queue.isEmpty() && queue.peek().hasNext());
		}

		public N next() {
			Iterator<N> iter = queue.peek();
			N node = iter.next();

			List<N> tmp = new ArrayList<N>();
			Enumeration<TreeNode> en = node.children();
			while (en.hasMoreElements()) {
				N n = (N) en.nextElement();
				tmp.add((N) n);
			}
			Iterator<N> children = tmp.iterator();

			logger.fine("BreadthFirstIterator " + this + ": node " + node);

			if (!iter.hasNext()) {
				queue.remove();
			}
			if (children.hasNext()) {
				queue.add(children);
			}
			return node;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * A PreorderIterator iterates the nodes in the following order:
	 * <ul>
	 * <li>Visit the root.
	 * <li>Visit the subtrees rooted at each children in order.
	 * </ul>
	 * The method {@link #endNode(N node)} is called after the last child of a
	 * node has been visited.
	 */
	public static class PreorderIterator<N extends TreeNode> extends
			TreeIterator<N> {
		private class Pair<N> {
			N node;
			Iterator<N> iterator;

			public Pair(N node, Iterator<N> iterator) {
				this.node = node;
				this.iterator = iterator;
			}

		}

		protected Stack<Pair<N>> stack;

		public PreorderIterator(N node) {
			stack = new Stack<Pair<N>>();
			Collection<N> c = new ArrayList<N>();
			if (node != null)
				c.add(node);
			Pair<N> pair = new Pair(null, c.iterator());
			stack.push(pair);
		}

		public boolean hasNext() {
			if (stack.empty())
				return false;

			Iterator<N> it = stack.peek().iterator;
			while (!it.hasNext()) {
				N current = stack.peek().node;
				endNode(current);
				stack.pop();
				if (stack.empty())
					return false;
				current = stack.peek().node;
				it = stack.peek().iterator;
			}
			return true;
		}

		public N next() {
			N current = stack.peek().node;
			Iterator<N> it = stack.peek().iterator;
			N node = it.next();

			if (logger.isLoggable(Level.FINE))
				logger.fine("PreorderIterator " + this + ": node " + node);
			Iterator<N> children = new EnumIterator(node.children());
			if (children.hasNext()) {
				Pair pair = new Pair(node, children);
				stack.push(pair);
			}

			return node;
		}

		public void endNode(N parent) {
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public static class PostorderIterator<N extends TreeNode> extends
			TreeIterator<N> {
		protected N root;
		protected Iterator<N> children;
		protected Iterator<N> subtree;

		public PostorderIterator(N rootNode) {
			root = rootNode;
			children = new EnumIterator(root.children());
			List<N> empty = Collections.emptyList();
			subtree = empty.iterator();
		}

		public boolean hasNext() {
			return root != null;
		}

		public N next() {
			N retval;

			if (subtree.hasNext()) {
				retval = subtree.next();
			} else if (children.hasNext()) {
				subtree = new PostorderIterator<N>(children.next());
				retval = subtree.next();
			} else {
				retval = root;
				root = null;
			}
			return retval;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static class EnumIterator<N extends TreeNode> implements Iterator<N> {
		Enumeration<N> e;

		EnumIterator(Enumeration<N> e) {
			this.e = e;
		}

		public boolean hasNext() {
			return e.hasMoreElements();
		}

		public N next() {
			return e.nextElement();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
