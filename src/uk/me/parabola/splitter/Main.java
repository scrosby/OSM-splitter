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

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;


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

	public static void main(String[] args) {
		Main m = new Main();
		m.readArgs(args);

		long start = System.currentTimeMillis();

		try {
			m.readFile();
		} catch (IOException e) {
			System.err.println("Error opening or reading file " + e);
		} catch (SAXException e) {
			System.err.println("Error parsing xml from file " + e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		System.out.println("Total time " + (System.currentTimeMillis() - start)/1000 + "s");
	}

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
	}

	private void readFile() throws IOException,
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
		} catch (SAXException e) {
			SubArea totalArea = xmlHandler.getTotalArea();
			AreaSplitter splitter = new AreaSplitter();
			SubArea[] areaList = splitter.split(totalArea, maxNodes);

			writeAreas(areaList);
		}
	}

	/**
	 * Second pass, write out all the files.
	 * @param areaList Area list determined on the first pass.
	 */
	private void writeAreas(SubArea[] areaList) throws
			IOException, SAXException, ParserConfigurationException
	{
		for (SubArea a : areaList)
			a.initForWrite(mapid++, overlapAmount);

		try {
			InputStream is = openFile(filename);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();

			SplitParser xmlHandler = new SplitParser(areaList);
			parser.parse(is, xmlHandler);
		} finally {
			for (SubArea a : areaList)
				a.finishWrite();
		}

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
