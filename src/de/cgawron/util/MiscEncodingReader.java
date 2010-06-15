/*
 *
 * $Id: MiscEncodingReader.java 129 2004-10-21 17:16:09Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.CoderResult;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.log4j.Logger;

public class MiscEncodingReader extends Reader
{
    private static Logger logger = Logger.getLogger(MiscEncodingReader.class.getName());
    private InputStream stream;
    private InputStreamReader reader;
    private Charset currentCharset;
    
    public MiscEncodingReader(InputStream stream)
    {
	this.stream = new BufferedInputStream(stream, 16384);
	this.stream.mark(0);
	reader = new InputStreamReader(this.stream);
    }

    public void setCharset(String charSetName, int charsUsed) throws IOException
    {
	Charset charset = Charset.forName(charSetName);
	logger.info("Setting charset from " + currentCharset + " to " + charset + ": " + reader.getEncoding());
	logger.info("CharsUsed is " + charsUsed);
	if (!charset.equals(currentCharset)) {
	    stream.reset();
	    stream.skip(charsUsed);

	    reader = new InputStreamReader(stream, charset);
	    currentCharset = charset;
	}
	logger.info("Charset is now: " + reader.getEncoding());
    }

    public void close() throws IOException
    {
	logger.info("close()");
	//reader.close();
    }

    public int read(char[] cbuf, int off, int len) throws IOException
    {
	stream.mark(len+1);
	int r = reader.read(cbuf, off, len);
	if (logger.isDebugEnabled())
	    logger.debug("Read: " + (new String(cbuf)) + ", " + off + ", " + len);
	return r;
    }

    public static void main(String args[])
    {
	try {
	    Writer writer = new PrintWriter(System.out);

	    /*
	    MiscEncodingReader reader = new MiscEncodingReader(new FileInputStream(args[0]));
	    reader.setCharset("UTF-8", 0);
	    char[] cbuf = new char[256];
	    int len;

	    while ((len = reader.read(cbuf)) > 0)
		writer.write(cbuf, 0, len);
	    

	    reader.close();
	    */

	    char c;
	    ReadableByteChannel in = Channels.newChannel(new FileInputStream(args[0]));
	    ByteBuffer bb = ByteBuffer.allocate(65535);
	    CharBuffer cb = CharBuffer.allocate(65535);
	    CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
	    CoderResult result;

	    while (in.read(bb) >= 0)
	    {
		bb.flip();
		while (bb.hasRemaining())
		{
		    result = decoder.decode(bb, cb, false);
		    cb.flip();
		    while (cb.hasRemaining())
		    {
			c = cb.get();
			writer.write(c);
		    }
		    cb.clear();
		}
		bb.clear();
	    }

	    writer.close();
	}
	catch (Throwable e) {
	    e.printStackTrace();
	}
    }
}
