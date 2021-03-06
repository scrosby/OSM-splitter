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

package uk.me.parabola.splitter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class OSMWriter {
	private final Area bounds;
	private Writer writer;
	private Area extendedBounds;

	public OSMWriter(Area bounds) {
		this.bounds = bounds;
	}

	public void initForWrite(int mapId, int extra) {
		extendedBounds = new Area(bounds.getMinLat() - extra,
						bounds.getMinLong() - extra,
						bounds.getMaxLat() + extra,
						bounds.getMaxLong() + extra);

		String filename = new Formatter().format(Locale.ROOT, "%08d.osm.gz", mapId).toString();
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			OutputStream zos = new GZIPOutputStream(fos);
			writer = new OutputStreamWriter(zos, "utf-8");
			writeHeader();
		} catch (IOException e) {
			System.out.println("Could not open or write file header. Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void writeHeader() throws IOException {
		writeString("<?xml version='1.0' encoding='UTF-8'?>\n");
		writeString("<osm version='0.5' generator='splitter'>\n");

		writeString("<bounds minlat='");
		writeDouble(Utils.toDegrees(bounds.getMinLat()));
		writeString("' minlon='");
		writeDouble(Utils.toDegrees(bounds.getMinLong()));
		writeString("' maxlat='");
		writeDouble(Utils.toDegrees(bounds.getMaxLat()));
		writeString("' maxlon='");
		writeDouble(Utils.toDegrees(bounds.getMaxLong()));
		writeString("'/>\n");
	}

	public void finishWrite() {
		try {
			writeString("</osm>\n");
			flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Could not write end of file: " + e);
		}
	}

	public boolean nodeBelongsToThisArea(Node node) {
		return (extendedBounds.contains(node.getMapLat(), node.getMapLon()));
	}
	
	public void write(Node node) throws IOException {
		writeString("<node id='");
		writeInt(node.getId());
		writeString("' lat='");
		writeDouble(node.getLat());
		writeString("' lon='");
		writeDouble(node.getLon());
		if (node.hasTags()) {
			writeString("'>\n");
			writeTags(node);
			writeString("</node>\n");
		} else {
			writeString("'/>\n");
		}
	}

	public void write(Way way) throws IOException {
		writeString("<way id='");
		writeInt(way.getId());
		writeString("'>\n");
		IntList refs = way.getRefs();
		for (int i = 0; i < refs.size(); i++) {
			writeString("<nd ref='");
			writeInt(refs.get(i));
			writeString("'/>\n");
		}
		if (way.hasTags())
			writeTags(way);
		writeString("</way>\n");
	}

	public void write(Relation rel) throws IOException {
		writeString("<relation id='");
		writeInt(rel.getId());
		writeString("'>\n");
		List<Relation.Member> memlist = rel.getMembers();
		for (Relation.Member m : memlist) {
			if (m.getType() == null || m.getRef() == 0) {
				System.err.println("Invalid relation member found in relation " + rel.getId() + ": member type=" + m.getType() + ", ref=" + m.getRef() + ", role=" + m.getRole() + ". Ignoring this member");
				continue;
			}
			writeString("<member type='");
			writeAttribute(m.getType());
			writeString("' ref='");
			writeInt(m.getRef());
			writeString("' role='");
			if (m.getRole() != null) {
				writeAttribute(m.getRole());
			}
			writeString("'/>\n");
		}
		if (rel.hasTags())
			writeTags(rel);
		writeString("</relation>\n");
	}

	private void writeTags(Element element) throws IOException {
		Iterator<Map.Entry<String, String>> it = element.tagsIterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			writeString("<tag k='");
			writeAttribute(entry.getKey());
			writeString("' v='");
			writeAttribute(entry.getValue());
			writeString("'/>\n");
		}
	}

	private void writeAttribute(String value) throws IOException {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\'':
					writeString("&apos;");
					break;
				case '&':
					writeString("&amp;");
					break;
				case '<':
					writeString("&lt;");
					break;
				default:
					writeChar(c);
			}
		}
	}

	private int index;
	private final char[] charBuf = new char[4096];

	private void checkFlush(int i) throws IOException {
		if (charBuf.length - index < i) {
			flush();
		}
	}

	private void flush() throws IOException {
		writer.write(charBuf, 0, index);
		index = 0;
	}

	private void writeString(String value) throws IOException {
		int start = 0;
		int end = value.length();
		int len;
		while ((len = charBuf.length - index) < end - start) {
			value.getChars(start, start + len, charBuf, index);
			start += len;
			index = charBuf.length;
			flush();
		}
		value.getChars(start, end, charBuf, index);
		index += end - start;
	}

	private void writeDouble(double value) throws IOException {
		checkFlush(22);
		writeString(Double.toString(value));
	}

	private void writeInt(int value) throws IOException {
		checkFlush(11);
		index += Convert.intToString(value, charBuf, index);
	}

	private void writeChar(char value) throws IOException {
		checkFlush(1);
		charBuf[index++] = value;
	}
}
