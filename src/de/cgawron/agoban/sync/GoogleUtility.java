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
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartRelatedContent;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.xml.atom.AtomContent;
import com.google.api.client.http.xml.atom.AtomParser;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import com.google.api.client.xml.XmlNamespaceDictionary;

import de.cgawron.agoban.provider.GameInfo;
import de.cgawron.agoban.provider.SGFProvider;

/**
 * Sync SGF files with Google documents. Based on sample code from google java
 * api client library.
 * <p>
 * To enable logging of HTTP requests/responses, run this command:
 * {@code adb shell setprop log.tag.HttpTransport DEBUG}.
 * </p>
 * 
 * @author Christian Gawron
 */
public final class GoogleUtility
{
	/** The token type for authentication */
	private static final String AUTH_TOKEN_TYPE = "writely";
	private static Level LOGGING_LEVEL = Level.ALL;
	private static final String PREFS = "SyncPrefs";
	private static final String TAG = "GoogleUtility";
	private static final String FOLDER_SGF = "SGF";
	private static final String CATEGORY_KIND = "http://schemas.google.com/g/2005#kind";
	private static final String CATEGORY_DOCUMENT = "http://schemas.google.com/docs/2007#document";
	private static final String CATEGORY_FILE = "http://schemas.google.com/docs/2007#file";
	private static final String LINK_PARENT = "http://schemas.google.com/docs/2007#parent";
	private static final String LINK_RESUMABLE_CREATE = "http://schemas.google.com/g/2005#resumable-create-media";
	private static final int REQUEST_AUTHENTICATE = 0;
	private static final int DIALOG_ACCOUNTS = 0;
	static final String PREF = TAG;
	private static final String PREF_ACCOUNT_NAME = "accountName";
	private static final String PREF_AUTH_TOKEN = "authToken";
	private static final String PREF_GSESSIONID = "gsessionid";
	private static final String PREF_SGF_FOLDER = "sgfFolder";
	private final SharedPreferences settings;
	private final Context context;
	private final GoogleAccountManager accountManager;
	private String gsessionid;
	private String authToken;
	private String accountName;
	//private String sgfFolder;

	private final HttpRequestFactory requestFactory;
	private final Account account;
	private GDocFeed folderFeed;
	private GDocEntry sgfFolder;

	private static DateFormat utcFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private static final String[] PROJECTION = new String[] { 
		GameInfo.KEY_ID,
		GameInfo.KEY_FILENAME, 
		GameInfo.KEY_LOCAL_MODIFIED_DATE,
		GameInfo.KEY_REMOTE_MODIFIED_DATE 
	};

	private static final XmlNamespaceDictionary DICTIONARY = new XmlNamespaceDictionary()
		.set("", "http://www.w3.org/2005/Atom")
		.set("app", "http://www.w3.org/2007/app")
		.set("batch", "http://schemas.google.com/gdata/batch")
		.set("docs", "http://schemas.google.com/docs/2007")
		.set("gAcl", "http://schemas.google.com/acl/2007")
		.set("gd", "http://schemas.google.com/g/2005")
		.set("openSearch", "http://a9.com/-/spec/opensearch/1.1/")
		.set("xml", "http://www.w3.org/XML/1998/namespace");

	private class MyHttpRequestInitializer implements HttpRequestInitializer 
	{
		public void initialize(HttpRequest request) {
			if (authToken == null) {
				// use the account manager to request the credentials
				try {
					authToken = accountManager.manager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(GoogleUtility.PREF_AUTH_TOKEN, authToken);
				}
				catch (Exception ex) {
					Log.e(TAG, "Could not get authToken", ex);
				}
			}
			GoogleHeaders headers = new GoogleHeaders();
			headers.setApplicationName("AGoban/1.0");
			headers.gdataVersion = "3.0";
			request.headers = headers;
			AtomParser parser = new AtomParser();
			parser.namespaceDictionary = DICTIONARY;
			request.addParser(parser);
			request.enableGZipContent = true;
			request.interceptor = new HttpExecuteInterceptor() {
				public void intercept(HttpRequest request) throws IOException {
					GoogleHeaders headers = (GoogleHeaders) request.headers;
					headers.setGoogleLogin(authToken);
					request.url.set("gsessionid", gsessionid);
				}
			};
			
			request.unsuccessfulResponseHandler = new HttpUnsuccessfulResponseHandler() {
				public boolean handleResponse(HttpRequest request, HttpResponse response, boolean retrySupported) {
					switch (response.statusCode) {
					case 302:
						GoogleUrl url = new GoogleUrl(response.headers.location);
						gsessionid = (String) url.getFirst("gsessionid");
						SharedPreferences.Editor editor = settings.edit();
						editor.putString(PREF_GSESSIONID, gsessionid);
						editor.commit();
						return true;
					case 401:
						accountManager.invalidateAuthToken(authToken);
						authToken = null;
						SharedPreferences.Editor editor2 = settings.edit();
						editor2.remove(PREF_AUTH_TOKEN);
						editor2.commit();
						return false;
					}
					return false;
				}
			};
		}
	}

