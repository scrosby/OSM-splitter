package uk.me.parabola.splitter;

/**
 * Builds up a map of node densities across the total area being split.
 * Density information is held at the maximum desired map resolution.
 * Every step up in resolution increases the size of the density map by
 * a factor of 4.
 *
 * @author Chris Miller
 */
public class DensityMap {
	private final int width, height, shift, shiftedUnit;
	private final int[][] nodeMap;
	private final Area bounds;
	private int totalNodeCount;

	/**
	 * Creates a density map.
	 * @param area the area that the density map covers.
	 * @param resolution the resolution of the density map. This must be a value between 1 and 24.
	 */
	public DensityMap(Area area, int resolution) {
		assert resolution >=1 && resolution <= 24;
		shift = 24 - resolution;

		int minLat = RoundingUtils.roundDown(area.getMinLat(), shift);
		int minLon = RoundingUtils.roundDown(area.getMinLong(), shift);
		int maxLat = RoundingUtils.roundUp(area.getMaxLat(), shift);
		int maxLon = RoundingUtils.roundUp(area.getMaxLong(), shift);
		bounds = new Area(minLat, minLon, maxLat, maxLon);

		shiftedUnit = 1 << shift;
		height = (maxLat - minLat) >> shift;
		width = (maxLon - minLon) >> shift;
		nodeMap = new int[width][];
	}

	public int getShift() {
		return shift;
	}

	public Area getBounds() {
		return bounds;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int addNode(int lat, int lon) {
		if (!bounds.contains(lat, lon))
			return 0;
		totalNodeCount++;
		lat -= bounds.getMinLat();
		lon -= bounds.getMinLong();
		int x = lon >>> shift;
		int y = lat >>> shift;

		// Any nodes that are right on the limit need to be pulled back to fit in the last array element
		if (x == width)
			x--;
		if (y == height)
			y--;

		if (nodeMap[x] == null)
			nodeMap[x] = new int[height];
		return ++nodeMap[x][y];
	}

	public int getNodeCount() {
		return totalNodeCount;
	}

	public int getNodeCount(int x, int y) {
		return nodeMap[x] != null ? nodeMap[x][y] : 0;
	}

	public DensityMap subset(Area subset) {
		int minLat = Math.max(bounds.getMinLat(), subset.getMinLat());
		int minLon = Math.max(bounds.getMinLong(), subset.getMinLong());
		int maxLat = Math.min(bounds.getMaxLat(), subset.getMaxLat());
		int maxLon = Math.min(bounds.getMaxLong(), subset.getMaxLong());

		// If the area doesn't intersect with the density map, return an empty map
		if (minLat > maxLat || minLon > maxLon) {
			return new DensityMap(new Area(0, 0, 0, 0), 24 - shift);
		}

		DensityMap result = new DensityMap(new Area(minLat, minLon, maxLat, maxLon), 24 - shift);

		int startX = (minLon - bounds.getMinLong()) >> shift;
		int startY = (minLat - bounds.getMinLat()) >> shift;
		int maxX = (maxLon - minLon) >> shift;
		int maxY = (maxLat - minLat) >> shift;
		for (int x = 0; x < maxX; x++) {
			if (startY == 0 && maxY == height) {
				result.nodeMap[x] = nodeMap[startX + x];
			} else if (nodeMap[startX + x] != null) {
				result.nodeMap[x] = new int[maxY];
				System.arraycopy(nodeMap[startX + x], startY, result.nodeMap[x], 0, maxY);
			}
			for (int y = 0; y < maxY; y++) {
				if (result.nodeMap[x] != null)
					result.totalNodeCount += result.nodeMap[x][y];
			}
		}
		return result;
	}
}
