/*
 * Copyright (C) 2010 Christian Gawron
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
 */

package de.cgawron.go.sgf;

import java_cup.runtime.Symbol;

import java.util.logging.Logger;

class Token extends Symbol
{
	private static Logger logger = Logger.getLogger(Token.class.getName());

	public int m_index;
	public String m_text;
	public int m_line;
	public int m_charBegin;

	Token(int index, String text, int line, int charBegin)
	{
		this(index, text, line, charBegin, null);
	}

	Token(int index, String text, int line, int charBegin, Object o)
	{
		super(index, charBegin, charBegin + text.length(), null);
		m_index = index;
		m_text = new String(text);
		m_line = line;
		m_charBegin = charBegin;
		value = o;
	}

	public String toString()
	{
		return "Token #" + m_index + ": " + m_text + " (line " + m_line + ")";
	}
}
