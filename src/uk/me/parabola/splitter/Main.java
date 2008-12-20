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
import java.util.zip.GZIPInputStream;


/**
 * Splitter for OSM files with the purpose of providing input files for mkgmap.
 *
 * The input file is split so that no piece has more than a given number of nodes in it.
 *
 * @author Steve Ratcliffe
 */
public class Main {
	public static void main(String[] args) {
		Main m = new Main();
		long start = System.currentTimeMillis();
		try {
			String filename = args[0];
			System.out.println("Reading file " + filename);
			m.readFile(filename);
		} catch (IOException e) {
			System.err.println("Error opening or reading file " + e);
		} catch (SAXException e) {
			System.err.println("Error parsing xml from file " + e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		System.out.println("Total time " + (System.currentTimeMillis() - start)/1000 + "s");
	}

	private void readFile(String filename) throws IOException,
			SAXException, ParserConfigurationException
	{
		InputStream is = openFile(filename);
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();

		DivisionParser xmlHandler = new DivisionParser();

		try {
			parser.parse(is, xmlHandler);
		} catch (SAXException e) {
			SubArea totalArea = xmlHandler.getTotalArea();
			AreaSplitter splitter = new AreaSplitter();
			SubArea[] areaList = splitter.split(totalArea, 1500000);

			writeAreas(areaList, filename);
		}
	}

	private void writeAreas(SubArea[] areaList, String filename) throws
			IOException, SAXException, ParserConfigurationException
	{
		int mapid = 1;
		for (SubArea a : areaList) 
			a.initForWrite(mapid++, 1000);

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
