package uk.me.parabola.splitter.geo;

/**
 * Holds information about a single city. Immutable.
 *
 * @author Chris Miller
 */
public class City {
	// The location of the city in Garmin map units
	private final int lat, lon;

	// A unique ID for the city
	private final int id;
	private final String countryCode;
	private final String name;
	private final int population;

	public City(int id, String countryCode, String name, int lat, int lon, int population) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.countryCode = countryCode;
		this.name = name;
		this.population = population;
	}

	public int getId() {
		return id;
	}

	public int getLat() {
		return lat;
	}

	public int getLon() {
		return lon;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getName() {
		return name;
	}

	public int getPopulation() {
		return population;
	}
}
