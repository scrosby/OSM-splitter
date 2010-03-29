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

public interface MapProcessor {

	/**
	 * @return {@code true} if this processor is only interested in
	 * {@link #startNode(int, double,double)} events, {@code false}
	 * if all events are handled.
	 * <p/>
	 * If this is set to {@code true}, the caller can significantly
	 * cut down on the amount of work it has to do.
	 */
	boolean isStartNodeOnly();

	/**
	 * Called when the bound tag is encountered. Note that it is possible
	 * for this to be called multiple times, eg if there are multiple OSM
	 * files provided as input.
	 * @param bounds the area covered by the map.
	 */
	void boundTag(Area bounds);

	/**
	 * Called when a node is encountered.
	 * @param id the node's ID.
	 * @param lat the node's latitude.
	 * @param lon the node's longitude.
	 */
	void startNode(int id, double lat, double lon);

	/**
	 * Called when a way is encountered.
	 * @param id the way's ID.
	 */
	void startWay(int id);

	/**
	 * Called when a relation is encountered.
	 * @param id the relation's ID.
	 */
	void startRelation(int id);

	/**
	 * Called when a tag is encountered on a node. This method will be
	 * called for every tag associated with the node that was specified
	 * in the most recent call to {@link #startNode(int, double, double)}.
	 * @param key the tag's key.
	 * @param value the tag's value.
	 */
	void nodeTag(String key, String value);

	/**
	 * Called when a tag is encountered on a way. This method will be
	 * called for every tag associated with the way that was specified
	 * in the most recent call to {@link #startWay(int)}.
	 * @param key the tag's key.
	 * @param value the tag's value.
	 */
	void wayTag(String key, String value);

	/**
	 * Called when a tag is encountered on a relation. This method will
	 * be called for every tag associated with the relation that was
	 * specified in the most recent call to {@link #startRelation(int)}.
	 * @param key the tag's key.
	 * @param value the tag's value.
	 */
	void relationTag(String key, String value);

	/**
	 * Called when a reference to a node is encountered within a way. This
	 * method will be called for every node associated with the way that was
	 * specified in the most recent call to {@link #startWay(int)} .
	 * @param nodeId the ID of the node.
	 */
	void wayNode(int nodeId);

	/**
	 * Called when a reference to a node is encountered within a relation.
	 * This method will be called for every node that is associated with the
	 * relation that was specified in the most recent call to {@link #startRelation(int)} .
	 * @param nodeId the ID of the node.
	 */
	void relationNode(int nodeId, String role);

	/**
	 * Called when a reference to a way is encountered within a relation.
	 * This method will be called for every way that is associated with the relation
	 * that was specified in the most recent call to {@link #startRelation(int)} .
	 * @param nodeId the ID of the node.
	 */
	void relationWay(int wayId, String role);

	/**
	 * Called when processing is complete for a node. This method will be called once
	 * there is no further data available for the node specified in the most recent
	 * call to {@link #startNode(int, double, double)}.
	 */
	void endNode();

	/**
	 * Called when processing is complete for a way. This method will be called once
	 * there is no further data available for the way specified in the most recent
	 * call to {@link #startWay(int)}.
	 */
	void endWay();

	/**
	 * Called when processing is complete for a relation. This method will be called once
	 * there is no further data available for the relation specified in the most recent
	 * call to {@link #startRelation(int)}.
	 */
	void endRelation();

	/**
	 * Called once the entire map has finished processing.
	 */
	void endMap();
}
