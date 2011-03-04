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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.apache.ApacheHttpTransport;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartRelatedContent;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.atom.AtomContent;
import com.google.api.client.xml.atom.AtomParser;

import de.cgawron.agoban.provider.GameInfo;
import de.cgawron.agoban.provider.SGFProvider;

/**
 * Sync SGF files with Google documents. Based on sample code from google java
 * api client library.
 * 
 * @author Christian Gawron
 */
public final class GoogleSync extends Activity {

	/** The token type for authentication */
	private static final String AUTH_TOKEN_TYPE = "writely";
	private static final String PREFS = "SyncPrefs";
	private static final String TAG = "GoogleSync";
	private static final String FOLDER_SGF = "SGF";
	private static final String CATEGORY_KIND = "http://schemas.google.com/g/2005#kind";
	private static final String CATEGORY_DOCUMENT = "http://schemas.google.com/docs/2007#document";
	private static final String LINK_PARENT = "http://schemas.google.com/docs/2007#parent";
	private static final int REQUEST_AUTHENTICATE = 0;
	private static final int DIALOG_ACCOUNTS = 0;

	private static HttpTransport transport;
	private String authToken;
	private String sgfFolder;

	private static final String[] PROJECTION = new String[] { GameInfo.KEY_ID,
			GameInfo.KEY_FILENAME, GameInfo.KEY_MODIFIED_DATE };

	public static final XmlNamespaceDictionary DICTIONARY = new XmlNamespaceDictionary();

	static {
		Map<String, String> map = DICTIONARY.namespaceAliasToUriMap;
		map.put("", "http://www.w3.org/2005/Atom");
		map.put("app", "http://www.w3.org/2007/app");
		map.put("atom", "http://www.w3.org/2005/Atom");
		map.put("batch", "http://schemas.google.com/gdata/batch");
		map.put("docs", "http://schemas.google.com/docs/2007");
		map.put("gAcl", "http://schemas.google.com/acl/2007");
		map.put("gd", "http://schemas.google.com/g/2005");
		map.put("openSearch", "http://a9.com/-/spec/opensearch/1.1/");
		map.put("xml", "http://www.w3.org/XML/1998/namespace");
	}

	public static class GDocFeed {
		@Key("openSearch:totalResults")
		public int totalResults;

		@Key("entry")
		public List<GDocEntry> entries;

		@Key("link")
		public List<Link> links;
	}

	public static class GDocEntry {
		@Key("@gd:etag")
		public String etag;

		@Key
		public String title;

		@Key
		public DateTime updated;

		@Key("link")
		public List<Link> links;

		@Key
		public ContentLink content;

		@Key
		public boolean convert;

		@Key("category")
		public List<Category> categories;

		public Date getUpdated() {
			return new Date(updated.value);
		}

		public GenericUrl getUpdateUrl() {
			for (Link link : links) {
				if (link.rel.equals("edit-media"))
					return new GenericUrl(link.href);
			}
			return null;
		}

		public String getDownloadLink() {
			return content.src;
		}

		public String getDownloadLink(String type) {
			return content.src + "&exportFormat=" + type;
		}

		public InputStream getStream() throws IOException {
			HttpRequest request = transport.buildGetRequest();
			request.setUrl(getDownloadLink());
			return request.execute().getContent();
		}

		public InputStream getStream(String type) throws IOException {
			HttpRequest request = transport.buildGetRequest();
			request.setUrl(getDownloadLink(type));
			return request.execute().getContent();
		}
	}

	public static class Category {
		@Key("@scheme")
		public String scheme;

		@Key("@term")
		public String term;

		@Key("@label")
		public String label;

		public Category() {
		}

		public Category(String scheme, String term, String label) {
			this.scheme = scheme;
			this.term = term;
			this.label = label;
		}

		@Override
		public String toString() {
			return "category: " + scheme + "->" + term + ", label=" + label;
		}
	}

	public static class ContentLink {
		@Key("@type")
		public String type;

		@Key("@src")
		public String src;

		@Override
		public String toString() {
			return "contentLink: " + type + ": " + src;
		}
	}

	public static class Link {
		@Key("@href")
		public String href;

		@Key("@title")
		public String title;

		@Key("@rel")
		public String rel;

