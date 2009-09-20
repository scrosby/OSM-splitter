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

import java.io.IOException;
import java.util.BitSet;
import java.util.Date;

/**
 * Splits a map into multiple areas.
 */
class SplitProcessor implements MapProcessor {

	private final SplitIntMap coords = new SplitIntMap();
	private final SplitIntMap ways = new SplitIntMap();
	private final IntObjMap<long[]> bigWays = new IntObjMap<long[]>();

	private final OSMWriter[] writers;

	private Node currentNode = new Node();
	private int currentNodeAreaSet;

	private Way currentWay = new Way();
	private BitSet currentWayAreaSet;

	private Relation currentRelation = new Relation();
	private BitSet currentRelAreaSet;

	SplitProcessor(OSMWriter[] writers) {
		this.writers = writers;
		currentWayAreaSet = new BitSet(writers.length);
		currentRelAreaSet = new BitSet(writers.length);
	}

	@Override
	public boolean isStartNodeOnly() {
		return false;
	}

	@Override
	public void startNode(int id, double lat, double lon) {
		currentNode.set(id, lat, lon);
	}

	@Override
	public void startWay(int id) {
		currentWay.set(id);
	}

	@Override
	public void startRelation(int id) {
		currentRelation.set(id);
	}

	@Override
	public void nodeTag(String key, String value) {
		currentNode.addTag(key, value);
	}

	@Override
	public void wayTag(String key, String value) {
		currentWay.addTag(key, value);
	}

	@Override
	public void relationTag(String key, String value) {
		currentRelation.addTag(key, value);
	}

	@Override
	public void wayNode(int id) {
		// Get the list of areas that the node is in.  A node may be in
		// more than one area because of overlap.
		int set = coords.get(id);

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
		currentWay.addRef(id);
	}

	@Override
	public void relationNode(int id, String role) {
		{
			currentRelation.addMember("node", id, role);
			int set = coords.get(id);
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
					if (val == 0)
						break;
					// val - 1 because the areas held in 'ways' are in the range 1-255
					currentRelAreaSet.set(val - 1);
				}
			}
		}
	}

	@Override
	public void relationWay(int id, String role) {
		{
			long[] bigSet;
			currentRelation.addMember("way", id, role);
			int set = ways.get(id);
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
					if (val == 0)
						break;
					// val - 1 because the areas held in 'ways' are in the range 1-255
					currentRelAreaSet.set(val - 1);
				}
			} else if ((bigSet = bigWays.get(id)) != null) {
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

	@Override
	public void endNode() {
		try {
			writeNode();
			currentNode.reset();
			currentNodeAreaSet = 0;
		} catch (IOException e) {
			throw new RuntimeException("failed to write node " + currentNode.getId(), e);
		}
	}

	@Override
	public void endWay() {
		try {
			writeWay();
			currentWay.reset();
			currentWayAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write way " + currentWay.getId(), e);
		}
	}

	@Override
	public void endRelation() {
		try {
			writeRelation();
			currentRelation.reset();
			currentRelAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write relation " + currentRelation.getId(), e);
		}
	}

	@Override
	public void endMap() {
		for (OSMWriter writer : writers) {
			writer.finishWrite();
		}
	}

	private void writeNode() throws IOException {
		for (int n = 0; n < writers.length; n++) {
			boolean found = writers[n].write(currentNode);
			if (found) {
				if (currentNodeAreaSet == 0) {
					currentNodeAreaSet = n + 1;
				} else {
					currentNodeAreaSet = addToSet(currentNodeAreaSet, n + 1, currentNode.getId());
				}
			}
		}
		// Only remember the node if it's in one or more of the areas we care about
		if (currentNodeAreaSet != 0) {
			coords.put(currentNode.getId(), currentNodeAreaSet);
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
					writers[n].write(currentWay);
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
					writers[n].write(currentWay);
					set[n / 64] |= 1L << (n % 64);
				}
				bigWays.put(currentWay.getId(), set);
			}
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
			writers[n].write(currentRelation);
		}
	}

	private int addToSet(int set, int v, int id) {
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
		System.err.println("Node " + id + " in too many areas. Already in areas 0x" + Integer.toHexString(set) + ", trying to add area 0x" + Integer.toHexString(v));
		return set;
	}
}