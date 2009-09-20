/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package uk.me.parabola.splitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a density map into multiple areas, none of which
 * exceed the desired threshold.
 *
 * @author Chris Miller
 */
public class SplittableDensityArea implements SplittableArea {
	private DensityMap densities;

	public SplittableDensityArea(DensityMap densities) {
		this.densities = densities;
	}

	@Override
	public Area getBounds() {
		return densities.getBounds();
	}

	@Override
	public List<Area> split(int maxNodes) {
		if (densities == null || densities.getNodeCount() == 0)
			return Collections.emptyList();

		Area bounds = densities.getBounds();
		if (densities.getNodeCount() < maxNodes) {
			densities = null;
			return Collections.singletonList(bounds);
		}

		if (densities.getWidth() <= 1 && densities.getHeight() <= 1) {
			System.out.println("Area " + bounds + " contains " + Utils.format(densities.getNodeCount())
							+ " nodes but is already at the minimum size so can't be split further");
			return Collections.singletonList(bounds);
		}

		List<Area> results = new ArrayList<Area>();

		// Decide whether to split vertically or horizontally and go ahead with the split
		int width1 = (int) (densities.getWidth() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMinLat()))));
		int width2 = (int) (densities.getWidth() * Math.cos(Math.toRadians(Utils.toDegrees(bounds.getMaxLat()))));
		int width = Math.max(width1, width2);

		SplittableDensityArea[] splitResult;
		if (densities.getHeight() > width && densities.getHeight() > 1) {
			splitResult = splitVert();
		} else {
			splitResult = splitHoriz();
		}
		densities = null;
		results.addAll(splitResult[0].split(maxNodes));
		results.addAll(splitResult[1].split(maxNodes));
		return results;
	}

	/**
	 * Split into left and right areas
	 */
	protected SplittableDensityArea[] splitHoriz() {
		long sum = 0, weightedSum = 0;
		int splitX;

		for (int x = 0; x < densities.getWidth(); x++) {
			for (int y = 0; y < densities.getHeight(); y++) {
				int count = densities.getNodeCount(x, y);
				sum += count;
				weightedSum += (count * x);
			}
		}
		splitX = limit(0, densities.getWidth(), (int) (weightedSum / sum));

		if (splitX % 2 != 0) {
			splitX--;
			if (splitX < 2)
				splitX = 2;
		}

		Area bounds = densities.getBounds();
		int mid = bounds.getMinLong() + (splitX << densities.getShift());
		Area leftArea = new Area(bounds.getMinLat(), bounds.getMinLong(), bounds.getMaxLat(), mid);
		Area rightArea = new Area(bounds.getMinLat(), mid, bounds.getMaxLat(), bounds.getMaxLong());
		DensityMap left = densities.subset(leftArea);
		DensityMap right = densities.subset(rightArea);

		return new SplittableDensityArea[] {new SplittableDensityArea(left), new SplittableDensityArea(right)};
	}

	protected SplittableDensityArea[] splitVert() {
		long sum = 0, weightedSum = 0;
		int splitY;

		for (int y = 0; y < densities.getHeight(); y++) {
			for (int x = 0; x < densities.getWidth(); x++) {
				int count = densities.getNodeCount(x, y);
				sum += count;
				weightedSum += (count * y);
			}
		}
		splitY = limit(0, densities.getHeight(), (int) (weightedSum / sum));

		if (splitY % 2 != 0) {
			splitY--;
			if (splitY < 2)
				splitY = 2;
		}

		Area bounds = densities.getBounds();
		int mid = bounds.getMinLat() + (splitY << densities.getShift());
		Area bottomArea = new Area(bounds.getMinLat(), bounds.getMinLong(), mid, bounds.getMaxLong());
		Area topArea = new Area(mid, bounds.getMinLong(), bounds.getMaxLat(), bounds.getMaxLong());
		DensityMap bottom = densities.subset(bottomArea);
		DensityMap top = densities.subset(topArea);

		return new SplittableDensityArea[]{new SplittableDensityArea(bottom), new SplittableDensityArea(top)};
	}

	private int limit(int first, int second, long calcOffset) {
		int mid = first + (int) calcOffset;
		int limitoff = (second - first) / 5;
		if (mid - first < limitoff)
			mid = first + limitoff;
		else if (second - mid < limitoff)
			mid = second - limitoff;
		return mid;
	}
}
