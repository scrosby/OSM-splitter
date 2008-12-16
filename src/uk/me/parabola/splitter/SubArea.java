/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import uk.me.parabola.imgfmt.Utils;
import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.Coord;
import uk.me.parabola.log.Logger;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author Steve Ratcliffe
 */
public class SubArea {
	private static final Logger log = Logger.getLogger(SubArea.class);

	private final Area bounds;

	private Area extendedBounds;
	private BufferedWriter writer;

	private Int2ReferenceOpenHashMap<Coord> coords;
	private int size;

	public SubArea(Area bounds, Int2ReferenceOpenHashMap<Coord> coords) {
		this.bounds = bounds;
		this.coords = coords;
	}

	public SubArea(Area area, int sizehint) {
		this.bounds = area;
		coords = new Int2ReferenceOpenHashMap<Coord>(sizehint, 0.8f);
		coords.growthFactor(8);
	}

	public void clear() {
		if (coords != null)
			size = coords.size();
		coords = null;
	}

	public Area getBounds() {
		return bounds;
	}

	public Int2ReferenceOpenHashMap<Coord> getCoords() {
		return coords;
	}

	public void put(int key, Coord co) {
		coords.put(key, co);
	}

	public int getSize() {
		if (coords != null)
			return coords.size();
		else
			return size;
	}

	public void initForWrite(int mapid, int extra) {
		extendedBounds = new Area(bounds.getMinLat() - extra,
				bounds.getMinLong() - extra,
				bounds.getMaxLat() + extra,
				bounds.getMaxLong() + extra);

		String filename = new Formatter().format("%08d.osm.gz", mapid).toString();
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			OutputStream zos = new GZIPOutputStream(fos);
			Writer w = new OutputStreamWriter(zos);
			writer = new BufferedWriter(w);
			writeHeader();
		} catch (IOException e) {
			System.out.println("Could not open, write file header");
			log.warn("Could not open file, or write header", filename);
		}
	}

	private void writeHeader() throws IOException {
		writer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
		writer.append("<osm version='0.5' generator='splitter'>\n");

		Formatter fmt = new Formatter(writer);
		fmt.format("<bounds minlat='%f' minlon='%f' maxlat='%f' maxlon='%f'/>\n",
				Utils.toDegrees(bounds.getMinLat()),
				Utils.toDegrees(bounds.getMinLong()),
				Utils.toDegrees(bounds.getMaxLat()),
				Utils.toDegrees(bounds.getMaxLong()));
		fmt.flush();
	}

	public void finishWrite() {
		try {
			writer.append("</osm>\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Could not write end of file: " + e);
			log.warn("Could not write end of file");
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

	private void writeTags(Element element) throws IOException {
		ObjectIterator<Object2ObjectMap.Entry<String,String>> it = element.tagsIterator();
		while (it.hasNext()) {
			Object2ObjectMap.Entry<String,String> entry = it.next();
			writer.append("<tag k='");
			writer.append(entry.getKey());
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
}
