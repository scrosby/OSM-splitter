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

import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses an OSM file, calling the appropriate methods on a
 * {@code MapProcessor} as it progresses.
 */
class OSMParser extends AbstractXppParser implements MapReader {

	// How many elements to process before displaying a status update
	private static final int NODE_STATUS_UPDATE_THRESHOLD = 2500000;
	private static final int WAY_STATUS_UPDATE_THRESHOLD = 500000;
	private static final int RELATION_STATUS_UPDATE_THRESHOLD = 50000;

	private enum State {
		Node, Way, Relation, None
	}

	private final MapProcessor processor;

	// There are mixed nodes and ways in the file
	private boolean mixed;
	private boolean startNodeOnly;

	private State state = State.None;
	private long nodeCount;
	private long wayCount;
	private long relationCount;
	private int minNodeId = Integer.MAX_VALUE;
	private int maxNodeId = Integer.MIN_VALUE;

	OSMParser(MapProcessor processor) throws XmlPullParserException {
		this.processor = processor;
		this.startNodeOnly = processor.isStartNodeOnly();
	}

	public void setMixed(boolean mixed) {
		this.mixed = mixed;
	}

	@Override
	public long getNodeCount() {
		return nodeCount;
	}

	@Override
	public long getWayCount() {
		return wayCount;
	}

	@Override
	public long getRelationCount() {
		return relationCount;
	}

	@Override
	public int getMinNodeId() {
		return minNodeId;
	}

	@Override
	public int getMaxNodeId() {
		return maxNodeId;
	}

	public void endMap() {
		processor.endMap();
	}

	/**
	 * Receive notification of the start of an element.
	 */
	@Override
	public boolean startElement(String name) {
		switch (state) {
		case None:
			CharSequence action = getAttr("action");
			if (action != null && action.equals("delete"))
				return false;
			if (name.equals("node")) {
				startNode();
			} else if (name.equals("way")) {
				if (!startNodeOnly)
					startWay();
				else if (!mixed)
					return true;
			} else if (name.equals("relation")) {
				if (!startNodeOnly)
					startRelation();
			}
			break;
		case Node:
			if (!startNodeOnly)
				processNode(name);
			break;
		case Way:
			if (!startNodeOnly)
				processWay(name);
			break;
		case Relation:
			if (!startNodeOnly)
				processRelation(name);
			break;
		}
		return false;
	}

	private void startNode() {
		String idStr = getAttr("id");
		String latStr = getAttr("lat");
		String lonStr = getAttr("lon");

		if (idStr == null || latStr == null || lonStr == null) {
			// This should never happen - bad/corrupt .osm file?
			System.out.println("Node encountered with missing data. Bad/corrupt osm file? id=" + idStr + ", lat=" + latStr + ", lon=" + lonStr + ". Ignoring this node");
			return;
		}

		int id = Integer.parseInt(idStr);
		double lat = Convert.parseDouble(latStr);
		double lon = Convert.parseDouble(lonStr);

		if (id < minNodeId) {
			minNodeId = id;
		}
		if (id > maxNodeId) {
			maxNodeId = id;
		}

		processor.startNode(id, lat, lon);
		state = State.Node;
	}

	private void startWay() {
		processor.startWay(getIntAttr("id"));
		state = State.Way;
	}

	private void startRelation() {
		processor.startRelation(getIntAttr("id"));
		state = State.Relation;
	}

	private void processNode(CharSequence name) {
		if (name.equals("tag")) {
			processor.nodeTag(getAttr("k"), getAttr("v"));
		}
	}

	private void processWay(CharSequence name) {
		if (name.equals("nd")) {
			processor.wayNode(getIntAttr("ref"));
		} else if (name.equals("tag")) {
			processor.wayTag(getAttr("k"), getAttr("v"));
		}
	}

	private void processRelation(CharSequence name) {
		if (name.equals("tag")) {
			processor.relationTag(getAttr("k"), getAttr("v"));
		} else if (name.equals("member")) {
			String type = getAttr("type");
			int id = getIntAttr("ref");
			String role = getAttr("role");
			if ("node".equals(type)) {
				processor.relationNode(id, role);
			} else if ("way".equals(type)) {
				processor.relationWay(id, role);
			}
		}
	}

	/**
	 * Receive notification of the end of an element.
	 */
	@Override
	public void endElement(String name) {
		if (state == State.Node) {
			if (name.equals("node")) {
				if (!startNodeOnly)
					processor.endNode();
				state = State.None;
				nodeCount++;
				if (nodeCount % NODE_STATUS_UPDATE_THRESHOLD == 0) {
					System.out.println(Utils.format(nodeCount) + " nodes processed...");
				}
			}
		} else if (state == State.Way) {
			if (name.equals("way")) {
				if (!startNodeOnly)
					processor.endWay();
				state = State.None;
				wayCount++;
				if (wayCount % WAY_STATUS_UPDATE_THRESHOLD == 0) {
					System.out.println(Utils.format(wayCount) + " ways processed...");
				}
			}
		} else if (state == State.Relation) {
			if (name.equals("relation")) {
				if (!startNodeOnly)
					processor.endRelation();
				state = State.None;
				relationCount++;
				if (relationCount % RELATION_STATUS_UPDATE_THRESHOLD == 0) {
					System.out.println(Utils.format(relationCount) + " relations processed...");
				}
			}
		}
	}
}