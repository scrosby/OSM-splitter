/**
 * Copyright 2009 Chris Miller
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