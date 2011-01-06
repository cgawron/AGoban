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

package de.cgawron.agoban.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.cgawron.agoban.R;
import de.cgawron.agoban.SGFApplication;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;

/**
 * {@code GameTreeControls} allow to navigate through a game tree.
 *
 */
public class GameTreeControls extends LinearLayout implements View.OnClickListener
{
    public interface GameTreeNavigationListener 
    {
	public void setCurrentNode(Node node);

    }

    private static String TAG = "GameTreeControls";

    private SharedPreferences settings; 
    private final Button buttonNext;
    private final Button buttonPrev;
    private final Button buttonNextMarkup;
    private final Button buttonPrevMarkup;
    private final TextView moveNoView;

    private GameTree gameTree;
    private GameTreeNavigationListener listener;
    private Node currentNode;

    public GameTreeControls(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
	settings = context.getSharedPreferences(SGFApplication.PREF, 0);

	buttonNext = new Button(context);
	buttonPrev = new Button(context);
	buttonNextMarkup = new Button(context);
	buttonPrevMarkup = new Button(context);
	moveNoView = new TextView(context);
	buttonNext.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
	buttonPrev.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
	buttonNextMarkup.setText("nm");
	buttonPrevMarkup.setText("pm");
	moveNoView.setText("-");

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GobanView);

        a.recycle();
	initView(context);
    }

    private void initView(Context context)
    {
	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
	params.gravity  = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	addView(buttonPrev, params);
	buttonPrev.setOnClickListener(this);
	addView(buttonPrevMarkup, params);
	buttonPrevMarkup.setOnClickListener(this);
	params.gravity = Gravity.CENTER;
	addView(moveNoView, params);
	params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
	addView(buttonNextMarkup, params);
	buttonNextMarkup.setOnClickListener(this);
	addView(buttonNext, params);
	buttonNext.setOnClickListener(this);
    }

    public void onClick(View v)
    {
	if (buttonPrev.equals(v)) {
	    Log.d(TAG, "button pressed: prev");
	    prevNode();
	}
	else if (buttonPrevMarkup.equals(v)) {
	    Log.d(TAG, "button pressed: prevMarkup");
	    prevMarkupNode();
	}
	else if (buttonNext.equals(v)) {
	    Log.d(TAG, "button pressed: next");
	    nextNode();
	}
	else if (buttonNextMarkup.equals(v)) {
	    Log.d(TAG, "button pressed: nextMarkup");
	    nextMarkupNode();
	}
    }

    public void setGameTreeNavigationListener(GameTreeNavigationListener listener) 
    {
	this.listener = listener;
    }

    public void setGameTree(GameTree gameTree) {
	Log.d(TAG, "setGameTree: " + gameTree);
	this.gameTree = gameTree;
	setCurrentNode(gameTree.getRoot());
    }

    public void setCurrentNode(Node node) {
	Log.d(TAG, "setCurrentNode: " + node);
	this.currentNode = node;
	moveNoView.setText(Integer.toString(node.getMoveNo()));
	if (listener != null)
	    listener.setCurrentNode(node);
    }

    public void nextNode() 
    {
	if (currentNode != null && currentNode.getChildCount() > 0) {
	    if (settings.getBoolean("sortVariations", false)) {
		Log.d(TAG, "nextNode: sort children by depth");
		final List<Node> children = currentNode.getChildren();

		final java.util.Comparator<Node> comparator = new java.util.Comparator<Node> () {
		    @Override
		    public int compare(Node n1, Node n2) 
		    {
			int d1 = n1.getDepth();
			int d2 = n2.getDepth();
			
			if (d1 > d2) return -1;
			else if (d1 < d2) return +1;
			else return 0;
		    }
		};

		java.util.Collections.sort(children, comparator);
	    }
	    setCurrentNode(currentNode.getChildAt(0));
	}
    }

    public void prevNode() 
    {
	if (currentNode != null && currentNode.getParent() != null) {
	    setCurrentNode(currentNode.getParent());
	}
    }

    public void nextMarkupNode() 
    {
	while (currentNode != null && currentNode.getChildCount() > 0 /* && !currentNode.isMarkup()*/) {
	    currentNode = currentNode.getChildAt(0);
	}
	throw new RuntimeException("Not yet implemented");
    }

    public void prevMarkupNode() 
    {
	throw new RuntimeException("Not yet implemented");
    }
}
