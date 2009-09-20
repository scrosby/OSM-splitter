package uk.me.parabola.splitter.args;

/**
 * Command line parameters for the splitter
 *
 * @author Chris Miller
 */
public interface SplitterParams {
	/**
	 * @return the ID for the first split area.
	 */
	@Option(defaultValue = "63240001", description = "The starting map ID.")
	int getMapid();

	@Option(description = "A default description to give to each area.")
	String getDescription();

	@Option(defaultValue = "255", description = "The maximum number of areas to process in a single pass. More areas require more memory. 1-255.")
	int getMaxAreas();

	@Option(defaultValue = "2000", description = "Nodes/ways/rels that fall outside an area will still be included if they are within this many map units.")
	int getOverlap();

	@Option(defaultValue = "1600000", description = "The maximum number of nodes permitted in each split area.")
	int getMaxNodes();

	@Option(defaultValue = "13", description = "The resolution of the overview map to be produced by mkgmap.")
	int getResolution();

	@Option(description = "Specify this if the input osm file has nodes, ways and relations intermingled.")
	boolean isMixed();

	@Option(description = "The path to the cache directory. If the path doesn't exist it will be created.")
	String getCache();

	@Option(description = "The name of the file containing the area definitions. Can be .list or .kml.")
	String getSplitFile();

	@Option(description = "The name of a GeoNames file to use for determining tile names. Typically cities15000.zip from http://download.geonames.org/export/dump/")
	String getGeonamesFile();

	@Option(description = "The name of a kml file to write out the areas to. This is in addition to areas.list (which is always written out).")
	String getWriteKml();

	@Option(description = "Enables the old area subdivision algorithm in case of compatibility problems. This requires lots of memory! Deprecated, will be removed in a future version.")
	boolean isLegacyMode();
}
