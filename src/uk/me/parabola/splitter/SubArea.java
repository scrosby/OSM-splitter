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
 * Create date: 14-Dec-2008
 */
package uk.me.parabola.splitter;

import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.Coord;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

/**
 * @author Steve Ratcliffe
 */
public class SubArea {
	private final Area bounds;

	private Int2ReferenceOpenHashMap<Coord> coords;
	private int size;

	public SubArea(Area bounds, Int2ReferenceOpenHashMap<Coord> coords) {
		this.bounds = bounds;
		this.coords = coords;
	}

	public SubArea(Area area, int sizehint) {
		this.bounds = area;
		coords = new Int2ReferenceOpenHashMap<Coord>(sizehint, 0.8f);
		coords.growthFactor(8);
	}

	public void clear() {
		if (coords != null)
			size = coords.size();
		coords = null;
	}

	public Area getBounds() {
		return bounds;
	}

	public Int2ReferenceOpenHashMap<Coord> getCoords() {
		return coords;
	}

	public void setCoords(Int2ReferenceOpenHashMap<Coord> coords) {
		this.coords = coords;
	}

	public int getSize() {
		if (coords != null)
			return coords.size();
		else
			return size;
	}

	public void put(int key, Coord co) {
		coords.put(key, co);
	}
}
