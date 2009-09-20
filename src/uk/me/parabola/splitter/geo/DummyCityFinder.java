package uk.me.parabola.splitter.geo;

import java.util.Collections;
import java.util.Set;

import uk.me.parabola.splitter.Area;

/**
 * @author Chris Miller
 */
public class DummyCityFinder implements CityFinder {
	private static final Set<City> DUMMY_RESULTS = Collections.emptySet();
	@Override
	public Set<City> findCities(Area area) {
		return DUMMY_RESULTS;
	}

	@Override
	public Set<City> findCities(int minLat, int minLon, int maxLat, int maxLon) {
		return DUMMY_RESULTS;
	}
}
