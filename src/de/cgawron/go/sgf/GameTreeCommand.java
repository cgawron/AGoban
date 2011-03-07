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

/**
 * A command to be run on a GameTree. Instances of this interface may be
 * {@link #run} on a {@link GameTree}.
 */
public interface GameTreeCommand
{
	/**
	 * Runs the command on a {@link GameTree}.
	 */
	public void run(GameTree gameTree) throws Exception;

	/**
	 * Returns the name of the command. The name should be used to describe the
	 * functionality this command implements.
	 * 
	 * @return the name of the command.
	 */
	public String getName();
}
