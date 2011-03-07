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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
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
public class GameTreeControls extends LinearLayout implements
		View.OnClickListener, AdapterView.OnItemSelectedListener
{
	private static String TAG = "GameTreeControls";

	private final SharedPreferences settings;
	private final Button buttonNext;
	private final Button buttonPrev;
	private final Button buttonLast;
	private final Button buttonFirst;
	private final TextView moveNoView;
	private final Spinner variations;

	private GameTree gameTree;
	private final VariationAdapter variationAdapter;
	private final List<GameTreeNavigationListener> listeners = new ArrayList<GameTreeNavigationListener>();
	private Node currentNode;

	public interface GameTreeNavigationListener
	{
		public void setCurrentNode(Node node);

	}

	public class SavedState extends View.BaseSavedState
	{
		private final int nodeId;

		private SavedState(Parcel in)
		{
			super(in);
			nodeId = in.readInt();
		}

		public SavedState(Parcelable superState, Node n)
		{
			super(superState);
			nodeId = n.getId();
		}

		@Override
		public void writeToParcel(Parcel out, int flags)
		{
			out.writeInt(nodeId);
		}

		@Override
		public String toString()
		{
			return "GameTreeControls.SavedState " + nodeId;
		}

		/*
		 * public static final Parcelable.Creator<SavedState> CREATOR = new
		 * Parcelable.Creator<SavedState>() { public SavedState
		 * createFromParcel(Parcel in) { return new SavedState(in); }
		 * 
		 * public SavedState[] newArray(int size) { return new SavedState[size];
		 * } };
		 */

		public Node getNode(GameTree gameTree)
		{
			return gameTree.getNode(nodeId);
		}
	}

	private class VariationAdapter extends BaseAdapter
	{
		Node currentNode = null;

		public long getItemId(int position)
		{
			return position;
		}

		public int getCount()
		{
			Log.d(TAG,
					"getCount: "
							+ (currentNode != null ? currentNode
									.getSiblingCount() : 0));
			if (currentNode != null && currentNode.getParent() != null
					&& currentNode.getParent().getChildren() != null
					&& currentNode.getParent().getChildren().size() > 1)
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
			TextView view = (TextView) getDropDownView(position, convertView,
					parent);
			view.setEms(3);
			return view;
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent)
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

		public int getPosition(Node node)
		{
			if (currentNode.getParent() != null)
				return currentNode.getParent().getChildren().indexOf(node);
			else
				return 0;
		}

		public void setCurrentNode(Node node, AbsSpinner spinner)
		{
			Log.d(TAG, "VariationTreeAdapter.setCurrentNode: " + node);
			currentNode = node;
			spinner.setSelection(getPosition(node));
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
		buttonLast = (Button) findViewById(R.id.lastNode);
		buttonFirst = (Button) findViewById(R.id.firstNode);
		moveNoView = (TextView) findViewById(R.id.moveNo);
		variations = (Spinner) findViewById(R.id.variations);

		variationAdapter = new VariationAdapter();
		variations.setAdapter(variationAdapter);

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.GobanView);

		a.recycle();
		initView(context);
	}

	private void initView(Context context)
	{
		buttonPrev.setOnClickListener(this);
		buttonFirst.setOnClickListener(this);
		buttonLast.setOnClickListener(this);
		buttonNext.setOnClickListener(this);
		variations.setOnItemSelectedListener(this);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id)
	{
		Node node = (Node) variationAdapter.getItem(position);
		Log.d(TAG, "onItemSelected: " + node);
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
		} else if (buttonFirst.equals(v)) {
			Log.d(TAG, "button pressed: prevMarkup");
			firstNode();
		} else if (buttonNext.equals(v)) {
			Log.d(TAG, "button pressed: next");
			nextNode();
		} else if (buttonLast.equals(v)) {
			Log.d(TAG, "button pressed: nextMarkup");
			lastNode();
		}
	}

	public void addGameTreeNavigationListener(
			GameTreeNavigationListener listener)
	{
		this.listeners.add(listener);
	}

	public void setGameTree(GameTree gameTree)
	{
		Log.d(TAG, "setGameTree: " + gameTree);
		this.gameTree = gameTree;
		if (currentNode == null || currentNode.getGameTree() != gameTree)
			setCurrentNode(gameTree.getRoot());
	}

	public void setCurrentNode(Node node)
	{
		Log.d(TAG, "setCurrentNode: " + node);
		this.currentNode = node;
		variationAdapter.setCurrentNode(node, variations);
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

				final java.util.Comparator<Node> comparator = new java.util.Comparator<Node>() {
					public int compare(Node n1, Node n2)
					{
						int d1 = n1.getDepth();
						int d2 = n2.getDepth();

						if (d1 > d2)
							return -1;
						else if (d1 < d2)
							return +1;
						else
							return 0;
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

	public void lastNode()
	{
		while (currentNode != null && currentNode.getChildCount() > 0) {
			currentNode = currentNode.getChildAt(0);
		}
		setCurrentNode(currentNode);
	}

	public void firstNode()
	{
		while (currentNode != null && currentNode.getParent() != null) {
			currentNode = currentNode.getParent();
		}
		setCurrentNode(currentNode);
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable state = super.onSaveInstanceState();
		Log.d(TAG, "onSaveInstanceState: superState class: " + state.getClass());

		state = new SavedState(state, currentNode);
		Log.d(TAG, "onSaveInstanceState: " + state);
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		Log.d(TAG, "onRestoreInstanceState: " + state + " " + state.getClass());
		SavedState saved = (SavedState) state;
		super.onRestoreInstanceState(saved.getSuperState());
		Node node = saved.getNode(gameTree);
		setCurrentNode(node);
	}
}
