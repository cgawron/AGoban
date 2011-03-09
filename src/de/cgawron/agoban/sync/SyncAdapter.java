/*
 * Copyright (C) 2011 Christian Gawron
 * Based on sample code Copyright (C) 2010 The Android Open Source Project
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

package de.cgawron.agoban.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.api.client.http.HttpResponseException;

import de.cgawron.agoban.provider.GameInfo;
import de.cgawron.agoban.provider.SGFProvider;
import de.cgawron.agoban.sync.GoogleUtility.GDocEntry;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	private static final String TAG = "SyncAdapter";
	private static final String ACCOUNT_TYPE = "com.google";
	private static final String AUTH_TOKEN_TYPE = "writely";
	private static final Date EPOCH = new Date(0);

	private final AccountManager accountManager;
	private final GoogleUtility googleUtility;
	private Date lastUpdated;
	private final Map<Long, GDocEntry> gdocMap = new HashMap<Long, GDocEntry>();

	public SyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		this.accountManager = AccountManager.get(context);
		this.googleUtility = new GoogleUtility();
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
							  ContentProviderClient provider, SyncResult syncResult)
	{
		// TODO: Take remote trash into account!
		/* TODO: Check for conflicts
		 * A Conflict is an entry where 
		 * - the local modification date is after remote modification (according DB)
		 * - the remote modification according to the DB is before remote modification in the cloud
		 */

		Log.d(TAG, String.format("onPerformSync: %s %s", account, authority));
		final String[] PROJECTION = { 
			GameInfo.KEY_ID,
			GameInfo.KEY_FILENAME,
			GameInfo.KEY_LOCAL_MODIFIED_DATE,
			GameInfo.KEY_REMOTE_MODIFIED_DATE 
		};

		String authtoken = null;
		try {
			// use the account manager to request the credentials
			authtoken = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);

			googleUtility.setAuthToken(authtoken);
			// update the last synced date.

			Log.d(TAG, "Retrieving modified games");
			List<GDocEntry> docs = googleUtility.getDocumentList(lastUpdated);
			lastUpdated = new Date();
			Log.d(TAG, "Modified games: " + docs);

			// check for remote updates
			if (docs != null) {
				for (GDocEntry doc : docs) {
					long id = getId(provider, doc);
					Date cloudModification = doc.getUpdated();
					Date remoteModification = getRemoteModification(provider, doc);
					Date localModification = getLocalModification(provider, doc);
					gdocMap.put(id, doc);
	
					if (localModification == null) {
						createLocal(provider, doc);
						syncResult.stats.numInserts++;
					}
					else if (remoteModification == null) {
						// File exists locally, but is not from cloud - conflict!
						syncResult.stats.numConflictDetectedExceptions++;
					}
					else if (remoteModification.before(cloudModification)) {
						updateLocal(provider, doc);
						syncResult.stats.numUpdates++;
					}
				}
			}

			// check for local updates
			Cursor cursor = provider.query(SGFProvider.CONTENT_URI, PROJECTION,
										   null, null, null);

			while (cursor.moveToNext()) {
				long id = cursor.getLong(0);
				String fileName = cursor.getString(1);
				Date localModification = new Date(cursor.getLong(2));
				Date remoteModification = new Date(cursor.getLong(3));
				GDocEntry doc = gdocMap.get(id);
				
				if (doc == null) {
					if (remoteModification.after(EPOCH)) {
						// remote deletion
						deleteLocal(provider, doc);
						syncResult.stats.numDeletes++;
					}
					else {
						// not yet present in cloud
						createRemote(provider, id, fileName);
					}
				}
				else if (localModification.after(remoteModification)) {
					updateRemote(provider, id, doc);
				}
			}
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final HttpResponseException e) {
			Log.e(TAG, "HttpResponseException", e);
			int statusCode = e.response.statusCode;
			if (statusCode == 401 || statusCode == 403) {
				accountManager.invalidateAuthToken(ACCOUNT_TYPE, authtoken);
				syncResult.stats.numAuthExceptions++;
			} else {
				syncResult.stats.numIoExceptions++;
			}
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		} catch (final Exception e) {
			Log.e(TAG, "Exception", e);
			syncResult.stats.numIoExceptions++;
		}
	}

	private void updateRemote(ContentProviderClient provider, long id, GDocEntry doc) throws RemoteException
	{
		Log.d(TAG, "updateRemote " + doc.title);

		File sgfFile = new File(SGFProvider.SGF_DIRECTORY, doc.title);
		doc = googleUtility.updateGoogleDoc(doc, sgfFile);

		ContentValues values = new ContentValues();
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.update(SGFProvider.CONTENT_URI, values,  
						GameInfo.KEY_ID + "=?", new String[] { Long.toString(id) });

		gdocMap.put(id, doc);
	}

	private void createRemote(ContentProviderClient provider, long id, String fileName) throws RemoteException
	{
		Log.d(TAG, "createRemote " + fileName);
		File sgfFile = new File(SGFProvider.SGF_DIRECTORY, fileName);
		GDocEntry doc = googleUtility.createGoogleDoc(sgfFile);

		ContentValues values = new ContentValues();
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.update(SGFProvider.CONTENT_URI, values,  
						GameInfo.KEY_ID + "=?", new String[] { Long.toString(id) });

		gdocMap.put(id, doc);
	}

	private void createLocal(ContentProviderClient provider, GDocEntry doc) throws Exception
	{
		Log.d(TAG, "createLocal " + doc.title);
		ContentValues values = new ContentValues();
		File localFile = download(doc);
		long id = localFile.hashCode();

		values.put(GameInfo.KEY_ID, id);
		values.put(GameInfo.KEY_FILENAME, doc.title);
		values.put(GameInfo.KEY_LOCAL_MODIFIED_DATE, localFile.lastModified());
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.insert(SGFProvider.CONTENT_URI, values);
	}

	private void  updateLocal(ContentProviderClient provider, GDocEntry doc) throws Exception
	{
		Log.d(TAG, "updateLocal " + doc.title);
		ContentValues values = new ContentValues();
		File localFile = download(doc);
		
		values.put(GameInfo.KEY_LOCAL_MODIFIED_DATE, localFile.lastModified());
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.update(SGFProvider.CONTENT_URI, values,  
						GameInfo.KEY_FILENAME + "=?", 
						new String[] { doc.title });
	}

	private void  deleteLocal(ContentProviderClient provider, GDocEntry doc) throws Exception
	{
		Log.d(TAG, "deleteLocal " + doc.title);
		provider.delete(SGFProvider.CONTENT_URI,  
						GameInfo.KEY_FILENAME + "=?", 
						new String[] { doc.title });
	}

	private Date getLocalModification(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		Log.d(TAG, "getLocalModification: " + doc.title);
		Date date = null;
		
		final String[] PROJECTION = { GameInfo.KEY_LOCAL_MODIFIED_DATE };
		Cursor cursor = provider.query(SGFProvider.CONTENT_URI, PROJECTION, 
									   GameInfo.KEY_FILENAME + "=?", 
									   new String[] { doc.title }, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			date = new Date(cursor.getInt(0));
		}
		cursor.close();
		return date;
	}
	private Date getRemoteModification(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		Log.d(TAG, "getRemoteModification: " + doc.title);
		Date date = null;
		
		final String[] PROJECTION = { GameInfo.KEY_REMOTE_MODIFIED_DATE };
		Cursor cursor = provider.query(SGFProvider.CONTENT_URI, PROJECTION, 
									   GameInfo.KEY_FILENAME + "=?", 
									   new String[] { doc.title }, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			date = new Date(cursor.getInt(0));
		}
		cursor.close();
		return date;
	}

	private long getId(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		long id = 0;
		Log.d(TAG, "getId: " + doc.title);
			
		final String[] PROJECTION = { GameInfo.KEY_ID };
		Cursor cursor = provider.query(SGFProvider.CONTENT_URI, PROJECTION, 
									   GameInfo.KEY_FILENAME + "=?", 
									   new String[] { doc.title }, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			id = cursor.getLong(0);
		}
		cursor.close();
		return id;
	}

	private File download(GDocEntry doc) throws IOException
	{
		File destination = new File(SGFProvider.SGF_DIRECTORY, doc.title);
		File tmp = new File(SGFProvider.SGF_DIRECTORY, "_tmp");
		InputStream is = doc.getStream();
		OutputStream os = new FileOutputStream(tmp);
		byte[] buffer = new byte[4*1024];
		int size;

		Log.d(TAG, "downloading " + doc.title);
		while ((size = is.read(buffer)) > 0) {
			os.write(buffer, 0, size);
		}
		
		if (!tmp.renameTo(destination)) {
			throw new RuntimeException("failed to rename " + tmp + " to " + destination);
		}
		destination.setLastModified(doc.getUpdated().getTime());

		return destination;
	}
}
