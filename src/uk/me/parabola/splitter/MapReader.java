package uk.me.parabola.splitter;

public interface MapReader {
	long getNodeCount();

	long getWayCount();

	long getRelationCount();

	int getMinNodeId();

	int getMaxNodeId();
}
