/**
 * Copyright 2009 Chris Miller
 */
package uk.me.parabola.splitter.disk;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import uk.me.parabola.splitter.IntObjMap;

/**
 * Reads in a set of strings to binary format so they can be indexed by ID
 */
public class KeyLookupReader {
	private final IntObjMap<String> lookup = new IntObjMap<String>();

	public KeyLookupReader(String keyFilename) throws IOException {
		File keyFile = new File(keyFilename);
		LengthPrefixInputStream is = new LengthPrefixInputStream(new FileInputStream(keyFile), 16384);
		load(is);
	}

	public KeyLookupReader(InputStream in) throws IOException {
		LengthPrefixInputStream is = new LengthPrefixInputStream(in, 16384);
		load(is);
	}

	private void load(LengthPrefixInputStream is) throws IOException {
		try {
			int s = 0;
			while (is.next()) {
				try {
					lookup.put(++s, is.readUTF());
				} catch (EOFException e) {
					break;
				}
			}
		} finally {
			is.close();
		}
	}

	public String get(int i) {
		return lookup.get(i);
	}
}
