/*
 * Copyright (C) 2006 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  as published by the Free Software Foundation.
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
import java.util.BitSet;
import java.util.Date;

import org.xmlpull.v1.XmlPullParserException;

/**
 * Parser for the second pass where we divide up the input file into the
 * individual files.
 *
 * @author Steve Ratcliffe
 */
class SplitParser extends AbstractXppParser {
	private static final int MODE_NODE = 1;
	private static final int MODE_WAY = 2;
	private static final int MODE_RELATION = 3;

	private int mode;

	private final SplitIntMap coords = new SplitIntMap();
	private final SplitIntMap ways = new SplitIntMap();

	private final SubArea[] areas;

	private StringNode currentNode;
	private int currentNodeAreaSet;
	private long nodeCount;

	private StringWay currentWay;
	private int currentWayAreaSet;
	private long wayCount;

	private StringRelation currentRelation;
	private BitSet currentRelAreaSet = new BitSet(255);
	private long relationCount;

	SplitParser(SubArea[] areas) throws XmlPullParserException {
		this.areas = areas;
	}

	public long getNodeCount() {
		return nodeCount;
	}

	public long getWayCount() {
		return wayCount;
	}

	public long getRelationCount() {
		return relationCount;
	}

	/**
	 * Receive notification of the start of an element.
	 */
	public boolean startElement()
	{
		String name = getParser().getName();
		if (mode == 0) {
			if (name.equals("node")) {
				mode = MODE_NODE;

				String id = getAttr("id");
				String slat = getAttr("lat");
				String slon = getAttr("lon");

				double lat = Double.parseDouble(slat);
				double lon = Double.parseDouble(slon);

				currentNode = new StringNode(Utils.toMapUnit(lat), Utils.toMapUnit(lon), id, slat, slon);
				currentNodeAreaSet = 0;

			} else if (name.equals("way")) {
				mode = MODE_WAY;
				String id = getAttr("id");
				currentWay = new StringWay(id);
				currentWayAreaSet = 0;
				
			} else if (name.equals("relation")) {
				mode = MODE_RELATION;
				String id = getAttr("id");
				currentRelation = new StringRelation(id);
				currentRelAreaSet.clear();
			}
		} else if (mode == MODE_NODE) {
			if (name.equals("tag")) {
				currentNode.addTag(getAttr("k"), getAttr("v"));
			}
		} else if (mode == MODE_WAY) {
			if (name.equals("nd")) {
				String sid = getAttr("ref");

				// Get the list of areas that the node is in.  A node may be in
				// more than one area because of overlap.
				int set = coords.get(Integer.parseInt(sid));

				// add the list of areas to the currentWayAreaSet
				if (currentWayAreaSet == set || set == 0) {
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
						currentWayAreaSet = addToSet(currentWayAreaSet, val, "Way " + currentWay.getStringId());
					}
				}

				currentWay.addRef(sid);
			} else if (name.equals("tag")) {
				currentWay.addTag(getAttr("k"), getAttr("v"));
			}
		} else if (mode == MODE_RELATION) {
			if (name.equals("tag")) {
				currentRelation.addTag(getAttr("k"), getAttr("v"));
			} else if (name.equals("member")) {
				String type = getAttr("type");
				String ref = getAttr("ref");
				currentRelation.addMember(type, ref, getAttr("role"));

				int iref = Integer.parseInt(ref);
				int set = 0;
				if ("node".equals(type)) {
					set = coords.get(iref);
				} else if ("way".equals(type)) {
					set = ways.get(iref);
				}
				if (set != 0) {
					int mask = 0xff;
					for (int slot = 0; slot < 4; slot++, mask <<= 8) {
						int val = (set & mask) >>> (slot * 8);
						if (val == 0)
							break;
						currentRelAreaSet.set(val - 1);
					}
				}
			}
		}
		return false;
	}

	/**
	 * Receive notification of the end of an element.
	 */
	public void endElement() throws XmlPullParserException
	{
		String name = getParser().getName();
		if (mode == MODE_NODE) {
			if (name.equals("node")) {
				mode = 0;
				nodeCount++;
				try {
					writeNode(currentNode);
				} catch (IOException e) {
					throw new XmlPullParserException("failed to write node", getParser(), e);
				}
			}
		} else if (mode == MODE_WAY) {
			if (name.equals("way")) {
				mode = 0;
				wayCount++;
				try {
					writeWay(currentWay);
				} catch (IOException e) {
					throw new XmlPullParserException("failed to write way", getParser(), e);
				}
			}
		} else if (mode == MODE_RELATION) {
			if (name.equals("relation")) {
				mode = 0;
				relationCount++;
				try {
					writeRelation(currentRelation);
				} catch (IOException e) {
					throw new XmlPullParserException("failed to write relation", getParser(), e);
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
		System.err.println(desc + " in too many areas.");
		return set;
	}

	private boolean seenRel;
	private void writeRelation(StringRelation relation) throws IOException {
		if (!seenRel) {
			seenRel = true;
			System.out.println("Writing relations " + new Date());
		}
		for (int n = currentRelAreaSet.nextSetBit(0); n >= 0; n = currentRelAreaSet.nextSetBit(n + 1)) {
			// if n is out of bounds, then something has gone wrong
			areas[n].write(relation);
		}
	}

	private boolean seenWay;
	private void writeWay(StringWay way) throws IOException {
		if (!seenWay) {
			seenWay = true;
			System.out.println("Writing ways " + new Date());
		}
		for (int slot = 0; slot < 4; slot++) {
			int n = (currentWayAreaSet >> (slot * 8)) & 0xff;
			if (n == 0)
				break;

			// if n is out of bounds, then something has gone wrong
			areas[n - 1].write(way);
		}
		// Only remember the way if it's in one or more of the areas we care about
		if (currentWayAreaSet != 0) {
			ways.put(way.getId(), currentWayAreaSet);
		}
	}

	private void writeNode(StringNode node) throws IOException {
		for (int n = 1; n <= areas.length; n++) {
			SubArea a = areas[n-1];
			boolean found = a.write(node);
			if (found) {
				if (currentNodeAreaSet == n) {
					System.out.println("Didn't think this could happen?! " + node.getStringId() + " " + n);
				} else if (currentNodeAreaSet == 0) {
					currentNodeAreaSet = n;
				} else {
					currentNodeAreaSet = addToSet(currentNodeAreaSet, n,
							"Node " + node.getStringId());
				}
			}
		}
		// Only remember the node if it's in one or more of the areas we care about
		if (currentNodeAreaSet != 0) {
			coords.put(node.getId(), currentNodeAreaSet);
		}
	}
}
