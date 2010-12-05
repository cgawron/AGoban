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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
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
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartRelatedContent;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.atom.AtomContent;
import com.google.api.client.xml.atom.AtomParser;

import de.cgawron.agoban.SGFApplication;
import de.cgawron.agoban.provider.GameInfo;
import static de.cgawron.agoban.provider.GameInfo.KEY_ID;
import static de.cgawron.agoban.provider.GameInfo.KEY_FILENAME;
import static de.cgawron.agoban.provider.GameInfo.KEY_MODIFIED_DATE;
import de.cgawron.agoban.provider.SGFProvider;

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

    private static final String TAG = "GoogleSync";

    private static final int MENU_ADD = 0;

    private static final int MENU_ACCOUNTS = 1;

    private static final int CONTEXT_EDIT = 0;

    private static final int CONTEXT_DELETE = 1;

    private static final int CONTEXT_LOGGING = 2;

    private static final int REQUEST_AUTHENTICATE = 0;

    private static final String PREF = "AGoban";

    private static final int DIALOG_ACCOUNTS = 0;
    
    private static HttpTransport transport;
    private String authToken;
    private SGFApplication application;

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

	@Key
	public DateTime updated;

	@Key("link")
	public List<Link> links;

	@Key
	public ContentLink content;

	@Key("category")
	public List<Category> categories;

	public Date getUpdated()
	{
	    return new Date(updated.value);
	}

	private String getDownloadLink() 
	{
	    return content.src;
	}

	private InputStream getStream() throws IOException
	{
	    HttpRequest request = transport.buildGetRequest();
	    request.setUrl(getDownloadLink());
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

	public String toString() {
	    return "category: " + scheme + "->" + term + ", label=" + label;
	}    
    }

    public static class ContentLink {
	@Key("@type")
	public String type;

	@Key("@src")
	public String src;
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

    private GenericUrl getDocUrl()
    { 
	return new GenericUrl("https://docs.google.com/feeds/default/private/full");
    }

    public GoogleSync() 
    {
	HttpTransport.setLowLevelHttpTransport(ApacheHttpTransport.INSTANCE);
	transport = GoogleTransport.create();
	GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
	headers.setApplicationName("AGoban");
	headers.gdataVersion = "3";
	AtomParser parser = new AtomParser();
	parser.namespaceDictionary = DICTIONARY;
	transport.addParser(parser);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
	super.onCreate(savedInstanceState);
	application = (SGFApplication) getApplication();
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	setLogging(true);
	Intent intent = getIntent();
	if (Intent.ACTION_SEND.equals(intent.getAction())) {
	    sendData = new SendData(intent, getContentResolver());
	} else if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
	    sendData = null;
	}

	gotAccount(true);
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
	Log.d(TAG, "gotAccount: " + tokenExpired);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
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
	Log.d(TAG, "gotAccount: " + manager + ", " + account);
	SharedPreferences settings = getSharedPreferences(PREF, 0);
	SharedPreferences.Editor editor = settings.edit();
	editor.putString("accountName", account.name);
	editor.commit();
	new Thread() {

	    @Override
	    public void run() {
		try {
		    final Bundle bundle =
			manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, null, null).getResult();
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
					Log.d(TAG, "gotAccount: login " + bundle.getString(AccountManager.KEY_AUTHTOKEN));
					authenticatedClientLogin(bundle.getString(AccountManager.KEY_AUTHTOKEN));
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
	Log.d(TAG, "authToken: " + authToken);
	((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
	authenticated();
    }

    static class SendData {
	String fileName;
	Uri uri;
	String contentType;
	long contentLength;

	SendData(Intent intent, ContentResolver contentResolver) 
	{
	    Bundle extras = intent.getExtras();
	    Log.d(TAG, "SendData: " + intent + ", " + extras);
	    if (extras.containsKey(Intent.EXTRA_STREAM)) {
		Uri uri = this.uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
		String scheme = uri.getScheme();
		Log.d(TAG, "SendData: uri=" + this.uri);
		this.contentType = SGFProvider.SGF_TYPE;
		if (scheme.equals("content")) {
		    Cursor cursor = contentResolver.query(uri, null, null, null, null);
		    cursor.moveToFirst();
		    this.fileName = cursor.getString(cursor.getColumnIndexOrThrow(GameInfo.KEY_FILENAME));
		    this.contentLength = 100;
		    cursor.close();
		}
	    }
	}
    }

    static SendData sendData;

    private void authenticated() {
	Log.d(TAG, "authenticated");
	
	retrieveDocuments();

	
	if (sendData != null)
	    createDoc();
	
    }

    private List<GDocEntry> getDocumentList()
    {
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

    private void retrieveDocuments()
    {
	String projection[] = { KEY_FILENAME, KEY_MODIFIED_DATE };
	ContentResolver resolver = getContentResolver();
	List<GDocEntry> entries = getDocumentList();
	for (GDocEntry entry : entries) {
	    Log.d(TAG, "doc: " + entry.title);
	    boolean isSgf = false;
	    for (Category category : entry.categories) {
		if (category.scheme.equals("http://schemas.google.com/g/2005#kind") &&
		    category.term.equals("http://schemas.google.com/docs/2007#file") &&
		    category.label.equals("application/x-go-sgf"))
		    isSgf = true;
	    }
	    if (!isSgf) continue;
	    
	    Log.d(TAG, String.format("file %s updated on %s", entry.title, entry.getUpdated().toString()));
	    Cursor cursor = resolver.query(SGFProvider.CONTENT_URI, projection, 
					   String.format("%s = '%s'", KEY_FILENAME, entry.title), null, null);
	    
	    if (cursor.getCount() > 0) {
		cursor.moveToFirst();
		Date localModification = new Date(cursor.getLong(1));
		Date remoteModification = entry.getUpdated();
		Log.d(TAG, "local  modification: " + localModification);
		Log.d(TAG, "remote modification: " + remoteModification);
		
		if (localModification.before(remoteModification)) {
		    retrieve(entry);
		}
		else {
		    Log.d(TAG, "not retrieving " + entry.title);
		}
		cursor.close();
	    }
	    else {
		retrieve(entry);
	    }
	}
    }

    private void retrieve(GDocEntry entry)
    {
	Log.d(TAG, "retrieving " + entry.title);
	try {
	    InputStream is = entry.getStream();
	    File file = new File(SGFProvider.getSGFDirectory(), entry.title);
	    OutputStream os = new FileOutputStream(file);
	    Log.d(TAG, "saving " + entry.title + " to " + file);
	    byte buf[] = new byte[1024];
	    int count;
	    while ((count=is.read(buf)) > 0)
		os.write(buf, 0, count);
	    os.close();
	    is.close();
	    Log.d(TAG, "succesfully saved " + entry.title);
	    //SGFProvider.doUpdateDatabase();
	}
	catch (IOException ex) {
	    Log.e(TAG, "retrieveDocuments: caught " + ex);
	}
    }

    private void createDoc()
    {
	Logger.getLogger("com.google.api.client").setLevel(Level.ALL);
	Log.d(TAG, "createDoc");
	InputStreamContent content = new InputStreamContent();
	AtomContent atom = new AtomContent();
	GDocEntry entry = new GDocEntry();
	Category category = new Category();
	atom.namespaceDictionary = DICTIONARY;
	atom.entry = entry;
	category.scheme = "http://schemas.google.com/g/2005#kind";
	category.term = "http://schemas.google.com/docs/2007#file";
	category.label = "application/x-go-sgf";
	entry.categories = new ArrayList();
	entry.categories.add(category);
	try {
            HttpRequest request = transport.buildPostRequest();
            request.url = getDocUrl();
            ((GoogleHeaders) request.headers).setSlugFromFileName(sendData.fileName);
	    MultipartRelatedContent mpContent = MultipartRelatedContent.forRequest(request);
            content.inputStream = getContentResolver().openInputStream(sendData.uri);
            content.type = sendData.contentType;
            //content.length = sendData.contentLength;
	    mpContent.parts.add(content);
	    mpContent.parts.add(atom);
            request.content = mpContent;

	    Log.d(TAG, "content: " + content);
	    Log.d(TAG, "request: " + request);
            HttpResponse response = request.execute();
	    Log.d(TAG, "status was " + response.statusMessage);
	    response.ignore();
	} catch (Exception e) {
	    Log.e(TAG, "exception in createDoc: " + e);
	    e.printStackTrace();
	}
	finally {
	    try {
		content.inputStream.close();
	    } catch (Exception e) {
		Log.e(TAG, "exception in createDoc: " + e);
	    }
	}
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
		e1.printStackTrace();
	    }
	    if (statusCode == 401 || statusCode == 403) {
		Log.d(TAG, "handleException: login required");
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
