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

import java.lang.reflect.Field;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class SGFDBOpenHelper extends SQLiteOpenHelper
{
	private static final String TAG = "SGFDBOpenHelper";
	private static final int DATABASE_VERSION = 4;
	private static final String DATABASE_NAME = "AGoban";
	static final String SGF_TABLE_NAME = "sgf";

	SGFDBOpenHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Log.d(TAG, "onCreate");
		db.execSQL(getCreateStatement());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		String upgradeStatement = null;
		if (oldVersion >= newVersion) return;
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE ").append(SGF_TABLE_NAME).append(" ADD COLUMN ");
		for (int version = oldVersion+1; version <= newVersion; version++) {
			switch (version) {
			case 2:
				sb.append(GameInfo.KEY_REMOTE_MODIFIED_DATE).append(" INTEGER");
				break;
			case 3:
				sb.append(GameInfo.KEY_METADATA_DATE).append(" INTEGER");
				break;
			case 4:
				sb.append(GameInfo.KEY_REMOTE_ID).append(" TEXT UNIQUE");
				break;
			default:
				throw new RuntimeException(String.format("Unknow DB version: %d", newVersion));
			}
			if (version == newVersion)
				sb.append(";");
			else
				sb.append(", ");
		}
		upgradeStatement = sb.toString();
		
		Log.d(TAG, "upgrade: " + upgradeStatement);
		db.execSQL(upgradeStatement);
	}

	public String getCreateStatement()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ").append(SGF_TABLE_NAME).append(" (")
				.append(GameInfo.KEY_ID).append(" INTEGER PRIMARY KEY, ");
		Field[] fields = GameInfo.class.getFields();
		for (Field field : fields) {
			if (field.getAnnotation(GameInfo.Column.class) != null) {
				try {
					Log.d(TAG, "Key: " + field.getName() + " " + field.get(null));
					sb.append(field.get(null)).append(" TEXT, ");
				} catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		sb.append(GameInfo.KEY_LOCAL_MODIFIED_DATE).append(" INTEGER, ");
		sb.append(GameInfo.KEY_REMOTE_MODIFIED_DATE).append(" INTEGER, ");
		sb.append(GameInfo.KEY_METADATA_DATE).append(" INTEGER, ");
		sb.append(GameInfo.KEY_REMOTE_ID).append(" TEXT UNIQUE);");
		Log.d(TAG, "SQL: " + sb.toString());

		return sb.toString();
	}

}
