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

import android.app.Application;
import android.util.Log;
import android.net.Uri;

import de.cgawron.go.sgf.GameTree;
import de.cgawron.agoban.provider.SGFProvider;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Application class for EditSGF.
 * Used to hold instances of @class{GameTree} and to pass them between activities. 
 */
public class SGFApplication extends Application
{
    public static String KEY_DEFAULT = "default";

    private Map<String, GameTree> gameTreeMap;
    private Uri data;

    public SGFApplication()
    {
	super();
	gameTreeMap = new java.util.HashMap<String, GameTree>();
    }

    public void init()
    {
	GameTree gameTree = null;
	if (data != null)
	{
	    try {
		InputStream is = getContentResolver().openInputStream(data);
		gameTree = new GameTree(new InputStreamReader(is));
		put(KEY_DEFAULT, gameTree); 
	    }
	    catch (Exception ex) {
		Log.e("EditSGF", "Exception while parsing SGF", ex);
		gameTree = new GameTree();
		put(KEY_DEFAULT, gameTree); 
	    }
	    
	}
	else gameTree = new GameTree();
	put(KEY_DEFAULT, gameTree); 
    }

    public void put(String key, GameTree gameTree)
    {
	gameTreeMap.put(key, gameTree);
    }

    public GameTree get(String key)
    {
	return gameTreeMap.get(key);
    }

    public GameTree getGameTree()
    {
	return get(KEY_DEFAULT);
    }

    public void setData(Uri data)
    {
	if (data == null) {
	    data = getContentResolver().insert(SGFProvider.CONTENT_URI, null);
	}
	this.data = data;
    }

    public Uri getData()
    {
	return data;
    }

    public File getNewFile() {
	return getFileStreamPath(UUID.randomUUID().toString() + ".sgf");
    }

    public void save() {
	if (data == null) {
	    setData(null);
	}
	
	Log.d("SGFApplication", "saving " + data);
	try {
	    OutputStream os = getContentResolver().openOutputStream(data);
	    gameTreeMap.get(KEY_DEFAULT).save(os);
	    os.close();
	}
	catch (Exception ex) {
	    Log.e("SGFApplication", "Exception while saving SGF", ex);
       }
    }
}
