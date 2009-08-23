/**
 * Copyright 2009 Chris Miller
 */
package uk.me.parabola.splitter.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads in ways from a binary file format
 */
public class WayStoreReader extends AbstractStoreReader {
	private int[] buf = new int[256];
	private int[] nodeIds;

	public WayStoreReader(String filename) throws IOException {
		this(new FileInputStream(new File(filename)), new KeyLookupReader(filename + ".keys"));
	}

	public WayStoreReader(InputStream in, KeyLookupReader keys) throws IOException {
		super(in, keys);
	}

	public int[] getNodeIds() {
		return nodeIds;
	}

	@Override
	protected void readHeader() throws IOException {
		int i = 0;
		int id;
		while ((id = getIn().readInt()) != 0) {
			if (buf.length <= i) {
				int[] temp = new int[(buf.length * 3) / 2 + 1];
				System.arraycopy(buf, 0, temp, 0, buf.length);
				buf = temp;
			}
			buf[i++] = id;
		}
		nodeIds = new int[i];
		System.arraycopy(buf, 0, nodeIds, 0, i);
	}
}