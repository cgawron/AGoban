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

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.provider.GameInfo;

import de.cgawron.go.sgf.GameTree;

/**
 * Shows the game info
 */
public class ChooseSGF extends Activity
{
    private static Resources resources;

    private SGFApplication application;
    private String gitId;
    private Intent intent;
    private GameTree gameTree;

    private TextView textView;
    private ListView listView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	application = (SGFApplication) getApplication();
	resources = getResources();
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
	final Cursor cursor = resolver.query(intent.getData(), null, null, null, null);

	String[] from = new String[] { GameInfo.KEY_FILENAME, GameInfo.KEY_MODIFIED_DATE};
	
	int[] to = new int[] { R.id.filename, R.id.modified };
	
	ListAdapter adapter = new SimpleCursorAdapter(this,
						      R.layout.game_list, cursor, from, to);
	if (listView != null)
	    listView.setAdapter(adapter);

	listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		    // Build the Intent used to open WordActivity with a specific word Uri
                    Intent sgfIntent = new Intent(getApplicationContext(), EditSGF.class);
                    Uri data = SGFProvider.CONTENT_URI.buildUpon().appendQueryParameter(GameInfo.KEY_ID, String.valueOf(id)).build();
                    sgfIntent.setData(data);
		    cursor.close();
                    startActivity(sgfIntent);
		    finish();
                }
            });
    }

    @Override
    protected void onStop() {
	Log.d("ChooseSGF", "onStop");
	super.onStop();
    }

    @Override
    protected void onPause() {
	Log.d("ChooseSGF", "onPause");
	super.onPause();
    }

}
