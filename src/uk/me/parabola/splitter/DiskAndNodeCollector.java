package uk.me.parabola.splitter;

import java.io.File;
import java.io.IOException;

import uk.me.parabola.splitter.disk.MemberType;
import uk.me.parabola.splitter.disk.NodeStoreWriter;
import uk.me.parabola.splitter.disk.RelationStoreWriter;
import uk.me.parabola.splitter.disk.WayStoreWriter;

/**
 * Collects node information and also writes the map out in a binary format to disk.
 *
 * @author Chris Miller
 */
public class DiskAndNodeCollector extends NodeCollector {

	private NodeStoreWriter nodeWriter;
	private WayStoreWriter wayWriter;
	private RelationStoreWriter relationWriter;

	private int currentNode;
	private int currentWay;
	private int currentRel;
	private boolean startedWayTags;
	private boolean startedRelTags;

	public DiskAndNodeCollector(String path) throws IOException {
		nodeWriter = new NodeStoreWriter(path + File.separatorChar + "nodes.bin");
		wayWriter = new WayStoreWriter(path + File.separatorChar + "ways.bin");
		relationWriter = new RelationStoreWriter(path + File.separatorChar + "relations.bin");
	}

	@Override
	public boolean isStartNodeOnly() {
		return false;
	}

	@Override
	public void startNode(int id, double lat, double lon) {
		currentNode = id;
		super.startNode(id, lat, lon);
		try {
			nodeWriter.write(id, lat, lon);
		} catch (IOException e) {
			System.out.println("Unable to write node " + id + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
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
	}

	@Override
	public void nodeTag(String key, String value) {
		try {
			nodeWriter.writeTag(key, value);
		} catch (IOException e) {
			System.out.println("Unable to write tag for node " + currentNode + ". key=" + key + ", value=" + value + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
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
	}

	@Override
	public void wayNode(int nodeId) {
		try {
			wayWriter.writeNodeRef(nodeId);
		} catch (IOException e) {
			System.out.println("Unable to write node reference for way " + currentWay + ", nodeId=" + nodeId + ". Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void relationNode(int nodeId, String role) {
		try {
			relationWriter.writeMember(MemberType.Node, nodeId, role);
		} catch (IOException e) {
			System.out.println("Unable to write node member for relation " + currentRel + ", nodeId=" + nodeId + ", role='" + role + "'. Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void relationWay(int wayId, String role) {
		try {
			relationWriter.writeMember(MemberType.Way, wayId, role);
		} catch (IOException e) {
			System.out.println("Unable to write way member for relation " + currentRel + ", wayId=" + wayId + ", role='" + role + "'. Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
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
	}

	@Override
	public void endMap() {
		try {
			nodeWriter.close();
			wayWriter.close();
			relationWriter.close();
		} catch (IOException e) {
			System.out.println("Unable to close node/way/relation writer(s). Reason: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
