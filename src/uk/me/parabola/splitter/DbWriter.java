/*
 * Copyright (C) 2007 Steve Ratcliffe
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
 * Create date: Dec 19, 2007
 */
package uk.me.parabola.splitter;

import uk.me.parabola.mkgmap.general.MapCollector;
import uk.me.parabola.mkgmap.general.MapPoint;
import uk.me.parabola.mkgmap.general.MapLine;
import uk.me.parabola.mkgmap.general.MapShape;
import uk.me.parabola.imgfmt.app.Coord;

/**
 * @author Steve Ratcliffe
 */
public class DbWriter implements MapCollector {
	private OsmDatabase database;

	public DbWriter(OsmDatabase db) {
		this.database = db;
	}

	public void addToBounds(Coord p) {
	}

	public void addPoint(MapPoint point) {
	}

	public void addLine(MapLine line) {
	}

	public void addShape(MapShape shape) {
	}
}
