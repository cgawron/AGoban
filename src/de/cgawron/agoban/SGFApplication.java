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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.net.Uri;

import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;
import de.cgawron.agoban.provider.SGFProvider;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;

/**
 * Application class for EditSGF. Used to hold instances of @class{GameTree} and
 * to pass them between activities.
 */
public class SGFApplication extends Application {
	private static final String TAG = "SGFApplication";
	public static final String PREF = "AGoban";
	public static String KEY_DEFAULT = "default";

	private GameTree gameTree;
	private Uri data;
	private boolean readOnly = true;
	private Map<Uri, GameTree> gameMap = new WeakHashMap<Uri, GameTree>();
	String gitId;

	public interface ExceptionHandler {
		public void handleException(String message, Throwable t);
	}

	public static class EditPreferences extends PreferenceActivity {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesName(PREF);
			addPreferencesFromResource(R.xml.prefs);
		}
	}

	public SGFApplication() {
		super();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			PackageItemInfo info = getPackageManager().getActivityInfo(
					new ComponentName(this, EditSGF.class),
					PackageManager.GET_META_DATA);
			gitId = info.metaData.getString("git-id");
		} catch (Exception e) {
			throw new RuntimeException("git-id", e);
		}
		Log.d(TAG, "git-id: " + gitId);
	}

	/**
	 * Initialize the application and load an SGF file if a data URI has been
	 * set. Due to the small default stack size, this has to be done in a worker
	 * thread.
	 */
	public void loadSGF(Context context, final Runnable loadedCB,
			final ExceptionHandler exceptionHandler) {
		if (data != null) {
			gameTree = gameMap.get(data);
			if (gameTree == null) {
				final ProgressDialog progressDialog = ProgressDialog.show(
						context, "", "Loading " + data, false, false);

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
							InputStream is = getContentResolver()
									.openInputStream(data);
							// The cup parser (or my code around it?) seems to
							// have a multithreading problem
							synchronized (de.cgawron.go.sgf.Parser.class) {
								gameTree = new GameTree(is);
								gameMap.put(data, gameTree);
							}
						} catch (Exception ex) {
							Log.e(TAG, "Exception while parsing SGF", ex);
							exceptionHandler.handleException(
									"Exception while parsing SGF", ex);
						}
						Message msg = handler.obtainMessage();
						Bundle b = new Bundle();
						b.putInt("total", 100);
						msg.setData(b);
						handler.sendMessage(msg);
					}
				};

				Thread thread = new Thread(Thread.currentThread()
						.getThreadGroup(), runnable, "loadSGF", 64 * 1024);
				thread.start();
			} else {
				if (loadedCB != null)
					loadedCB.run();
			}
		}
		/*
		 * else { gameTree = new GameTree(); initProperties(gameTree); }
		 */
	}

	/**
	 * Initialize properties of a new {@code GameTree}. This routine sets the
	 * following properties:
	 * <ul>
	 * <li>DaTe
	 * <li>PlaCe
	 * <li>GaMe
	 * <li>SiZe
	 * <li>APplication
	 * <li>FileFormat
	 * </ul>
	 */
	public void initProperties(GameTree gameTree) {
		Log.d(TAG, "initProperties");
		Node root = gameTree.getRoot();

		root.setFileFormat(4);
		root.setGame(1);
		root.setProperty(Property.APPLICATION, "AGoban:" + gitId);

		Charset defaultCharset = Charset.defaultCharset();
		if (defaultCharset == null)
			defaultCharset = Charset.forName("UTF-8");
		root.setProperty(Property.CHARACTER_SET, defaultCharset.name());

		root.setProperty(Property.DATE,
				String.format("%1$tY-%1$tm-%1$td", Calendar.getInstance()));
	}

	public GameTree getGameTree() {
		return gameTree;
	}

	public void setGameTree(GameTree gameTree) {
		this.gameTree = gameTree;
		gameMap.put(data, gameTree);
	}

	public Uri getNewGameUri() {
		Uri uri = getContentResolver().insert(SGFProvider.CONTENT_URI, null);
		Log.d(TAG, "getNewGameUri: uri=" + uri);
		return uri;
	}

	public void setData(Uri data) {
		if (data == null) {
			data = getNewGameUri();
		}
		this.data = data;
	}

	public Uri getData() {
		return data;
	}

	public File getNewFile() {
		return getFileStreamPath(UUID.randomUUID().toString() + ".sgf");
	}

	public void save() {
		if (gameTree == null)
			return;
		if (false && !gameTree.isModified()) {
			Log.i(TAG, "not saving unmodified GameTree");
			return;
		}

		if (data == null) {
			setData(null);
		}

		try {
			OutputStream os = getContentResolver()
					.openOutputStream(data, "rwt");
			gameTree.save(os);
			os.close();
		} catch (java.io.IOException ex) {
			throw new RuntimeException("save failed", ex);
		}
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean checkNotReadOnly(Context context) {
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

			// TODO: This probably does not work!

			return !readOnly;
		}
	}
}
