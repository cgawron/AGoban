/*
 *
 * $Id: ParseError.java 93 2004-08-07 19:33:46Z  $
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

public class ParseError extends RuntimeException 
{
    int line;
    int column;
    String text;
    String message;

    public ParseError(String message, Object info)
    {
	super(message + " at " + info);
	this.message = message + " at " + info;
    }

    public ParseError(String message, InputPosition position, Object info)
    {
	super(message + " at " + info);
	this.message = message + " at " + info;
	text = position.getCurrentLine();
	line = position.getLine();
	column = position.getColumn();
    }

    public String getMessage()
    {
	int i;
	StringBuffer buffer = new StringBuffer();
	
	buffer.append(message).append(" at line ").append(line);
	buffer.append(", column").append(column).append(":\n");
	buffer.append(text).append("\n");
	for (i=1; i<column; i++)
	    buffer.append(' ');
	buffer.append('^');
	
	return buffer.toString();
    }
}
