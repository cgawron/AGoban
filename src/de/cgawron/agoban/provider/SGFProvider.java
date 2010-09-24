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

package de.cgawron.agoban.provider;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.cgawron.agoban.provider.GameInfo.KEY_ID;
import static de.cgawron.agoban.provider.GameInfo.KEY_FILENAME;
import static de.cgawron.agoban.provider.GameInfo.KEY_MODIFIED_DATE;
import de.cgawron.go.sgf.Property.Key;

public class SGFProvider extends ContentProvider
{

    final public static Uri CONTENT_URI = new Uri.Builder().scheme("content").authority("de.cgawron.agoban").build();
    final public static String SGF_TYPE = "application/x-go-sgf";

    final static String QUERY_STRING = "_id=?";
    final static String[] COLUMNS_FILENAME_ONLY = { GameInfo.KEY_FILENAME };
    final static File SGF_DIRECTORY;
    static 
    {
	// TODO: Handle exceptions here
	SGF_DIRECTORY = new File(Environment.getExternalStorageDirectory(), "sgf");
	if (!SGF_DIRECTORY.exists())
	    SGF_DIRECTORY.mkdir();
    }

    private String[] columns = null;
    private SGFDBOpenHelper dbHelper = null;
    private SQLiteDatabase db = null;
    private long lastChecked = 0;

    private void initColumns()
    {
	ArrayList<String> _columns = new ArrayList<String>();
	_columns.add(KEY_ID);
	_columns.add(KEY_MODIFIED_DATE);
	Field[] fields = GameInfo.class.getFields();
	for (Field field : fields) {
	    try {
		if (field.getAnnotation(GameInfo.Column.class) != null) {
		    _columns.add(field.get(null).toString());
		}
	    }
	    catch (IllegalAccessException ex) {
		throw new RuntimeException(ex);
	    }
	}
	columns = _columns.toArray(new String[0]);
    }

    private Cursor queryDB(String[] projection, String query, String[] args)
    {
	Log.d("SGFProvider", String.format("queryDB(%s, %s, %s)", projection, query, args));
	
	SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
	qb.setTables(SGFDBOpenHelper.SGF_TABLE_NAME);
	Cursor cursor = qb.query(db, projection, query, args, null, null, null);
	
	return cursor;
    }

    private void updateDatabase()
    {
	Log.d("SGFProvider", "initFileMap");
	
	//fileMap = new HashMap<Integer, GameInfo>();
	String state = Environment.getExternalStorageState();
	if (Environment.MEDIA_MOUNTED.equals(state)) {
	    Log.d("SGFProvider", "reading directory " + SGF_DIRECTORY);
	    File[] files = SGF_DIRECTORY.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String fileName) 
		    {
			return fileName.endsWith(".sgf");
		    }
		});

	    for (File file : files) {
		try {
		    int id = file.hashCode();
		    long lastModified = file.lastModified();
		    if (lastModified <= lastChecked)
			continue;

		    String[] args = new String[1];
		    args[0] = Integer.toString(id);
		    Log.d("SGFProvider", "checking if information for " + file + " is available");
		    Cursor cursor = queryDB(getColumns(), QUERY_STRING, args);
		    cursor.moveToFirst();
		    Log.d("SGFProvider", "getCount(): " + cursor.getCount());
		    
		    if (cursor.getCount() > 0 && cursor.getLong(cursor.getColumnIndex(KEY_MODIFIED_DATE)) == lastModified) {
			Log.d("SGFProvider", "found entry");
			//GameInfo gameInfo = new GameInfo(file, cursor);
			//fileMap.put(file.hashCode(), gameInfo);
		    }
		    else {
			Log.d("SGFProvider", "parsing " + file);
			GameInfo gameInfo = new GameInfo(file);
			long _id = db.insertWithOnConflict(SGFDBOpenHelper.SGF_TABLE_NAME, "", 
							   gameInfo.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
			Log.d("SGFProvider", "insert: " + id + " " + _id);
			//fileMap.put(file.hashCode(), gameInfo);
		    }
		    cursor.close();
		}
		catch (Exception ex) {
		    Log.d("SGFProvider", "caught " + ex);
		    throw new RuntimeException(ex);
		}
	    }
	}
	lastChecked = System.currentTimeMillis();
    }


    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
	Log.d("SGFProvider", String.format("query(uri=%s, projection=%s, selection=%s)", uri, projection, selection));
	updateDatabase();

	String path = uri.getPath();
	Log.d("SGFProvider", String.format("path=%s", path));

	Cursor cursor = queryDB(projection, selection, selectionArgs);
	/*
	if (path.length() > 0) {
	    cursor.newRow().add(path).add(path);
	}
	else {
	    for (Map.Entry<Integer, GameInfo> entry : fileMap.entrySet()) {
		Log.d("SGFProvider", "adding " + entry.getValue().getContentValues());
		ContentValues values = entry.getValue().getContentValues();
		MatrixCursor.RowBuilder row = cursor.newRow();
		for (Map.Entry<String, Object> _entry : values.valueSet()) {
		    row.add(_entry.getValue());
		}
	    }
	}
	*/
	return cursor;
    }
	
    public Uri insert(Uri uri, ContentValues values) 
    {
	Log.d("SGFProvider", String.format("insert(uri=%s, values=%s)", uri, values));
	String path = uri.getPath();
	Log.d("SGFProvider", String.format("path=%s", path));
	if (path.equals("")) {
	    path = UUID.randomUUID().toString() + ".sgf";
	}

	uri = new Uri.Builder().scheme("content").authority("de.cgawron.agoban").path(path).build();
	Log.d("SGFProvider", String.format("insert: returning %s", uri));
	
	return uri;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
	Log.d("SGFProvider", String.format("update(uri=%s, values=%s, selection=%s)", uri, values, selection));
	return 0;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
	return 0;
    }

    public String getType(Uri uri)
    {
	return SGF_TYPE;
    }

    public boolean onCreate()
    {
	Log.d("SGFProvider", "onCreate");
	dbHelper = new SGFDBOpenHelper(getContext());
	db = dbHelper.getWritableDatabase();
	return true;
    }
    
    public String[] getColumns()
    {
	if (columns == null)
	    initColumns();

	return columns;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException
    {
	Log.d("SGFProvider", String.format("openFile(uri=%s, mode=%s)", uri, mode));
	File file;
	String query = uri.getQuery();
	if (query != null && query.length() > 0) {
	    UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(uri.toString()); 
	    String id = sanitizer.getValue(KEY_ID);
	    if (id == null) throw new IllegalArgumentException("Currently only _id is support for queries");

	    String[] args = new String[1];
	    args[0] = id;
	    Cursor cursor = queryDB(COLUMNS_FILENAME_ONLY, QUERY_STRING, args);
	    cursor.moveToFirst();
	    String fileName = cursor.getString(0);
	    Log.d("SGFProvider", "openFile: filename=" + fileName);
	    cursor.close();
	    file = new File(SGF_DIRECTORY, fileName);
	}
	else {
	    file = new File(SGF_DIRECTORY, uri.getPath());
	}
	Log.d("SGFProvider", String.format("openFile: file=%s", file));
	return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE);
    }
}
