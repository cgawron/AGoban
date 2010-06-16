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
import java.io.StringReader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity implements SeekBar.OnSeekBarChangeListener
{
    public static Resources resources;
    private GobanView gobanView;
    private SeekBar seekBar;
    private GameTree gameTree;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	resources = getResources();
        setContentView(R.layout.main);

	gobanView = (GobanView) findViewById(R.id.goban);
	seekBar = (SeekBar) findViewById(R.id.seekBar);

	seekBar.setOnSeekBarChangeListener(this);
    }
    
    @Override
    public void onStart() {
	super.onStart();
	gameTree = null;

	try {
	    gameTree = new GameTree(new StringReader("(;FF[4]DT[2004-12-06]EV[Nikolaus Meschede]HA[0]KM[1,5]RE[B+5]RO[3]SZ[9]GM[1]FF[4];B[cc];W[gf];B[dg];W[ce];B[de];W[df];B[cf];W[ef];B[dd];W[cg];B[bf];W[bg];B[be];W[ec];B[ee];W[fe];B[dc];W[eb];B[db];W[ed];B[eg];W[ff];B[fg];W[gh];B[gg];W[hg];B[fh];W[hh];B[ch];W[fi];B[ei];W[dh];B[gi];W[hi];B[di];W[bh];B[eh];W[fi];B[bi];W[da];B[ca];W[ea];B[hc];W[gc];B[gb];W[gd];B[he];W[hd];B[ic];W[id];B[ha];W[ib];B[fc];W[fd];B[fb];W[hb];B[gi];W[ai];B[ah];W[fi];B[];W[gi])"));
	    
	    seekBar.setMax(gameTree.getNoOfMoves());
	    seekBar.setKeyProgressIncrement(1);

	    Goban goban = (MarkupModel) gameTree.getRoot().getGoban();
	    gobanView.setGoban(goban);
	}
	catch (Exception ex) {
	    Log.e("EditSGF", "Exception while parsing SGF", ex);
	}

    }

    @Override
    public void onProgressChanged(SeekBar  seekBar, int moveNo, boolean fromUser) {
	Log.d("EditSGF", "move " + moveNo);
	Node moveNode = gameTree.getMove(moveNo);
	if (moveNode != null) {
	    Goban goban = (MarkupModel) moveNode.getGoban();
	    gobanView.setGoban(goban);
	}
    }
    
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
