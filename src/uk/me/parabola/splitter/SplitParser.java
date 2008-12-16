/*
 * Copyright (C) 2006 Steve Ratcliffe
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
 * Create date: 16-Dec-2006
 */
package uk.me.parabola.splitter;

import java.io.IOException;
import java.util.List;

import uk.me.parabola.imgfmt.app.Coord;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for the second pass where we divide up the input file into the
 * individual files.
 *
 * @author Steve Ratcliffe
 */
class SplitParser extends DefaultHandler {
	private int mode;

	private static final int MODE_NODE = 1;
	private static final int MODE_WAY = 2;
//	private static final int MODE_RELATION = 3;

	private Int2ReferenceOpenHashMap<ReferenceArraySet<SubArea>> coords = new Int2ReferenceOpenHashMap<ReferenceArraySet<SubArea>>(500000, 0.8f);
	private Int2ReferenceOpenHashMap<ReferenceArraySet> ways = new Int2ReferenceOpenHashMap<ReferenceArraySet>(500000, 0.8f);

	private List<SubArea> areas;

	private StringNode currentNode;
	private ReferenceArraySet<SubArea> currentNodeAreaSet;

	private StringWay currentWay;
	private ReferenceArraySet<SubArea> currentWayAreaSet;

	SplitParser(List<SubArea> areas) {
		this.areas = areas;
		coords.growthFactor(8);
		ways.growthFactor(8);
	}

	/**
	 * Receive notification of the start of an element.
	 *
	 * @param uri The Namespace URI, or the empty string if the
	 * element has no Namespace URI or if Namespace
	 * processing is not being performed.
	 * @param localName The local name (without prefix), or the
	 * empty string if Namespace processing is not being
	 * performed.
	 * @param qName The qualified name (with prefix), or the
	 * empty string if qualified names are not available.
	 * @param attributes The attributes attached to the element.  If
	 * there are no attributes, it shall be an empty
	 * Attributes object.
	 * @throws SAXException Any SAX exception, possibly
	 * wrapping another exception.
	 * @see ContentHandler#startElement
	 */
	public void startElement(String uri, String localName,
			String qName, Attributes attributes)
			throws SAXException
	{

		if (mode == 0) {
			if (qName.equals("node")) {
				mode = MODE_NODE;

				String id = attributes.getValue("id");
				String slat = attributes.getValue("lat");
				String slon = attributes.getValue("lon");

				double lat = Double.parseDouble(slat);
				double lon = Double.parseDouble(slon);
				Coord coord = new Coord(lat, lon);

				currentNode = new StringNode(coord, id, slat, slon);
				currentNodeAreaSet = new ReferenceArraySet<SubArea>(6);
				coords.put(Integer.parseInt(id), currentNodeAreaSet);

			} else if (qName.equals("way")) {
				mode = MODE_WAY;
				String id = attributes.getValue("id");
				currentWay = new StringWay(id);
				currentWayAreaSet = new ReferenceArraySet<SubArea>(6);
				ways.put(Integer.parseInt(id), currentWayAreaSet);
			}
		} else if (mode == MODE_NODE) {
			if (qName.equals("tag")) {
				currentNode.addTag(attributes.getValue("k"), attributes.getValue("v"));
			}
		} else if (mode == MODE_WAY) {
			if (qName.equals("nd")) {
				String sid = attributes.getValue("ref");
				ReferenceArraySet<SubArea> set = coords.get(Integer.parseInt(sid));
				for (SubArea a : set)
					currentWayAreaSet.add(a);
				currentWay.addRef(sid);
			} else if (qName.equals("tag")) {
				currentWay.addTag(attributes.getValue("k"),
						attributes.getValue("v"));
			}
		}
	}

	/**
	 * Receive notification of the end of an element.
	 *
	 * @param uri The Namespace URI, or the empty string if the
	 * element has no Namespace URI or if Namespace
	 * processing is not being performed.
	 * @param localName The local name (without prefix), or the
	 * empty string if Namespace processing is not being
	 * performed.
	 * @param qName The qualified name (with prefix), or the
	 * empty string if qualified names are not available.
	 * @throws SAXException Any SAX exception, possibly
	 * wrapping another exception.
	 * @see ContentHandler#endElement
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException
	{
		if (mode == MODE_NODE) {
			if (qName.equals("node")) {
				mode = 0;
				try {
					writeNode(currentNode);
				} catch (IOException e) {
					throw new SAXException("failed to write", e);
				}
			}
		} else if (mode == MODE_WAY) {
			if (qName.equals("way")) {
				mode = 0;
				try {
					writeWay(currentWay);
				} catch (IOException e) {
					throw new SAXException("failed to write way", e);
				}
			}
		}
	}

	private void writeWay(StringWay way) throws IOException {
		for (SubArea a : currentWayAreaSet) {
			a.write(way);
		}
	}

	private void writeNode(StringNode node) throws IOException {
		for (SubArea a : areas) {
			boolean found = a.write(node);
			if (found)
				currentNodeAreaSet.add(a);
		}
	}


	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println("Error at line " + e.getLineNumber() + ", col "
				+ e.getColumnNumber());
		super.fatalError(e);
	}
}