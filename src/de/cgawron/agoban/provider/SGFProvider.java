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
import static de.cgawron.agoban.provider.GameInfo.KEY_MODIFIED_DATE;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class SGFProvider extends ContentProvider 
{
	private static String TAG = "SGFProvider";
	final public static String AUTHORITY = "de.cgawron.agoban";
	final public static Uri CONTENT_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).build();
	final public static String SGF_TYPE = "application/x-go-sgf";

	public final static String QUERY_STRING = "_id=?";
	final static String[] COLUMNS_FILENAME_ONLY = { GameInfo.KEY_FILENAME };
	final static String[] COLUMNS_ID_FILENAME = { KEY_ID, KEY_FILENAME };
	public final static File SGF_DIRECTORY;
	static {
		// TODO: Handle exceptions here
		SGF_DIRECTORY = new File(Environment.getExternalStorageDirectory(),
				"sgf");
		if (!SGF_DIRECTORY.exists())
			SGF_DIRECTORY.mkdir();
	}

	private String[] columns = null;
	private SGFDBOpenHelper dbHelper = null;
	private SQLiteDatabase db = null;
	private long lastChecked = 0;
	private static Map<Long, GameInfo> sgfMap = new HashMap<Long, GameInfo>();

	private void initColumns() {
		ArrayList<String> _columns = new ArrayList<String>();
		_columns.add(KEY_ID);
		_columns.add(KEY_MODIFIED_DATE);
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

	private Cursor queryDB(String[] projection, String query, String[] args) {
		Log.d(TAG,
				String.format("queryDB(%s, %s, %s)", projection, query, args));

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SGFDBOpenHelper.SGF_TABLE_NAME);
		Cursor cursor = qb.query(db, projection, query, args, null, null, null);

		return cursor;
	}

	private void deleteDB(String id) {
		Log.i(TAG, "deleting " + id);
		String[] args = { id };
		db.delete(SGFDBOpenHelper.SGF_TABLE_NAME, QUERY_STRING, args);
	}

	Thread updateThread = null;

	private void updateDatabase() {
		Runnable runnable = new Runnable() {
			public void run() {
				doUpdateDatabase();
			}
		};

		synchronized (this) {
			if (updateThread == null) {
				updateThread = new Thread(Thread.currentThread()
						.getThreadGroup(), runnable, "updateDatabase",
						64 * 1024);
				updateThread.start();
			}
		}
	}

	public void doUpdateDatabase() {
		Log.d(TAG, "updateDatabase");
		// Debug.startMethodTracing("updateDatabase");

		// fileMap = new HashMap<Integer, GameInfo>();
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			Log.d(TAG, "reading directory " + SGF_DIRECTORY);
			File[] files = SGF_DIRECTORY.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.endsWith(".sgf");
				}
			});

			for (File file : files) {
				Cursor cursor = null;
				try {
					int id = file.hashCode();
					long lastModified = file.lastModified();
					if (lastModified <= lastChecked)
						continue;

					String[] args = new String[1];
					args[0] = Integer.toString(id);
					Log.d(TAG, "checking if information for " + file
							+ " is available");
					cursor = queryDB(getColumns(), QUERY_STRING, args);
					cursor.moveToFirst();
					Log.d(TAG, "getCount(): " + cursor.getCount());

					if (cursor.getCount() > 0
							&& cursor.getLong(cursor
									.getColumnIndex(KEY_MODIFIED_DATE)) == lastModified) {
						Log.d(TAG, "found entry");
					} else {
						Log.d(TAG, "parsing " + file);
						GameInfo gameInfo;
						try {
							gameInfo = new GameInfo(file);
						} catch (Exception ex) {
							Log.e(TAG, "parse error: " + ex);
							ex.printStackTrace();
							continue;
						}
						long _id = db.insertWithOnConflict(
								SGFDBOpenHelper.SGF_TABLE_NAME, "",
								gameInfo.getContentValues(),
								SQLiteDatabase.CONFLICT_REPLACE);
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

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(TAG, String.format("query(uri=%s, projection=%s, selection=%s)",
				uri, projection, selection));
		updateDatabase();

		String path = uri.getPath();
		Log.d(TAG, String.format("path=%s", path));

		Cursor cursor = queryDB(projection, selection, selectionArgs);
		Log.d(TAG,
				String.format("query: returning cursor with %d rows",
						cursor.getCount()));
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, String.format("insert(uri=%s, values=%s)", uri, values));
		String path = uri.getPath();
		Log.d(TAG, String.format("path=%s", path));
		if (path.equals("")) {
			path = UUID.randomUUID().toString() + ".sgf";
		}

		uri = new Uri.Builder().scheme("content")
				.authority("de.cgawron.agoban").path(path).build();
		Log.d(TAG, String.format("insert: returning %s", uri));

		return uri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.d(TAG, String.format("update(uri=%s, values=%s, selection=%s)",
				uri, values, selection));
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return SGF_TYPE;
	}

	@Override
	public boolean onCreate() {
		Log.d(TAG, "onCreate");
		dbHelper = new SGFDBOpenHelper(getContext());
		db = dbHelper.getWritableDatabase();
		db.setLockingEnabled(true);
		return true;
	}

	public String[] getColumns() {
		if (columns == null)
			initColumns();

		return columns;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws java.io.FileNotFoundException {
		Log.d(TAG, String.format("openFile(uri=%s, mode=%s)", uri, mode));
		File file;
		String query = uri.getQuery();
		if (query != null && query.length() > 0) {
			UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(uri.toString());
			String id = sanitizer.getValue(KEY_ID);
			if (id == null)
				throw new IllegalArgumentException(
						"Currently only _id is support for queries");

			String[] args = new String[1];
			args[0] = id;
			Cursor cursor = queryDB(COLUMNS_FILENAME_ONLY, QUERY_STRING, args);
			cursor.moveToFirst();
			String fileName = cursor.getString(0);
			Log.d(TAG, "openFile: filename=" + fileName);
			cursor.close();
			file = new File(SGF_DIRECTORY, fileName);
		} else {
			file = new File(SGF_DIRECTORY, uri.getPath());
		}
		Log.d(TAG,
				String.format("openFile: file=%s, read=%b write=%b", file,
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

	public static GameInfo getGameInfo(long id) {
		Log.d(TAG, String.format("getGameInfo(%d)=%s", id, sgfMap.get(id)));
		return sgfMap.get(id);
	}

	public static File getSGFDirectory() {
		return SGF_DIRECTORY;
	}

}
