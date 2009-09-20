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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads in nodes from a binary format
 */
public class NodeStoreReader extends AbstractStoreReader {
	private double lat;
	private double lon;

	public NodeStoreReader(String filename) throws IOException {
		this(new FileInputStream(new File(filename)), new KeyLookupReader(filename + ".keys"));
	}

	public NodeStoreReader(InputStream in, KeyLookupReader keys) throws IOException {
		super(in, keys);
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	@Override
	protected void readHeader() throws IOException {
		lat = getIn().readDouble();
		lon = getIn().readDouble();
	}
}