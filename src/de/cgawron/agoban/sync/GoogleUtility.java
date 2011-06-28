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
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
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
	private static final String AUTH_TOKEN_TYPE = "writely";
	private static final String FOLDER_SGF = "SGF";
	static final String CATEGORY_KIND = "http://schemas.google.com/g/2005#kind";
	static final String CATEGORY_LABELS = "http://schemas.google.com/g/2005/labels";
	static final String CATEGORY_DOCUMENT = "http://schemas.google.com/docs/2007#document";
	static final String CATEGORY_FILE = "http://schemas.google.com/docs/2007#file";
	static final String LABEL_TRASHED = "http://schemas.google.com/g/2005/labels#trashed";
	static final String LINK_PARENT = "http://schemas.google.com/docs/2007#parent";
	static final String LINK_RESUMABLE_CREATE = "http://schemas.google.com/g/2005#resumable-create-media";
	static final String PREF = "GoogleUtility";
	static final String TAG = PREF;
	private static final Logger logger = Logger.getLogger(TAG);
	private static final String PREF_AUTH_TOKEN = "authToken";
	private static final String PREF_GSESSIONID = "gsessionid";
	private static final String PREF_SGF_FOLDER = "sgfFolder";
	private static final String PREF_FOLDER_URL = "folderURL";
	private static final String PREF_LAST_ETAG = "lastETag";
	private final SharedPreferences settings;
	private final SharedPreferences.Editor editor;
	private final Context context;
	private final AccountManager accountManager;
	private String gsessionid;
	private String authToken;
	private String lastETag;
	private String folderURL;
	
	private final HttpRequestFactory requestFactory;
	private GDocFeed folderFeed;
	private static GDocEntry sgfFolder;

	private static DateFormat utcFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

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
						editor.putString(PREF_GSESSIONID, gsessionid);
						editor.commit();
						return true;
					case 401:
						accountManager.invalidateAuthToken(AUTH_TOKEN_TYPE,authToken);
						authToken = null;
						editor.remove(PREF_AUTH_TOKEN);
						editor.commit();
						return false;
					}
					return false;
				}
			};
		}
	}

	public static class GDocFeed
	{
		@Key("@gd:etag")
		public String etag;
		
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
		}
	}

	public static class GDocEntry
	{
		@Key("@gd:etag")
		public String etag;

		@Key("gd:resourceId")
		public String resourceId;

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

		public Category getCategory(String scheme)
		{
			for (Category category : categories) {
				if (scheme.equals(category.scheme)) return category;
			}
			return null;
		}

		public Category getKind()
		{
			return getCategory(CATEGORY_KIND);
		}

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
		
		public boolean isTrashed() 
		{
			return Category.hasCategory(categories, CATEGORY_LABELS, LABEL_TRASHED);
		}

		@Override
		public String toString()
		{
			return "DocEntry: " + resourceId + " \"" + title + "\"";
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
		
		public static boolean hasCategory(List<Category> categories, String scheme, String term)
		{
			for (Category category : categories) {
				if (scheme.equals(category.scheme) && term.equals(category.term)) return true;
			}
			return false;
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
		
		@Key("showdeleted")
		Boolean showDeleted;

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
			logger.fine("SendData: " + cursor.getString(1));
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

	public GoogleUtility(Context context, AccountManager accountManager, Account account)
	{
		this.context = context;
		this.settings = this.context.getSharedPreferences(GoogleUtility.PREF, 0);
		this.editor = settings.edit();
		this.accountManager = accountManager;
		authToken = settings.getString(PREF_AUTH_TOKEN, null);
	    gsessionid = settings.getString(PREF_GSESSIONID, null);
		lastETag = settings.getString(PREF_LAST_ETAG, null);
		folderURL = settings.getString(PREF_FOLDER_URL, null);
		Log.d(TAG, "authToken: " + authToken);
		if (authToken == null) {
			// use the account manager to request the credentials
			try {
				authToken = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, false);
				Log.d(TAG, "authToken after blockingGetAuthToken: " + authToken);
				editor.putString(GoogleUtility.PREF_AUTH_TOKEN, authToken);
				editor.commit();
			}
			catch (Exception ex) {
				logger.log(Level.SEVERE, "Could not get authToken", ex);
			}
			// if (authToken == null)
			//	throw new RuntimeException("Failed to obtain authToken");
		}
		HttpTransport transport = new ApacheHttpTransport();
		requestFactory = transport.createRequestFactory(new MyHttpRequestInitializer());
		try {
			if (folderURL == null) initFolder();
		} 
		catch (IOException ex) {
			logger.log(Level.SEVERE, "initFolder() failed", ex);
		}
	}

	private synchronized void initFolder() throws IOException
	{		
		String sgfFolderName = settings.getString(PREF_SGF_FOLDER, FOLDER_SGF);
		GDocUrl url = new GDocUrl("https://docs.google.com/feeds/default/private/full/-/folder");
		HttpRequest request = requestFactory.buildGetRequest(url);
		logger.config("retrieving " + url);

		HttpResponse response = request.execute();
		GDocFeed feed = response.parseAs(GDocFeed.class);
		for (GDocEntry entry : feed.entries)
		{
			logger.config("initFolder: entry=" + entry);
			if (entry.title.equals(sgfFolderName)) {
				sgfFolder = entry;
				folderURL = Link.find(sgfFolder.links, "self");
				editor.putString(GoogleUtility.PREF_FOLDER_URL, folderURL);
				editor.commit();
			}	
		}
	}
	
	private GDocUrl getFolderUrl()
	{
		GDocUrl url = new GDocUrl(folderURL);
		url.appendRawPath("/contents");
		url.convert = false;
		return url;
	}

	public boolean updateDocumentList() throws IOException
	{
		//TODO: Handle continuation
		logger.info("updateDocumentList: lastUpdate=" + (lastETag != null ? lastETag : "null"));
		logger.config("TEST1");
		if (Log.isLoggable(TAG, Log.DEBUG))
			Log.d(TAG, "Test2");
		GDocUrl url = getFolderUrl();
		url.showDeleted = true;
		HttpRequest request = requestFactory.buildGetRequest(url);
		GoogleHeaders headers = (GoogleHeaders) request.headers;
		headers.ifNoneMatch = lastETag; 
		Log.d(TAG, "retrieving " + url);

		HttpResponse response = request.execute();
		boolean modified = response.statusCode != 304;
		if (modified) {
			folderFeed = response.parseAs(GDocFeed.class);
			lastETag = folderFeed.etag;
			editor.putString(GoogleUtility.PREF_LAST_ETAG, lastETag);
			editor.commit();
		}
		return modified;
	}

	public GenericUrl getUploadUrl(File file) throws IOException
	{
		GDocUrl url = new GDocUrl(folderFeed.getLink(LINK_RESUMABLE_CREATE));
		url.convert = false;
		HttpRequest request = requestFactory.buildPostRequest(url, null);
		GoogleHeaders headers = (GoogleHeaders) request.headers;
		headers.setSlugFromFileName(file.getName());
		headers.set("Content-Type", "application/x-go-sgf");
		headers.set("X-Upload-Content-Type", "application/x-go-sgf");
		headers.set("X-Upload-Content-Length", file.length());

		logger.config("request: " + request);
		HttpResponse response = request.execute();
		headers = (GoogleHeaders) response.headers;
		logger.config("status is " + response.statusMessage);
		logger.config("location is " + headers.location);

		return new GDocUrl(headers.location);
	}
	
	public GDocEntry createGoogleDoc(File file) throws IOException
	{
		GenericUrl targetUrl = getUploadUrl(file);
		
		Date newModification = null;
		SendData sendData = new SendData(file);
		
		logger.config("createGoogleDoc: url=" + targetUrl + ", data=" + sendData);
		InputStreamContent content = new InputStreamContent();
		//AtomContent atom = new AtomContent();
		GDocEntry entry = null;// = new GDocEntry();
		//atom.namespaceDictionary = DICTIONARY;
		//atom.entry = entry;
		//entry.convert = false;
		//entry.categories = new ArrayList<Category>();
		//Category category = new Category(CATEGORY_KIND, CATEGORY_FILE, "file");
		//entry.categories.add(category);
		//logger.config("category: " + category);

		try {
			HttpRequest request = requestFactory.buildPostRequest(targetUrl, content);
			//((GoogleHeaders) request.headers).setSlugFromFileName(sendData.getTitle());
			//HttpContent mpContent = MultipartRelatedContent.forRequest(request);
			content.inputStream = sendData.getInputStream();
			content.type = "application/x-go-sgf";
			// content.length = sendData.contentLength;
			//mpContent.parts.add(content);
			//mpContent.parts.add(atom);
			//request.content = mpContent;

			logger.config("content: " + content);
			logger.config("request: " + request);
			HttpResponse response = request.execute();
			logger.config("status was " + response.statusMessage);
			entry = response.parseAs(GDocEntry.class);
			logger.config("new entry: " + entry);
			newModification = entry.getUpdated();
			logger.config("newModification: " + newModification);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "exception in createGoogleDoc: " + e);
			e.printStackTrace();
		} finally {
			try {
				content.inputStream.close();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "exception in createGoogleDoc: " + e);
			}
		}
		return entry;
	}

	public GDocEntry updateGoogleDoc(GDocEntry doc, File file) 
	{
		Date newModification = null;
		SendData sendData = new SendData(file);
		GenericUrl targetUrl = doc.getUpdateUrl();
		logger.config("updateGoogleDoc: url=" + targetUrl + ", etag=" + doc.etag + ", data=" + sendData);
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
			mpContent.parts.add(content);
			mpContent.parts.add(atom);
			request.content = mpContent;

			logger.config("request: " + request);
			HttpResponse response = request.execute();
			logger.config("status was " + response.statusMessage);
			entry = response.parseAs(GDocEntry.class);
			logger.config("new entry: " + entry);
			newModification = entry.getUpdated();
			logger.config("newModification: " + newModification);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "exception in updateGoogleDoc: " + e);
			e.printStackTrace();
		} finally {
			try {
				content.inputStream.close();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "exception in updateGoogleDoc: " + e);
			}
		}
		return entry;
	}

	public InputStream getStream(GDocEntry doc) throws IOException
	{
		return doc.getStream(requestFactory);
	}

	public List<GDocEntry> getDocs()
	{
		if (folderFeed != null)
			return folderFeed.entries;
		return null;
	}

}
