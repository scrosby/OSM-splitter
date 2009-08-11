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

	// How many elements to process before displaying a status update
	private static final int NODE_STATUS_UPDATE_THRESHOLD = 2500000;
	private static final int WAY_STATUS_UPDATE_THRESHOLD = 500000;
	private static final int RELATION_STATUS_UPDATE_THRESHOLD = 50000;

	private static final int MODE_NODE = 1;
	private static final int MODE_WAY = 2;
	private static final int MODE_RELATION = 3;

	private int mode;

	private final SplitIntMap coords = new SplitIntMap();
	private final SplitIntMap ways = new SplitIntMap();
	private final IntObjMap<long[]> bigWays = new IntObjMap<long[]>();

	private final SubArea[] areas;

	private StringNode currentNode;
	private int currentNodeAreaSet;
	private long nodeCount;

	private StringWay currentWay;
	private BitSet currentWayAreaSet;
	private long wayCount;

	private StringRelation currentRelation;
	private BitSet currentRelAreaSet;
	private long relationCount;

	SplitParser(SubArea[] areas) throws XmlPullParserException {
		this.areas = areas;
		currentWayAreaSet = new BitSet(areas.length);
		currentRelAreaSet = new BitSet(areas.length);
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
	public boolean startElement() {
		String name = getParser().getName();
		if (mode == 0) {
			if (name.equals("node")) {
				startNode();
			} else if (name.equals("way")) {
				startWay();
			} else if (name.equals("relation")) {
				startRelation();
			}
		} else if (mode == MODE_NODE) {
			processNode(name);
		} else if (mode == MODE_WAY) {
			processWay(name);
		} else if (mode == MODE_RELATION) {
			processRelation(name);
		}
		return false;
	}

	private void startNode() {
		mode = MODE_NODE;

		String id = getAttr("id");
		String slat = getAttr("lat");
		String slon = getAttr("lon");

		double lat = Double.parseDouble(slat);
		double lon = Double.parseDouble(slon);

		currentNode = new StringNode(Utils.toMapUnit(lat), Utils.toMapUnit(lon), id, slat, slon);
		currentNodeAreaSet = 0;
	}

	private void startWay() {
		mode = MODE_WAY;
		String id = getAttr("id");
		currentWay = new StringWay(id);
		currentWayAreaSet.clear();
	}

	private void startRelation() {
		mode = MODE_RELATION;
		String id = getAttr("id");
		currentRelation = new StringRelation(id);
		currentRelAreaSet.clear();
	}

	private void processNode(String name) {
		if (name.equals("tag")) {
			currentNode.addTag(getAttr("k"), getAttr("v"));
		}
	}

	private void processWay(String name) {
		if (name.equals("nd")) {
			String sid = getAttr("ref");

			// Get the list of areas that the node is in.  A node may be in
			// more than one area because of overlap.
			int set = coords.get(Integer.parseInt(sid));

			// add the list of areas to the currentWayAreaSet
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
					if (val == 0)
						break;
					currentWayAreaSet.set(val - 1);
				}
			}
			currentWay.addRef(sid);
		} else if (name.equals("tag")) {
			currentWay.addTag(getAttr("k"), getAttr("v"));
		}
	}

	private void processRelation(String name) {
		if (name.equals("tag")) {
			currentRelation.addTag(getAttr("k"), getAttr("v"));
		} else if (name.equals("member")) {
			String type = getAttr("type");
			String ref = getAttr("ref");
			currentRelation.addMember(type, ref, getAttr("role"));

			int iref = Integer.parseInt(ref);
			int set = 0;
			long[] bigSet;
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
					// val - 1 because the areas held in 'ways' are in the range 1-255
					currentRelAreaSet.set(val - 1);
				}
			} else if ((bigSet = bigWays.get(iref)) != null) {
				// Copy bits from bigSet to currentRelAreaSet
				for (int i = 0; i < bigSet.length; i++) {
					for (int j = 0; j < 64; j++) {
						if ((bigSet[i] & (1L << (j % 64))) != 0) {
							currentRelAreaSet.set(i * 64 + j);
						}
					}
				}
			}
		}
	}

	/**
	 * Receive notification of the end of an element.
	 */
	public void endElement() throws XmlPullParserException {
		String name = getParser().getName();
		if (mode == MODE_NODE) {
			if (name.equals("node")) {
				endNode();
			}
		} else if (mode == MODE_WAY) {
			if (name.equals("way")) {
				endWay();
			}
		} else if (mode == MODE_RELATION) {
			if (name.equals("relation")) {
				endRelation();
			}
		}
	}

	private void endNode() throws XmlPullParserException {
		mode = 0;
		nodeCount++;
		try {
			writeNode();
		} catch (IOException e) {
			throw new XmlPullParserException("failed to write node", getParser(), e);
		}
		if (nodeCount % NODE_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(nodeCount) + " nodes processed...");
		}
	}

	private void endWay() throws XmlPullParserException {
		mode = 0;
		wayCount++;
		try {
			writeWay();
		} catch (IOException e) {
			throw new XmlPullParserException("failed to write way", getParser(), e);
		}
		if (wayCount % WAY_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(wayCount) + " ways processed...");
		}
	}

	private void endRelation() throws XmlPullParserException {
		mode = 0;
		relationCount++;
		try {
			writeRelation();
		} catch (IOException e) {
			throw new XmlPullParserException("failed to write relation", getParser(), e);
		}
		if (relationCount % RELATION_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(relationCount) + " relations processed...");
		}
	}

	private boolean seenRel;

	private void writeRelation() throws IOException {
		if (!seenRel) {
			seenRel = true;
			System.out.println("Writing relations " + new Date());
		}
		for (int n = currentRelAreaSet.nextSetBit(0); n >= 0; n = currentRelAreaSet.nextSetBit(n + 1)) {
			// if n is out of bounds, then something has gone wrong
			areas[n].write(currentRelation);
		}
	}

	private boolean seenWay;

	private void writeWay() throws IOException {
		if (!seenWay) {
			seenWay = true;
			System.out.println("Writing ways " + new Date());
		}
		if (!currentWayAreaSet.isEmpty()) {
			if (currentWayAreaSet.cardinality() <= 4) {
				// this way falls into 4 or less areas (the normal case). Store these areas in the ways map
				int set = 0;
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					areas[n].write(currentWay);
					// add one to the area so we're in the range 1-255. This is because we treat 0 as the
					// equivalent of a null
					set = set << 8 | (n + 1);
				}
				ways.put(currentWay.getId(), set);
			} else {
				// this way falls into 5 or more areas. Convert the currentWayAreaSet into a long[] and store
				// these areas in the bigWays map
				long[] set = new long[currentWayAreaSet.size() / 64];
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					areas[n].write(currentWay);
					set[n / 64] |= 1L << (n % 64);
				}
				bigWays.put(currentWay.getId(), set);
			}
		}
	}

	private void writeNode() throws IOException {
		for (int n = 0; n < areas.length; n++) {
			boolean found = areas[n].write(currentNode);
			if (found) {
				if (currentNodeAreaSet == 0) {
					currentNodeAreaSet = n + 1;
				} else {
					currentNodeAreaSet = addToSet(currentNodeAreaSet, n + 1, currentNode.getStringId());
				}
			}
		}
		// Only remember the node if it's in one or more of the areas we care about
		if (currentNodeAreaSet != 0) {
			coords.put(currentNode.getId(), currentNodeAreaSet);
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
		System.err.println("Node " + desc + " in too many areas. Already in areas 0x" + Integer.toHexString(set) + ", trying to add area 0x" + Integer.toHexString(v));
		return set;
	}
}
