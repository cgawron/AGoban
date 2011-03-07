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

import de.cgawron.agoban.view.PropertyView;
import de.cgawron.go.sgf.GameTree;

/**
 * Shows the game info
 */
public class ShowGameInfo extends TabActivity
{
	private SGFApplication application;
	private GameTree gameTree;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		application = (SGFApplication) getApplication();
		setContentView(R.layout.game_info);

		Log.d("ShowGameInfo", "thread: " + Thread.currentThread().getId() + " "
				+ getIntent().getClass().toString());
		Intent intent = getIntent();
		GameTree gameTree = application.getGameTree();
		((PropertyView) findViewById(R.id.PW)).setPropertyList(gameTree
				.getRoot());
		((PropertyView) findViewById(R.id.PB)).setPropertyList(gameTree
				.getRoot());

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("player")
				.setIndicator(res.getString(R.string.player))
				.setContent(R.id.players);
		tabHost.addTab(spec);
		spec = tabHost.newTabSpec("rules").setIndicator("Rules")
				.setContent(R.id.rules);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		Log.d("ShowGameInfo", "starting!");
	}

	@Override
	protected void onStop()
	{
		Log.d("ShowGameInfo", "onStop");
		super.onStop();

		application.save();
	}

	@Override
	protected void onPause()
	{
		Log.d("ShowGameInfo", "onPause");
		super.onPause();

		application.save();
	}

}
