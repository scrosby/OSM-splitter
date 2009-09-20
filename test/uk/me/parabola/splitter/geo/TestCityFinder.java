package uk.me.parabola.splitter.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the CityFinder interface
 *
 * @author Chris Miller
 */
public class TestCityFinder {
	@Test
	public void testFinder() {
		List<City> cities = getCities();
		CityFinder cityFinder = new DefaultCityFinder(cities);

		Collection<City> results = cityFinder.findCities(10,10,10,10);
		Assert.assertEquals(results.size(), 2);

		results = cityFinder.findCities(10, -10, 12, 0);
		Assert.assertEquals(results.size(), 1);
		Assert.assertEquals(results.iterator().next().getId(), 0);

		results = cityFinder.findCities(10, -10, 12, -4);
		Assert.assertEquals(results.size(), 0);
	}

	public List<City> getCities() {
		List<City> cities = new ArrayList<City>();
		cities.add(new City(2, "EF", "Efefef", 10, 10, 100000));
		cities.add(new City(1, "CD", "Cdcdcd", 10, 10, 100000));
		cities.add(new City(4, "IJ", "Ijijij", 12, 11, 100000));
		cities.add(new City(3, "GH", "Ghghgh", -2, 10, 100000));
		cities.add(new City(0, "AB", "Ababab", 10, -1, 100000));
		return cities;
	}
}
