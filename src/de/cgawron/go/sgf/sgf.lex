/**
 *
 * $Id: sgf.lex,v 1.7.2.6 2003/02/16 14:23:38 cgawron Exp $
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

import java.lang.System;
import java.io.IOException;
import de.cgawron.util.MiscEncodingReader;
import org.apache.log4j.Logger;

%%

%public
%implements java_cup.runtime.Scanner, InputPosition
%function next_token
%type java_cup.runtime.Symbol
%unicode
%char
%line
%unicode
//%debug

%eof{
  logger.info("eof reached");
%eof}

%eofclose

%state TOP
%state NODE
%state MOVE
%state POINTS
%state LABEL
%state NUMBER
%state TEXT
%state PROPERTY

%{
    private static Logger logger = Logger.getLogger(Yylex.class);
    
    private int level = 0;

    public int getLine()
    {
	return yyline+1;
    }

    public int getColumn()
    {
	return yycolumn+1;
    }

    public String getCurrentLine()
    {
	int beg = yy_currentPos; 
	int end = yy_currentPos; 
	while (beg > 0 && yy_buffer[beg-1] != '\n') beg--;
	while (end < yy_endRead && yy_buffer[end] != '\n') end++;
	
	return new String(yy_buffer, beg, end-beg);
    }


    private static Yylex theLexer = null;

    public static void setCharset(String charSetName)
    {
	if (theLexer != null)
	    theLexer._setCharset(charSetName);
    }

    public void _setCharset(String charSetName)
    {
	MiscEncodingReader mer = (MiscEncodingReader) yy_reader;
	try {
	  //mer.setCharset(charSetName, yy_currentPos);
	  mer.setCharset(charSetName, yy_markedPos);
	  yy_reader = null;
	  yyclose();
	  yy_reader = mer;
	  yy_atEOF  = false;
	  yy_endRead = yy_startRead = 0;
	  yy_currentPos = yy_markedPos = yy_pushbackPos = 0;
	}
	catch (IOException err) {
	  throw new Error(err);
	}
    }
%}

%init{
    theLexer = this;
%init}

%%


<YYINITIAL>  "("/[ \r\n\t]*";"   
{
    yybegin(TOP);
    level++;
    return (new Token(Symbols.Open, yytext(), yyline, yychar));
}

<YYINITIAL>  .   
{
}

";"    
{
    yybegin(TOP);
    return (new Token(Symbols.Semi, yytext(), yyline, yychar)); 
}

<TOP> "("    { level++; return (new Token(Symbols.Open, yytext(), yyline, yychar)); }
<TOP> ")"    { level--; /*if (level==0) yybegin(YYINITIAL); */ return (new Token(Symbols.Close, yytext(), yyline, yychar)); }

<MOVE> "]" 
{
    yybegin(TOP);
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createValue(null));
}

<MOVE> [a-z][a-z]"]" 
{
    yybegin(TOP);
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createPoint(yytext().substring(0,2)));
}

<POINTS> "["
{
}

<POINTS> [a-z][a-z]"]" 
{
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createPointList(yytext().substring(0,2)));
}

<POINTS> "]" 
{
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createPointList(""));
}

<POINTS> [a-z][a-z]":"[a-z][a-z]"]" 
{
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createPointList(yytext().substring(0,5)));
}

<POINTS> "("    { return (new Token(Symbols.Open, yytext(), yyline, yychar)); }
<POINTS> ")"    { return (new Token(Symbols.Close, yytext(), yyline, yychar)); }

<LABEL> "["
{
}

<LABEL> [a-z][a-z]":"[^\]]+"]" 
{
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createLabel(yytext().substring(0,2), yytext().substring(3, yytext().length()-1)));
}

<LABEL> "("    { return (new Token(Symbols.Open, yytext(), yyline, yychar)); }
<LABEL> ")"    { return (new Token(Symbols.Close, yytext(), yyline, yychar)); }

<NODE> "[" {
    String text = yytext();
    Token next = (Token) next_token();
    return new Token(Symbols.Value, text + next.m_text, yyline, yychar, AbstractValue.createValue(text + next.m_text));
}

<NUMBER> [\+\-]*[0-9]+"]" 
{
    yybegin(TOP);
    Integer number = new Integer(yytext().substring(0, yytext().length()-1));
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createValue(number));
}

<TEXT> \\((\r(\n)?)|(\n(\r)?)) { 
}

<TEXT> (\r(\n)?)|(\n(\r)?) { 
    String text = "\n";
    Token next = (Token) next_token();
    return new Token(Symbols.Value, text + next.m_text, yyline, yychar, AbstractValue.createValue(text + next.m_text));
}

<TEXT> ([^\]\r\n\\])* { 
    String text = yytext();
    Token next = (Token) next_token();
    return new Token(Symbols.Value, text + next.m_text, yyline, yychar, AbstractValue.createValue(text + next.m_text));
}

<TEXT> \\[^\n\r] { 
    Token next = (Token) next_token();
    return new Token(Symbols.Value, yytext().substring(1) + next.m_text, yyline, yychar, AbstractValue.createValue(yytext().substring(1) + next.m_text));
}

<TEXT> "]" { 
    yybegin(TOP);
    return new Token(Symbols.Value, "", yyline, yychar, AbstractValue.createValue(""));
}

<PROPERTY> ([^\]])*"]"([ \r\n\t])*"["
{
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createValue(yytext()));
}

<PROPERTY> ([^\]])*"]"
{
    yybegin(TOP);
    return new Token(Symbols.Value, yytext(), yyline, yychar, AbstractValue.createValue(yytext().substring(0, yytext().length()-1)));
}

[a-z]*[A-Z][a-z]*[A-Z]*[a-z]*"[" 
{
    Property property = Property.createProperty(yytext().substring(0, yytext().length()-1));
    if (property instanceof Property.Move)
    {
	yybegin(MOVE);
    }
    else if (property instanceof Property.AddStones)
    {
	yybegin(POINTS);
    }
    else if (property instanceof Property.Label)
    {
        yybegin(LABEL);
    }
    else if (property instanceof Property.Markup)
    {
        yybegin(POINTS);
    }
    else if (property instanceof Property.View)
    {
        yybegin(POINTS);
    }
    else if (property instanceof Property.Text)
    {
        yybegin(TEXT);
    }
    else if (property instanceof Property.Charset)
    {
        yybegin(TEXT);
    }
    else if (property instanceof Property.Number)
    {
        yybegin(NUMBER);
    }
    else if (property instanceof Property.RootNumber)
    {
        yybegin(NUMBER);
    }
    else if (property instanceof Property.GameInfo)
    {
	yybegin(TEXT);
    }
    else
    {
	yybegin(PROPERTY);
    }
    return new Token(Symbols.Property, yytext(), yyline, yychar, property);
}

" " { }
\t { }
\n { }
\r { }

.  { 
       throw new ParseError("Illegal character <"+ yytext() + ">", this); 
   }

