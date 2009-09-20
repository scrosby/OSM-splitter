package uk.me.parabola.splitter.geo;

import java.util.Set;

import uk.me.parabola.splitter.Area;

public interface CityFinder {
	Set<City> findCities(Area area);

	Set<City> findCities(int minLat, int minLon, int maxLat, int maxLon);
}
