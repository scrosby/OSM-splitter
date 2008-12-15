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

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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
				Long2IntOpenHashMap map = workarea.getLats();
				if (map == null)
					continue;
				int size = map.size();
				System.out.println("size is " + size + ", " + workarea.getLons().size());
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
		SubArea a1 = new SubArea(b1);
		Long2IntOpenHashMap a1lats = new Long2IntOpenHashMap(size, 0.8f);
		Long2IntOpenHashMap a1lons = new Long2IntOpenHashMap(size, 0.8f);
		a1lats.growthFactor(4);
		a1lons.growthFactor(4);
		a1.setLats(a1lats);
		a1.setLons(a1lons);

		SubArea a2 = new SubArea(b2);
		Long2IntOpenHashMap a2lats = new Long2IntOpenHashMap(size, 0.8f);
		Long2IntOpenHashMap a2lons = new Long2IntOpenHashMap(size, 0.8f);
		a2lats.growthFactor(4);
		a2lons.growthFactor(4);
		a2.setLats(a2lats);
		a2.setLons(a2lons);

		Long2IntOpenHashMap baseLons = base.getLons();
		Long2IntOpenHashMap baseLats = base.getLats();

		Long2IntMap.FastEntrySet fastEntrySet = baseLons.long2IntEntrySet();
		ObjectIterator<Long2IntMap.Entry> it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Long2IntMap.Entry entry = it.next();
			long key = entry.getLongKey();
			int lon = entry.getIntValue();
			if (lon > mid) {
				a1lats.put(key, baseLats.get(key));
				a1lons.put(key, lon);
			} else {
				a2lats.put(key, baseLats.get(key));
				a2lons.put(key, lon);
			}
		}

		assert a1lats.size() == a1lons.size();
		assert a2lats.size() == a2lons.size();
		System.out.println("lat size " + a1lats.size() +", " + a2lats.size());
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

		SubArea a1 = new SubArea(b1);
		Long2IntOpenHashMap a1lats = new Long2IntOpenHashMap();
		Long2IntOpenHashMap a1lons = new Long2IntOpenHashMap();
		a1.setLats(a1lats);
		a1.setLons(a1lons);

		SubArea a2 = new SubArea(b2);
		Long2IntOpenHashMap a2lats = new Long2IntOpenHashMap();
		Long2IntOpenHashMap a2lons = new Long2IntOpenHashMap();
		a2.setLats(a2lats);
		a2.setLons(a2lons);

		Long2IntOpenHashMap baseLons = base.getLons();
		Long2IntOpenHashMap baseLats = base.getLats();

		Long2IntMap.FastEntrySet fastEntrySet = baseLats.long2IntEntrySet();
		ObjectIterator<Long2IntMap.Entry> it = fastEntrySet.fastIterator();
		while (it.hasNext()) {
			Long2IntMap.Entry entry = it.next();
			long key = entry.getLongKey();
			int lat = entry.getIntValue();
//			System.out.printf("lat %d, mid %d\n", lat, mid);
			if (lat > mid) {
//				System.out.println("top");
				a1lats.put(key, lat);
				a1lons.put(key, baseLons.get(key));
			} else {
//				System.out.println("bottom");
				a2lats.put(key, lat);
				a2lons.put(key, baseLons.get(key));
			}
		}

		assert a1lats.size() == a1lons.size();
		assert a2lats.size() == a2lons.size();
		System.out.println("lat size " + a1lats.size() +", " + a2lats.size());
		System.out.println("lon size " + a1lons.size() +", " + a2lons.size());

		return new SubArea[]{a1, a2};
	}
}
