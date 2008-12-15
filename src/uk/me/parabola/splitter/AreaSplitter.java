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
import java.util.ListIterator;

import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.Coord;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author Steve Ratcliffe
 */
public class AreaSplitter {

	public void split(SubArea area, int max) {
		LinkedList<SubArea> l = new LinkedList<SubArea>();

		l.add(area);

		boolean notDone = true;
		while (notDone) {
			System.out.println("Need to split");

			notDone = false;
			ListIterator<SubArea> it = l.listIterator();
			while (it.hasNext()) {
				SubArea workarea = it.next();
				Int2ReferenceOpenHashMap map = workarea.getCoords();
				if (map == null)
					continue;
				int size = map.size();
				System.out.println("size is " + size + ", " + workarea.getSize());
				if (size < max) {
					workarea.clear();
					continue;
				}

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
	}

	private SubArea[] splitHoriz(SubArea base) {
		System.out.println("split horiz");
		Area bounds = base.getBounds();
		int right = bounds.getMaxLong();
		int left = bounds.getMinLong();
		int mid = (right + left )/2;
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

		Int2ReferenceOpenHashMap<Coord> baseLats = base.getCoords();

		Int2ReferenceMap.FastEntrySet<Coord> fastEntrySet = baseLats.int2ReferenceEntrySet();
		ObjectIterator<Int2ReferenceMap.Entry<Coord>> it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			int key = entry.getIntKey();
			Coord co = entry.getValue();
			if (co.getLongitude() > mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("lat size " + a1.getSize() +", " + a2.getSize());
		return new SubArea[]{a1, a2};
	}

	private SubArea[] splitVert(SubArea base) {
		System.out.println("split vert");
		Area bounds = base.getBounds();
		int top = bounds.getMaxLat();
		int bot = bounds.getMinLat();
		int mid = (top + bot )/2;

		Area b1 = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());
		Area b2 = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());

		int size = base.getSize()/2;
		if (size < 1000)
			size = 1000;

		SubArea a1 = new SubArea(b1, size);
		SubArea a2 = new SubArea(b2, size);

		Int2ReferenceOpenHashMap<Coord> baseLats = base.getCoords();

		Int2ReferenceMap.FastEntrySet<Coord> fastEntrySet = baseLats.int2ReferenceEntrySet();
		ObjectIterator<Int2ReferenceMap.Entry<Coord>> it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Int2ReferenceMap.Entry<Coord> entry = it.next();
			int key = entry.getIntKey();
			Coord co = entry.getValue();
			if (co.getLatitude() > mid) {
				a1.put(key, co);
			} else {
				a2.put(key, co);
			}
		}

		System.out.println("lat size " + a1.getSize() +", " + a2.getSize());

		return new SubArea[]{a1, a2};
	}
}
