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
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.Node;

import de.cgawron.agoban.view.GobanView;

import java.util.List;
import java.util.UUID;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity implements SeekBar.OnSeekBarChangeListener, GobanEventListener
{
    public static Resources resources;
    private static String gitId = "$Id$";


    private GobanView gobanView;
    private SeekBar seekBar;
    private GameTree gameTree;
    private Node currentNode;
    private Uri data;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	resources = getResources();
        setContentView(R.layout.main);

	gobanView = (GobanView) findViewById(R.id.goban);
	gobanView.addGobanEventListener(this);
	registerForContextMenu(gobanView);

	seekBar = (SeekBar) findViewById(R.id.seekBar);
	seekBar.setOnSeekBarChangeListener(this);
	seekBar.requestFocus();

	Intent intent = getIntent();
	data = intent.getData();

	gameTree = null;
	if (data == null) {
	    File file = getNewFile();
	    Uri.Builder ub = new Uri.Builder();
	    ub.path(file.getAbsolutePath());
	    ub.scheme("file");
	    data = ub.build();
	}
	Log.d("EditSGF", "OnCreate: data=" + data);
    }

    @Override
    public File getFileStreamPath(String name) {
	File directory = Environment.getExternalStorageDirectory();
	directory = new File(directory, "sgf");
	
	if (!directory.exists())
	    directory.mkdir();

	return new File(directory, name);
    }
    
    public File getNewFile() {
	return getFileStreamPath(UUID.randomUUID().toString() + ".sgf");
    }
    
    @Override
    public void onStart() {
	super.onStart();
	Log.d("EditSGF", "OnStart: data=" + data);

	if (data != null)
	{
	    try {
		InputStream is = getContentResolver().openInputStream(data);
		gameTree = new GameTree(new InputStreamReader(is));
	    }
	    catch (Exception ex) {
		Log.e("EditSGF", "Exception while parsing SGF", ex);
		gameTree = new GameTree();
	    }
	    
	}
	else gameTree = new GameTree();
	currentNode = gameTree.getRoot();

	seekBar.setMax(gameTree.getNoOfMoves());
	seekBar.setKeyProgressIncrement(1);
	
	Goban goban = (MarkupModel) gameTree.getRoot().getGoban();
	gobanView.setGoban(goban);

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
	}
	return false;
    }

    @Override
    public void onProgressChanged(SeekBar  seekBar, int moveNo, boolean fromUser) {
	Log.d("EditSGF", "move " + moveNo);
	setCurrentNode(gameTree.getMove(moveNo));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
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
	    Goban goban = (MarkupModel) currentNode.getGoban();
	    gobanView.setGoban(goban);
	}
    }

    public void save() {
	if (data == null) {
	    File file = getFileStreamPath("test.sgf");
	    Uri.Builder ub = new Uri.Builder();
	    ub.path(file.getAbsolutePath());
	    ub.scheme("file");
	    data = ub.build();
	}
	
	Log.d("EditSGF", "saving " + data);
	try {
	    OutputStream os = getContentResolver().openOutputStream(data);
	    gameTree.save(os);
	    os.close();
	}
	catch (Exception ex) {
	    Log.e("EditSGF", "Exception while saving SGF", ex);
       }
    }
}
