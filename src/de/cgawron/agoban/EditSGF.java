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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import de.cgawron.agoban.view.GobanView;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.go.Goban;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity implements SeekBar.OnSeekBarChangeListener, GobanEventListener
{
    public static Resources resources;

    private GobanView gobanView;
    private SeekBar seekBar;
    private GameTree gameTree;
    private Node currentNode;
    private String gitId;
    private SGFApplication application;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	application = (SGFApplication) getApplication();
	resources = getResources();
	try {
	    PackageItemInfo info = getPackageManager().getActivityInfo(new ComponentName(this, EditSGF.class), PackageManager.GET_META_DATA);
	    gitId = info.metaData.getString("git-id");
	}
	catch (Exception e)
	{
	    throw new RuntimeException("git-id", e);
	}
	Log.d("EditSGF", "git-id: " + gitId);

	setContentView(R.layout.main);

	gobanView = (GobanView) findViewById(R.id.goban);
	gobanView.addGobanEventListener(this);
	registerForContextMenu(gobanView);

	seekBar = (SeekBar) findViewById(R.id.seekBar);
	seekBar.setOnSeekBarChangeListener(this);
	seekBar.requestFocus();

	Intent intent = getIntent();
	Log.d("EditSGF", "Uri: " + intent.getData());
	if (intent.getData() == null) {
	    Intent searchSGF = new Intent(Intent.ACTION_SEARCH, SGFProvider.CONTENT_URI, this, ChooseSGF.class);
	    //Intent searchSGF = new Intent(Intent.ACTION_SEARCH, SGFProvider.CONTENT_URI, this, ListGoogleSGF.class);
	    startActivity(searchSGF);
	    finish();
	}

	application.setData(intent.getData());

	gameTree = null;
    }

    @Override
    public File getFileStreamPath(String name) {
	File directory = Environment.getExternalStorageDirectory();
	directory = new File(directory, "sgf");
	
	if (!directory.exists())
	    directory.mkdir();

	return new File(directory, name);
    }
        
    @Override
    public void onStart() {
	super.onStart();
	Log.d("EditSGF", "OnStart");

	final Runnable afterLoaded = new Runnable() {
		public void run() {
		    gameTree = application.get(application.KEY_DEFAULT); 
		    if (gameTree != null) {
			currentNode = gameTree.getRoot();
			
			seekBar.setMax(gameTree.getNoOfMoves());
			seekBar.setKeyProgressIncrement(1);
			
			Goban goban = gameTree.getRoot().getGoban();
			gobanView.setGoban(goban);
		    }
		}
	    };

	application.loadSGF(this, afterLoaded);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.options_menu, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.save:
	    save();
	    return true;

	case R.id.open:
	    open();
	    return true;

	case R.id.game_info:
	    showGameInfo();
	    return true;

	case R.id.about:
	    Context context = getApplicationContext();
	    CharSequence text = String.format("AGoban, ©2010 Christian Gawron\nGit-Id: %s", gitId);
	    int duration = Toast.LENGTH_LONG;
	    Toast toast = Toast.makeText(context, text, duration);
	    toast.show();
	    return true;
	}
	return false;
    }

    public void onProgressChanged(SeekBar  seekBar, int moveNo, boolean fromUser) {
	Log.d("EditSGF", "move " + moveNo);
	setCurrentNode(gameTree.getMove(moveNo));
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onGobanEvent(GobanEvent gobanEvent) {
	Log.d("EditSGF", "onGobanEvent: " + gobanEvent);
	if (currentNode != null) {
	    Node node = new Node(gameTree);
	    try {
		node.setGoban(currentNode.getGoban().clone());
	    }
	    catch (CloneNotSupportedException ex) {
		Log.e("EditSGF", "onGobanEvent", ex);
	    }
	    currentNode.add(node);
	    Log.d("EditSGF", "addMove: " + node + ", " + currentNode);
	    node.move(gobanEvent.getPoint());	
	    setCurrentNode(node);
	}
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
	Log.d("EditSGF", "onCreateContextMenu");
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.stone_context_menu, menu);
    }

    @Override
    protected void onStop() {
       super.onStop();

       save();
    }

    public void setCurrentNode(Node node) {
	currentNode = node;
	if (currentNode != null) {
	    Goban goban = currentNode.getGoban();
	    if (currentNode.getSiblingCount() > 0) {
		Log.d("EditSGF", "siblingCount: " + currentNode.getSiblingCount());
	    }
	    gobanView.setGoban(goban);
	}
    }

    public void save() {
	application.save();
    }

    public void open() {
	Log.d("EditSGF", "open()");
    }

    public void showGameInfo() {
	Log.d("EditSGF", "Show game info");
	Intent viewGameInfo = new Intent(Intent.ACTION_VIEW, application.getData());
	viewGameInfo.setClassName("de.cgawron.agoban", "de.cgawron.agoban.ShowGameInfo");

	Log.d("EditSGF", "thread: " + Thread.currentThread().getId() + " " + viewGameInfo.getClass().toString());
	startActivity(viewGameInfo);
    }
}
