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
import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.cgawron.agoban.view.GameTreeControls;
import de.cgawron.agoban.view.GameTreeControls.GameTreeNavigationListener;
import de.cgawron.agoban.view.GobanView;
import de.cgawron.agoban.view.GobanView.GobanContextMenuInfo;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.sync.GoogleSync;
import de.cgawron.go.Goban;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity 
    implements GobanEventListener, GameTreeNavigationListener, SGFApplication.ExceptionHandler
{
    private static String TAG = "EditSGF";

    public static Resources resources;

    private GobanView gobanView;
    private TextView commentView;
    private GameTree gameTree;
    private GameTreeControls gameTreeControls;
    private Node currentNode;
    private Map<Point, Node> variations = new HashMap<Point, Node>();
    private String gitId;
    private SGFApplication application;
    private SharedPreferences settings; 

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	application = (SGFApplication) getApplication();
	resources = getResources();
	settings = getSharedPreferences(SGFApplication.PREF, 0);
	try {
	    PackageItemInfo info = getPackageManager().getActivityInfo(new ComponentName(this, EditSGF.class), 
								       PackageManager.GET_META_DATA);
	    gitId = info.metaData.getString("git-id");
	}
	catch (Exception e)
	{
	    throw new RuntimeException("git-id", e);
	}
	Log.d(TAG, "git-id: " + gitId);

	setContentView(R.layout.main);

	gobanView = (GobanView) findViewById(R.id.goban);
	gobanView.addGobanEventListener(this);
	registerForContextMenu(gobanView);

	commentView = (TextView) findViewById(R.id.comment);

	gameTreeControls = (GameTreeControls) findViewById(R.id.controls);
	gameTreeControls.setGameTreeNavigationListener(this);
	
	Intent intent = getIntent();
	if (Intent.ACTION_EDIT.equals(intent.getAction()))
	    application.setReadOnly(false);

	Log.d(TAG, "Uri: " + intent.getData());
	if (intent.getData() == null) {
	    Intent searchSGF = new Intent(Intent.ACTION_SEARCH, SGFProvider.CONTENT_URI, this, ChooseSGF.class);
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
	Log.d(TAG, "OnStart");

	final Runnable afterLoaded = new Runnable() {
		public void run() {
		    gameTree = application.getGameTree(); 
		    if (gameTree != null) {
			gameTreeControls.setGameTree(gameTree);
			//seekBar.setMax(gameTree.getNoOfMoves());
		    }
		}
	    };

	application.loadSGF(this, afterLoaded, this);
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

	case R.id.new_game:
	    newGame();
	    return true;

	case R.id.game_info:
	    showGameInfo();
	    return true;

	case R.id.google_sync:
	    googleSync();
	    return true;

	case R.id.preferences:
	    editPreferences();
	    return true;

	case R.id.about:
	    Context context = getApplicationContext();
	    CharSequence text = String.format("AGoban, (c)2010 Christian Gawron\nGit-Id: %s", gitId);
	    int duration = Toast.LENGTH_LONG;
	    Toast toast = Toast.makeText(context, text, duration);
	    toast.show();
	    return true;
	}
	return false;
    }

    public void onGobanEvent(GobanEvent gobanEvent) 
    {
	Log.d(TAG, "onGobanEvent: " + gobanEvent);
	if (currentNode != null) {
	    Point point = gobanEvent.getPoint();
	    Log.d(TAG, "onGobanEvent: variations: " + variations.keySet());
	    if (variations.containsKey(point)) {
		setCurrentNode(variations.get(point));
	    }
	    else if (application.checkNotReadOnly(this)) {
		Node node = new Node(gameTree);
		try {
		    node.setGoban(currentNode.getGoban().clone());
		}
		catch (CloneNotSupportedException ex) {
		    Log.e(TAG, "onGobanEvent", ex);
		}
		currentNode.add(node);
		Log.d(TAG, "addMove: " + node + ", " + currentNode);
		node.move(point);	
		setCurrentNode(node);
	    }
	}
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
	Log.d(TAG, "onCreateContextMenu");
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.stone_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
	GobanContextMenuInfo info = (GobanContextMenuInfo) item.getMenuInfo();
	Log.d("onContextMenuItemSelected", item.toString() + " " + info);
	return true;
    }

    @Override
    protected void onStop() {
       super.onStop();
       
       if (gameTree != null)
	   save();
    }

    public void setCurrentNode(Node node) 
    {
	if (!node.equals(currentNode)) {
	    currentNode = node;
	    variations.clear();
	    gobanView.resetMarkup();
	    if (currentNode != null) {
		Goban goban = currentNode.getGoban();
		if (currentNode.getSiblingCount() > 0) {
		    for (Node sibling : currentNode.getSiblings()) {
			Log.d(TAG, "sibling: " + sibling);
			if (sibling.isMove()) {
			    markVariation(gobanView, sibling);
			}
		    }
		}
		Point lastMove = goban.getLastMove();
		if (lastMove != null)
		    gobanView.markLastMove(lastMove, goban.getStone(lastMove));

		gobanView.setGoban(goban);
		commentView.setText(currentNode.getComment());
	    }
	    gameTreeControls.setCurrentNode(node);
	}
    }

    private void markVariation(GobanView view, Node node)
    {
	Point point = null;
	if (node.contains(Property.BLACK)) {
	    point = node.getPoint(Property.BLACK);
	}
	else if (node.contains(Property.WHITE)) {
	    point = node.getPoint(Property.WHITE);
	}

	if (point != null) {
	    variations.put(point, node);
	    view.addVariation(point);
	}
    }

    public void save() 
    {
	application.save();
    }

    public void editPreferences() 
    {
	Intent intent = new Intent(this, SGFApplication.EditPreferences.class);
	Log.d(TAG, "Starting " + intent);
	startActivity(intent);
    }

    public void open() 
    {
	Log.d(TAG, "open()");
    }

    public void newGame()
    {
	Intent sgfIntent = new Intent(Intent.ACTION_EDIT, application.getNewGameUri());
	startActivity(sgfIntent);
	finish();
    }

    public void googleSync()
    {
	Intent send = new Intent(Intent.ACTION_SEND, application.getData(), this, GoogleSync.class);
	send.setType("application/x-go-sgf");
	send.putExtra(Intent.EXTRA_STREAM, application.getData());
	startActivity(send);
	finish();
    }

    public void showGameInfo() 
    {
	Log.d(TAG, "Show game info");
	Intent viewGameInfo = new Intent(Intent.ACTION_VIEW, application.getData());
	viewGameInfo.setClassName("de.cgawron.agoban", "de.cgawron.agoban.ShowGameInfo");

	Log.d(TAG, "thread: " + Thread.currentThread().getId() + " " + viewGameInfo.getClass().toString());
	startActivity(viewGameInfo);
    }

    public void handleException(final String message, Throwable ex)
    {
	Runnable runnable = new Runnable() {
		public void run() {
		    AlertDialog.Builder builder = new AlertDialog.Builder(EditSGF.this);
		    builder.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				    EditSGF.this.finish();
				}
			    });
		    AlertDialog alert = builder.show();
		}
	    };
	runOnUiThread(runnable);
    }
}
