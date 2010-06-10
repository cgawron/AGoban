/*
 * Copyright (C) 2010 Christian Gawron
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
 */

package de.cgawron.agoban;

import de.cgawron.go.Goban;
import static de.cgawron.go.Goban.BoardType.WHITE;
import static de.cgawron.go.Goban.BoardType.BLACK;
import de.cgawron.go.SimpleGoban;

import android.app.Activity;
import android.os.Bundle;

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	GobanView gobanView = (GobanView) findViewById(R.id.goban);
	Goban goban = new SimpleGoban();

	goban.move(3, 3, WHITE); 
	goban.move(3, 10, BLACK); 
	gobanView.setGoban(goban);
    }
}
