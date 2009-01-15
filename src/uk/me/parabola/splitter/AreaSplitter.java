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
 * Create date: 14-Dec-2008
 */
package uk.me.parabola.splitter;

import uk.me.parabola.imgfmt.Utils;
import uk.me.parabola.imgfmt.app.Area;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Used to split the SubArea's down into roughly equal sized pieces.
 * They will all have less than a specified number of nodes.
 * 
 * @author Steve Ratcliffe
 */
public class AreaSplitter {
	private static final int SHIFT = 11;

	/**
	 * Split a single area which would normally be the complete area of the map.
	 * We just split areas that are too big into two.  We make a rough determination
	 * of the largest dimention and split that way.
	 * 
	 * @param area The original area.
	 * @param max The maximimum number of nodes that any area can contain.
	 * @return An array of areas.  Each will have less than the specified number of nodes.
	 */
	public AreaList split(SubArea area, int max) {
		LinkedList<SubArea> l = new LinkedList<SubArea>();

		l.add(area);

		boolean notDone = true;
		while (notDone) {

			notDone = false;
			ListIterator<SubArea> it = l.listIterator();
			while (it.hasNext()) {
				SubArea workarea = it.next();
				SplitIntMap map = workarea.getCoords();
				if (map == null) {
					continue;
				}

				int size = map.size();
				System.out.println("comparing size " + workarea.getSize());
				if (size < max) {
					workarea.clear();
					continue;
				}
				System.out.println("Need to split");

				notDone = true;
				Area bounds = workarea.getBounds();
				int height = bounds.getHeight();
				int width = (int) (bounds.getWidth() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
				SubArea[] sub;
				if (height > width)
					sub = splitVert(workarea);
				else
					sub = splitHoriz(workarea);

				it.set(sub[0]);
				it.add(sub[1]);
				workarea.clear();
			}
		}

		for (SubArea a : l) 
			System.out.println("a " + a.getBounds() + ", size=" + a.getSize());

		return new AreaList(l);
	}

	private SubArea[] splitHoriz(SubArea base) {
		System.out.println("split horiz size=" + base.getSize());
		Area bounds = base.getBounds();
		int left = bounds.getMinLong();
		int right = bounds.getMaxLong();
		System.out.println("left = " + left);
		System.out.println("right = " + right);

		SplitIntMap baseCoords = base.getCoords();

		Iterator<IntIntMap.Entry> it = baseCoords.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			IntIntMap.Entry entry = it.next();
			int lon = extractLongitude(entry.getValue());
			assert lon >= left && lon <= right : lon;
			count++;
			total += lon - left + 1;
		}
		int mid = limit(left, right, total / count);
		System.out.println("mid = " + mid + ", tot=" + total + ", count=" + count);

		System.out.println("in " + bounds);
		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area b2 = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());

		System.out.println("out 1 " + b1);
		System.out.println("out 2 " + b2);

		SubArea a1 = new SubArea(b1);
		SubArea a2 = new SubArea(b2);

		it = baseCoords.fastDeletingIterator();
		while (it.hasNext()) {
			IntIntMap.Entry entry = it.next();
			int key = entry.getKey();
			int co = entry.getValue();
			if (extractLongitude(co) < mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("split sizes " + a1.getSize() +", " + a2.getSize());
		return new SubArea[]{a1, a2};
	}

	private SubArea[] splitVert(SubArea base) {
		System.out.println("split vert");
		Area bounds = base.getBounds();
		int top = bounds.getMaxLat();
		int bot = bounds.getMinLat();

		SplitIntMap caseCoords = base.getCoords();

		Iterator<IntIntMap.Entry> it = caseCoords.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			IntIntMap.Entry entry = it.next();
			int lat = extractLatitude(entry.getValue());
			assert lat >= bot && extractLongitude(entry.getValue()) <= top : lat;
			count++;
			total += lat - bot;
		}
		int mid = limit(bot, top, total / count);
		System.out.println("bot = " + bot);
		System.out.println("top = " + top);
		System.out.println("mid = " + mid);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area b2 = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());

		SubArea a1 = new SubArea(b1);
		SubArea a2 = new SubArea(b2);

		caseCoords = base.getCoords();

		it = caseCoords.fastDeletingIterator();
		while (it.hasNext()) {
			IntIntMap.Entry entry = it.next();
			int key = entry.getKey();
			int co = entry.getValue();
			if (extractLatitude(co) <= mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("split sizes " + a1.getSize() +", " + a2.getSize());

		return new SubArea[]{a1, a2};
	}

	private int extractLatitude(Integer value) {
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

		// Round to a garmin map unit at the given zoom level.
		int nmid = (mid + (1 << (SHIFT - 1)));
		nmid &= ~((1<<SHIFT)-1);

		assert nmid >= first && nmid <= second;
		return nmid;
	}
}
