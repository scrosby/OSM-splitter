package uk.me.parabola.splitter;

/**
 * Collects node coordinates in a map.
 */
class NodeCollector implements MapProcessor {

	private SplitIntList coords = new SplitIntList();
	private final MapDetails details = new MapDetails();

	@Override
	public boolean isStartNodeOnly() {
		return true;
	}

	@Override
	public void startNode(int id, double lat, double lon) {
		// Since we are rounding areas to fit on a low zoom boundary we
		// can drop the bottom 8 bits of the lat and lon and then fit
		// the whole lot into a single int.
		int glat = Utils.toMapUnit(lat);
		int glon = Utils.toMapUnit(lon);
		int coord = ((glat << 8) & 0xffff0000) + ((glon >> 8) & 0xffff);

		coords.add(coord);
		details.addToBounds(glat, glon);
	}

	@Override
	public void startWay(int id) {}

	@Override
	public void startRelation(int id) {}

	@Override
	public void nodeTag(String key, String value) {}

	@Override
	public void wayTag(String key, String value) {}

	@Override
	public void relationTag(String key, String value) {}

	@Override
	public void wayNode(int nodeId) {}

	@Override
	public void relationNode(int nodeId, String role) {}

	@Override
	public void relationWay(int wayId, String role) {}

	@Override
	public void endNode() {}

	@Override
	public void endWay() {}

	@Override
	public void endRelation() {}

	@Override
	public void endMap() {}

	public Area getExactArea() {
		return details.getBounds();
	}

	public SubArea getRoundedArea(int resolution) {
		Area bounds = round(details.getBounds(), resolution);
		SubArea sub = new SubArea(bounds, coords);
		coords = null;
		return sub;
	}

	/**
	 * Rounds an area's borders off to suit the supplied resolution. This
	 * means edges are aligned at 2 ^ (24 - resolution) boundaries, and area
	 * widths and heights are multiples of twice the alignment.
	 *
	 * @param b the area to round
	 * @param resolution the map resolution to align the borders at
	 * @return the rounded area
	 */
	private static Area round(Area b, int resolution) {
		int shift = 24 - resolution;
		int alignment = 1 << shift;
		int doubleAlignment = alignment << 1;

		int roundedMinLat = Utils.roundDown(b.getMinLat(), shift);
		int roundedMaxLat = Utils.roundUp(b.getMaxLat(), shift);
		if ((roundedMinLat & alignment) != (roundedMaxLat & alignment)) {
			// The new height isn't a multiple of twice the alignment. Fix it by pushing
			// the tile edge that moved the least out by another 'alignment' units.
			if (b.getMinLat() - roundedMinLat < b.getMaxLat() - roundedMaxLat) {
				roundedMinLat -= alignment;
			} else {
				roundedMaxLat += alignment;
			}
		}
		assert roundedMinLat % alignment == 0 : "The area's min latitude is not aligned to a multiple of " + alignment;
		assert roundedMaxLat % alignment == 0 : "The area's max latitude is not aligned to a multiple of " + alignment;
		assert (roundedMaxLat - roundedMinLat) % doubleAlignment == 0 : "The area's height is not a multiple of " + doubleAlignment;

		int roundedMinLon = Utils.roundDown(b.getMinLong(), shift);
		int roundedMaxLon = Utils.roundUp(b.getMaxLong(), shift);
		if ((roundedMinLon & alignment) != (roundedMaxLon & alignment)) {
			// The new width isn't a multiple of twice the alignment. Fix it by pushing
			// the tile edge that moved the least out by another 'alignment' units.
			if (b.getMinLong() - roundedMinLon < b.getMaxLong() - roundedMaxLon) {
				roundedMinLon -= alignment;
			} else {
				roundedMaxLon += alignment;
			}
		}
		assert roundedMinLon % alignment == 0 : "The area's min longitude is not aligned to a multiple of " + alignment;
		assert roundedMaxLon % alignment == 0 : "The area's max longitude is not aligned to a multiple of " + alignment;
		assert (roundedMaxLon - roundedMinLon) % doubleAlignment == 0 : "The area's width is not a multiple of " + doubleAlignment;

		return new Area(roundedMinLat, roundedMinLon, roundedMaxLat, roundedMaxLon);
	}
}