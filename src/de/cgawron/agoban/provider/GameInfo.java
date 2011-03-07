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

import android.content.ContentValues;
import android.database.Cursor;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.RootNode;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.Key;

/**
 * This class contains meta information on a game, i.e. players, results, event,
 * rules etc. The information is hashed in a database and only updated as
 * needed.
 * 
 * @author Christian Gawron
 */
public class GameInfo {
	@Retention(value = RUNTIME)
	@Target(value = ElementType.FIELD)
	public @interface Column {
	}

	@Retention(value = RUNTIME)
	@Target(value = ElementType.FIELD)
	public @interface SGFProperty {
	}

	final public static String KEY_ID = "_id";
	final public static String KEY_LOCAL_MODIFIED_DATE = "MDATE";
	final public static String KEY_REMOTE_MODIFIED_DATE = "RDATE";
	final public static @Column
	String KEY_URI = "URI";
	final public static @Column
	String KEY_FILENAME = "FILENAME";
	final public static @Column
	@SGFProperty
	String KEY_GAMENAME = "GN";
	final public static @Column
	@SGFProperty
	String KEY_DATE = "DT";
	final public static @Column
	@SGFProperty
	String KEY_RESULT = "RE";
	final public static @Column
	@SGFProperty
	String KEY_PLAYER_WHITE = "PW";
	final public static @Column
	@SGFProperty
	String KEY_PLAYER_BLACK = "PB";
	final public static @Column
	@SGFProperty
	String KEY_WHITE_RANK = "WR";
	final public static @Column
	@SGFProperty
	String KEY_BLACK_RANK = "BR";

	private static Key[] sgfKeys;
	private static String[] displayColumns = { KEY_ID, 
											   KEY_FILENAME,
											   KEY_LOCAL_MODIFIED_DATE };
	private File file;
	private ContentValues values;

	/**
	 * Create GameInfo from file.
	 * 
	 * @todo This should be optimized - it's not necessary to create the whole
	 *       GameTree structure to build the GameInfo.
	 */
	public GameInfo(File file) throws Exception {
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
		values.put(KEY_ID, file.hashCode());
		values.put(KEY_FILENAME, file.getName());
		values.put(KEY_LOCAL_MODIFIED_DATE, file.lastModified());

		for (Key key : sgfKeys) {
			Property property = gameTree.getRoot().get(key);
			if (property != null)
				values.put(key.toString(), property.getValue().toString());
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

	public ContentValues getContentValues() {
		return values;
	}

	public File getFile() {
		return file;
	}
}
