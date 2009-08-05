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

import org.xmlpull.v1.XmlPullParserException;

/**
 * First pass of the OSM file for the initial dividing up of the
 * areas.
 *
 * @author Steve Ratcliffe
 */
class DivisionParser extends AbstractXppParser {
	private int mode;

	private static final int SHIFT = 11;

	private static final int MODE_NODE = 1;

	// How many nodes to process before displaying a status update
	private static final int STATUS_UPDATE_THRESHOLD = 2500000;

	private SplitIntList coords = new SplitIntList();
	private final MapDetails details = new MapDetails();

	// Mixed nodes and ways in the file.
	private boolean mixed;

	private int minNodeId = Integer.MAX_VALUE;
	private int maxNodeId = Integer.MIN_VALUE;
	private int nodeCount;

	DivisionParser() throws XmlPullParserException {
	}

	/**
	 * Receive notification of the start of an element.
	 *
	 * @returns {@code true} to prevent any further parsing.
	 */
	public boolean startElement()
	{
		if (mode == 0) {
			String name = getParser().getName();
			if (name.equals("node")) {
				mode = MODE_NODE;

				nodeCount++;
				int id = Integer.parseInt(getAttr("id"));
				double lat = Double.parseDouble(getAttr("lat"));
				double lon = Double.parseDouble(getAttr("lon"));
				Coord co = new Coord(lat, lon);

				if (id < minNodeId) {
					minNodeId = id;
				} else if (id > maxNodeId) {
					// Theoretically we shouldn't have the 'else' above, but unless the nodes are strictly
					// in order of decreasing IDs it's not a problem and it saves a branch instruction
					maxNodeId = id;
				}

				// Since we are rounding areas to fit on a low zoom boundary we
				// can drop the bottom 8 bits of the lat and lon and then fit
				// the whole lot into a single int.
				int glat = co.getLatitude();
				int glon = co.getLongitude();
				int coord = ((glat << 8) & 0xffff0000) + ((glon >> 8) & 0xffff);

				coords.add(coord);

				details.addToBounds(co);

				if (nodeCount % STATUS_UPDATE_THRESHOLD == 0) {
					System.out.println(Utils.format(nodeCount) + " nodes processed...");
				}

			} else if (!mixed && name.equals("way")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Receive notification of the end of an element.
	 */
	public void endElement()
	{
		if (mode == MODE_NODE) {
			if (getParser().getName().equals("node")) {
				mode = 0;
			}
		}
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public int getMinNodeId() {
		return minNodeId;
	}

	public int getMaxNodeId() {
		return maxNodeId;
	}

	public SubArea getTotalArea() {
		Area bounds = round(details.getBounds());
		SubArea sub = new SubArea(bounds, coords);
		coords = null;
		return sub;
	}

	private Area round(Area b) {
		return new Area(roundDown(b.getMinLat()), roundDown(b.getMinLong()),
				roundUp(b.getMaxLat()), roundUp(b.getMaxLong()));
	}

	private int roundUp(int val) {
		int mask = (1 << SHIFT) - 1;
		if (val > 0) {
			return (val + mask) & ~mask;
		} else {
			return val & ~mask;
		}
	}

	private int roundDown(int val) {
		int mask = (1 << SHIFT) - 1;
		if (val > 0) {
			return (val) & ~mask;
		} else {
			return (val- mask) & ~mask;
		}
	}

	public void setMixed(boolean mixed) {
		this.mixed = mixed;
	}
}
