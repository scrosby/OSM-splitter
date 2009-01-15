/*
 * Copyright (C) 2007 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: Dec 18, 2007
 */
package uk.me.parabola.splitter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;


/**
 * Splitter for OSM files with the purpose of providing input files for mkgmap.
 *
 * The input file is split so that no piece has more than a given number of nodes in it.
 *
 * @author Steve Ratcliffe
 */
public class Main {

	private String filename;

	// Traditional default, but please use a different one!
	private int mapid = 63240001;

	// The amount in map units that tiles overlap (note that the final img's will not overlap
	// but the input files do).
	private int overlapAmount = 2000;

	// The max number of nodes that will appear in a single file.
	private int maxNodes = 1600000;

	// Set if there is a previous area file given on the command line.
	private AreaList areaList;

	public static void main(String[] args) {
		Main m = new Main();

		long start = System.currentTimeMillis();

		try {
			m.split(args);
		} catch (IOException e) {
			System.err.println("Error opening or reading file " + e);
		} catch (SAXException e) {
			System.err.println("Error parsing xml from file " + e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		System.out.println("Total time " + (System.currentTimeMillis() - start)/1000 + "s");
	}

	private void split(String[] args) throws IOException, SAXException, ParserConfigurationException {
		readArgs(args);
		if (areaList == null)
			calculateAndSplit();
		else
			justSplit();

		writeArgsFile();
	}

	/**
	 * Deal with the command line arguments.
	 */
	private void readArgs(String[] args) {
		Properties props = new Properties();

		for (String arg : args) {
			if (arg.startsWith("--")) {
				Pattern pattern = Pattern.compile("--(.*)=(.*)");
				Matcher m = pattern.matcher(arg);
				if (m.find()) {
					String key = m.group(1);
					String val = m.group(2);
					System.out.printf("key %s/ val %s\n", key, val);
					props.setProperty(key, val);
				}
			} else {
				filename = arg;
			}
		}

		EnhancedProperties config = new EnhancedProperties(props);

		mapid = config.getProperty("mapname", mapid);
		overlapAmount = config.getProperty("overlap", overlapAmount);
		maxNodes = config.getProperty("max-nodes", maxNodes);

		if (config.containsKey("split-file")) {
			String splitFile = config.getProperty("split-file");
			try {
				areaList = new AreaList();
				areaList.read(splitFile);
				areaList.dump();
			} catch (IOException e) {
				areaList = null;
				System.err.println("Could not read area list file " + e);
			}
		}
	}

	/**
	 * Calculate the areas that we are going to split into by getting the total area and
	 * then subdividing down until each area has at most max-nodes nodes in it.
	 */
	private void calculateAndSplit() throws IOException,
			SAXException, ParserConfigurationException
	{
		if (filename == null)
			throw new FileNotFoundException("No filename given");

		InputStream is = openFile(filename);
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();

		DivisionParser xmlHandler = new DivisionParser();

		try {
			// First pass, read nodes and split into areas.
			parser.parse(is, xmlHandler);
		} catch (EndOfNodesException e) {
			// Now split the area up
			SubArea totalArea = xmlHandler.getTotalArea();
			AreaSplitter splitter = new AreaSplitter();
			areaList = splitter.split(totalArea, maxNodes);

			// Set the mapid's
			for (SubArea a : areaList)
				a.setMapid(mapid++);

			areaList.write("areas.list");

			// Finally write it out, this re-reads the file from scratch.
			writeAreas(areaList);
		}
	}

	/**
	 * Just split based on a previously prepared area list file.
	 */
	private void justSplit() throws IOException, SAXException, ParserConfigurationException {
		writeAreas(areaList);
	}

	/**
	 * Second pass, we have the areas so read in the file again and write out each element
	 * to the files that should contain it.
	 * @param areaList Area list determined on the first pass.
	 */
	private void writeAreas(AreaList areaList) throws
			IOException, SAXException, ParserConfigurationException
	{
		for (SubArea a : areaList)
			a.initForWrite(overlapAmount);

		try {
			InputStream is = openFile(filename);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();

			SplitParser xmlHandler = new SplitParser(areaList.getAreas());
			parser.parse(is, xmlHandler);
		} finally {
			for (SubArea a : areaList)
				a.finishWrite();
		}

	}

	/**
	 * Write a file that can be given to mkgmap that contains the correct arguments
	 * for the split file pieces.  You are encouraged to edit the file and so it
	 * contains a template of all the arguments that you might want to use.
	 */
	public void writeArgsFile() {
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
		for (SubArea a : areaList) {
			w.println();
			w.format("mapname: %d\n", a.getMapid());
			w.println("description: OSM Map");
			w.format("input-file: %d.osm.gz\n", a.getMapid());
		}

		w.println();
		w.close();
	}

	/**
	 * Open a file and apply filters necessary to reading it such as decompression.
	 *
	 * @param name The file to open.
	 * @return A stream that will read the file, positioned at the beginning.
	 * @throws FileNotFoundException If the file cannot be opened for any reason.
	 */
	private InputStream openFile(String name) throws FileNotFoundException {
		InputStream is = new FileInputStream(name);
		if (name.endsWith(".gz")) {
			try {
				is = new GZIPInputStream(is);
			} catch (IOException e) {
				throw new FileNotFoundException( "Could not read as compressed file");
			}
		}
		return is;
	}

}
