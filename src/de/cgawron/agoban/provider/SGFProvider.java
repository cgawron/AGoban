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
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

public class SGFProvider extends ContentProvider
{
    final public static Uri CONTENT_URI = new Uri.Builder().scheme("content").authority("de.cgawron.agoban").build();
    final public static String SGF_TYPE = "application/x-go-sgf";

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
	MatrixCursor cursor = new MatrixCursor();
	Log.d("SGFProvider", String.format("query(uri=%s)", uri));
	String path = uri.getPath();
	Log.d("SGFProvider", String.format("path=%s", path));
	return cursor;
    }
	
    public Uri insert(Uri uri, ContentValues values) 
    {
	return null;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
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
}
