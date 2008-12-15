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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * @author Steve Ratcliffe
 */
public class SubArea {
	private final Area bounds;

	private Long2IntOpenHashMap lats;
	private Long2IntOpenHashMap lons;
	private int size;

	public SubArea(Area bounds, Long2IntOpenHashMap lats, Long2IntOpenHashMap lons) {
		this.bounds = bounds;
		this.lats = lats;
		this.lons = lons;
	}

	public SubArea(Area area) {
		this.bounds = area;
	}

	public void clear() {
		if (lats != null)
			size = lats.size();
		lats = null;
		lons = null;
	}

	public Area getBounds() {
		return bounds;
	}

	public Long2IntOpenHashMap getLats() {
		return lats;
	}

	public void setLats(Long2IntOpenHashMap lats) {
		this.lats = lats;
	}

	public Long2IntOpenHashMap getLons() {
		return lons;
	}

	public void setLons(Long2IntOpenHashMap lons) {
		this.lons = lons;
	}

	public int getSize() {
		if (lats != null)
			return lats.size();
		else
			return size;
	}
}
