/*
 * Copyright (C) 2008 Steve Ratcliffe
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  as published by the Free Software Foundation.
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A list of areas.  It can be read and written to a file.
 */
public class AreaList {
	private SubArea[] areas;
	private static final SubArea[] SUB_AREA = new SubArea[0];

	public AreaList(List<SubArea> areas) {
		this.areas = areas.toArray(new SubArea[areas.size()]);
	}

	/**
	 * This constructor is called when you are going to be reading in the list from
	 * a file, rather than making it from an already constructed list.
	 */
	public AreaList() {
	}

	/**
	 * Write out a file containing the list of areas that we calculated.  This allows us to reuse the
	 * same areas on a subsequent run without having to re-calculate them.
	 *
	 * @param filename The filename to write to.
	 */
	public void write(String filename) throws IOException {

		Writer w = null;
		try {
			w = new FileWriter(filename);
			PrintWriter pw = new PrintWriter(w);

			pw.println("# List of areas");
			pw.format("# Generated %s\n", new Date());
			//pw.format("# Options: max-nodes=%d\n", main.getMaxNodes());
			pw.println("#");

			for (SubArea a : areas) {
				Area b = a.getBounds();
				pw.format(Locale.ROOT, "%d: %d,%d to %d,%d\n",
						a.getMapid(),
						b.getMinLat(), b.getMinLong(),
						b.getMaxLat(), b.getMaxLong());
				pw.format(Locale.ROOT, "#       : %f,%f to %f,%f\n",
						Utils.toDegrees(b.getMinLat()), Utils.toDegrees(b.getMinLong()),
						Utils.toDegrees(b.getMaxLat()), Utils.toDegrees(b.getMaxLong()));
				pw.println();
			}

		} catch (IOException e) {
			System.err.println("Could not write areas.list file");
		} finally {
			if (w != null)
				w.close();
		}
	}

	/**
	 * Write out a KML file containing the areas that we calculated. This KML file
	 * can be opened in Google Earth etc to see the areas that were split.
	 *
	 * @param filename The KML filename to write to.
	 */
	public void writeKml(String filename) throws IOException {

		Writer w = null;
		try {
			w = new FileWriter(filename);
			PrintWriter pw = new PrintWriter(w);

			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								 "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
								 "<Document>");

			for (SubArea a : areas) {
				Area b = a.getBounds();
				double south = Utils.toDegrees(b.getMinLat());
				double west = Utils.toDegrees(b.getMinLong());
				double north = Utils.toDegrees(b.getMaxLat());
				double east = Utils.toDegrees(b.getMaxLong());

				pw.format(Locale.ROOT,
								  "  <Placemark>\n" +
									"    <name>%1$d</name>\n" +
									"    <Polygon>\n" +
									"      <outerBoundaryIs>\n" +
									"        <LinearRing>\n" +
									"          <coordinates>\n" +
									"            %3$f,%2$f\n" +
									"            %3$f,%4$f\n" +
									"            %5$f,%4$f\n" +
									"            %5$f,%2$f\n" +
									"            %3$f,%2$f\n" +
									"          </coordinates>\n" +
									"        </LinearRing>\n" +
									"      </outerBoundaryIs>\n" +
									"    </Polygon>\n" +
									"  </Placemark>\n", a.getMapid(), south, west, north, east);
			}
			pw.print("</Document>\n</kml>");
		} catch (IOException e) {
			System.err.println("Could not write KML file " + filename);
		} finally {
			if (w != null)
				w.close();
		}
	}

	/**
	 * Read in an area definition file that we previously wrote.
	 * Obviously other tools could create the file too.
	 */
	public void read(String filename) throws IOException {
		Reader r = null;
		List<SubArea> list = new ArrayList<SubArea>();

		Pattern pattern = Pattern.compile("([0-9]{8}):" +
		" ([\\p{XDigit}x-]+),([\\p{XDigit}x-]+)" +
		" to ([\\p{XDigit}x-]+),([\\p{XDigit}x-]+)");

		try {
			r = new FileReader(filename);
			BufferedReader br = new BufferedReader(r);

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#')
					continue;

				Matcher matcher = pattern.matcher(line);
				matcher.find();
				String mapid = matcher.group(1);

				Area b = new Area(
						Integer.decode(matcher.group(2)),
						Integer.decode(matcher.group(3)),
						Integer.decode(matcher.group(4)),
						Integer.decode(matcher.group(5)));
				SubArea a = new SubArea(b);
				a.setMapid(Integer.parseInt(mapid));
				list.add(a);
			}

			areas = list.toArray(new SubArea[list.size()]);
		} catch (NumberFormatException e) {
			areas = SUB_AREA;
			System.err.println("Bad number in areas list file");
		} finally {
			if (r != null)
				r.close();
		}
	}

	public SubArea[] getAreas() {
		return areas;
	}

	public void dump() {
		System.out.println("Areas read from file");
		for (SubArea a : areas) {
			Area b = a.getBounds();
			System.out.println(a.getMapid() + " " + b.toString());
		}
	}
}
