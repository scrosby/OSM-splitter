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

import uk.me.parabola.splitter.disk.CacheVerifier;
import uk.me.parabola.splitter.disk.MemberType;
import uk.me.parabola.splitter.disk.NodeStoreWriter;
import uk.me.parabola.splitter.disk.RelationStoreWriter;
import uk.me.parabola.splitter.disk.WayStoreWriter;

/**
 * Writes the map out in a binary format to a disk cache and then
 * delegates calls on to another {@link MapProcessor}.
 *
 * @author Chris Miller
 */
public class CachingMapProcessor implements MapProcessor {

	private NodeStoreWriter nodeWriter;
	private WayStoreWriter wayWriter;
	private RelationStoreWriter relationWriter;

	private MapProcessor delegate;
	CacheVerifier verifier;
	private int currentNode;
	private int currentWay;
	private int currentRel;
	private boolean startedWayTags;
	private boolean startedRelTags;

	public CachingMapProcessor(String outputDir, CacheVerifier verifier, MapProcessor delegate) throws IOException {
		this.delegate = delegate;
		this.verifier = verifier;
		verifier.clearEntries();
		nodeWriter = new NodeStoreWriter(outputDir + File.separatorChar + "nodes.bin");
		wayWriter = new WayStoreWriter(outputDir + File.separatorChar + "ways.bin");
		relationWriter = new RelationStoreWriter(outputDir + File.separatorChar + "relations.bin");
	}

	@Override
	public boolean isStartNodeOnly() {
		return false;
	}

	@Override
	public void boundTag(Area bounds) {
		// todo: write the bounds out to the cache
		delegate.boundTag(bounds);
	}

	@Override
	public void startNode(int id, double lat, double lon) {
		currentNode = id;
		try {
			nodeWriter.write(id, lat, lon);
		} catch (IOException e) {
			System.out.println("Unable to write node " + id + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		delegate.startNode(id, lat, lon);
	}

	@Override
	public void startWay(int id) {
		currentWay = id;
		startedWayTags = false;
		try {
			wayWriter.write(id);
		} catch (IOException e) {
			System.out.println("Unable to write way " + id + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.startWay(id);
	}

	@Override
	public void startRelation(int id) {
		currentRel = id;
		startedRelTags = false;
		try {
			relationWriter.write(id);
		} catch (IOException e) {
			System.out.println("Unable to write relation " + id + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.startRelation(id);
	}

	@Override
	public void nodeTag(String key, String value) {
		try {
			nodeWriter.writeTag(key, value);
		} catch (IOException e) {
			System.out.println("Unable to write tag for node " + currentNode + ". key=" + key + ", value=" + value + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.nodeTag(key, value);
	}

	@Override
	public void wayTag(String key, String value) {
		try {
			if (!startedWayTags) {
				startedWayTags = true;
				wayWriter.closeNodeRefs();
			}
			wayWriter.writeTag(key, value);
		} catch (IOException e) {
			System.out.println("Unable to write tag for way " + currentWay + ". key=" + key + ", value=" + value + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.wayTag(key, value);
	}

	@Override
	public void relationTag(String key, String value) {
		try {
			if (!startedRelTags) {
				startedRelTags = true;
				relationWriter.closeMembers();
			}
			relationWriter.writeTag(key, value);
		} catch (IOException e) {
			System.out.println("Unable to write tag for relation " + currentRel + ". key=" + key + ", value=" + value + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.relationTag(key, value);
	}

	@Override
	public void wayNode(int nodeId) {
		try {
			wayWriter.writeNodeRef(nodeId);
		} catch (IOException e) {
			System.out.println("Unable to write node reference for way " + currentWay + ", nodeId=" + nodeId + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.wayNode(nodeId);
	}

	@Override
	public void relationNode(int nodeId, String role) {
		try {
			relationWriter.writeMember(MemberType.Node, nodeId, role);
		} catch (IOException e) {
			System.out.println("Unable to write node member for relation " + currentRel + ", nodeId=" + nodeId + ", role='" + role + "'. Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.relationNode(nodeId, role);
	}

	@Override
	public void relationWay(int wayId, String role) {
		try {
			relationWriter.writeMember(MemberType.Way, wayId, role);
		} catch (IOException e) {
			System.out.println("Unable to write way member for relation " + currentRel + ", wayId=" + wayId + ", role='" + role + "'. Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.relationWay(wayId, role);
	}

	@Override
	public void endNode() {
		try {
			nodeWriter.closeTags();
			nodeWriter.next();
		} catch (IOException e) {
			System.out.println("Unable to finish writing node " + currentNode + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.endNode();
	}

	@Override
	public void endWay() {
		try {
			if (!startedWayTags)
				wayWriter.closeNodeRefs();
			wayWriter.closeTags();
			wayWriter.next();
		} catch (IOException e) {
			System.out.println("Unable to finish writing way " + currentWay + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.endWay();
	}

	@Override
	public void endRelation() {
		try {
			if (!startedRelTags)
				relationWriter.closeMembers();
			relationWriter.closeTags();
			relationWriter.next();
		} catch (IOException e) {
			System.out.println("Unable to finish writing relation " + currentRel + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.endRelation();
	}

	@Override
	public void endMap() {
		try {
			nodeWriter.close();
			wayWriter.close();
			relationWriter.close();
			verifier.saveEntries();
		} catch (IOException e) {
			System.out.println("Unable to close node/way/relation writer(s). Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
		if (!delegate.isStartNodeOnly())
			delegate.endMap();
	}
}
