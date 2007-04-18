/* *************************************************************************
 
 IT Mill Toolkit 

 Development of Browser User Interfaces Made Easy

 Copyright (C) 2000-2006 IT Mill Ltd
 
 *************************************************************************

 This product is distributed under commercial license that can be found
 from the product package on license.pdf. Use of this product might 
 require purchasing a commercial license from IT Mill Ltd. For guidelines 
 on usage, see licensing-guidelines.html

 *************************************************************************
 
 For more information, contact:
 
 IT Mill Ltd                           phone: +358 2 4802 7180
 Ruukinkatu 2-4                        fax:   +358 2 4802 7181
 20540, Turku                          email:  info@itmill.com
 Finland                               company www: www.itmill.com
 
 Primary source for information and releases: www.itmill.com

 ********************************************************************** */

package com.itmill.toolkit.terminal.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletContext;

/**
 * Theme source for reading themes from a JAR archive. At this time only jar
 * files are supported and an archive may not contain any recursive archives.
 * 
 * @author IT Mill Ltd.
 * @version
 * @VERSION@
 * @since 3.0
 */
public class ServletThemeSource implements ThemeSource {

	private ServletContext context;

	private Theme theme;

	private String path;

	private ApplicationServlet webAdapterServlet;

	private Cache resourceCache = new Cache();

	/**
	 * Collection of subdirectory entries.
	 */
	private URL descFile;

	/**
	 * Creates a new instance of ThemeRepository by reading the themes from a
	 * local directory.
	 * 
	 * @param file
	 *            the Path to the JAR archive .
	 * @param path
	 *            the Path inside the archive to be processed.
	 * @throws IOException
	 *             if the writing failed due to input/output error.
	 * @throws ThemeException
	 *             If the resource is not found or there was some problem
	 *             finding the resource.
	 */
	public ServletThemeSource(ServletContext context,
			ApplicationServlet webAdapterServlet, String path)
			throws IOException, ThemeException {

		this.theme = null;
		this.webAdapterServlet = webAdapterServlet;
		this.context = context;

		// Formats path
		this.path = path;
		if ((this.path.length() > 0) && !this.path.endsWith("/")) {
			this.path = this.path + "/";
		}
		if ((this.path.length() > 0) && !this.path.startsWith("/")) {
			this.path = "/" + this.path;
		}

		// Loads description file
		this.descFile = context.getResource(this.path + Theme.DESCRIPTIONFILE);
		InputStream entry = context.getResourceAsStream(this.path
				+ Theme.DESCRIPTIONFILE);
		try {
			if (entry != null) {
				try {
					this.theme = new Theme(entry);
				} catch (Exception e) {
					throw new ThemeException(
							"ServletThemeSource: Failed to load '" + path
									+ "': " + e);
				}
				entry.close();

				// Debug info
				if (webAdapterServlet.isDebugMode(null)) {
					Log.debug("Added ServletThemeSource: " + this.path);
				}

			} else {
				throw new IllegalArgumentException(
						"ServletThemeSource: Invalid theme resource: " + path);
			}
		} finally {
			if (entry != null)
				entry.close();
		}
	}

	/**
	 * Gets the XSL stream for the specified theme and web-browser type.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getXSLStreams(Theme,
	 *      WebBrowser)
	 */
	public Collection getXSLStreams(Theme theme, WebBrowser type)
			throws ThemeException {
		Collection xslFiles = new LinkedList();

		// If this directory contains a theme
		// return XSL from this theme
		if (this.theme != null) {

			if (webAdapterServlet.isDebugMode(null)) {
				Log.info("ServletThemeSource: Loading theme: " + theme);
			}

			// Reloads the description file
			InputStream entry = context.getResourceAsStream(this.path
					+ Theme.DESCRIPTIONFILE);
			try {
				if (entry != null) {
					this.theme = new Theme(entry);
				}
			} catch (Exception e) {
				throw new ThemeException("ServletThemeSource: Failed to load '"
						+ path + "': " + e);
			} finally {
				if (entry != null)
					try {
						entry.close();
					} catch (IOException ignored) {
					}
			}

			Collection fileNames = theme.getFileNames(type, Theme.MODE_HTML);
			// Adds all XSL file streams
			for (Iterator i = fileNames.iterator(); i.hasNext();) {
				String entryName = (String) i.next();
				if (entryName.endsWith(".xsl")) {

					entry = context
							.getResourceAsStream((this.path + entryName));
					xslFiles.add(new XSLStream(entryName, entry));
				}
			}

		}
		return xslFiles;
	}

	/**
	 * Returns the modification time of the description file.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getModificationTime()
	 */
	public long getModificationTime() {
		long modTime = 0;
		try {
			URLConnection conn = this.descFile.openConnection();
			modTime = conn.getLastModified();
		} catch (Exception ignored) {
			// In case of exceptions, return zero
		}
		return modTime;
	}

