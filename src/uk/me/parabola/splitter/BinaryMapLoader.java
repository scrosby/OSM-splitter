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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import uk.me.parabola.splitter.disk.Member;
import uk.me.parabola.splitter.disk.NodeStoreReader;
import uk.me.parabola.splitter.disk.RelationStoreReader;
import uk.me.parabola.splitter.disk.WayStoreReader;

/**
 * Loads binary map files, calling the appropriate methods on a
 * {@code MapProcessor} as it progresses.
 */
class BinaryMapLoader implements MapReader {

	// How many elements to process before displaying a status update
	private static final int NODE_STATUS_UPDATE_THRESHOLD = 2500000;
	private static final int WAY_STATUS_UPDATE_THRESHOLD = 500000;
	private static final int RELATION_STATUS_UPDATE_THRESHOLD = 50000;

	private final String path;
	private final MapProcessor processor;

	private boolean startNodeOnly;

	private long nodeCount;
	private long wayCount;
	private long relationCount;
	private int minNodeId = Integer.MAX_VALUE;
	private int maxNodeId = Integer.MIN_VALUE;

	BinaryMapLoader(String path, MapProcessor processor) {
		this.path = path;
		this.processor = processor;
		this.startNodeOnly = processor.isStartNodeOnly();
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

	public void load() throws IOException {
		processNodes();
		if (!startNodeOnly) {
			processWays();
			processRelations();
		}
		processor.endMap();
	}

	private void processNodes() throws IOException {
		System.out.println("Loading and processing nodes");
		NodeStoreReader reader = new NodeStoreReader(path + File.separatorChar + "nodes.bin");
		while (reader.next()) {
			int id = reader.getId();
			processor.startNode(id, reader.getLat(), reader.getLon());
			if (!startNodeOnly) {
				for (Map.Entry<String, String> entry : reader.getTags().entrySet()) {
					processor.nodeTag(entry.getKey(), entry.getValue());
				}
				processor.endNode();
			}

			if (id < minNodeId) {
				minNodeId = id;
			}
			if (id > maxNodeId) {
				maxNodeId = id;
			}

			nodeCount++;
			if (nodeCount % NODE_STATUS_UPDATE_THRESHOLD == 0) {
				System.out.println(Utils.format(nodeCount) + " nodes processed...");
			}
		}
		reader.close();
	}

	private void processWays() throws IOException {
		System.out.println("Loading and processing ways");
		WayStoreReader reader = new WayStoreReader(path + File.separatorChar + "ways.bin");
		while (reader.next()) {
			processor.startWay(reader.getId());
			for (int nodeId : reader.getNodeIds()) {
				processor.wayNode(nodeId);
			}
			for (Map.Entry<String, String> entry : reader.getTags().entrySet()) {
				processor.wayTag(entry.getKey(), entry.getValue());
			}
			processor.endWay();
			wayCount++;
			if (wayCount % WAY_STATUS_UPDATE_THRESHOLD == 0) {
				System.out.println(Utils.format(wayCount) + " ways processed...");
			}
		}
		reader.close();
	}

	private void processRelations() throws IOException {
		System.out.println("Loading and processing relations");
		RelationStoreReader reader = new RelationStoreReader(path + File.separatorChar + "relations.bin");
		while (reader.next()) {
			processor.startRelation(reader.getId());
			for (Map.Entry<String, String> entry : reader.getTags().entrySet()) {
				processor.relationTag(entry.getKey(), entry.getValue());
			}
			for (Member member : reader.getMembers()) {
				switch (member.getType()) {
				case Node:
					processor.relationNode(member.getId(), member.getRole());
					break;
				case Way:
					processor.relationWay(member.getId(), member.getRole());
					break;
				}
			}
			processor.endRelation();
			relationCount++;
			if (relationCount % RELATION_STATUS_UPDATE_THRESHOLD == 0) {
				System.out.println(Utils.format(relationCount) + " relations processed...");
			}
		}
		reader.close();
	}
}