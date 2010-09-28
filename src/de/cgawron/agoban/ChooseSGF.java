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
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import de.cgawron.agoban.provider.GameInfo;
import de.cgawron.agoban.provider.SGFProvider;

/**
 * Shows the game info
 */
public class ChooseSGF extends Activity implements ViewBinder
{
    private static DateFormat dateFormat = DateFormat.getDateInstance();

    private SGFApplication application;
    private String gitId;
    private Intent intent;

    private TextView textView;
    private ListView listView;

    private Cursor cursor = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	application = (SGFApplication) getApplication();
    	try {
    		PackageItemInfo info = getPackageManager().getActivityInfo(new ComponentName(this, ChooseSGF.class), PackageManager.GET_META_DATA);
    		gitId = info.metaData.getString("git-id");
    	}
    	catch (Exception e)
    	{
    		throw new RuntimeException("git-id", e);
    	}
    	Log.d("ChooseSGF", "git-id: " + gitId);

    	intent = getIntent();
    	Log.d("ChooseSGF", String.format("onCreate: intent=%s", intent));
    	setContentView(R.layout.choose_sgf_dialog);

    	textView = (TextView) findViewById(R.id.text);
    	listView = (ListView) findViewById(R.id.list);
    }

    @Override
    public void onStart() {
	super.onStart();
	Log.d("ChooseSGF", "OnStart");

	ContentResolver resolver = getContentResolver();
	cursor = resolver.query(intent.getData(), null, null, null, null);

	String[] from = new String[] {GameInfo.KEY_FILENAME, GameInfo.KEY_MODIFIED_DATE, 
				      GameInfo.KEY_PLAYER_WHITE, GameInfo.KEY_PLAYER_BLACK, GameInfo.KEY_RESULT};
	
	int[] to = new int[] {R.id.filename, R.id.modified, R.id.player_white, R.id.player_black, R.id.result};
	
	SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
							      R.layout.game_list, cursor, from, to);
	adapter.setViewBinder(this);

	if (listView != null)
	    listView.setAdapter(adapter);

	listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		    // Build the Intent used to open WordActivity with a specific word Uri
                    Intent sgfIntent = new Intent(getApplicationContext(), EditSGF.class);
                    Uri data = SGFProvider.CONTENT_URI.buildUpon().appendQueryParameter(GameInfo.KEY_ID, String.valueOf(id)).build();
                    sgfIntent.setData(data);
                    startActivity(sgfIntent);
		    finish();
                }
            });
    }

    @Override
    protected void onStop() {
	Log.d("ChooseSGF", "onStop");
	super.onStop();
	if (cursor != null)
	    cursor.close();
    }

    @Override
    protected void onPause() {
	Log.d("ChooseSGF", "onPause");
	super.onPause();
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex)
    {
	String columnName = cursor.getColumnName(columnIndex);
	int colDate = cursor.getColumnIndex(GameInfo.KEY_DATE);
	Log.d("ChooseSGF", "setViewValue: " + cursor + ", column=" + columnName);
	
	if (view.getId() == R.id.modified) {
	    TextView text = (TextView) view;
	    String sgfDate = cursor.getString(colDate);
	    if (sgfDate != null && sgfDate.length() > 0) 
		text.setText(sgfDate);
	    else
		text.setText(dateFormat.format(new Date(cursor.getLong(columnIndex))));
	    return true;
	}
	return false;
    }
}
