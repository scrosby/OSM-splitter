/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 or
 *  version 3 as published by the Free Software Foundation.
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

/**
 * A node where the originally read strings are also saved so that we can
 * easily print them out again.
 *
 * @author Steve Ratcliffe
 */
public class StringNode extends Element {
	private final Coord coord;
	private final String stringLat;
	private final String stringLon;

	public StringNode(Coord co, String stringId, String stringLat, String stringLon) {
		super(stringId);
		coord = co;
		this.stringLat = stringLat;
		this.stringLon = stringLon;
	}

	public Coord getLocation() {
		return coord;
	}

	public String getStringLat() {
		return stringLat;
	}

	public String getStringLon() {
		return stringLon;
	}
}
