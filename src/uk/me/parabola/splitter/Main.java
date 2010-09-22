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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.me.parabola.splitter.args.ParamParser;
import uk.me.parabola.splitter.args.SplitterParams;
import uk.me.parabola.splitter.geo.City;
import uk.me.parabola.splitter.geo.CityFinder;
import uk.me.parabola.splitter.geo.CityLoader;
import uk.me.parabola.splitter.geo.DefaultCityFinder;
import uk.me.parabola.splitter.geo.DummyCityFinder;

import org.xmlpull.v1.XmlPullParserException;

import crosby.binary.file.BlockInputStream;

/**
 * Splitter for OSM files with the purpose of providing input files for mkgmap.
 * <p/>
 * The input file is split so that no piece has more than a given number of nodes in it.
 *
 * @author Steve Ratcliffe
 */
public class Main {

	// We can only process a maximum of 255 areas at a time because we
	// compress an area ID into 8 bits to save memory (and 0 is reserved)
	private int maxAreasPerPass = 255;

	private List<String> filenames;

	// The description to write into the template.args file.
	private String description;

	// Traditional default, but please use a different one!
	private int mapId = 63240001;

	// The amount in map units that tiles overlap (note that the final img's will not overlap
	// but the input files do).
	private int overlapAmount = 2000;

	// The max number of nodes that will appear in a single file.
	private int maxNodes = 1600000;

	// The maximum resolution of the map to be produced by mkgmap. This is a value in the range
	// 0-24. Higher numbers mean higher detail. The resolution determines how the tiles must
	// be aligned. Eg a resolution of 13 means the tiles need to have their edges aligned to
	// multiples of 2 ^ (24 - 13) = 2048 map units, and their widths and heights must be a multiple
	// of 2 * 2 ^ (24 - 13) = 4096 units. The tile widths and height multiples are double the tile
	// alignment because the center point of the tile is stored, and that must be aligned the
	// same as the tile edges are.
	private int resolution = 13;

	// Whether or not to trim tiles of any empty space around their edges.
	private boolean trim;

	// Set if there is a previous area file given on the command line.
	private AreaList areaList;
	private boolean mixed;
	// A GeoNames file to use for naming the tiles.
	private String geoNamesFile;

	private boolean densityMap;

	private String kmlOutputFile;
	
	private int maxThreads;

	private SplitterParams params;

	public static void main(String[] args) {

		Main m = new Main();
		m.start(args);
	}

	private void start(String[] args) {
		readArgs(args);
		if (params.getStatusFreq() > 0) {
			JVMHealthMonitor.start(params.getStatusFreq());
		}
		long start = System.currentTimeMillis();
		System.out.println("Time started: " + new Date());
		try {
			split();
		} catch (IOException e) {
			System.err.println("Error opening or reading file " + e);
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			System.err.println("Error parsing xml from file " + e);
			e.printStackTrace();
		}
		System.out.println("Time finished: " + new Date());
		System.out.println("Total time taken: " + (System.currentTimeMillis() - start) / 1000 + 's');
	}

	private void split() throws IOException, XmlPullParserException {
		if (filenames.isEmpty()) {
			throw new IllegalArgumentException("No .osm files were supplied and no --cache parameter was specified to load data from");
		}

		if (areaList == null) {
			int alignment = 1 << (24 - resolution);
			System.out.println("Map is being split for resolution " + resolution + ':');
			System.out.println(" - area boundaries are aligned to 0x" + Integer.toHexString(alignment) + " map units");
			System.out.println(" - areas are multiples of 0x" + Integer.toHexString(alignment * 2) + " map units wide and high");
			areaList = calculateAreas();
			for (Area area : areaList.getAreas()) {
				area.setMapId(mapId++);
			}
			areaList.write("areas.list");
		}

		nameAreas();
		System.out.println(areaList.getAreas().size() + " areas:");
		for (Area area : areaList.getAreas()) {
			System.out.print("Area " + area.getMapId() + " covers " + area.toHexString());
			if (area.getName() != null)
				System.out.print(' ' + area.getName());
			System.out.println();
		}


		if (kmlOutputFile != null) {
			System.out.println("Writing KML file to " + kmlOutputFile);
			areaList.writeKml(kmlOutputFile);
		}

		writeAreas(areaList.getAreas());
		writeArgsFile(areaList.getAreas());
	}

