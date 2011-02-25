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

import de.cgawron.go.Goban;
import de.cgawron.go.GobanEvent;
import de.cgawron.go.GobanListener;

/**
 * A MarkupModelListener gets informed when the model changes, i.e. when a
 * region is set or a letter is added.
 */
public interface MarkupModelListener extends GobanListener {
	/**
	 * The region (or VieW in SGF speak) of the model has changed.
	 * 
	 * @param e
	 *            gawron.go.GobanEvent
	 */
	void regionChanged(GobanEvent e);
}
