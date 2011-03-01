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
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.sync.GoogleSync;
import de.cgawron.agoban.view.GameTreeControls;
import de.cgawron.agoban.view.GameTreeControls.GameTreeNavigationListener;
import de.cgawron.agoban.view.GobanView;
import de.cgawron.agoban.view.GobanView.GobanContextMenuInfo;
import de.cgawron.agoban.view.tool.MoveTool;
import de.cgawron.go.Goban;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;

/**
 * Provides an sgf editor.
 */
public class EditSGF extends Activity implements GobanEventListener,
		GameTreeNavigationListener, SGFApplication.ExceptionHandler {
	private static final String TAG = "EditSGF";
	private static final String KEY_URI = "de.cgawron.agoban:URI";

	public static Resources resources;

	private GobanView gobanView;
	private TextView titleView;
	private TextView commentView;
	private GameTree gameTree;
	private GameTreeControls gameTreeControls;
	private Node currentNode;
	private final Map<Point, Node> variations = new HashMap<Point, Node>();
	private SGFApplication application;
	private SharedPreferences settings;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "oncreate: " + savedInstanceState);
		application = (SGFApplication) getApplication();
		resources = getResources();
		settings = getSharedPreferences(SGFApplication.PREF, 0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.custom_title);

		gobanView = (GobanView) findViewById(R.id.goban);
		gobanView.addGobanEventListener(this);
		registerForContextMenu(gobanView);

		GobanView.Tool tool = new MoveTool(this);
		gobanView.setTool(tool);
		titleView = (TextView) findViewById(R.id.title);
		// commentView = (TextView) findViewById(R.id.comment);

		gameTreeControls = (GameTreeControls) findViewById(R.id.controls);
		gameTreeControls.addGameTreeNavigationListener(this);

		Uri savedUri = null;
		if (savedInstanceState != null) {
			savedUri = savedInstanceState.getParcelable(KEY_URI);
		}

		Intent intent = getIntent();
		if (intent.getData() == null)
			intent.setData(savedUri);
		if (Intent.ACTION_EDIT.equals(intent.getAction()))
			application.setReadOnly(false);

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

		Intent intent = getIntent();
		Log.d(TAG, "Uri: " + intent.getData());
		if (intent.getData() == null
				|| Intent.ACTION_INSERT.equals(intent.getAction())) {
			Log.d(TAG, "onStart: new file");
			Uri data = application.getNewGameUri();
			application.setData(data);
			application.setReadOnly(false);
			intent.setData(data);
			setGameTree(new GameTree());
			application.initProperties(gameTree);
			gameTree.setModified(false);
		} else {
			Log.d(TAG, "onStart: EDIT");
			application.setData(intent.getData());
			final Runnable afterLoaded = new Runnable() {
				public void run() {
					gameTree = application.getGameTree();
					if (gameTree != null) {
						setGameTree(gameTree);
					}
				}
			};

			application.loadSGF(this, afterLoaded, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		Intent intent = new Intent(null, getIntent().getData());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
				new ComponentName(this, EditSGF.class), null, intent, 0, null);
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
			CharSequence text = String.format(
					"AGoban, (c)2010 Christian Gawron\nGit-Id: %s",
					application.gitId);
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onGobanEvent(GobanEvent gobanEvent) {
		Log.d(TAG, "onGobanEvent: " + gobanEvent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		Log.d(TAG, "onCreateContextMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stone_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		GobanContextMenuInfo info = (GobanContextMenuInfo) item.getMenuInfo();
		Log.d(TAG, item.toString() + " " + info);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_URI, application.getData());
		Log.d(TAG, "onSaveInstanceState: " + outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {
		Log.d(TAG, "onRestoreInstanceState: " + savedState);
		super.onRestoreInstanceState(savedState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");

		if (gameTree != null)
			save();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop");

		if (gameTree != null)
			save();
	}

	public Node getCurrentNode() {
		return currentNode;
	}

	public void setCurrentNode(Node node) {
		Log.d(TAG, "setCurrentNode: new=" + node + ", old=" + currentNode);
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
					gobanView.markLastMove(lastMove);

				doMarkup(gobanView, currentNode);
				gobanView.setGoban(goban);
				// commentView.setText(currentNode.getComment());
			}
			gameTreeControls.setCurrentNode(node);
		}
	}

	private void doMarkup(GobanView view, Node node) {
		Goban goban = node.getGoban();
		for (Property property : node.values()) {
			if (property instanceof Property.Markup) {
				view.addMarkup(goban, (Property.Markup) property);
			}
		}
	}

	private void markVariation(GobanView view, Node node) {
		Point point = null;
		if (node.contains(Property.BLACK)) {
			point = node.getPoint(Property.BLACK);
		} else if (node.contains(Property.WHITE)) {
			point = node.getPoint(Property.WHITE);
		}

		if (point != null) {
			variations.put(point, node);
			view.addVariation(point);
		}
	}

	public Map<Point, Node> getVariations() {
		return variations;
	}

	public boolean checkNotReadOnly() {
		return application.checkNotReadOnly(this);
	}

	public void save() {
		application.save();
	}

	public void editPreferences() {
		Intent intent = new Intent(this, SGFApplication.EditPreferences.class);
		Log.d(TAG, "Starting " + intent);
		startActivity(intent);
	}

	public void open(View view) {
		open();
	}

	public void open() {
		Log.d(TAG, "open()");
		Intent intent = new Intent(Intent.ACTION_SEARCH,
				SGFProvider.CONTENT_URI, this, ChooseSGF.class);
		startActivity(intent);
		finish();
	}

	public void newGame() {
		Intent sgfIntent = new Intent(Intent.ACTION_INSERT,
				application.getNewGameUri());
		startActivity(sgfIntent);
		finish();
	}

	public GameTree getGameTree() {
		return gameTree;
	}

	private void setGameTree(GameTree gameTree) {
		this.gameTree = gameTree;
		application.setGameTree(gameTree);
		gameTreeControls.setGameTree(gameTree);
		titleView.setText(gameTree.getGameName());

		if (gameTree.getRoot() instanceof de.cgawron.go.sgf.CollectionRoot) {
			Toast toast = Toast.makeText(
					getApplicationContext(),
					String.format("CollectionRoot: %s, %d children",
							gameTree.getRoot(), gameTree.getRoot()
									.getChildren().size()), Toast.LENGTH_LONG);
			toast.show();
		}

	}

	public void googleSync() {
		Intent send = new Intent(Intent.ACTION_SEND, application.getData(),
				this, GoogleSync.class);
		send.setType("application/x-go-sgf");
		send.putExtra(Intent.EXTRA_STREAM, application.getData());
		startActivity(send);
		finish();
	}

	public void showGameInfo(View view) {
		showGameInfo();
	}

	public void showGameInfo() {
		Log.d(TAG, "Show game info");
		Intent viewGameInfo = new Intent(Intent.ACTION_VIEW,
				application.getData());
		viewGameInfo.setClassName("de.cgawron.agoban",
				"de.cgawron.agoban.ShowGameInfo");

		Log.d(TAG, "thread: " + Thread.currentThread().getId() + " "
				+ viewGameInfo.getClass().toString());
		startActivity(viewGameInfo);
	}

	public void handleException(final String message, Throwable ex) {
		Runnable runnable = new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						EditSGF.this);
				builder.setMessage(message)
						.setCancelable(false)
						.setPositiveButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										EditSGF.this.finish();
									}
								});
				AlertDialog alert = builder.show();
			}
		};
		runOnUiThread(runnable);
	}
}
