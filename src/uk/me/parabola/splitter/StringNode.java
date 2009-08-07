/*
 * Copyright (C) 2008 Steve Ratcliffe
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
 * Create date: 16-Dec-2008
 */
package uk.me.parabola.splitter;

/**
 * A node where the originally read strings are also saved so that we can
 * easily print them out again.
 *
 * @author Steve Ratcliffe
 */
public class StringNode extends Element {
	private final int lat;
	private final int lon;
	private final String stringLat;
	private final String stringLon;

	public StringNode(int lat, int lon, String stringId, String stringLat, String stringLon) {
		super(stringId);
		this.lat = lat;
		this.lon = lon;
		this.stringLat = stringLat;
		this.stringLon = stringLon;
	}

	public int getLat() {
		return lat;
	}

	public int getLon() {
		return lon;
	}

	public String getStringLat() {
		return stringLat;
	}

	public String getStringLon() {
		return stringLon;
	}
}
