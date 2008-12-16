/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
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

import uk.me.parabola.imgfmt.app.Coord;
import uk.me.parabola.mkgmap.reader.osm.Node;

/**
 * A node where the originally read strings are also saved so that we can
 * easily print them out again.
 *
 * @author Steve Ratcliffe
 */
public class StringNode extends Node {
	private String stringId;
	private String stringLat;
	private String stringLon;

	public StringNode(long id, Coord co, String stringId, String stringLat, String stringLon) {
		super(id, co);
		this.stringId = stringId;
		this.stringLat = stringLat;
		this.stringLon = stringLon;
	}

	public String getStringId() {
		return stringId;
	}

	public String getStringLat() {
		return stringLat;
	}

	public String getStringLon() {
		return stringLon;
	}
}
