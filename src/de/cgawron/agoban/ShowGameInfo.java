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

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import de.cgawron.go.sgf.GameTree;

/**
 * Shows the game info
 */
public class ShowGameInfo extends TabActivity
{
    public static Resources resources;

    private GameTree gameTree;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	resources = getResources();
	super.onCreate(savedInstanceState);
	setContentView(R.layout.game_info);

	Resources res = getResources(); // Resource object to get Drawables
	TabHost tabHost = getTabHost();  // The activity TabHost
	TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	Intent intent;  // Reusable Intent for each tab
	
	// Create an Intent to launch an Activity for the tab (to be reused)
	// intent = new Intent().setClass(this, ArtistsActivity.class);
	intent = new Intent(Intent.ACTION_VIEW); //.setClass(this, Show.class);
	
	// Initialize a TabSpec for each tab and add it to the TabHost
	spec = tabHost.newTabSpec("artists").setIndicator("Artists").setContent(intent);
	tabHost.addTab(spec);
	
	tabHost.setCurrentTab(1);
	
	Log.d("ShowGameInfo", "starting!");

        setContentView(R.layout.game_info);
    }
}
