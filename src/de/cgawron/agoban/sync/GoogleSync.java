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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.api.client.apache.ApacheHttpTransport;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.atom.AtomParser;

/**
 * Sync SGF files with Google documents.
 * Based on sample code from google java api client library.
 * 
 * @author Christian Gawron
 */
public final class GoogleSync extends Activity 
{
    /** The token type for authentication */
    private static final String AUTH_TOKEN_TYPE = "writely";
    
    private static HttpTransport transport;
    private String authToken;

    public static final XmlNamespaceDictionary DICTIONARY =
	new XmlNamespaceDictionary();

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
	@Key
	public String title;

	@Key("link")
	public List<Link> links;

	@Key("category")
	public List<Category> categories;

	private String getEditLink() {
	    return Link.find(links, "edit");
	}
    }

    public static class Category {
	@Key("@scheme")
	public String scheme;

	@Key("@term")
	public String term;

	public String toString() {
	    return "category: " + scheme + "->" + term;
	}    
    }

    public static class Link {
	
	@Key("@href")
	public String href;

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

	public String toString() {
	    return "link: " + rel + "->" + href;
	}    
    }

    //private final List<AlbumEntry> albums = new ArrayList<AlbumEntry>();

    public ListGoogleSGF() 
    {
	HttpTransport.setLowLevelHttpTransport(ApacheHttpTransport.INSTANCE);
	transport = GoogleTransport.create();
	GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
	headers.setApplicationName("google-docsaatomsample-1.0");
	headers.gdataVersion = "3";
	AtomParser parser = new AtomParser();
	parser.namespaceDictionary = DICTIONARY;
	transport.addParser(parser);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
	super.onCreate(savedInstanceState);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	setLogging(true);
	getListView().setTextFilterEnabled(true);
	registerForContextMenu(getListView());
	Intent intent = getIntent();
	if (Intent.ACTION_SEND.equals(intent.getAction())) {
	    sendData = new SendData(intent, getContentResolver());
	} else if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
	    sendData = null;
	}
	gotAccount(false);
    }

    @Override
    protected Dialog onCreateDialog(int id) 
    {
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

    private void gotAccount(boolean tokenExpired) 
    {
	Log.d("ListGoogleSGF", "gotAccount: " + tokenExpired);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	String accountName = settings.getString("accountName", null);
	if (accountName != null) {
	    AccountManager manager = AccountManager.get(this);
	    Account[] accounts = manager.getAccountsByType("com.google");
	    int size = accounts.length;
	    for (int i = 0; i < size; i++) {
		Account account = accounts[i];
		if (accountName.equals(account.name)) {
		    if (tokenExpired) {
			manager.invalidateAuthToken("com.google", this.authToken);
		    }
		    gotAccount(manager, account);
		    return;
		}
	    }
	}
	showDialog(DIALOG_ACCOUNTS);
    }

    private void gotAccount(final AccountManager manager, final Account account) 
    {
	Log.d("ListGoogleSGF", "gotAccount: " + manager + ", " + account);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	SharedPreferences.Editor editor = settings.edit();
	editor.putString("accountName", account.name);
	editor.commit();
	new Thread() {

	    @Override
	    public void run() {
		try {
		    final Bundle bundle =
			manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, null, null)
			.getResult();
		    runOnUiThread(new Runnable() {

			    public void run() {
				try {
				    if (bundle.containsKey(AccountManager.KEY_INTENT)) {
					Intent intent =
					    bundle.getParcelable(AccountManager.KEY_INTENT);
					int flags = intent.getFlags();
					flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
					intent.setFlags(flags);
					startActivityForResult(intent, REQUEST_AUTHENTICATE);
				    } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
					authenticatedClientLogin(
								 bundle.getString(AccountManager.KEY_AUTHTOKEN));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
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
	Log.d("ListGoogleDocs", "authToken: " + authToken);
	((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
	authenticated();
    }

    static class SendData {
	String fileName;
	Uri uri;
	String contentType;
	long contentLength;

	SendData(Intent intent, ContentResolver contentResolver) {
	    Bundle extras = intent.getExtras();
	    if (extras.containsKey(Intent.EXTRA_STREAM)) {
		Uri uri = this.uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
		String scheme = uri.getScheme();
		if (scheme.equals("content")) {
		    Cursor cursor = contentResolver.query(uri, null, null, null, null);
		    cursor.moveToFirst();
		    this.fileName = cursor.getString(
						     cursor.getColumnIndexOrThrow(Images.Media.DISPLAY_NAME));
		    this.contentType = intent.getType();
		    this.contentLength =
			cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media.SIZE));
		}
	    }
	}
    }

    static SendData sendData;

    private void authenticated() {
	Log.d("ListGoogleDocs", "authenticated");
	try {
            HttpRequest request = transport.buildGetRequest();
            request.url = new GenericUrl("https://docs.google.com/feeds/default/private/full/-/{http://schemas.google.com/g/2005#kind}application/x-go-sgf");
	    Log.d("ListGoogleDocs", "url: " + request.url);
	    Log.d("ListGoogleDocs", "headers: " + request.headers);
            HttpResponse response = request.execute();
	    GDocFeed feed = response.parseAs(GDocFeed.class);
	    Log.d("ListGoogleDocs", "feed: " + feed);

	    for (GDocEntry entry : feed.entries) {
		Log.d("ListGoogleDocs", "doc: " + entry.title);
		for (Link link : entry.links) {
		    Log.d("ListGoogleDocs", "link: " + link);
		}
		for (Category category : entry.categories) {
		    Log.d("ListGoogleDocs", "category: " + category);
		}
	    }
	} catch (IOException e) {
            throw new RuntimeException(e);
	}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	menu.add(0, MENU_ADD, 0, "New album");
	menu.add(0, MENU_ACCOUNTS, 0, "Switch Account");
	return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case MENU_ADD:
	    /*
	      AlbumEntry album = new AlbumEntry();
	      album.access = "private";
	      album.title = "Album " + new DateTime(new Date());
	      try {
	      AlbumEntry.executeInsert(transport, album, this.postLink);
	      } catch (IOException e) {
	      handleException(e);
	      }
	      executeRefreshAlbums();
	    */
	    return true;
	case MENU_ACCOUNTS:
	    showDialog(DIALOG_ACCOUNTS);
	    return true;
	}
	return false;
    }

    @Override
	public void onCreateContextMenu(
					ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	super.onCreateContextMenu(menu, v, menuInfo);
	menu.add(0, CONTEXT_EDIT, 0, "Update Title");
	menu.add(0, CONTEXT_DELETE, 0, "Delete");
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	boolean logging = settings.getBoolean("logging", false);
	menu.add(0, CONTEXT_LOGGING, 0, "Logging").setCheckable(true).setChecked(
										 logging);
    }

    @Override
	public boolean onContextItemSelected(MenuItem item) {
	/*
	  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	  AlbumEntry album = albums.get((int) info.id);
	  try {
	  switch (item.getItemId()) {
	  case CONTEXT_EDIT:
          AlbumEntry patchedAlbum = album.clone();
          patchedAlbum.title =
	  album.title + " UPDATED " + new DateTime(new Date());
          patchedAlbum.executePatchRelativeToOriginal(transport, album);
          executeRefreshAlbums();
          return true;
	  case CONTEXT_DELETE:
          album.executeDelete(transport);
          executeRefreshAlbums();
          return true;
	  case CONTEXT_LOGGING:
          SharedPreferences settings = getSharedPreferences(PREF, 0);
          boolean logging = settings.getBoolean("logging", false);
          setLogging(!logging);
          return true;
	  default:
          return super.onContextItemSelected(item);
	  }
	  } catch (IOException e) {
	  handleException(e);
	  }
	*/
	return false;
    }

    private void executeRefreshAlbums() {
	/*
	  String[] albumNames;
	  List<AlbumEntry> albums = this.albums;
	  albums.clear();
	  try {
	  GenericUrl url = new GenericUrl("http://picasa.com/feed/api/user/default");
	  // page through results
	  while (true) {
	  UserFeed userFeed = UserFeed.executeGet(transport, url);
	  this.postLink = userFeed.getPostLink();
	  if (userFeed.albums != null) {
          albums.addAll(userFeed.albums);
	  }
	  String nextLink = userFeed.getNextLink();
	  if (nextLink == null) {
          break;
	  }
	  }
	  int numAlbums = albums.size();
	  albumNames = new String[numAlbums];
	  for (int i = 0; i < numAlbums; i++) {
	  albumNames[i] = albums.get(i).title;
	  }
	  } catch (IOException e) {
	  handleException(e);
	  albumNames = new String[] {e.getMessage()};
	  albums.clear();
	  }
	  setListAdapter(new ArrayAdapter<String>(
	  this, android.R.layout.simple_list_item_1, albumNames));
	*/
    }

    private void setLogging(boolean logging) {
	Logger.getLogger("com.google.api.client").setLevel(
							   logging ? Level.CONFIG : Level.OFF);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	boolean currentSetting = settings.getBoolean("logging", false);
	if (currentSetting != logging) {
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putBoolean("logging", logging);
	    editor.commit();
	}
    }

    private void handleException(Throwable e) 
    {
	Log.d("ListGoogleSGF", "handleException: " + e);
	e.printStackTrace();
	while (e instanceof RuntimeException) {
	    e = e.getCause();
	}
	if (e instanceof HttpResponseException) {
	    HttpResponse response = ((HttpResponseException) e).response;
	    int statusCode = response.statusCode;
	    Log.d("ListGoogleSGF", "handleException: status=" + statusCode);
	    try {
		response.ignore();
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    if (statusCode == 401 || statusCode == 403) {
		Log.d("ListGoogleSGF", "handleException: login required");
		gotAccount(true);
		return;
	    }
	    try {
		Log.e(TAG, response.parseAsString());
	    } catch (IOException parseException) {
		parseException.printStackTrace();
	    }
	}
    }
}
