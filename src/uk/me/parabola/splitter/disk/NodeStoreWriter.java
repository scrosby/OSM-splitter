/**
 * Copyright 2009 Chris Miller
 */
package uk.me.parabola.splitter.disk;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Persists a collection of nodes to binary format on disk for later retrieval
 */
public class NodeStoreWriter extends AbstractStoreWriter {

	public NodeStoreWriter(String filename) throws IOException {
		this(new FileOutputStream(filename), new KeyLookupWriter(filename + ".keys"));
	}

	public NodeStoreWriter(OutputStream out, KeyLookupWriter keys) {
		super(out, keys);
	}

	public void write(int nodeId, double lat, double lon) throws IOException {
		getOut().writeInt(nodeId);
		getOut().writeDouble(lat);
		getOut().writeDouble(lon);
	}
}