		public static String find(List<Link> links, String rel) {
			if (links != null) {
				for (Link link : links) {
					if (rel.equals(link.rel)) {
						return link.href;
					}
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return "link: " + rel + "->" + href;
		}
	}

	static class SendData {
		String fileName;
		String contentType;
		long contentLength;

		SendData(Cursor cursor) {
			Log.d(TAG, "SendData: " + cursor.getString(1));
			this.fileName = cursor.getString(cursor.getColumnIndexOrThrow(GameInfo.KEY_FILENAME));
		}

		public InputStream getInputStream() throws FileNotFoundException {
			return new FileInputStream(new File(SGFProvider.SGF_DIRECTORY, fileName));
		}
		/*
		 * SendData(Intent intent, ContentResolver contentResolver) { Bundle
		 * extras = intent.getExtras(); Log.d(TAG, "SendData: " + intent + ", "
		 * + extras); if (extras.containsKey(Intent.EXTRA_STREAM)) { Uri uri =
		 * this.uri = (Uri) extras .getParcelable(Intent.EXTRA_STREAM); String
		 * scheme = uri.getScheme(); Log.d(TAG, "SendData: uri=" + this.uri);
		 * this.contentType = SGFProvider.SGF_TYPE; if
		 * (scheme.equals("content")) { Cursor cursor =
		 * contentResolver.query(uri, null, null, null, null);
		 * cursor.moveToFirst(); this.fileName = cursor.getString(cursor
		 * .getColumnIndexOrThrow(GameInfo.KEY_FILENAME)); this.contentLength =
		 * 100; cursor.close(); } } }
		 */
	}

	private GenericUrl getDocUrl() {
		GenericUrl url = new GenericUrl(
				"https://docs.google.com/feeds/default/private/full");

		return url;
	}

	private GenericUrl getFolderUrl() {
		GenericUrl url = new GenericUrl(sgfFolder);
		url.appendRawPath("/contents");

		return url;
	}

	public GoogleSync() {
		HttpTransport.setLowLevelHttpTransport(ApacheHttpTransport.INSTANCE);
		transport = GoogleTransport.create();
		GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
		headers.setApplicationName("NotePad");
		headers.gdataVersion = "3";
		AtomParser parser = new AtomParser();
		parser.namespaceDictionary = DICTIONARY;
		transport.addParser(parser);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSharedPreferences(PREFS, 0);
		setLogging(true);
		gotAccount(true);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ACCOUNTS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select a Google account");
			final AccountManager manager = AccountManager.get(this);
			final Account[] accounts = manager.getAccountsByType("com.google");
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					gotAccount(manager, accounts[which]);
				}
			});
			return builder.create();
		}
		return null;
	}

	private void gotAccount(boolean tokenExpired) {
		Log.d(TAG, "gotAccount: " + tokenExpired);
		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		String accountName = settings.getString("accountName", null);
		if (accountName != null) {
			Log.d(TAG, "gotAccount: looking for " + accountName);
			AccountManager manager = AccountManager.get(this);
			Account[] accounts = manager.getAccountsByType("com.google");
			int size = accounts.length;
			for (int i = 0; i < size; i++) {
				Account account = accounts[i];
				Log.d(TAG, "gotAccount: got " + account.name);
				if (accountName.equals(account.name)) {
					if (tokenExpired) {
						manager.invalidateAuthToken("com.google",
								this.authToken);
					}
					gotAccount(manager, account);
					return;
				}
			}
		}
		showDialog(DIALOG_ACCOUNTS);
	}

	private void gotAccount(final AccountManager manager, final Account account) {
		Log.d(TAG, "gotAccount: " + manager + ", " + account);
		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", account.name);
		editor.commit();
		new Thread() {

			@Override
			public void run() {
				try {
					final Bundle bundle = manager.getAuthToken(account,
							AUTH_TOKEN_TYPE, true, null, null).getResult();
					runOnUiThread(new Runnable() {

						public void run() {
							try {
								if (bundle
										.containsKey(AccountManager.KEY_INTENT)) {
									Intent intent = bundle
											.getParcelable(AccountManager.KEY_INTENT);
									int flags = intent.getFlags();
									flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
									intent.setFlags(flags);
									startActivityForResult(intent,
											REQUEST_AUTHENTICATE);
								} else if (bundle
										.containsKey(AccountManager.KEY_AUTHTOKEN)) {
									Log.d(TAG,
											"gotAccount: login "
													+ bundle.getString(AccountManager.KEY_AUTHTOKEN));
									authenticatedClientLogin(bundle
											.getString(AccountManager.KEY_AUTHTOKEN));
								}
							} catch (Exception e) {
								handleException(e);
							}
						}
					});
				} catch (Exception e) {
					handleException(e);
				}
			}
		}.start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				gotAccount(false);
			} else {
				showDialog(DIALOG_ACCOUNTS);
			}
			break;
		}
	}

	private void authenticatedClientLogin(String authToken) {
		this.authToken = authToken;
		Log.d(TAG, "authToken: " + authToken);
		((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
		try {
			authenticated();
		} catch (Exception ex) {
			handleException(ex);
		}
		finish();
	}

	private void authenticated() {
		Log.d(TAG, "authenticated");

		Map<String, GDocEntry> docMap = retrieveDocuments();
		sendDocuments(docMap);
	}

	private List<GDocEntry> getDocumentList() {
		List<GDocEntry> entries;
		try {
			HttpRequest request = transport.buildGetRequest();
			request.url = getDocUrl();
			Log.d(TAG, "url: " + request.url);
			Log.d(TAG, "headers: " + request.headers);
			HttpResponse response = request.execute();
			GDocFeed feed = response.parseAs(GDocFeed.class);
			Log.d(TAG, "feed: " + feed);
			entries = feed.entries;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return entries;
	}

	private Map<String, GDocEntry> retrieveDocuments() {
		Map<String, GDocEntry> docMap = new HashMap<String, GDocEntry>();
		ContentResolver resolver = getContentResolver();
		List<GDocEntry> entries = getDocumentList();
		for (GDocEntry entry : entries) {
			String parent = null;
			String parentFolder = null;
			for (Link link : entry.links) {
				if (link.rel.equals(LINK_PARENT)) {
					parent = link.title;
					parentFolder = link.href;
					Log.d(TAG, "parent: " + parent);
				}
			}
			if (parent == null || !parent.equals(FOLDER_SGF))
				continue;
			sgfFolder = parentFolder;

			Log.d(TAG, "doc: '" + entry.title + "'");
			Log.d(TAG, "content: " + entry.content);
			for (Category category : entry.categories) {
				Log.d(TAG, "category: " + category);
			}

			Log.d(TAG, String.format("file %s updated on %s", entry.title,
					entry.getUpdated().toString()));
			docMap.put(entry.title, entry);
			Cursor cursor = resolver.query(SGFProvider.CONTENT_URI, PROJECTION,
					String.format("%s = '%s'", GameInfo.KEY_ID, entry.title),
					null, null);

			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				Date localModification = new Date(cursor.getLong(1));
				Date remoteModification = entry.getUpdated();
				Log.d(TAG, "local  modification: " + localModification);
				Log.d(TAG, "remote modification: " + remoteModification);

				if (localModification.before(remoteModification)) {
					retrieve(entry, false);
				} else {
					Log.d(TAG, "not retrieving " + entry.title);
				}
			} else {
				Log.d(TAG, entry.title + " not found locally");
				retrieve(entry, true);
			}
			cursor.close();
		}
		return docMap;
	}

	private void retrieve(GDocEntry entry, boolean create) {
		Log.d(TAG, "retrieving " + entry.title);

		try {
			InputStream is = entry.getStream();
			File file = new File(SGFProvider.getSGFDirectory(), entry.title);
			OutputStream os = new FileOutputStream(file);
			Log.d(TAG, "saving " + entry.title + " to " + file);
			byte buf[] = new byte[1024];
			int count;
			while ((count = is.read(buf)) > 0)
				os.write(buf, 0, count);
			os.close();
			is.close();
			Log.d(TAG, "succesfully saved " + entry.title);
			// SGFProvider.doUpdateDatabase();
		} catch (IOException ex) {
			Log.e(TAG, "retrieveDocuments: caught " + ex);
		}
	}

	private void sendDocuments(Map<String, GDocEntry> docMap) {
		// Perform a managed query. The Activity will handle closing and
		// requerying the cursor
		// when needed.
		setLogging(true);
		Cursor cursor = managedQuery(SGFProvider.CONTENT_URI, PROJECTION, null,
				null, null);

		while (cursor.moveToNext()) {
			String title = cursor.getString(1);
			Date localModification = new Date(cursor.getLong(2));
			Log.d(TAG, "sendDocuments: '" + title + "'");
			Log.d(TAG, "local date: " + localModification);

			GDocEntry entry = docMap.get(title);
			SendData data = new SendData(cursor);
			Date newModification = null;
			if (entry == null) {
				newModification = createGoogleDoc(getFolderUrl(), data);
			} else if (entry.getUpdated().before(localModification)) {
				newModification = updateGoogleDoc(entry.getUpdateUrl(),
						entry.etag, data);
			}
			if (newModification != null) {
				updateModificationDate(title, newModification);
			}
		}
		cursor.close();
		Log.d(TAG, "sendDocuments - end");
	}

	private Date createGoogleDoc(GenericUrl targetUrl, SendData sendData) {
		Date newModification = null;
		Log.d(TAG, "createGoogleDoc: url=" + targetUrl + ", data=" + sendData);
		InputStreamContent content = new InputStreamContent();
		AtomContent atom = new AtomContent();
		GDocEntry entry = new GDocEntry();
		atom.namespaceDictionary = DICTIONARY;
		atom.entry = entry;
		entry.categories = new ArrayList<Category>();
		Category category = new Category(CATEGORY_KIND, CATEGORY_DOCUMENT,
				"document");
		entry.categories.add(category);
		entry.convert = false;
		Log.d(TAG, "category: " + category);

		try {
			HttpRequest request = transport.buildPostRequest();
			request.url = targetUrl;
			((GoogleHeaders) request.headers)
					.setSlugFromFileName(sendData.fileName);
			MultipartRelatedContent mpContent = MultipartRelatedContent
					.forRequest(request);
			content.inputStream = sendData.getInputStream();
			content.type = "text/plain";
			// content.length = sendData.contentLength;
			mpContent.parts.add(content);
			mpContent.parts.add(atom);
			request.content = mpContent;

			Log.d(TAG, "content: " + content);
			Log.d(TAG, "request: " + request);
			HttpResponse response = request.execute();
			Log.d(TAG, "status was " + response.statusMessage);
			entry = response.parseAs(GDocEntry.class);
			Log.d(TAG, "new entry: " + entry);
			newModification = entry.getUpdated();
			Log.d(TAG, "newModification: " + newModification);
		} catch (Exception e) {
			Log.e(TAG, "exception in createDoc: " + e);
			e.printStackTrace();
		} finally {
			try {
				content.inputStream.close();
			} catch (Exception e) {
				Log.e(TAG, "exception in createDoc: " + e);
			}
		}
		return newModification;
	}

	private Date updateGoogleDoc(GenericUrl targetUrl, String etag,
			SendData sendData) {
		Date newModification = null;
		Log.d(TAG, "updateGoogleDoc: url=" + targetUrl + ", etag=" + etag
				+ ", data=" + sendData);
		InputStreamContent content = new InputStreamContent();
		AtomContent atom = new AtomContent();
		GDocEntry entry = new GDocEntry();
		atom.namespaceDictionary = DICTIONARY;
		atom.entry = entry;
		entry.categories = new ArrayList<Category>();
		Category category = new Category(CATEGORY_KIND, CATEGORY_DOCUMENT,
				"document");
		entry.categories.add(category);
		entry.convert = false;
		entry.etag = etag;
		atom.entry = entry;

		try {
			HttpRequest request = transport.buildPutRequest();
			request.url = targetUrl;
			((GoogleHeaders) request.headers)
					.setSlugFromFileName(sendData.fileName);
			MultipartRelatedContent mpContent = MultipartRelatedContent
					.forRequest(request);
			content.inputStream = sendData.getInputStream();
			content.type = "text/plain";
			// content.length = sendData.contentLength;
			mpContent.parts.add(content);
			mpContent.parts.add(atom);
			request.content = mpContent;

			Log.d(TAG, "content: " + content);
			Log.d(TAG, "request: " + request);
			HttpResponse response = request.execute();
			Log.d(TAG, "status was " + response.statusMessage);
			entry = response.parseAs(GDocEntry.class);
			Log.d(TAG, "new entry: " + entry);
			newModification = entry.getUpdated();
			Log.d(TAG, "newModification: " + newModification);
		} catch (Exception e) {
			Log.e(TAG, "exception in createDoc: " + e);
			e.printStackTrace();
		} finally {
			try {
				content.inputStream.close();
			} catch (Exception e) {
				Log.e(TAG, "exception in createDoc: " + e);
			}
		}
		return newModification;
	}

	private void setLogging(boolean logging) {
		Logger.getLogger("com.google.api.client").setLevel(
				logging ? Level.FINEST : Level.OFF);
		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		boolean currentSetting = settings.getBoolean("logging", false);
		if (currentSetting != logging) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("logging", logging);
			editor.commit();
		}
	}

	private void updateModificationDate(String title, Date modificationDate) {
		ContentResolver resolver = getContentResolver();
		ContentValues values = new ContentValues();
		String[] args = { title };

		values.put(GameInfo.KEY_MODIFIED_DATE, modificationDate.getTime());

		Log.d(TAG, "values: " + values);
		resolver.update(SGFProvider.CONTENT_URI, values,
				SGFProvider.QUERY_STRING, args);
	}

	private void handleException(Throwable e) {
		Log.d(TAG, "handleException: " + e);
		e.printStackTrace();
		while (e instanceof RuntimeException) {
			e = e.getCause();
		}
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).response;
			int statusCode = response.statusCode;
			Log.d(TAG, "handleException: status=" + statusCode);
			try {
				response.ignore();
			} catch (IOException e1) {
				showError("I'm sorry", e1);
			}
			if (statusCode == 401 || statusCode == 403) {
				Log.d(TAG, "handleException: login required");
				gotAccount(true);
				return;
			}
			try {
				Log.e(TAG, response.parseAsString());
			} catch (IOException parseException) {
				showError("I'm sorry", parseException);
			}
		} else {
			showError("I'm sorry", e);
		}
	}

	private void showError(String message, Throwable ex) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(context, message + ": " + ex, duration);
		toast.show();
	}

}
