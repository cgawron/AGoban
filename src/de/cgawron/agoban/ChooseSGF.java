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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import de.cgawron.agoban.view.PropertyView;
import de.cgawron.agoban.provider.GameInfo;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.sync.GoogleSync;

/**
 * Shows the game info
 */
public class ChooseSGF extends Activity implements ViewBinder {
	private static String TAG = "ChooseSGF";
	private static DateFormat dateFormat = DateFormat.getDateInstance();

	private SGFApplication application;
	private String gitId;
	private Intent intent;

	private TextView textView;
	private ListView listView;
	private ViewGroup footerView;

	private Cursor cursor = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (SGFApplication) getApplication();
		try {
			PackageItemInfo info = getPackageManager().getActivityInfo(
					new ComponentName(this, ChooseSGF.class),
					PackageManager.GET_META_DATA);
			gitId = info.metaData.getString("git-id");
		} catch (Exception e) {
			throw new RuntimeException("git-id", e);
		}
		Log.d(TAG, "git-id: " + gitId);

		intent = getIntent();
		Log.d(TAG, String.format("onCreate: intent=%s", intent));
		setContentView(R.layout.choose_sgf_dialog);

		textView = (TextView) findViewById(R.id.text);
		listView = (ListView) findViewById(R.id.list);
		// footerView = (ViewGroup) findViewById(R.id.footer);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "OnStart");

		ContentResolver resolver = getContentResolver();
		cursor = managedQuery(intent.getData(), null, null, null, null);
		Log.d(TAG, String.format(
				"query: returned cursor with %d rows, position=%d",
				cursor.getCount(), cursor.getPosition()));

		String[] from = new String[] { GameInfo.KEY_FILENAME,
				GameInfo.KEY_MODIFIED_DATE, GameInfo.KEY_PLAYER_WHITE,
				GameInfo.KEY_PLAYER_BLACK, GameInfo.KEY_RESULT };

		int[] to = new int[] { R.id.filename, R.id.modified, R.id.player_white,
				R.id.player_black, R.id.result };

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.game_list, cursor, from, to);
		adapter.setViewBinder(this);

		listView.setAdapter(adapter);

		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Uri data = SGFProvider.CONTENT_URI
						.buildUpon()
						.appendQueryParameter(GameInfo.KEY_ID,
								String.valueOf(id)).build();
				Intent sgfIntent = new Intent(Intent.ACTION_VIEW, data);
				startActivity(sgfIntent);
				finish();
				return true;
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				updateFooter(cursor, position);
			}
		});

		listView.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				updateFooter(cursor, position);
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// updateFooter(null);
			}
		});
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		if (cursor != null)
			cursor.close();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
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

		case R.id.google_sync:
			googleSync();
			return true;

		case R.id.new_game:
			newGame();
			return true;

		case R.id.preferences:
			editPreferences();
			return true;

		case R.id.about:
			Context context = getApplicationContext();
			CharSequence text = String.format(
					"AGoban, (c)2010 Christian Gawron\nGit-Id: %s", gitId);
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		}
		return false;
	}

	public void newGame() {
		Intent sgfIntent = new Intent(Intent.ACTION_INSERT,
				application.getNewGameUri());
		startActivity(sgfIntent);
		finish();
	}

	public void editPreferences() {
		Intent intent = new Intent(this, SGFApplication.EditPreferences.class);
		Log.d(TAG, "Starting " + intent);
		startActivity(intent);
	}

	public void googleSync() {
		Intent searchSGF = new Intent(Intent.ACTION_SEARCH,
				SGFProvider.CONTENT_URI, this, GoogleSync.class);
		startActivity(searchSGF);
		finish();
	}

	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		Log.d(TAG,
				"setViewValue: " + cursor + ", position="
						+ cursor.getPosition());
		String columnName = cursor.getColumnName(columnIndex);
		int colDate = cursor.getColumnIndex(GameInfo.KEY_DATE);
		Log.d(TAG,
				"setViewValue: " + cursor + ", position="
						+ cursor.getPosition() + ", column=" + columnName);

		if (view.getId() == R.id.modified) {
			TextView text = (TextView) view;
			String sgfDate = cursor.getString(colDate);
			if (sgfDate != null && sgfDate.length() > 0)
				text.setText(sgfDate);
			else
				text.setText(dateFormat.format(new Date(cursor
						.getLong(columnIndex))));
			return true;
		}
		return false;
	}

	private void updateFooter(Cursor cursor, int position) {
		Log.d(TAG, String.format("updateFooter(%s, %d)", cursor, position));

		if (footerView != null)
			for (int i = 0; i < footerView.getChildCount(); i++) {
				PropertyView view = (PropertyView) footerView.getChildAt(i);
				view.setValue(cursor, position);
			}
	}
}
