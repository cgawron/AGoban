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

import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.SeekBar;

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
    }
    
    @Override
    public void onStart() {
	super.onStart();

	Intent intent = getIntent();
	Uri data = intent.getData();
	Log.d("EditSGF", "Data: " + data);
	gameTree = null;

	if (data != null)
	{
	    try {
		InputStream is = getContentResolver().openInputStream(data);
		gameTree = new GameTree(new InputStreamReader(is));
	    }
	    catch (Exception ex) {
		Log.e("EditSGF", "Exception while parsing SGF", ex);
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
    }

    public void setCurrentNode(Node node) {
	currentNode = node;
	if (currentNode != null) {
	    Goban goban = (MarkupModel) currentNode.getGoban();
	    gobanView.setGoban(goban);
	}
    }
}
