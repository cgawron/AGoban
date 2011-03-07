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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.http.HttpResponseException;

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

    private final AccountManager accountManager;
    private final Context context;
	private final GoogleUtility googleUtility;
    private Date lastUpdated;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
        this.accountManager = AccountManager.get(context);
		this.googleUtility = new GoogleUtility();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
							  ContentProviderClient provider, SyncResult syncResult) 
    {
    	Log.d(TAG, String.format("onPerformSync: %s %s", account, authority));

        String authtoken = null;
		try {
			// use the account manager to request the credentials
			authtoken = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);

			googleUtility.setAuthToken(authtoken);
            Log.d(TAG, "Retrieving modified games");
			List<GDocEntry> docs = googleUtility.getDocumentList(lastUpdated); 
            Log.d(TAG, "Modified games: " + docs);
			

            // update the last synced date.
            lastUpdated = new Date();

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
			}
			else {
				syncResult.stats.numIoExceptions++;
			}
		} catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
		/*			
        } catch (final AuthenticationException e) {
            accountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authtoken);
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "AuthenticationException", e);
		*/
		}
	}
}
