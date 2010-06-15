/*
 *
 * $Id: GameTreeCommand.java 38 2003-09-12 19:22:24Z cgawron $
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

/**
 * A command to be run on a GameTree.
 * Instances of this interface may be {@link #run} on a {@link GameTree}. 
 */
public interface GameTreeCommand
{
    /**
     * Runs the command on a {@link GameTree}.
     */
    public void run(GameTree gameTree) throws Exception;
    
    /**
     * Returns the name of the command. 
     * The name should be used to describe the functionality this command implements.
     * @return the name of the command.
     */
    public String getName();
}
