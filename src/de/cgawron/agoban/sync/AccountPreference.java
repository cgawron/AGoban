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

package de.cgawron.agoban.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class AccountPreference extends ListPreference {
	public AccountPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AccountPreference(Context context) {
		super(context);
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		final AccountManager manager = AccountManager.get(getContext());
		final Account[] accounts = manager.getAccountsByType("com.google");
		final int size = accounts.length;
		String[] names = new String[size];
		for (int i = 0; i < size; i++) {
			names[i] = accounts[i].name;
		}
		setEntries(names);
		setEntryValues(names);

		super.onPrepareDialogBuilder(builder);
	}
}