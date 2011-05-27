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

import static de.cgawron.agoban.provider.GameInfo.KEY_FILENAME;
import static de.cgawron.agoban.provider.GameInfo.KEY_ID;
import static de.cgawron.agoban.provider.GameInfo.KEY_LOCAL_MODIFIED_DATE;
import static de.cgawron.agoban.provider.GameInfo.KEY_METADATA_DATE;
import static de.cgawron.agoban.provider.GameInfo.KEY_REMOTE_MODIFIED_DATE;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class SGFProvider extends ContentProvider
{
	private static String TAG = "SGFProvider";
	final public static String SGF_TYPE = "application/x-go-sgf";

	public final static String QUERY_STRING = "_id=?";
	final static String[] COLUMNS_FILENAME_ONLY = { GameInfo.KEY_FILENAME };
	final static String[] COLUMNS_ID_FILENAME = { KEY_ID, KEY_FILENAME };
	public final static File SGF_DIRECTORY;
	
	private static final int GAMES = 1;
	private static final int GAME_ID = 2;
	private static final int FILE_ID = 3;

	private String[] columns = null;
	private SGFDBOpenHelper dbHelper = null;
	private SQLiteDatabase db = null;
	private long lastChecked = 0;
	private static Map<Long, GameInfo> sgfMap = new HashMap<Long, GameInfo>();
	private static UriMatcher uriMatcher;

    static {
		// TODO: Handle exceptions here
		SGF_DIRECTORY = new File(Environment.getExternalStorageDirectory(), "sgf");
		if (!SGF_DIRECTORY.exists())
			SGF_DIRECTORY.mkdir();

		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(GameInfo.AUTHORITY, "games", GAMES);
        uriMatcher.addURI(GameInfo.AUTHORITY, "games/*", GAME_ID);
        uriMatcher.addURI(GameInfo.AUTHORITY, "files/*", FILE_ID);
    }

	private void initColumns()
	{
		ArrayList<String> _columns = new ArrayList<String>();
		_columns.add(KEY_ID);
		_columns.add(KEY_LOCAL_MODIFIED_DATE);
		_columns.add(KEY_REMOTE_MODIFIED_DATE);
		_columns.add(KEY_METADATA_DATE);
		Field[] fields = GameInfo.class.getFields();
		for (Field field : fields) {
			try {
				if (field.getAnnotation(GameInfo.Column.class) != null) {
					_columns.add(field.get(null).toString());
				}
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}
		columns = _columns.toArray(new String[0]);
	}

	private Cursor queryDB(String[] projection, String query, String[] args)
	{
		Log.d(TAG,
				String.format("queryDB(%s, %s, %s)", projection, query, args));

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SGFDBOpenHelper.SGF_TABLE_NAME);
		Cursor cursor = qb.query(db, projection, query, args, null, null, null);

		return cursor;
	}

	private void deleteDB(String id)
	{
		Log.i(TAG, "deleting " + id);
		String[] args = { id };
		db.delete(SGFDBOpenHelper.SGF_TABLE_NAME, QUERY_STRING, args);
	}

	Thread updateThread = null;

	private void updateDatabase()
	{
		Runnable runnable = new Runnable() {
			public void run()
			{
				doUpdateDatabase();
			}
		};

		synchronized (this) {
			if (updateThread == null) {
				updateThread = new Thread(runnable, "updateDatabase");
				updateThread.start();
			}
		}
	}

	public void doUpdateDatabase()
	{
		Log.d(TAG, "updateDatabase");
		// Debug.startMethodTracing("updateDatabase");

		// fileMap = new HashMap<Integer, GameInfo>();
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			Log.d(TAG, "reading directory " + SGF_DIRECTORY);
			File[] files = SGF_DIRECTORY.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName)
				{
					return fileName.endsWith(".sgf");
				}
			});

			for (File file : files) {
				Cursor cursor = null;
				try {
					long id = GameInfo.getId(file);
					long lastModified = file.lastModified();
					if (lastModified <= lastChecked)
						continue;

					String[] args = new String[1];
					args[0] = Long.toString(id);
					Log.d(TAG, "checking if information for " + file + " is available");
					cursor = queryDB(getColumns(), QUERY_STRING, args);
					cursor.moveToFirst();
					int indexMetadata = cursor.getColumnIndex(KEY_METADATA_DATE);
					Log.d(TAG, "getCount(): " + cursor.getCount());
					
					
					if (cursor.getCount() > 0) {
						Log.d(TAG, String.format("METADATA: db=%d file=%d", cursor.getLong(indexMetadata), lastModified));
	                     			
						if (cursor.getLong(indexMetadata) == lastModified) {
							Log.d(TAG, "found entry");
						}
						else {
							updateFile(file, false);
						}
					} else {
						updateFile(file, true);
					}
				} catch (Exception ex) {
					Log.d(TAG, "caught " + ex);
					throw new RuntimeException(ex);
				} finally {
					if (cursor != null)
						cursor.close();
				}
			}

			Cursor cursor = queryDB(COLUMNS_ID_FILENAME, null, null);
			while (cursor.moveToNext()) {
				File file = new File(SGF_DIRECTORY, cursor.getString(1));
				Log.d(TAG, "updateDatabase: checking " + file);
				if (!file.exists()) {
					Log.i(TAG, "updateDatabase: deleting stale entry for "
							+ file);
					deleteDB(cursor.getString(0));
				}
			}
			cursor.close();
		}
		lastChecked = System.currentTimeMillis();
		updateThread = null;
	}

	private void updateFile(File file, boolean insert)
	{
		Log.d(TAG, "updateFile file=" + file + ", insert=" + insert);
		GameInfo gameInfo;
		try {
			gameInfo = new GameInfo(file);
		} catch (Exception ex) {
			Log.e(TAG, "parse error: " + ex);
			ex.printStackTrace();
			return;
		}
		ContentValues contentValues = gameInfo.getContentValues();
		Log.d(TAG, "updateFile: values=" + contentValues);
		long rowId = 0;
		if (insert) {
			rowId = db.insertWithOnConflict(SGFDBOpenHelper.SGF_TABLE_NAME, "",
					                        contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			Log.d(TAG, "updateFile: new rowId=" + rowId);	
			if (rowId != 0) {
				Uri newUri = ContentUris.withAppendedId(GameInfo.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(newUri, null);
			}
		}
		else {
			update(GameInfo.CONTENT_URI, contentValues, GameInfo.KEY_ID + "=?", 
				   new String[] { Long.toString(GameInfo.getId(file)) });
		}
		
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String orderBy)
	{
		Log.d(TAG, String.format("query(uri=%s, projection=%s, selection=%s)", uri, projection, selection));

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SGFDBOpenHelper.SGF_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
		case GAMES:
			break;

		case GAME_ID:
            qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}

        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

		Log.d(TAG, String.format("query: returning cursor with %d rows", cursor.getCount()));
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
		Log.d(TAG, String.format("insert(uri=%s, values=%s)", uri, initialValues));

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        switch (uriMatcher.match(uri)) {
		case GAMES:
		case GAME_ID:
			break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}

		long rowId = db.insertWithOnConflict(SGFDBOpenHelper.SGF_TABLE_NAME, "", values, SQLiteDatabase.CONFLICT_REPLACE);		

        if (rowId != 0) {
            Uri newUri = ContentUris.withAppendedId(GameInfo.CONTENT_URI, rowId);
			Log.d(TAG, String.format("insert: returning %s", newUri));
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		Log.d(TAG, String.format("update(uri=%s, values=%s, selection=%s)", uri, values, selection));
		db.update(SGFDBOpenHelper.SGF_TABLE_NAME, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return 1;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs)
	{
		Log.d(TAG, "deleteFiles " + where);
        int count;
        switch (uriMatcher.match(uri)) {
        case GAMES:
        	deleteFiles(where, whereArgs);
            count = db.delete(SGFDBOpenHelper.SGF_TABLE_NAME, where, whereArgs);
            break;

        case GAME_ID:
            String id = uri.getPathSegments().get(1);
            deleteFiles(GameInfo.KEY_ID + "=" + id
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            count = db.delete(SGFDBOpenHelper.SGF_TABLE_NAME, GameInfo.KEY_ID + "=" + id
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	private void deleteFiles(String where, String[] whereArgs)
	{
		Log.d(TAG, "deleteFiles " + where);
		Cursor cursor = db.query(SGFDBOpenHelper.SGF_TABLE_NAME, COLUMNS_FILENAME_ONLY, 
				                 where, whereArgs, null, null, null);
		
		while (cursor.moveToNext()) {
			String fileName = cursor.getString(0);
			File file = new File(SGF_DIRECTORY, fileName);
			Log.d(TAG, "deleting " + file);
			file.delete();
		}
		cursor.close();
	}

	@Override
	public String getType(Uri uri)
	{
		String type;

        switch (uriMatcher.match(uri)) {
        case GAMES:
            type = GameInfo.CONTENT_TYPE;
			break;

        case GAME_ID:
            type = GameInfo.CONTENT_ITEM_TYPE;
			break;

        case FILE_ID:
            type = SGF_TYPE;
			break;

        default:
			Log.e(TAG, "getType: unknown uri " + uri);
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

		Log.d(TAG, "getType: " + uri + " -> " + type);
		return type;
	}

	@Override
	public boolean onCreate()
	{
		Log.d(TAG, "onCreate");
		dbHelper = new SGFDBOpenHelper(getContext());
		db = dbHelper.getWritableDatabase();
		db.setLockingEnabled(true);
		updateDatabase();
		return true;
	}

	public String[] getColumns()
	{
		if (columns == null)
			initColumns();

		return columns;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws java.io.FileNotFoundException
	{
		Log.d(TAG, String.format("openFile(uri=%s, mode=%s)", uri, mode));
		File file;
		String id;
        switch (uriMatcher.match(uri)) {
        case GAME_ID:
			id = uri.getPathSegments().get(1);
			break;

        case FILE_ID:
			id = uri.getPathSegments().get(1);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

		String[] args = new String[] { id };
		Cursor cursor = queryDB(COLUMNS_FILENAME_ONLY, QUERY_STRING, args);
		if (cursor.getCount() < 1)
			throw new IllegalArgumentException("Unknown URI " + uri);
		cursor.moveToFirst();
		String fileName = cursor.getString(0);
		Log.d(TAG, "openFile: filename=" + fileName);
		cursor.close();
		file = new File(SGF_DIRECTORY, fileName);

		Log.d(TAG, String.format("openFile: file=%s, read=%b write=%b", file,
								 file.canRead(), file.canWrite()));
		int _mode = ParcelFileDescriptor.MODE_CREATE;
		if (mode.contains("w"))
			_mode |= ParcelFileDescriptor.MODE_READ_WRITE;
		else
			_mode |= ParcelFileDescriptor.MODE_READ_ONLY;
		if (mode.contains("t"))
			_mode |= ParcelFileDescriptor.MODE_TRUNCATE;
		if (mode.contains("a"))
			_mode |= ParcelFileDescriptor.MODE_APPEND;
		Log.d(TAG, String.format("openFile: file=%s, mode=%s", file, _mode));
		return ParcelFileDescriptor.open(file, _mode);
	}

	public static GameInfo getGameInfo(long id)
	{
		Log.d(TAG, String.format("getGameInfo(%d)=%s", id, sgfMap.get(id)));
		return sgfMap.get(id);
	}

	public static File getSGFDirectory()
	{
		return SGF_DIRECTORY;
	}

}
