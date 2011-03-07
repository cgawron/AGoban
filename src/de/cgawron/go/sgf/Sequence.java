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

import java.util.Collection;
import java.util.Iterator;

public class Sequence extends Node
{
	Node first = null;
	Node last = null;
	int len = 0;

	public Sequence(Node n)
	{
		super(n);
		first = this;
		last = this;
		len = 1;
	}

	public boolean addAll(Collection<Node> c)
	{
		Iterator<Node> i = c.iterator();
		while (i.hasNext()) {
			last.add(i.next());
		}
		return true;
	}

	public boolean append(Node o)
	{
		boolean b = false;
		if (last == this)
			b = super.add(o);
		else
			b = last.add(o);
		last = (Node) o;
		len++;
		return b;
	}

	public int size()
	{
		return len;
	}

	public Node getFirst()
	{
		return first;
	}

	public String toString()
	{
		String s = "Sequence: length " + size() + ": ";
		s += super.toString();
		return s;
	}
}
