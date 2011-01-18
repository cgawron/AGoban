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

import java.lang.System;
import java.io.IOException;
import java.util.logging.Logger;

import de.cgawron.util.MiscEncodingReader;


%%

%public
%implements java_cup.runtime.Scanner, InputPosition
%function next_token
%type java_cup.runtime.Symbol
%unicode
%char
%line
%column
%unicode
%buffer 4096
//%debug

%eof{
  logger.info("eof reached");
%eof}

%state TOP
%state NODE
%state MOVE
%state POINTS
%state LABEL
%state NUMBER
%state TEXT
%state PROPERTY
%state PROPERTY_NEXT

%{
    private static Logger logger = Logger.getLogger(Yylex.class.getName());
    
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
	int beg = zzCurrentPos; 
	int end = zzCurrentPos; 
	while (beg > 0 && zzBuffer[beg-1] != '\n') beg--;
	while (end < zzEndRead && zzBuffer[end] != '\n') end++;
	
	return new String(zzBuffer, beg, end-beg);
    }


    private static Yylex theLexer = null;

    public static void setCharset(String charSetName)
    {
	if (theLexer != null)
	    theLexer._setCharset(charSetName);
    }

    public void _setCharset(String charSetName)
    {
	MiscEncodingReader mer = (MiscEncodingReader) zzReader;
	try {
	  mer.setCharset(charSetName, zzMarkedPos);
	  zzReader = null;
	  yyclose();
	  zzReader = mer;
	  zzAtEOF  = false;
	  zzEndRead = zzStartRead = 0;
	  zzCurrentPos = zzMarkedPos = 0;
	}
	catch (IOException err) {
	  throw new RuntimeException(err);
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
    return (new Token(Symbols.Open, "(", yyline, yychar));
}

<YYINITIAL>  .   
{
}

";"    
{
    yybegin(TOP);
    return (new Token(Symbols.Semi, ";", yyline, yychar)); 
}

<TOP> "("    { level++; return (new Token(Symbols.Open, "(", yyline, yychar)); }
<TOP> ")"    { level--; /*if (level==0) yybegin(YYINITIAL); */ return (new Token(Symbols.Close, ")", yyline, yychar)); }

<MOVE> "]" 
{
    yybegin(TOP);
    return new Token(Symbols.Value, "]", yyline, yychar, AbstractValue.createValue(null));
}

<MOVE> [a-z][a-z]"]" 
{
    yybegin(TOP);
    String text = yytext();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createPoint(text.substring(0,2)));
}

<POINTS> "["
{
}

<POINTS> [a-z][a-z]"]" 
{
    String text = yytext();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createPointList(text.substring(0,2)));
}

<POINTS> "]" 
{
    return new Token(Symbols.Value, "]", yyline, yychar, AbstractValue.createPointList(""));
}

<POINTS> [a-z][a-z]":"[a-z][a-z]"]" 
{
    String text = yytext();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createPointList(text.substring(0,5)));
}


<LABEL> "["
{
}

<LABEL> [a-z][a-z]":"[^\]]+"]" 
{
    String text = yytext();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createLabel(text.substring(0,2), text.substring(3, text.length()-1)));
}

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
    StringBuffer tb = new StringBuffer("\n");
    Token next = (Token) next_token();
    String text = tb.append(next.m_text).toString();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createValue(text));
}

<TEXT> ([^\]\r\n\\])* { 
    StringBuffer tb = new StringBuffer(yytext());
    Token next = (Token) next_token();
    String text = tb.append(next.m_text).toString();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createValue(text));
}

<TEXT> \\[^\n\r] { 
    StringBuffer tb = new StringBuffer(yytext().substring(1));
    Token next = (Token) next_token();
    String text = tb.append(next.m_text).toString();
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createValue(text));
}

<TEXT> "]" { 
    yybegin(TOP);
    return new Token(Symbols.Value, "", yyline, yychar, AbstractValue.createValue(""));
}


"("    { return (new Token(Symbols.Open, "(", yyline, yychar)); }
")"    { return (new Token(Symbols.Close, ")", yyline, yychar)); }

[a-z]*[A-Z][a-z]*[A-Z]*[a-z]*"[" 
{
    String text = yytext();
    Property property = Property.createProperty(text.substring(0, text.length()-1));
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
      logger.warning("Unknown property: " + text);
      yybegin(PROPERTY);
    }
    return new Token(Symbols.Property, text, yyline, yychar, property);
}


// Fallback rules for unknown properties for which we can't specify a property type 
<PROPERTY_NEXT>"]" [ \t\r\n]* "["
{
  yybegin(PROPERTY);
}

<PROPERTY_NEXT>"]"
{
  yybegin(TOP);
}

<PROPERTY> [a-z][a-z] / [ \t\r\n]* "]"
{
    String text = yytext();
    yybegin(PROPERTY_NEXT);
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createPointList(text));
}

<PROPERTY> [a-z][a-z]":"[a-z][a-z] / [ \t\r\n]* "]"
{
    String text = yytext();
    yybegin(PROPERTY_NEXT);
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createPointList(text));
}

<PROPERTY> [^()\]]+ / [ \t\r\n]* "]"
{
    String text = yytext();
    yybegin(PROPERTY_NEXT);
    return new Token(Symbols.Value, text, yyline, yychar, AbstractValue.createValue(text));
}

<PROPERTY>"]["
{
  yybegin(PROPERTY);
  return new Token(Symbols.Value, "", yyline, yychar, AbstractValue.createValue(""));
}

<PROPERTY>"]"
{
  yybegin(TOP);
  return new Token(Symbols.Value, "", yyline, yychar, AbstractValue.createValue(""));
}

" " { }
\t { }
\n { }
\r { }

.  { 
      logger.severe(String.format("Illegal character: text=%s, line=%d, column %d, char=%d", 
				  yytext(), yyline, getColumn(), yychar));
      return new Token(Symbols.error, yytext(), yyline, yychar, null);
   }

