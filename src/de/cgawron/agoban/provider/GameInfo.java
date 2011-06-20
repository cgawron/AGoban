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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.Key;

/**
 * This class contains meta information on a game, i.e. players, results, event,
 * rules etc. The information is hashed in a database and only updated as
 * needed.
 * 
 * @author Christian Gawron
 */
public class GameInfo
{
	@Retention(value = RUNTIME)
	@Target(value = ElementType.FIELD)
	public @interface Column {
		boolean unique() default false;
	}

	@Retention(value = RUNTIME)
	@Target(value = ElementType.FIELD)
	public @interface SGFProperty {
	}

	public static final String TAG = "GameInfo";
	public static final String AUTHORITY = "de.cgawron.agoban";
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cgawron.sgf";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cgawron.sgf";

	public static final String KEY_ID = "_id";
	public static final String KEY_LOCAL_MODIFIED_DATE = "MDATE";
	public static final String KEY_REMOTE_MODIFIED_DATE = "RDATE";
	public static final String KEY_METADATA_DATE = "METADATE";
	public static final String KEY_REMOTE_ID = "REMOTEID";
	
	public static final Uri     CONTENT_URI = new Uri.Builder().scheme("content").authority(GameInfo.AUTHORITY).path("games").build();

	public static final @Column                String KEY_URI = "URI";
	public static final @Column(unique = true) String KEY_FILENAME = "FILENAME";
	public static final @Column @SGFProperty   String KEY_GAMENAME = "GN";
	public static final @Column @SGFProperty   String KEY_DATE = "DT";
	public static final @Column @SGFProperty   String KEY_RESULT = "RE";
	public static final @Column @SGFProperty   String KEY_PLAYER_WHITE = "PW";
	public static final @Column @SGFProperty   String KEY_PLAYER_BLACK = "PB";
	public static final @Column @SGFProperty   String KEY_WHITE_RANK = "WR";
	public static final @Column @SGFProperty   String KEY_BLACK_RANK = "BR";

	private static Key[] sgfKeys;
	/*
	private static String[] displayColumns = { 
		KEY_ID, KEY_FILENAME,
		KEY_LOCAL_MODIFIED_DATE 
	};
	*/
	private final File file;
	private ContentValues values;


	/**
	 * Create GameInfo from file.
	 * 
	 * @todo This should be optimized - it's not necessary to create the whole
	 *       GameTree structure to build the GameInfo.
	 */
	public GameInfo(GameTree gameTree)
	{
		this.file = gameTree.getFile();
		init(gameTree);
	}

	/**
	 * Create GameInfo from file.
	 * 
	 * @todo This should be optimized - it's not necessary to create the whole
	 *       GameTree structure to build the GameInfo.
	 */
	public GameInfo(File file) throws Exception
	{
		this.file = file;
		GameTree gameTree = null;
		// The cup parser (or my code around it?) seems to have a multithreading
		// problem
		synchronized (de.cgawron.go.sgf.Parser.class) {
			gameTree = new GameTree(file);
		}
		init(gameTree);
	}

	/**
	 * Create GameInfo from file and a cursor containing the meta information.
	 * 
	 * @todo This should be optimized - it's not necessary to create the whole
	 *       GameTree structure to build the GameInfo.
	 */
	public GameInfo(File file, Cursor cursor) throws Exception
	{
		this.file = file;
		init(cursor);
	}

	private void init(Cursor cursor)
	{
		if (sgfKeys == null)
			initSGFKeys();

		values.put(KEY_FILENAME, file.getName());
		values.put(KEY_LOCAL_MODIFIED_DATE, file.lastModified());
		for (Key key : sgfKeys) {
			String _key = key.toString();
			values.put(_key, cursor.getString(cursor.getColumnIndex(_key)));
		}
	}

	private void init(GameTree gameTree)
	{
		if (sgfKeys == null)
			initSGFKeys();

		values = new ContentValues();
		//XXX values.put(KEY_ID, getId(file));
		values.put(KEY_FILENAME, file.getName());
		values.put(KEY_LOCAL_MODIFIED_DATE, file.lastModified());
		values.put(KEY_METADATA_DATE, file.lastModified());

		for (Key key : sgfKeys) {
			Property property = gameTree.getRoot().get(key);
			if (property != null) {
				Log.d(TAG, "property=" + property);
				if (property.getValue() != null)
					values.put(key.toString(), property.getValue().toString());
				else
					values.put(key.toString(), "");
			}
		}
	}

	private void initSGFKeys()
	{
		ArrayList<Key> keys = new ArrayList<Key>();
		Field[] fields = GameInfo.class.getFields();
		for (Field field : fields) {
			try {
				if (field.getAnnotation(SGFProperty.class) != null) {
					keys.add(new Key(field.get(null).toString()));
				}
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}
		sgfKeys = keys.toArray(new Key[0]);
	}

	public ContentValues getContentValues()
	{
		return values;
	}

	public File getFile()
	{
		return file;
	}
	
	/* Bad idea - prevents renaming of files!
	public static long getId(GameTree gameTree)
	{
		return getId(gameTree.getFile());
	}
	
	public static long getId(File file)
	{
		return getId(file.getName());
	}
	
	public static long getId(String fileName)
	{
		long id = fileName.hashCode();
		if (id < 0) id = -id;
		
		return id;
	}
	*/
}