	/**
	 * Gets the input stream for the resource with the specified resource id.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getResource(String)
	 */
	public InputStream getResource(String resourceId)
			throws ThemeSource.ThemeException {

		// Checks the id
		String name = this.getName();
		int namelen = name.length();
		if (resourceId == null || !resourceId.startsWith(name + "/")
				|| resourceId.length() <= (namelen + 1)) {
			return null;
		}

		// Finds the resource
		String streamName = this.path + resourceId.substring(namelen + 1);
		InputStream stream = context.getResourceAsStream(streamName);
		if (stream != null)
			try {

				// Try cache
				byte[] data = (byte[]) resourceCache.get(stream);
				if (data != null)
					return new ByteArrayInputStream(data);

				// Read data
				int bufSize = 1024;
				ByteArrayOutputStream out = new ByteArrayOutputStream(bufSize);
				byte[] buf = new byte[bufSize];
				int n = 0;
				while ((n = stream.read(buf)) >= 0) {
					out.write(buf, 0, n);
				}
				try {
					stream.close();
				} catch (IOException ignored) {
				}
				data = out.toByteArray();

				// Cache data
				resourceCache.put(stream, data);
				return new ByteArrayInputStream(data);
			} catch (IOException e) {
			}

		throw new ThemeSource.ThemeException("Resource " + resourceId
				+ " not found.");
	}

	/**
	 * Gets the list of themes in the theme source.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getThemes()
	 */
	public Collection getThemes() {
		Collection themes = new LinkedList();
		if (this.theme != null) {
			themes.add(this.theme);
		}
		return themes;
	}

	/**
	 * Gets the name of the ThemeSource.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getName()
	 */
	public String getName() {
		return this.theme.getName();
	}

	/**
	 * Gets the Theme instance by name.
	 * 
	 * @see com.itmill.toolkit.terminal.web.ThemeSource#getThemeByName(String)
	 */
	public Theme getThemeByName(String name) {
		Collection themes = this.getThemes();
		for (Iterator i = themes.iterator(); i.hasNext();) {
			Theme t = (Theme) i.next();
			if (name != null && name.equals(t.getName()))
				return t;
		}
		return null;
	}

	/**
	 * @author IT Mill Ltd.
	 * @version
	 * @VERSION@
	 * @since 3.0
	 */
	private class Cache {

		private Map data = new HashMap();

		/**
		 * Associates the specified value with the specified key in this map. If
		 * the map previously contained a mapping for this key, the old value is
		 * replaced by the specified value.
		 * 
		 * @param key
		 *            the key with which the specified value is to be
		 *            associated.
		 * @param value
		 *            the value to be associated with the specified key.
		 */
		public void put(Object key, Object value) {
			data.put(key, new SoftReference(new CacheItem(value)));
		}

		/**
		 * Returns the value to which this map maps the specified key. Returns
		 * null if the map contains no mapping for this key.
		 * <p>
		 * A return value of null does not necessarily indicate that the map
		 * contains no mapping for the key; it's also possible that the map
		 * explicitly maps the key to null. The containsKey operation may be
		 * used to distinguish these two cases.
		 * </p>
		 * 
		 * @param key
		 *            the key whose associated value is to be returned.
		 * @return the value to which this map maps the specified key, or null
		 *         if the map contains no mapping for this key.
		 */
		public Object get(Object key) {
			SoftReference ref = (SoftReference) data.get(key);
			if (ref != null)
				return ((CacheItem) ref.get()).getData();
			return null;
		}

		/**
		 * Clears the data and removes all mappings from this map .
		 */
		public void clear() {
			data.clear();
		}
	}

	/**
	 * @author IT Mill Ltd.
	 * @version
	 * @VERSION@
	 * @since 3.0
	 */
	private class CacheItem {

		private Object data;

		/**
		 * 
		 * @param data
		 */
		public CacheItem(Object data) {
			this.data = data;
		}

		/**
		 * Gets the data.
		 * 
		 * @return the data.
		 */
		public Object getData() {
			return this.data;
		};

		/**
		 * Called by the garbage collector on an object when garbage collection
		 * determines that there are no more references to the object. A
		 * subclass overrides the finalize method to dispose of system resources
		 * or to perform other cleanup.
		 * <p>
		 * The finalize method is never invoked more than once by a Java virtual
		 * machine for any given object.
		 * </p>
		 * 
		 * @throws Throwable
		 *             the Exception raised by this method
		 * @see java.lang.Object#finalize()
		 */
		public void finalize() throws Throwable {
			this.data = null;
			super.finalize();
		}

	}

}
