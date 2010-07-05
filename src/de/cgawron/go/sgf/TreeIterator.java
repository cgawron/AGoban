/*
 *
 * $Id: TreeIterator.java 363 2005-12-15 23:28:33Z cgawron $
 *
 * � 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go.sgf;

import java.util.*;

import org.apache.log4j.Logger;

public class TreeIterator<N extends TreeNode> implements Iterator<N>
{
    private static Logger logger = Logger.getLogger(TreeIterator.class.getName());
    private Iterator<N> iterator;

    private TreeIterator()
    {
    }

    public TreeIterator(N node)
    {
        iterator = new DepthFirstIterator<N>(node);
    }

    public TreeIterator(TreeModel model)
    {
        this((N) model.getRoot());
    }

    public boolean hasNext()
    {
        return iterator.hasNext();
    }

    public N next()
    {
        return iterator.next();
    }

    public void remove()
    {
        iterator.remove();
    }

    static class BreadthFirstIterator<N extends TreeNode> extends TreeIterator<N>
    {
        protected Queue<Iterator<N>> queue;

        public BreadthFirstIterator(N node)
        {
            queue = new LinkedList<Iterator<N>>();
            /* queue.enqueue(new EnumIterator(node.children()));*/
	    Collection<N> c = new ArrayList<N>();
	    c.add(node);
            queue.add(c.iterator());
        }

        public boolean hasNext()
        {
            return (!queue.isEmpty() && queue.peek().hasNext());
        }

        public N next()
        {
            Iterator<N> iter = queue.peek();
            N node = iter.next();
	    
	    List<N> tmp = new ArrayList<N>(); 
	    Enumeration<TreeNode> en = node.children();
	    while (en.hasMoreElements())
	    {
		TreeNode n = en.nextElement();
		tmp.add((N) n);
	    }
            Iterator<N> children = tmp.iterator();
	    
            logger.debug("BreadthFirstIterator " + this + ": node " + node);

            if (!iter.hasNext())
            {
                queue.remove();
            }
            if (children.hasNext())
            {
                queue.add(children);
            }
            return node;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        // A simple queue with a linked list data structure.
	/*
        final class Queue
        {
            QNode head; // null if empty
            QNode tail;

            final class QNode
            {
                public Object object;
                public QNode next; // null if end

                public QNode(Object object, QNode next)
                {
                    this.object = object;
                    this.next = next;
                }
            }


            public void enqueue(Object anObject)
            {
                if (head == null)
                {
                    head = tail = new QNode(anObject, null);
                }
                else
                {
                    tail.next = new QNode(anObject, null);
                    tail = tail.next;
                }
            }

            public Object dequeue()
            {
                if (head == null)
                {
                    throw new NoSuchElementException("No more elements");
                }

                Object retval = head.object;
                QNode oldHead = head;
                head = head.next;
                if (head == null)
                {
                    tail = null;
                }
                else
                {
                    oldHead.next = null;
                }
                return retval;
            }

            public Object firstObject()
            {
                if (head == null)
                {
                    throw new NoSuchElementException("No more elements");
                }

                return head.object;
            }

            public boolean isEmpty()
            {
                return head == null;
            }

        }
	*/
    }

    public static class MyOrderIterator<N extends TreeNode> extends TreeIterator<N>
    {
        protected Stack<N> stack;

        public MyOrderIterator(N node)
        {
            stack = new Stack<N>();
            /* queue.enqueue(new EnumIterator(node.children()));*/
            stack.push(node);
        }

        public boolean hasNext()
        {
            return !stack.empty();
        }

        public N next()
        {
            N node = stack.pop();
	    Enumeration<TreeNode> enumeration = node.children();
	    
	    while (enumeration.hasMoreElements())
		stack.push((N) enumeration.nextElement());

            return node;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }


    public static class DepthFirstIterator<N extends TreeNode> extends TreeIterator<N>
    {
        protected Stack<Iterator<N>> stack;

        public DepthFirstIterator(N node)
        {
            stack = new Stack<Iterator<N>>();
	    Collection<N> c = new ArrayList<N>();
	    if (node != null)
		c.add(node);
            stack.push(c.iterator());
        }

        public boolean hasNext()
        {
            return !stack.empty() && stack.peek().hasNext();
        }

        public N next()
        {
            Iterator<N> it = stack.peek();
            N node = it.next();
            logger.debug("DepthFirstIterator " + this + ": node " + node);
            Iterator<N> children = new EnumIterator(node.children());
            if (!it.hasNext())
            {
                stack.pop();
            }
            if (children.hasNext())
            {
                stack.push(children);
            }
            return node;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    public static class PostorderIterator<N extends TreeNode> extends TreeIterator<N>
    {
        protected N root;
        protected Iterator<N> children;
        protected Iterator<N> subtree;

        public PostorderIterator(N rootNode)
        {
            root = rootNode;
            children = new EnumIterator(root.children());
	    List<N> empty = Collections.emptyList(); 
            subtree = empty.iterator();
        }

        public boolean hasNext()
        {
            return root != null;
        }

        public N next()
        {
            N retval;

            if (subtree.hasNext())
            {
                retval = subtree.next();
            }
            else if (children.hasNext())
            {
                subtree = new PostorderIterator<N>(children.next());
                retval = subtree.next();
            }
            else
            {
                retval = root;
                root = null;
            }
            return retval;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    static class EnumIterator implements Iterator
    {
        Enumeration e;

        EnumIterator(Enumeration e)
        {
            this.e = e;
        }

        public boolean hasNext()
        {
            return e.hasMoreElements();
        }

        public Object next()
        {
            return e.nextElement();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}