	public static class GDocFeed
	{
		@Key("openSearch:totalResults")
		public int totalResults;

		@Key("entry")
		public List<GDocEntry> entries;

		@Key("link")
		public List<Link> links;
		
		@Key
		public DateTime updated;

		public String getLink(String key)
		{
			return Link.find(links, key);
		}
		
		public GDocFeed()
		{
			Log.d(TAG, "GDocFeed()");
		}
	}

	public static class GDocEntry
	{
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

		public Date getUpdated()
		{
			return new Date(updated.value);
		}

		public GenericUrl getUpdateUrl()
		{
			for (Link link : links) {
				if (link.rel.equals("edit-media"))
					return new GenericUrl(link.href);
			}
			return null;
		}

		public GenericUrl getDownloadLink()
		{
			return new GenericUrl(content.src);
		}

		public GenericUrl getDownloadLink(String type)
		{
			return new GenericUrl(content.src + "&exportFormat=" + type);
		}

		public InputStream getStream(HttpRequestFactory requestFactory) throws IOException
		{
			HttpRequest request = requestFactory.buildGetRequest(getDownloadLink());
			return request.execute().getContent();
		}

		public InputStream getStream(HttpRequestFactory requestFactory, String type) throws IOException
		{
			HttpRequest request = requestFactory.buildGetRequest(getDownloadLink(type));	
			return request.execute().getContent();
		}
		
		@Override
		public String toString()
		{
			return "DocEntry: " + title;
		}
	}

	public static class Category
	{
		@Key("@scheme")
		public String scheme;

		@Key("@term")
		public String term;

		@Key("@label")
		public String label;

		public Category()
		{
		}

		public Category(String scheme, String term, String label)
		{
			this.scheme = scheme;
			this.term = term;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return "category: " + scheme + "->" + term + ", label=" + label;
		}
	}

	public static class ContentLink
	{
		@Key("@type")
		public String type;

		@Key("@src")
		public String src;

		@Override
		public String toString()
		{
			return "contentLink: " + type + ": " + src;
		}
	}

	public static class Link
	{
		@Key("@href")
		public String href;

		@Key("@title")
		public String title;

		@Key("@rel")
		public String rel;

		public static String find(List<Link> links, String rel)
		{
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
		public String toString()
		{
			return "link: " + rel + "->" + href;
		}
	}

	public static class GDocUrl extends GenericUrl
	{
		@Key("updated-min")
		String updatedMin;

		@Key("max-results")
		Integer maxResults;

		@Key
		Boolean convert;
		
		GDocUrl(String url)
		{
			super(url);
		}
	}

	static class SendData
	{
		File file;
		String contentType;
		long contentLength;

		SendData(Cursor cursor)
		{
			Log.d(TAG, "SendData: " + cursor.getString(1));
			String fileName = cursor.getString(cursor.getColumnIndexOrThrow(GameInfo.KEY_FILENAME));
			file = new File(SGFProvider.SGF_DIRECTORY, fileName);
		}

		public SendData(File file)
		{
			this.file = file;
		}

		public InputStream getInputStream() throws FileNotFoundException
		{
			return new FileInputStream(file);
		}

		public String getTitle()
		{
			return file.getName();
		}
	}

	public GoogleUtility(Context context, GoogleAccountManager accountManager, Account account)
	{
		// transport = new NetHttpTransport();
		Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);
		this.context = context;
		this.settings = context.getSharedPreferences(GoogleUtility.PREF, 0);
		this.accountManager = accountManager;
		this.account = account;
		authToken = settings.getString(PREF_AUTH_TOKEN, null);
	    gsessionid = settings.getString(PREF_GSESSIONID, null);
		HttpTransport transport = new ApacheHttpTransport();
		requestFactory = transport.createRequestFactory(new MyHttpRequestInitializer());
		try {
			if (true) initFolder();
		} 
		catch (IOException ex) {
			Log.e(TAG, "initFolder() failed", ex);
		}
	}

	private void initFolder() throws IOException
	{
		/*
		GDocUrl url = new GDocUrl("https://docs.google.com/feeds/metadata/default");
		HttpRequest request = requestFactory.buildGetRequest(url);
		Log.d(TAG, "retrieving " + url);
		request.execute().ignore();
		*/
		
		String sgfFolderName = settings.getString(PREF_SGF_FOLDER, "SGF");
		GDocUrl url = new GDocUrl("https://docs.google.com/feeds/default/private/full/-/folder");
		HttpRequest request = requestFactory.buildGetRequest(url);
		Log.d(TAG, "retrieving " + url);

		HttpResponse response = request.execute();
		GDocFeed feed = response.parseAs(GDocFeed.class);
		for (GDocEntry entry : feed.entries)
		{
			Log.d(TAG, "initFolder: entry=" + entry);
			if (entry.title.equals(sgfFolderName)) {
				sgfFolder = entry;
				if (entry.links != null) {
					for (Link link : entry.links) {
						Log.d(TAG, "link=" + link);
					}
				}
			}	
		}
	}
	
