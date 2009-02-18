/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 or
 *  version 3 as published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 14-Dec-2008
 */
package uk.me.parabola.splitter;

import java.io.BufferedWriter;
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

/**
 * Represents a tile, a subarea of the whole map.
 * 
 * @author Steve Ratcliffe
 */
public class SubArea {

	private final Area bounds;
	private int mapid;

	private Area extendedBounds;
	private BufferedWriter writer;

	private SplitIntMap coords;
	private int size;

	public SubArea(Area bounds, SplitIntMap coords) {
		this.bounds = bounds;
		this.coords = coords;
	}

	public SubArea(Area area) {
		this.bounds = area;
		coords = new SplitIntMap();
	}

	public void clear() {
		if (coords != null)
			size = coords.size();
		coords = null;
	}

	public Area getBounds() {
		return bounds;
	}

	public SplitIntMap getCoords() {
		return coords;
	}

	public int getMapid() {
		return mapid;
	}

	public void put(int key, int co) {
		coords.put(key, co);
	}

	public int getSize() {
		if (coords != null)
			return coords.size();
		else
			return size;
	}

	public void initForWrite(int extra) {
		extendedBounds = new Area(bounds.getMinLat() - extra,
				bounds.getMinLong() - extra,
				bounds.getMaxLat() + extra,
				bounds.getMaxLong() + extra);

		String filename = new Formatter().format(Locale.ROOT, "%08d.osm.gz", mapid).toString();
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			OutputStream zos = new GZIPOutputStream(fos);
			Writer w = new OutputStreamWriter(zos, "utf-8");
			writer = new BufferedWriter(w);
			writeHeader();
		} catch (IOException e) {
			System.out.println("Could not open or write file header");
		}
	}

	private void writeHeader() throws IOException {
		writer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
		writer.append("<osm version='0.5' generator='splitter'>\n");

		writer.append("<bounds minlat='");
		writer.append(String.valueOf(Utils.toDegrees(bounds.getMinLat())));
		writer.append("' minlon='");
		writer.append(String.valueOf(Utils.toDegrees(bounds.getMinLong())));
		writer.append("' maxlat='");
		writer.append(String.valueOf(Utils.toDegrees(bounds.getMaxLat())));
		writer.append("' maxlon='");
		writer.append(String.valueOf(Utils.toDegrees(bounds.getMaxLong())));
		writer.append("'/>\n");
	}

	public void finishWrite() {
		try {
			writer.append("</osm>\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Could not write end of file: " + e);
		}
	}

	public boolean write(StringNode node) throws IOException {
		if (extendedBounds.contains(node.getLocation())) {
			writer.append("<node id='");
			writer.append(node.getStringId());
			writer.append("' lat='");
			writer.append(node.getStringLat());
			writer.append("' lon='");
			writer.append(node.getStringLon());
			if (node.hasTags()) {
				writer.append("'>\n");
				writeTags(node);
				writer.append("</node>\n");
			} else {
				writer.append("'/>\n");
			}
			return true;
		}
		return false;
	}

	public void write(StringWay way) throws IOException {
		writer.append("<way id='");
		writer.append(way.getStringId());
		writer.append("'>\n");
		List<String> refs = way.getRefs();
		for (String ref : refs) {
			writer.append("<nd ref='");
			writer.append(ref);
			writer.append("'/>\n");
		}

		if (way.hasTags())
			writeTags(way);
		writer.append("</way>\n");
	}

	public void write(StringRelation rel) throws IOException {
		writer.append("<relation id='");
		writer.append(rel.getStringId());
		writer.append("'>\n");
		List<StringRelation.Member> memlist = rel.getMembers();
		for (StringRelation.Member m : memlist) {
			writer.append("<member type='");
			writer.append(m.getType());
			writer.append("' ref='");
			writer.append(m.getRef());
			writer.append("' role='");
			writer.append(m.getRole());
			writer.append("'/>\n");
		}
		if (rel.hasTags())
			writeTags(rel);
		writer.append("</relation>\n");
	}

	private void writeTags(Element element) throws IOException {
		Iterator<Map.Entry<String,String>> it = element.tagsIterator();
		while (it.hasNext()) {
			Map.Entry<String,String> entry = it.next();
			writer.append("<tag k='");
			writeAttribute(entry.getKey());
			writer.append("' v='");
			writeAttribute(entry.getValue());
			writer.append("'/>\n");
		}
	}

	private void writeAttribute(String value) throws IOException {
		for (char c : value.toCharArray()) {
			if (c == '\'')
				writer.append("&apos;");
			else if (c == '&') {
				writer.append("&amp;");
			} else if (c == '<') {
				writer.append("&lt;");
			} else
				writer.append(c);
		}
	}

	void setMapid(int mapid) {
		this.mapid = mapid;
	}
}