	/**
	 * Deal with the command line arguments.
	 */
	private void readArgs(String[] args) {
		ParamParser parser = new ParamParser();
		params = parser.parse(SplitterParams.class, args);

		if (!parser.getErrors().isEmpty()) {
			System.out.println();
			System.out.println("Invalid parameter(s):");
			for (String error : parser.getErrors()) {
				System.out.println("  " + error);
			}
			System.out.println();
			parser.displayUsage();
			System.exit(-1);
		}

		for (Map.Entry<String, Object> entry : parser.getConvertedParams().entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			System.out.println(name + '=' + (value == null ? "" : value));
		}

		mapId = params.getMapid();
		overlapAmount = params.getOverlap();
		maxNodes = params.getMaxNodes();
		description = params.getDescription();
		geoNamesFile = params.getGeonamesFile();
		resolution = params.getResolution();
		trim = !params.isNoTrim();
		if (resolution < 1 || resolution > 24) {
			System.err.println("The --resolution parameter must be a value between 1 and 24. Resetting to 13.");
			resolution = 13;
		}
		mixed = params.isMixed();
		maxAreasPerPass = params.getMaxAreas();
		if (maxAreasPerPass < 1 || maxAreasPerPass > 20000) {
			System.err.println("The --max-areas parameter must be a value between 1 and 255. Resetting to 255.");
			maxAreasPerPass = 255;
		}
		kmlOutputFile = params.getWriteKml();
		densityMap = !params.isLegacyMode();
		if (!densityMap) {
			System.out.println("WARNING: Specifying --legacy-split will cause the first stage of the split to take much more memory! This option is considered deprecated and will be removed in a future build.");
		}

		maxThreads = params.getMaxThreads().getCount();
		filenames = parser.getAdditionalParams();

		String splitFile = params.getSplitFile();
		if (splitFile != null) {
			try {
				areaList = new AreaList();
				areaList.read(splitFile);
				areaList.dump();
			} catch (IOException e) {
				areaList = null;
				System.err.println("Could not read area list file");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Calculate the areas that we are going to split into by getting the total area and
	 * then subdividing down until each area has at most max-nodes nodes in it.
	 */
	private AreaList calculateAreas() throws IOException, XmlPullParserException {

		MapCollector nodes = densityMap ? new DensityMapCollector(trim, resolution) : new NodeCollector();
		MapProcessor processor = nodes;
		//MapReader mapReader = 
		processMap(processor);
		//System.out.print("A total of " + Utils.format(mapReader.getNodeCount()) + " nodes, " +
		//				Utils.format(mapReader.getWayCount()) + " ways and " +
		//				Utils.format(mapReader.getRelationCount()) + " relations were processed ");

		//System.out.println("in " + filenames.size() + (filenames.size() == 1 ? " file" : " files"));
		//System.out.println("Min node ID = " + mapReader.getMinNodeId());
		//System.out.println("Max node ID = " + mapReader.getMaxNodeId());

		System.out.println("Time: " + new Date());

		Area exactArea = nodes.getExactArea();
		SplittableArea splittableArea = nodes.getRoundedArea(resolution);
		System.out.println("Exact map coverage is " + exactArea);
		System.out.println("Trimmed and rounded map coverage is " + splittableArea.getBounds());
		System.out.println("Splitting nodes into areas containing a maximum of " + Utils.format(maxNodes) + " nodes each...");

		List<Area> areas = splittableArea.split(maxNodes);
		return new AreaList(areas);
	}

	private void nameAreas() throws IOException {
		CityFinder cityFinder;
		if (geoNamesFile != null) {
			CityLoader cityLoader = new CityLoader(true);
			List<City> cities = cityLoader.load(geoNamesFile);
			cityFinder = new DefaultCityFinder(cities);
		} else {
			cityFinder = new DummyCityFinder();
		}

		for (Area area : areaList.getAreas()) {
			// Decide what to call the area
			Set<City> found = cityFinder.findCities(area);
			City bestMatch = null;
			for (City city : found) {
				if (bestMatch == null || city.getPopulation() > bestMatch.getPopulation()) {
					bestMatch = city;
				}
			}
			if (bestMatch != null)
				area.setName(bestMatch.getCountryCode() + '-' + bestMatch.getName());
			else
				area.setName(description);
		}
	}

	/**
	 * Second pass, we have the areas so parse the file(s) again and write out each element
	 * to the file(s) that should contain it.
	 *
	 * @param areaList Area list determined on the first pass.
	 */
	private void writeAreas(List<Area> areas) throws IOException, XmlPullParserException {
		System.out.println("Writing out split osm files " + new Date());

		int passesRequired = (int) Math.ceil((double) areas.size() / (double) maxAreasPerPass);
		maxAreasPerPass = (int) Math.ceil((double) areas.size() / (double) passesRequired);

		if (passesRequired > 1) {
			System.out.println("Processing " + areas.size() + " areas in " + passesRequired + " passes, " + maxAreasPerPass + " areas at a time");
		} else {
			System.out.println("Processing " + areas.size() + " areas in a single pass");
		}

		for (int i = 0; i < passesRequired; i++) {
			OSMWriter[] currentWriters = new OSMWriter[Math.min(maxAreasPerPass, areas.size() - i * maxAreasPerPass)];
			for (int j = 0; j < currentWriters.length; j++) {
				Area area = areas.get(i * maxAreasPerPass + j);
				currentWriters[j] = new OSMWriter(area);
				currentWriters[j].initForWrite(area.getMapId(), overlapAmount);
			}

			System.out.println("Starting pass " + (i + 1) + " of " + passesRequired + ", processing " + currentWriters.length +
							" areas (" + areas.get(i * maxAreasPerPass).getMapId() + " to " +
							areas.get(i * maxAreasPerPass + currentWriters.length - 1).getMapId() + ')');

			MapProcessor processor = new SplitProcessor(currentWriters, maxThreads);
			processMap(processor);
			//System.out.println("Wrote " + Utils.format(mapReader.getNodeCount()) + " nodes, " +
			//				Utils.format(mapReader.getWayCount()) + " ways, " +
			//				Utils.format(mapReader.getRelationCount()) + " relations");
		}
	}
	
	private void processMap(MapProcessor processor) throws XmlPullParserException {
	// Create both an XML reader and a binary reader, Dispatch each input to the
    // Appropriate parser.
    OSMParser parser = new OSMParser(processor, mixed);
    for (String filename : filenames) {
      System.out.println("Processing " + filename);
      try {
        if (filename.endsWith(".bin")) {
          // Is it a binary file?
          File file = new File(filename);
          BlockInputStream blockinput = (new BlockInputStream(
              new FileInputStream(file), new BinaryMapParser(processor)));
          try {
            blockinput.process();
          } finally {
            blockinput.close();
          }
        } else {
          // No, try XML.
	  Reader reader = Utils.openFile(filename, maxThreads > 1);
          parser.setReader(reader);
          try {
            parser.parse();
          } finally {
            reader.close();
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    processor.endMap();
  }
	
	/**
	 * Write a file that can be given to mkgmap that contains the correct arguments
	 * for the split file pieces.  You are encouraged to edit the file and so it
	 * contains a template of all the arguments that you might want to use.
	 */
	protected void writeArgsFile(List<Area> areas) {
		PrintWriter w;
		try {
			w = new PrintWriter(new FileWriter("template.args"));
		} catch (IOException e) {
			System.err.println("Could not write template.args file");
			return;
		}

		w.println("#");
		w.println("# This file can be given to mkgmap using the -c option");
		w.println("# Please edit it first to add a description of each map.");
		w.println("#");
		w.println();

		w.println("# You can set the family id for the map");
		w.println("# family-id: 980");
		w.println("# product-id: 1");

		w.println();
		w.println("# Following is a list of map tiles.  Add a suitable description");
		w.println("# for each one.");
		for (Area a : areas) {
			w.println();
			w.format("mapname: %d\n", a.getMapId());
			if (a.getName() == null)
				w.println("# description: OSM Map");
			else
				w.println("description: " + a.getName());
			w.format("input-file: %d.osm.gz\n", a.getMapId());
		}

		w.println();
		w.close();
	}
}
