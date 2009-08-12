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
 * Create date: 14-Dec-2008
 */
package uk.me.parabola.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Used to split the SubArea's down into roughly equal sized pieces.
 * They will all have less than a specified number of nodes.
 * 
 * @author Steve Ratcliffe
 */
public class AreaSplitter {
	private final int shift;

	public AreaSplitter(int resolution) {
		shift = 24 - resolution;
	}

	/**
	 * Split a single area which would normally be the complete area of the map.
	 * We just split areas that are too big into two.  We make a rough determination
	 * of the largest dimension and split that way.
	 * 
	 * @param area The original area.
	 * @param max The maximum number of nodes that any area can contain.
	 * @return An array of areas.  Each will have less than the specified number of nodes.
	 */
	public AreaList split(SubArea area, int max) {
		List<SubArea> areas = new ArrayList<SubArea>();

		areas.add(area);

		boolean notDone = true;
		while (notDone) {

			notDone = false;
			ListIterator<SubArea> it = areas.listIterator();
			while (it.hasNext()) {
				SubArea workarea = it.next();
				SplitIntList list = workarea.getCoords();
				if (list == null) {
					continue;
				}

				Area bounds = workarea.getBounds();

				// If there's already less than max-nodes in the area we don't split it further
				int size = list.size();
				if (size < max) {
					workarea.clear();
					continue;
				}
				int height = bounds.getHeight();
				int width = bounds.getWidth();

				// If we've already split the area down to the minimum allowable size, we don't split it further
				boolean minHeight = height <= 2 << shift;
				boolean minWidth = width <= 2 << shift;
				if (minHeight && minWidth) {
					System.out.println("Area " + bounds + " contains " + Utils.format(workarea.getSize())
									+ " nodes but is already at the minimum size so can't be split further");
					continue;
				}

				// Decide whether to split vertically or horizontally and go ahead with the split
				SubArea[] sub;
				int width1 = (int) (width * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
				int width2 = (int) (width * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMaxLat()))));
				width = Math.max(width1, width2);
				if (height > width && !minHeight) {
					sub = splitVert(workarea);
				}
				else {
					sub = splitHoriz(workarea);
				}

				it.set(sub[0]);
				it.add(sub[1]);
				workarea.clear();
				notDone = true;
			}
		}
		return new AreaList(areas);
	}

	private SubArea[] splitHoriz(SubArea base) {
		Area bounds = base.getBounds();
		int left = bounds.getMinLong();
		int right = bounds.getMaxLong();

		SplitIntList baseCoords = base.getCoords();

	  SplitIntList.Iterator it = baseCoords.getIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			int val = it.next();
			int lon = extractLongitude(val);
			assert lon >= left && lon <= right : lon;
			count++;
			total += lon - left + 1;
		}
		int mid = limit(left, right, total / count);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area b2 = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());

		SubArea a1 = new SubArea(b1);
		SubArea a2 = new SubArea(b2);

		it = baseCoords.getDeletingIterator();
		while (it.hasNext()) {
			int co = it.next();
			if (extractLongitude(co) < mid) {
				a1.add(co);
			} else {
				a2.add(co);
			}
		}

		return new SubArea[]{a1, a2};
	}

	private SubArea[] splitVert(SubArea base) {

		Area bounds = base.getBounds();
		int top = bounds.getMaxLat();
		int bot = bounds.getMinLat();

		SplitIntList caseCoords = base.getCoords();

		SplitIntList.Iterator it = caseCoords.getIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			int val = it.next();
			int lat = extractLatitude(val);
			assert lat >= bot && extractLongitude(val) <= top : lat;
			count++;
			total += lat - bot;
		}
		int mid = limit(bot, top, total / count);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area b2 = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());

		SubArea a1 = new SubArea(b1);
		SubArea a2 = new SubArea(b2);

		caseCoords = base.getCoords();

		it = caseCoords.getDeletingIterator();
		while (it.hasNext()) {
			int co = it.next();
			if (extractLatitude(co) <= mid) {
				a1.add(co);
			} else {
				a2.add(co);
			}
		}

		return new SubArea[]{a1, a2};
	}

	private int extractLatitude(int value) {
		return ((value & 0xffff0000) >> 8);
	}

	private int extractLongitude(int value) {
		int lon = value & 0xffff;
		if ((lon & 0x8000) != 0)
			lon |= 0xffff0000;
		return lon << 8;
	}

	private int limit(int first, int second, long calcOffset) {
		int mid = first + (int) calcOffset;
		int limitoff = (second - first) / 5;
		if (mid - first < limitoff)
			mid = first + limitoff;
		else if (second - mid < limitoff)
			mid = second - limitoff;

		// Round to a garmin map unit at the desired zoom level.
		int nmid = Utils.round(mid, shift);

		// Check that the midpoint is on the appropriate alignment boundary. If not, adjust
		int alignment = 1 << shift;
		if ((nmid & alignment) != (first & alignment)) {
			if (nmid < mid) {
				nmid += alignment;
  		} else {
				nmid -= alignment;
			}
		}

		// Check if we're going to end up on the edge of a tile. If so, move away. We always
		// have room to move away because a split is only attempted in the first place if
		// the tile to split is bigger than the minimum tile width.
		if (nmid == first) {
			nmid += alignment << 1;
		} else if (nmid == second) {
			nmid -= alignment << 1;
		}

		assert nmid > first && nmid < second;
		return nmid;
	}
}
