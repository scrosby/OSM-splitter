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