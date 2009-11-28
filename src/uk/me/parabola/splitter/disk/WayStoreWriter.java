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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes out a sequence of ways to binary format for later retrieval
 */
public class WayStoreWriter extends AbstractStoreWriter {

	public WayStoreWriter(String filename) throws IOException {
		this(new FileOutputStream(filename), new KeyLookupWriter(filename + ".keys"));
	}

	public WayStoreWriter(OutputStream out, KeyLookupWriter keys) {
		super(out, keys);
	}

	public void write(int id) throws IOException {
		getOut().writeInt(id);
	}

	public void writeNodeRef(int id) throws IOException {
		getOut().writeInt(id);
	}

	public void closeNodeRefs() throws IOException {
		getOut().writeInt(0);
	}
}