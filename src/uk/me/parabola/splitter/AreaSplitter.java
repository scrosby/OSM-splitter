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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.Coord;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Used to split the SubArea's down into roughly sized pieces.
 * 
 * @author Steve Ratcliffe
 */
public class AreaSplitter {
	private static final int SHIFT = 11;

	public List<SubArea> split(SubArea area, int max) {
		LinkedList<SubArea> l = new LinkedList<SubArea>();

		l.add(area);

		boolean notDone = true;
		while (notDone) {

			notDone = false;
			ListIterator<SubArea> it = l.listIterator();
			while (it.hasNext()) {
				SubArea workarea = it.next();
				Int2ReferenceOpenHashMap map = workarea.getCoords();
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
				int width = bounds.getWidth();
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

		for (SubArea a : l) {
			System.out.println("a " + a.getBounds() + ", size=" + a.getSize());
		}
		return l;
	}

	private SubArea[] splitHoriz(SubArea base) {
		System.out.println("split horiz size=" + base.getSize());
		Area bounds = base.getBounds();
		int left = bounds.getMinLong();
		int right = bounds.getMaxLong();
		System.out.println("left = " + left);
		System.out.println("right = " + right);

		Int2ReferenceOpenHashMap<Coord> baseCoords = base.getCoords();

		Int2ReferenceMap.FastEntrySet<Coord> fastEntrySet = baseCoords.int2ReferenceEntrySet();
		ObjectIterator<Int2ReferenceMap.Entry<Coord>> it = fastEntrySet.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			assert entry.getValue().getLongitude() >= left && entry.getValue().getLongitude() <= right : entry.getValue().getLongitude();
			count++;
			total += entry.getValue().getLongitude() - left + 1;
		}
		int mid = limit(left, right, total / count);
		System.out.println("mid = " + mid + ", tot=" + total + ", count=" + count);

		System.out.println("in " + bounds);
		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area b2 = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());

		System.out.println("out 1 " + b1);
		System.out.println("out 2 " + b2);
		int size = base.getSize()/2;
		if (size < 1000)
			size = 1000;

		SubArea a1 = new SubArea(b1, size);
		SubArea a2 = new SubArea(b2, size);

		fastEntrySet = baseCoords.int2ReferenceEntrySet();
		it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			int key = entry.getIntKey();
			Coord co = entry.getValue();
			if (co.getLongitude() < mid) {
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

		Int2ReferenceOpenHashMap<Coord> caseCoords = base.getCoords();

		Int2ReferenceMap.FastEntrySet<Coord> fastEntrySet = caseCoords.int2ReferenceEntrySet();
		ObjectIterator<Int2ReferenceMap.Entry<Coord>> it = fastEntrySet.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			assert entry.getValue().getLatitude() >= bot && entry.getValue().getLongitude() <= top;
			count++;
			total += entry.getValue().getLatitude() - bot;
		}
		int mid = limit(bot, top, total / count);
		System.out.println("bot = " + bot);
		System.out.println("top = " + top);
		System.out.println("mid = " + mid);

		Area b1 = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area b2 = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());

		int size = base.getSize()/2;
		if (size < 1000)
			size = 1000;

		SubArea a1 = new SubArea(b1, size);
		SubArea a2 = new SubArea(b2, size);

		caseCoords = base.getCoords();

		fastEntrySet = caseCoords.int2ReferenceEntrySet();
		it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			int key = entry.getIntKey();
			Coord co = entry.getValue();
			if (co.getLatitude() <= mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("split sizes " + a1.getSize() +", " + a2.getSize());

		return new SubArea[]{a1, a2};
	}

	private int limit(int first, int second, long calcOffset) {
		int mid = first + (int) calcOffset;
		int limitoff = (second - first) / 5;
		if (mid - first < limitoff)
			mid = first + limitoff;
		else if (second - mid < limitoff)
			mid = second - limitoff;

		int nmid = (mid + (1 << (SHIFT - 1)));
		nmid &= ~((1<<SHIFT)-1);
//		if (nmid < first || n)
		assert nmid >= first && nmid <= second;
		return nmid;
	}
}
