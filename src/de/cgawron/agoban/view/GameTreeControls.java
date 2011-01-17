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
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import de.cgawron.agoban.R;
import de.cgawron.agoban.SGFApplication;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;

/**
 * {@code GameTreeControls} allow to navigate through a game tree.
 *
 */
public class GameTreeControls extends LinearLayout implements View.OnClickListener, AdapterView.OnItemSelectedListener
{
    private static String TAG = "GameTreeControls";

    private SharedPreferences settings; 
    private final Button buttonNext;
    private final Button buttonPrev;
    private final Button buttonNextMarkup;
    private final Button buttonPrevMarkup;
    private final TextView moveNoView;
    private final Spinner variations;

    private GameTree gameTree;
    private VariationAdapter variationAdapter;
    private List<GameTreeNavigationListener> listeners = new ArrayList<GameTreeNavigationListener>();
    private Node currentNode;

    public interface GameTreeNavigationListener 
    {
	public void setCurrentNode(Node node);

    }

    private class VariationAdapter extends BaseAdapter implements GameTreeNavigationListener 
    {
	Node currentNode = null;

	public long getItemId(int position) 
	{
	    return position;
	}

	public int getCount() 
	{
	    Log.d(TAG, "getCount: " + (currentNode != null ? currentNode.getSiblingCount() : 0));
	    if (currentNode != null && currentNode.getParent() != null && 
		currentNode.getParent().getChildren() != null && currentNode.getParent().getChildren().size() > 1)
		return currentNode.getParent().getChildren().size();
	    else
		return 0;
	}

	public Object getItem(int position)
	{
	    Log.d(TAG, "getItem: " + position);
	    return currentNode.getParent().getChildren().get(position);
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
	    TextView view = (TextView) getDropDownView(position, convertView, parent);
	    view.setEms(3);
	    return view;
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
	    Node node = (Node) getItem(position);
	    Log.d(TAG, "getView: " + position + " " + node);
	    if (convertView == null) {
		convertView = new TextView(GameTreeControls.this.getContext());
	    }
	    ((TextView) convertView).setText(node.getName());
	    if (node.equals(currentNode))
		((TextView) convertView).setTextColor(Color.BLACK);
	    return convertView;
	}
	
	public void setCurrentNode(Node node)
	{
	    currentNode = node;
	    notifyDataSetChanged();
	}
    }


    public GameTreeControls(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
	LayoutInflater.from(context).inflate(R.layout.game_control, this);
	settings = context.getSharedPreferences(SGFApplication.PREF, 0);
	
	buttonNext = (Button) findViewById(R.id.nextNode);
	buttonPrev = (Button) findViewById(R.id.prevNode);
	buttonNextMarkup = (Button) findViewById(R.id.nextMarkup);
	buttonPrevMarkup = (Button) findViewById(R.id.prevMarkup);
	moveNoView = (TextView) findViewById(R.id.moveNo);
	variations = (Spinner) findViewById(R.id.variations);

	variationAdapter = new VariationAdapter();
	variations.setAdapter(variationAdapter);
	addGameTreeNavigationListener((GameTreeNavigationListener) variationAdapter);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GobanView);

        a.recycle();
	initView(context);
    }

    private void initView(Context context)
    {
	buttonPrev.setOnClickListener(this);
	buttonPrevMarkup.setOnClickListener(this);
	buttonNextMarkup.setOnClickListener(this);
	buttonNext.setOnClickListener(this);
	variations.setOnItemSelectedListener(this);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
    {
	Node node = (Node) variationAdapter.getItem(position);
	setCurrentNode(node);
    }

    public void onNothingSelected(AdapterView<?> parent)
    {
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

    public void addGameTreeNavigationListener(GameTreeNavigationListener listener) 
    {
	this.listeners.add(listener);
    }

    public void setGameTree(GameTree gameTree) {
	Log.d(TAG, "setGameTree: " + gameTree);
	this.gameTree = gameTree;
	if (currentNode == null || currentNode.getGameTree() != gameTree)
	    setCurrentNode(gameTree.getRoot());
    }

    public void setCurrentNode(Node node) {
	Log.d(TAG, "setCurrentNode: " + node);
	this.currentNode = node;
	moveNoView.setText(Integer.toString(node.getMoveNo()));
	for (GameTreeNavigationListener listener : listeners) {
	    listener.setCurrentNode(node);
	}
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
