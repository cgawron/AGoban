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
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SGFProvider extends ContentProvider
{
    final public static Uri CONTENT_URI = new Uri.Builder().scheme("content").authority("de.cgawron.agoban").build();
    final public static String SGF_TYPE = "application/x-go-sgf";
    final public static String KEY_ID = "_id";
    final public static String KEY_FILENAME = "filename";

    final static File SGF_DIRECTORY = new File("/data/data/de.cgawron.agoban");
    final static String[] COLUMNS = { KEY_ID, KEY_FILENAME };

    private Map<Integer, String> fileMap = null;

    private void initFileMap()
    {
	fileMap = new HashMap<Integer, String>();

	String[] files = SGF_DIRECTORY.list(new FilenameFilter() {
		public boolean accept(File dir, String fileName) 
		{
		    return fileName.endsWith(".sgf");
		}
	    });

	for (String file : files) {
	    fileMap.put(file.hashCode(), file);
	}
    }


    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
	MatrixCursor cursor = new MatrixCursor(COLUMNS);
	Log.d("SGFProvider", String.format("query(uri=%s, projection=%s, selection=%s)", uri, projection, selection));
	String path = uri.getPath();
	Log.d("SGFProvider", String.format("path=%s", path));

	if (path.length() > 0) {
	    cursor.newRow().add(path).add(path);
	}
	else {
	    if (fileMap == null)
		initFileMap();
	    
	 
	    for (Map.Entry<Integer, String> entry : fileMap.entrySet()) {
		Log.d("SGFProvider", "adding " + entry);
		cursor.newRow().add(entry.getKey()).add(entry.getValue());
	    }
	}
	
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
	return true;
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
	    if (fileMap == null) initFileMap();
	    file = new File(SGF_DIRECTORY, fileMap.get(Integer.parseInt(id)));
	}
	else {
	    file = new File(SGF_DIRECTORY, uri.getPath());
	}
	Log.d("SGFProvider", String.format("openFile: file=%s", file));
	return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE);
    }
}