	private GDocUrl getFolderUrl()
	{
		GDocUrl url = new GDocUrl(Link.find(sgfFolder.links, "self"));
		url.appendRawPath("/contents");
		url.convert = false;
		return url;
	}

	public void updateDocumentList() throws IOException
	{
		GDocUrl url = getFolderUrl();
		if (folderFeed != null) {
		    url.updatedMin = utcFormatter.format(folderFeed.updated);
		}
		HttpRequest request = requestFactory.buildGetRequest(url);
		Log.d(TAG, "retrieving " + url);

		HttpResponse response = request.execute();
		folderFeed = response.parseAs(GDocFeed.class);
		if (folderFeed.links != null) {
			for (Link link : folderFeed.links) {
				Log.d(TAG, "link: " + link);
			}
		}
	}

	public GenericUrl getUploadUrl(File file) throws IOException
	{
		GDocUrl url = new GDocUrl(folderFeed.getLink(LINK_RESUMABLE_CREATE));
		url.convert = false;
		HttpRequest request = requestFactory.buildPostRequest(url, null);
		HttpHeaders headers = request.headers;
		((GoogleHeaders) headers).setSlugFromFileName(file.getName());
		headers.set("Content-Type", "application/x-go-sgf");
		headers.set("X-Upload-Content-Type", "application/x-go-sgf");
		headers.set("X-Upload-Content-Length", file.length());

		Log.d(TAG, "request: " + request);
		HttpResponse response = request.execute();
		headers = response.headers;
		Log.d(TAG, "status is " + response.statusMessage);
		Log.d(TAG, "location is " + headers.location);

		return new GDocUrl(headers.location);
	}
	
	public GDocEntry createGoogleDoc(File file) throws IOException
	{
		GenericUrl targetUrl = getUploadUrl(file);
		
		Date newModification = null;
		SendData sendData = new SendData(file);
		
		Log.d(TAG, "createGoogleDoc: url=" + targetUrl + ", data=" + sendData);
		InputStreamContent content = new InputStreamContent();
		//AtomContent atom = new AtomContent();
		GDocEntry entry = null;// = new GDocEntry();
		//atom.namespaceDictionary = DICTIONARY;
		//atom.entry = entry;
		//entry.convert = false;
		//entry.categories = new ArrayList<Category>();
		//Category category = new Category(CATEGORY_KIND, CATEGORY_FILE, "file");
		//entry.categories.add(category);
		//Log.d(TAG, "category: " + category);

		try {
			HttpRequest request = requestFactory.buildPostRequest(targetUrl, content);
			//((GoogleHeaders) request.headers).setSlugFromFileName(sendData.getTitle());
			//HttpContent mpContent = MultipartRelatedContent.forRequest(request);
			content.inputStream = sendData.getInputStream();
			content.type = "application/x-go-sgf";
			//content.type = "application/ms-word";
			//content.type = "text/plain";
			// content.length = sendData.contentLength;
			//mpContent.parts.add(content);
			//mpContent.parts.add(atom);
			//request.content = mpContent;

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
		return entry;
	}

	public GDocEntry updateGoogleDoc(GDocEntry doc, File file) 
	{
		Date newModification = null;
		SendData sendData = new SendData(file);
		GenericUrl targetUrl = doc.getUpdateUrl();
		Log.d(TAG, "updateGoogleDoc: url=" + targetUrl + ", etag=" + doc.etag + ", data=" + sendData);
		InputStreamContent content = new InputStreamContent();
		AtomContent atom = new AtomContent();
		GDocEntry entry = new GDocEntry();
		atom.namespaceDictionary = DICTIONARY;
		atom.entry = entry;
		entry.categories = new ArrayList<Category>();
		Category category = new Category(CATEGORY_KIND, CATEGORY_DOCUMENT, "document");
		entry.categories.add(category);
		entry.convert = false;
		entry.etag = doc.etag;
		atom.entry = entry;

		try {
			HttpRequest request = requestFactory.buildPutRequest(targetUrl, null);
			MultipartRelatedContent mpContent = MultipartRelatedContent.forRequest(request);
			((GoogleHeaders) request.headers).setSlugFromFileName(sendData.getTitle());

			content.inputStream = sendData.getInputStream();
			//content.type = "text/plain";
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
		return entry;
	}

	public InputStream getStream(GDocEntry doc, String type) throws IOException
	{
		return doc.getStream(requestFactory, type);
	}

	public List<GDocEntry> getDocs()
	{
		if (folderFeed != null)
			return folderFeed.entries;
		return null;
	}

}
