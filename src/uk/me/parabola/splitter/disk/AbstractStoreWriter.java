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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides base functionality for writing elements out to disk in binary format
 */
public abstract class AbstractStoreWriter {

	private final LengthPrefixOutputStream out;
	private final KeyLookupWriter keys;

	public AbstractStoreWriter(OutputStream out, KeyLookupWriter keys) {
		this.out = new LengthPrefixOutputStream(out);
		this.keys = keys;
	}

	protected LengthPrefixOutputStream getOut() {
		return out;
	}

	public void writeTag(CharSequence key, CharSequence value) throws IOException {
		out.writeShort(keys.set(key));
		out.writeUTF(value);
	}

	public void closeTags() throws IOException {
		out.writeShort(0);
	}

	/**
	 * Inserts the length header and writes the element through to
	 * the underlying output stream.
	 *
	 * @throws IOException
	 */
	public void next() throws IOException {
		out.next();
	}

	public void close() throws IOException {
		out.close();
		keys.close();
	}
}
