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
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
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
	private static final Date EPOCH = new Date(0);

	private final AccountManager accountManager;
	private GoogleUtility googleUtility = null;
	private final Map<Long, GDocEntry> gdocMap = new HashMap<Long, GDocEntry>();
	private long lastSyncTime = 0;

	public SyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		Log.d(TAG, "Constructor");
		this.accountManager = AccountManager.get(context);	
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
							  ContentProviderClient provider, SyncResult syncResult)
	{
		ConnectivityManager connectivity = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		// Skip if no connection, or background data disabled
		NetworkInfo info = connectivity.getActiveNetworkInfo();
		if (info == null || !connectivity.getBackgroundDataSetting()) {
			return;
		}
		
		lastSyncTime = System.currentTimeMillis();
		
		// TODO: Select/Create folder
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
	
		this.googleUtility = new GoogleUtility(getContext(), accountManager, account);
		Log.d(TAG, "Retrieving modified games");
		
		try {
			if (googleUtility.updateDocumentList()) {
				checkRemoteUpdates(provider, syncResult);
			}
		}
		catch (final Exception ex)
		{
			handleException(ex, syncResult);
		}
		
		try {
			// check for local updates
			Cursor cursor = provider.query(GameInfo.CONTENT_URI, PROJECTION,
					                       GameInfo.KEY_LOCAL_MODIFIED_DATE + " > " + lastSyncTime, null, null);

			checkLocalUpdates(cursor, provider, syncResult);
		}
		catch (final Exception ex)
		{
			handleException(ex, syncResult);
		}
		
		Log.d(TAG, "onperformSync: " + syncResult);
	}

	private void checkLocalUpdates(Cursor cursor,
			                       ContentProviderClient provider, SyncResult syncResult)
	{
		while (cursor.moveToNext()) {
			long id = cursor.getLong(0);
			String fileName = cursor.getString(1);
			Date localModification = new Date(cursor.getLong(2));
			Date remoteModification = new Date(cursor.getLong(3));
			GDocEntry doc = gdocMap.get(id);
			Log.d(TAG, String.format("get: %d->%s", id, doc));
			Log.d(TAG, String.format("sync (up): local=%s, remote=%s, cloud=%s", localModification, remoteModification, doc != null ? doc.getUpdated() : "<null>"));
			
			try {
				syncResult.stats.numEntries++;
				if (doc == null) {
					if (remoteModification.after(EPOCH)) {
						//TODO: remote deletion?
						// deleteLocal(provider, id);
						// syncResult.stats.numDeletes++;
						syncResult.stats.numSkippedEntries++;
					}
					else {
						// not yet present in cloud
						createRemote(provider, id, fileName);
						syncResult.stats.numInserts++;
					}
				}
				else if (localModification.after(remoteModification)) {
					updateRemote(provider, id, doc);
					syncResult.stats.numUpdates++;
				}
				else {
					syncResult.stats.numSkippedEntries++;
				}
			}
			catch (final Exception ex) {
				handleException(ex, syncResult);
			}
		}
		cursor.close();
	}

	private void checkRemoteUpdates(ContentProviderClient provider, SyncResult syncResult)
	{
		List<GDocEntry> docs = googleUtility.getDocs();

		// check for remote updates
		if (docs != null) {
			for (GDocEntry doc : docs) {
				if (GoogleUtility.CATEGORY_FILE.equals(doc.getKind().term)) {
					syncResult.stats.numEntries++;
					
					Log.d(TAG, "Modified: " + doc);
					try {
						long id = getId(provider, doc);

						if (id == 0) {
							// not in DB
							if (!doc.isTrashed()) {
								createLocal(provider, doc);
								syncResult.stats.numInserts++;
							}
							else {
								syncResult.stats.numSkippedEntries++;
							}
						}
						else {
							Date cloudModification = doc.getUpdated();
							Date remoteModification = getRemoteModification(provider, doc);
							Date localModification = getLocalModification(provider, doc);
							Log.d(TAG, String.format("remote check: cloud=%s, remote=%s, local=%s", 
									                 cloudModification, remoteModification, localModification));

							Log.d(TAG, String.format("put: %d->%s", id, doc));
							gdocMap.put(id, doc);

							if (doc.isTrashed()) {
								// remote deletion
								deleteLocal(provider, id);
								syncResult.stats.numDeletes++;
							}
							else if (localModification == null) {
								createLocal(provider, doc);
								syncResult.stats.numInserts++;
							}
							else if (remoteModification == null) {
								// File already exists locally, but is not from cloud - conflict!
								syncResult.stats.numConflictDetectedExceptions++;
							}
							else if (remoteModification.before(cloudModification)) {
								updateLocal(provider, doc);
								syncResult.stats.numUpdates++;
							}
							else {
								syncResult.stats.numSkippedEntries++;
							}
						} 
					} catch(final Exception ex) {
						handleException(ex, syncResult);
					}
				}
			}
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
		provider.update(GameInfo.CONTENT_URI, values,  
						GameInfo.KEY_ID + "=?", new String[] { Long.toString(id) });

		gdocMap.put(id, doc);
	}

	private void createRemote(ContentProviderClient provider, long id, String fileName) throws RemoteException, IOException
	{
		Log.d(TAG, "createRemote " + fileName);
		File sgfFile = new File(SGFProvider.SGF_DIRECTORY, fileName);
		GDocEntry doc = googleUtility.createGoogleDoc(sgfFile);

		ContentValues values = new ContentValues();
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.update(GameInfo.CONTENT_URI, values,  
						GameInfo.KEY_ID + "=?", new String[] { Long.toString(id) });

		gdocMap.put(id, doc);
	}

	private void createLocal(ContentProviderClient provider, GDocEntry doc) throws Exception
	{
		Log.d(TAG, "createLocal " + doc.title);
		File localFile = download(doc);
		GameInfo info = new GameInfo(localFile);
		ContentValues values = info.getContentValues();
		
		values.put(GameInfo.KEY_FILENAME, doc.title);
		values.put(GameInfo.KEY_REMOTE_ID, doc.resourceId);
		values.put(GameInfo.KEY_LOCAL_MODIFIED_DATE, localFile.lastModified());
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());

		Log.d(TAG, "values: " + values);
		provider.insert(GameInfo.CONTENT_URI, values);
	}

	private void  updateLocal(ContentProviderClient provider, GDocEntry doc) throws Exception
	{
		Log.d(TAG, "updateLocal " + doc.title);
		File localFile = download(doc);
		GameInfo info = new GameInfo(localFile);
		ContentValues values = info.getContentValues();
		
		values.put(GameInfo.KEY_FILENAME, localFile.getName());
		values.put(GameInfo.KEY_LOCAL_MODIFIED_DATE, localFile.lastModified());
		values.put(GameInfo.KEY_REMOTE_MODIFIED_DATE, doc.getUpdated().getTime());
		
		Log.d(TAG, String.format("file: %s, values: %s", localFile, values));
		provider.update(GameInfo.CONTENT_URI, values,  
						GameInfo.KEY_REMOTE_ID + "=?", 
						new String[] { doc.resourceId });
	}

	private void  deleteLocal(ContentProviderClient provider, long id) throws Exception
	{
		Log.d(TAG, "deleteLocal " + id);
		Uri uri = ContentUris.withAppendedId(GameInfo.CONTENT_URI, id);
		provider.delete(uri, null, null);
	}

	private Date getLocalModification(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		Log.d(TAG, "getLocalModification: " + doc.title);
		Date date = null;
		
		final String[] PROJECTION = { GameInfo.KEY_LOCAL_MODIFIED_DATE };
		Cursor cursor = provider.query(GameInfo.CONTENT_URI, PROJECTION, 
	                     			   GameInfo.KEY_REMOTE_ID + "=?", 
	                     			   new String[] { doc.resourceId }, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			Log.d(TAG, "getLocalModification: " + cursor.getLong(0));
			date = new Date(cursor.getLong(0));
		}
		cursor.close();
		return date;
	}
	private Date getRemoteModification(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		Log.d(TAG, "getRemoteModification: " + doc.title);
		Date date = null;
		
		final String[] PROJECTION = { GameInfo.KEY_REMOTE_MODIFIED_DATE };
		Cursor cursor = provider.query(GameInfo.CONTENT_URI, PROJECTION, 
									   GameInfo.KEY_REMOTE_ID + "=?", 
									   new String[] { doc.resourceId }, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			Log.d(TAG, "getRemoteModification: " + cursor.getLong(0));
			date = new Date(cursor.getLong(0));
		}
		cursor.close();
		return date;
	}

	private long getId(ContentProviderClient provider, GDocEntry doc) throws RemoteException
	{
		long id = 0;
		Log.d(TAG, "getId: " + doc.title);
			
		final String[] PROJECTION = { GameInfo.KEY_ID };
		Cursor cursor = provider.query(GameInfo.CONTENT_URI, PROJECTION, 
				                       GameInfo.KEY_REMOTE_ID + "=?", 
				                       new String[] { doc.resourceId }, null);

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
		InputStream is = googleUtility.getStream(doc);
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
	
	private void handleException(Exception e, SyncResult syncResult)
	{
		Log.e(TAG, "handleException: ", e);
		
		if (e instanceof AuthenticatorException) {
			syncResult.stats.numParseExceptions++;
		}
		else {
			syncResult.stats.numIoExceptions++;
		}
	}
	
}
