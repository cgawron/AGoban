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

public class ParseError extends RuntimeException {
	int line;
	int column;
	String text;
	String message;

	public ParseError(String message, Object info) {
		super(message + " at " + info);
		this.message = message + " at " + info;
	}

	public ParseError(String message, InputPosition position, Object info) {
		super(message + " at " + info);
		this.message = message + " at " + info;
		text = position.getCurrentLine();
		line = position.getLine();
		column = position.getColumn();
	}

	public String getMessage() {
		int i;
		StringBuffer buffer = new StringBuffer();

		buffer.append(message).append(" at line ").append(line);
		buffer.append(", column").append(column).append(":\n");
		buffer.append(text).append("\n");
		for (i = 1; i < column; i++)
			buffer.append(' ');
		buffer.append('^');

		return buffer.toString();
	}
}
