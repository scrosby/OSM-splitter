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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import uk.me.parabola.imgfmt.app.Coord;

import java.io.IOException;

/**
 * Parser for the second pass where we divide up the input file into the
 * individual files.
 *
 * @author Steve Ratcliffe
 */
class SplitParser extends DefaultHandler {
	private static final int MODE_NODE = 1;
	private static final int MODE_WAY = 2;
	private static final int MODE_RELATION = 3;

	private int mode;

	private SplitIntMap coords = new SplitIntMap();
	private SplitIntMap ways = new SplitIntMap();

	private SubArea[] areas;

	private StringNode currentNode;
	private int currentNodeAreaSet;

	private StringWay currentWay;
	private int currentWayAreaSet;

	private StringRelation currentRelation;
	private int currentRelAreaSet;

	SplitParser(SubArea[] areas) {
		this.areas = areas;
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
				currentNodeAreaSet = 0;

			} else if (qName.equals("way")) {
				mode = MODE_WAY;
				String id = attributes.getValue("id");
				currentWay = new StringWay(id);
				currentWayAreaSet = 0;
				
			} else if (qName.equals("relation")) {
				mode = MODE_RELATION;
				String id = attributes.getValue("id");
				currentRelation = new StringRelation(id);
				currentRelAreaSet = 0;
			}
		} else if (mode == MODE_NODE) {
			if (qName.equals("tag")) {
				currentNode.addTag(attributes.getValue("k"), attributes.getValue("v"));
			}
		} else if (mode == MODE_WAY) {
			if (qName.equals("nd")) {
				String sid = attributes.getValue("ref");

				// Get the list of areas that the node is in.  A node may be in
				// more than one area because of overlap.
				int set = coords.get(Integer.parseInt(sid));

				// add the list of areas to the currentWayAreaSet
				if (currentWayAreaSet == set) {
					// nothing to do, this will be the most common case
				} else if (currentWayAreaSet == 0) {
					currentWayAreaSet = set;
				} else {
					int mask = 0xff;
					for (int slot = 0; slot < 4; slot++, mask <<= 8) {
						int val = (set & mask) >>> (slot * 8);
						if (val == 0)
							break;
						// Now find it in the destination set or add it
						currentWayAreaSet = addToSet(currentWayAreaSet, val, "way" + currentWay.getStringId());
					}
				}

				currentWay.addRef(sid);
			} else if (qName.equals("tag")) {
				currentWay.addTag(attributes.getValue("k"),
						attributes.getValue("v"));
			}
		} else if (mode == MODE_RELATION) {
			if (qName.equals("tag")) {
				currentRelation.addTag(attributes.getValue("k"), attributes.getValue("v"));
			} else if (qName.equals("member")) {
				String type = attributes.getValue("type");
				String ref = attributes.getValue("ref");
				currentRelation.addMember(type, ref, attributes.getValue("role"));

				int iref = Integer.parseInt(ref);
				int set = 0;
				if ("node".equals(type)) {
					set = coords.get(iref);
				} else if ("way".equals(type)) {
					set = ways.get(iref);
				}
				if (currentRelAreaSet == set) {
					// nothing to do
				} else if (currentRelAreaSet == 0) {
					currentRelAreaSet = set;
				} else {
					int mask = 0xff;
					for (int slot = 0; slot < 4; slot++, mask <<= 8) {
						int val = (set & mask) >>> (slot * 8);
						if (val == 0)
							break;
						// Now find it in the destination set or add it
						currentRelAreaSet = addToSet(currentRelAreaSet, val, "relation" + currentRelation.getId());
					}
				}
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
		} else if (mode == MODE_RELATION) {
			if (qName.equals("relation")) {
				mode = 0;
				try {
					writeRelation(currentRelation);
				} catch (IOException e) {
					throw new SAXException("failed to write relation", e);
				}
			}
		}
	}

	private int addToSet(int set, int v, String desc) {
		int val = v;
		for (int mask = 0xff; mask != 0; mask <<= 8) {
			int setval = set & mask;
			if (setval == 0) {
				return set | val;
			} else if (setval == val) {
				return set;
			}
			val <<= 8;
		}
		// it was not added
		System.err.println("Crosses too many areas " + desc);
		return set;
	}

	private void writeRelation(StringRelation relation) throws IOException {
		for (int slot = 0; slot < 4; slot++) {
			int n = (currentRelAreaSet >> (slot*8)) & 0xff;
			if (n == 0)
				break;

			// if n is out of bounds, then something has gone wrong
			areas[n-1].write(relation);
		}
	}

	private void writeWay(StringWay way) throws IOException {
		for (int slot = 0; slot < 4; slot++) {
			int n = (currentWayAreaSet >> (slot*8)) & 0xff;
			if (n == 0)
				break;

			// if n is out of bounds, then something has gone wrong
			areas[n-1].write(way);
		}
	}

	private void writeNode(StringNode node) throws IOException {
		for (int n = 1; n <= areas.length; n++) {
			SubArea a = areas[n-1];
			boolean found = a.write(node);
			if (found) {
				if (currentNodeAreaSet == n) {
					// Do nothing, already in, the normal case
				} else if (currentNodeAreaSet == 0) {
					currentNodeAreaSet = n;
				} else {
					currentNodeAreaSet = addToSet(currentNodeAreaSet, n,
							"node" + node.getStringId());
				}
			}
		}
		coords.put(node.getId(), currentNodeAreaSet);
	}


	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println("Error at line " + e.getLineNumber() + ", col "
				+ e.getColumnNumber());
		super.fatalError(e);
	}
}