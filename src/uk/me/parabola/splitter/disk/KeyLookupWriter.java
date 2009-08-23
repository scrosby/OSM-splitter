/**
 * Copyright 2009 Chris Miller
 */
package uk.me.parabola.splitter.disk;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes out a set of strings to binary format so they can be indexed by ID
 */
public class KeyLookupWriter {
	private final LengthPrefixOutputStream out;
	private final Map<CharSequence, Short> lookup = new HashMap<CharSequence, Short>(20);

	public KeyLookupWriter(String keyFilename) throws IOException {
		out = new LengthPrefixOutputStream(new FileOutputStream(keyFilename), 16384);
	}

	public KeyLookupWriter(OutputStream out) throws IOException {
		this.out = new LengthPrefixOutputStream(out, 16384);
	}

	public short set(CharSequence key) throws IOException {
		Short result = lookup.get(key);
		if (result == null) {
			result = Short.valueOf((short) (lookup.size() + 1));
			lookup.put(key, result);
			out.writeUTF(key);
			out.next();
		}
		return result;
	}

	public void close() throws IOException {
		out.close();
	}
}