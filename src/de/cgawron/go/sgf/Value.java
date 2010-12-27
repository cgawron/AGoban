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

import de.cgawron.go.Symmetry;
import java.io.PrintWriter;

/**
 * A value of a {@link Property}
 */
public interface Value extends Cloneable
{
    void write(PrintWriter out);
    
    String getString();

    public interface Transformable
    {
	void transform(Symmetry symmetry);
    }

    public interface Void extends Value 
    {
    }

    public interface Point extends Value, Transformable 
    {
	de.cgawron.go.Point getPoint();
    }

    public interface PointList extends Value, Transformable, java.util.Collection<de.cgawron.go.Point> 
    {
	short getMinX();
	short getMinY();
	short getMaxX();
	short getMaxY();
    }

    public interface Label extends Value, Transformable 
    {
	de.cgawron.go.Point getPoint();
    }

    public interface Text extends Value {}

    public interface Result extends Value {}

    public interface Number extends Value 
    {
	int intValue();
    }

    public interface ValueList extends Value, java.util.List<Value> 
    {
    }

    public Value clone();
}
