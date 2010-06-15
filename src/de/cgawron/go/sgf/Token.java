/*
 *
 * $Id: Token.java 34 2003-07-12 16:42:17Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go.sgf;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;

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
	logger.debug("Token created: " + this);
    }

    public String toString()
    {
        return "Token #" + m_index + ": " + m_text + " (line " + m_line + ")";
    }
}
