/*
 * Copyright (C) 2006 Steve Ratcliffe
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
 * Create date: 16-Dec-2006
 */
package uk.me.parabola.splitter;

import uk.me.parabola.imgfmt.app.Coord;
import uk.me.parabola.mkgmap.general.MapDetails;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads and parses the OSM XML format in a way that is useful for the splitter.
 *
 * @author Steve Ratcliffe
 */
class OsmXmlHandler extends DefaultHandler {
	private int mode;

	private static final int MODE_NODE = 1;

	private Long2IntOpenHashMap lats = new Long2IntOpenHashMap(100000, 0.8f);
	private Long2IntOpenHashMap lons = new Long2IntOpenHashMap(100000, 0.8f);

	private MapDetails details = new MapDetails();

	OsmXmlHandler() {
		lats.growthFactor(8);
		lons.growthFactor(8);
	}

	/**
	 * Receive notification of the start of an element.
	 *
	 * @param uri The Namespace URI, or the empty string if the
	 * element has no Namespace URI or if Namespace
	 * processing is not being performed.
	 * @param localName The local name (without prefix), or the
	 * empty string if Namespace processing is not being
	 * performed.
	 * @param qName The qualified name (with prefix), or the
	 * empty string if qualified names are not available.
	 * @param attributes The attributes attached to the element.  If
	 * there are no attributes, it shall be an empty
	 * Attributes object.
	 * @throws SAXException Any SAX exception, possibly
	 * wrapping another exception.
	 * @see ContentHandler#startElement
	 */
	public void startElement(String uri, String localName,
			String qName, Attributes attributes)
			throws SAXException
	{

		if (mode == 0) {
			if (qName.equals("node")) {
				mode = MODE_NODE;

				String id = attributes.getValue("id");
				String slat = attributes.getValue("lat");
				String slon = attributes.getValue("lon");

				double lat = Double.parseDouble(slat);
				double lon = Double.parseDouble(slon);
				Coord coord = new Coord(lat, lon);
				
				lats.put(Long.parseLong(id), coord.getLatitude());
				lons.put(Long.parseLong(id), coord.getLongitude());

				details.addToBounds(coord);

			} else if (qName.equals("way")) {
				throw new SAXException("end of nodes");
			}
		}
	}

	/**
	 * Receive notification of the end of an element.
	 *
	 * @param uri The Namespace URI, or the empty string if the
	 * element has no Namespace URI or if Namespace
	 * processing is not being performed.
	 * @param localName The local name (without prefix), or the
	 * empty string if Namespace processing is not being
	 * performed.
	 * @param qName The qualified name (with prefix), or the
	 * empty string if qualified names are not available.
	 * @throws SAXException Any SAX exception, possibly
	 * wrapping another exception.
	 * @see ContentHandler#endElement
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException
	{
		if (mode == MODE_NODE) {
			if (qName.equals("node")) {
				mode = 0;
			}

		}
	}

	public SubArea getTotalArea() {
		SubArea sub = new SubArea(details.getBounds(), lats, lons);
		lats = null;
		lons = null;
		return sub;
	}


	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println("Error at line " + e.getLineNumber() + ", col "
				+ e.getColumnNumber());
		super.fatalError(e);
	}
}