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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    private GameTree gameTree;
    private Uri data;
    private boolean readOnly = true;

    public SGFApplication()
    {
	super();
    }

    /**
     * Initialize the application and load an SGF file if a data URI has been set.
     * Due to the small default stack size, this has to be done in a worker thread.
     */
    public void loadSGF(Context context, final Runnable loadedCB)
    {
	if (data != null)
	{
	    final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Loading " + data, false, false);

	    final Handler handler = new Handler() {
		    public void handleMessage(Message msg) {
			progressDialog.dismiss();
			if (loadedCB != null)
			    loadedCB.run();
		    }
		};
	    
	    Runnable runnable = new Runnable() {
		    public void run() {
			try {
			    InputStream is = getContentResolver().openInputStream(data);
			    gameTree = new GameTree(is);
			}
			catch (Exception ex) {
			    Log.e("EditSGF", "Exception while parsing SGF", ex);
			    gameTree = new GameTree();
			}
			Message msg = handler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("total", 100);
			msg.setData(b);
			handler.sendMessage(msg);
		    }
		};
	    
	    Thread thread = new Thread(Thread.currentThread().getThreadGroup(), runnable, "loadSGF", 64*1024);
	    thread.start();
	}
	else gameTree = new GameTree(); 
    }

    public GameTree getGameTree()
    {
	return gameTree;
    }

    public Uri getNewGameUri()
    {
	return getContentResolver().insert(SGFProvider.CONTENT_URI, null);
    }

    public void setData(Uri data)
    {
	if (data == null) {
	    data = getNewGameUri();
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
	save(false);
    }

    /**
     * Save the GameTree.
     * Due to the small default stack size, this has to be done in a worker thread.
     */
    public void save(boolean async) {
	if (data == null) {
	    setData(null);
	}

	Runnable runnable = new Runnable() {
		public void run() {
		    Log.d("SGFApplication", "saving " + data);
		    try {
			OutputStream os = getContentResolver().openOutputStream(data);
			gameTree.save(os);
			os.close();
		    }
		    catch (Exception ex) {
			Log.e("SGFApplication", "Exception while saving SGF", ex);
		    }
		}
	    };
	
	Thread thread = new Thread(Thread.currentThread().getThreadGroup(), runnable, "saveSGF", 64*1024);
	thread.start();
	while (!async && thread.isAlive()) {
	    try {
		thread.join();
	    }
	    catch (InterruptedException ex) {
	    }
	}
    }

    public void setReadOnly(boolean readOnly)
    {
	this.readOnly = readOnly;
    }

    public boolean checkNotReadOnly(Context context)
    {
	if (!readOnly)
	    return true;
	else {
	    Dialog dialog;
	    OnClickListener listener = new OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE)
			    readOnly = false;
			dialog.dismiss();
		    }
		};
	    AlertDialog.Builder builder = new AlertDialog.Builder(context);
	    builder.setTitle("Modify File?");
	    builder.setMessage("Do you want to edit this file?");
	    builder.setPositiveButton("Yes", listener);
	    builder.setNegativeButton("No", listener);
	    dialog = builder.show();
	    
	    return !readOnly;
	}
    }
}
