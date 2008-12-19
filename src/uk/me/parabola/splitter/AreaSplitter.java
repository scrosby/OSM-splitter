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

import uk.me.parabola.imgfmt.Utils;
import uk.me.parabola.imgfmt.app.Area;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
				Int2IntOpenHashMap map = workarea.getCoords();
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
				int height = (int) (bounds.getHeight() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
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

		Int2IntOpenHashMap baseCoords = base.getCoords();

		Int2IntMap.FastEntrySet fastEntrySet = baseCoords.int2IntEntrySet();
		ObjectIterator<Int2IntMap.Entry> it = fastEntrySet.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			Int2IntMap.Entry entry = it.next();
			assert extractLongitude(entry.getValue()) >= left && extractLongitude(entry.getValue()) <= right ;
			count++;
			total += extractLongitude(entry.getIntValue()) - left + 1;
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

		fastEntrySet = baseCoords.int2IntEntrySet();
		it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2IntMap.Entry entry = it.next();
			int key = entry.getIntKey();
			int co = entry.getIntValue();
			if (extractLongitude(co) < mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("split sizes " + a1.getSize() +", " + a2.getSize());
		return new SubArea[]{a1, a2};
	}

	private int extractLongitude(int value) {
		return (value&0xffff) << 8;
	}

	private SubArea[] splitVert(SubArea base) {
		System.out.println("split vert");
		Area bounds = base.getBounds();
		int top = bounds.getMaxLat();
		int bot = bounds.getMinLat();

		Int2IntOpenHashMap caseCoords = base.getCoords();

		Int2IntMap.FastEntrySet fastEntrySet = caseCoords.int2IntEntrySet();
		ObjectIterator<Int2IntMap.Entry> it = fastEntrySet.fastIterator();
		int count = 0;
		long total = 0;
		while (it.hasNext()) {
			Int2IntMap.Entry entry = it.next();
			assert extractLatitude(entry.getValue()) >= bot && extractLongitude(entry.getValue()) <= top;
			count++;
			total += extractLatitude(entry.getIntValue()) - bot;
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

		fastEntrySet = caseCoords.int2IntEntrySet();
		it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2IntMap.Entry entry = it.next();
			int key = entry.getIntKey();
			int co = entry.getIntValue();
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
		return (value >> 8) & 0xffffff;
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
