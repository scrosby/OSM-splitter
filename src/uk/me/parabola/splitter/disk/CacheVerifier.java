/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package uk.me.parabola.splitter.disk;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maintains a persistent list of the .osm file(s) used to build the cache. Also verifies this
 * list against a list of files that are to be processed, so a decision can be made about
 * whether to flush the cache or not.
 *
 * @author Chris Miller
 */
public class CacheVerifier {
	private final String cacheDirectory;	// the cache location
	private final File entriesFile;			 // the file containing a list of the cache entries
	private final List<String> filenames; // the .osm files we expect to have been cached

	public CacheVerifier(String cacheDirectory, List<String> filenames) {
		this.cacheDirectory = cacheDirectory;
		entriesFile = new File(cacheDirectory, "cache.entries");
		this.filenames = filenames;
	}

	/**
	 * Checks to see if the cache already contains suitable data. It does this by comparing
	 * filenames and timestamps from the cache.entries file against the files that are
	 * being processed by this run of the splitter.
	 *
	 * @param filenames the files to process.
	 * @return {@code true} if there is an existing cache and it is valid for this run.
	 */
	public boolean validateCache() throws IOException {
		List<CacheEntry> existingEntries = loadEntries();

		// If no cache.entries file was found, we have to regenerate the cache
		if (existingEntries == null)
			return false;

		// See if there are any cache files
		boolean nodesExist = new File(cacheDirectory, "nodes.bin").exists();

		// If no filenames were provided we use the cache (if it exists)
		if (filenames.isEmpty())
			return nodesExist && !existingEntries.isEmpty();

		// Compare the existing cache entries with the .osm filenames provided. If they match we can use the cache
		List<CacheEntry> entries = new ArrayList<CacheEntry>(filenames.size());
		for (String filename : filenames) {
			entries.add(new CacheEntry(filename));
		}
		Collections.sort(existingEntries);
		Collections.sort(entries);

		return existingEntries.equals(entries) && nodesExist;
	}

	public void saveEntries() throws IOException {
		Writer out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(entriesFile), 4096), "UTF-8");
		try {
			for (String filename : filenames) {
				File file = new File(filename);
				if (file.exists()) {
					long size = file.length();
					long timestamp = file.lastModified();
					out.write(String.valueOf(size));
					out.write(',');
					out.write(String.valueOf(timestamp));
					out.write(',');
					out.write(file.getCanonicalPath());
					out.write('\n');
				}
			}
		} finally {
			out.close();
		}
	}

	protected List<CacheEntry> loadEntries() throws IOException {
		if (!entriesFile.exists()) {
			return null;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(entriesFile), "UTF-8"));
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		int count = 0;
		String line;
		try {
			while ((line = in.readLine()) != null) {
				count++;
				String[] parts = line.split(",", 3);
				if (parts.length < 3) {
					System.err.println("Invalid cache.entries file on line " + count + ". The cache will be rebuilt");
					return null;
				}
				long size = Long.valueOf(parts[0]);
				long timestamp = Long.valueOf(parts[1]);
				result.add(new CacheEntry(parts[2], size, timestamp));
			}
		} finally {
			in.close();
		}
		return result;
	}

	public boolean clearEntries() {
		return entriesFile.delete();
	}

	protected static class CacheEntry implements Comparable<CacheEntry> {
		private final String filename;
		private final long size;
		private final long timestamp;

		public CacheEntry(String filename, long size, long timestamp) {
			this.filename = filename;
			this.size = size;
			this.timestamp = timestamp;
		}

		protected CacheEntry(String filename) throws IOException {
			File file = new File(filename);
			this.filename = file.getCanonicalPath();
			size = file.length();
			timestamp = file.lastModified();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CacheEntry that = (CacheEntry) o;

			if (size != that.size) return false;
			if (timestamp != that.timestamp) return false;
			if (!filename.equals(that.filename)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = filename.hashCode();
			result = 31 * result + (int) (size ^ (size >>> 32));
			result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
			return result;
		}

		@Override
		public int compareTo(CacheEntry o) {
			int result = filename.compareTo(o.filename);
			if (result == 0) {
				result = size < o.size ? -1 : size > o.size ? 1 : 0;
			}
			if (result == 0) {
				result = timestamp < o.timestamp ? -1 : timestamp > o.timestamp ? 1 : 0;
			}
			return result;
		}
	}
}
