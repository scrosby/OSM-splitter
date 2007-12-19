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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads and parses the OSM XML format in a way that is usefull for the splitter.
 *
 * @author Steve Ratcliffe
 */
class OsmXmlHandler extends DefaultHandler {
	private int mode;

	private static final int MODE_NODE = 1;
	private static final int MODE_WAY = 2;

	private MapCollector callbacks;

	private Way currentWay;
	private OsmNode currentNode;

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
				String lat = attributes.getValue("lat");
				String lon = attributes.getValue("lon");

				currentNode = new OsmNode(id, lat, lon);
//				callbacks.addNode(Long.parseLong(id), Float.parseFloat(lat),
//						Float.parseFloat(lon));

			} else if (qName.equals("way")) {
				mode = MODE_WAY;
				currentWay = new Way();
			}
		} else if (mode == MODE_NODE) {
			if (qName.equals("tag")) {
				String key = attributes.getValue("k");
				String val = attributes.getValue("v");

				// We only want to create a full node for nodes that are POI's
				// and not just point of a way.  Only create if it has tags that
				// are not in a list of ignorables ones such as 'created_by'
				if (currentNode != null || !key.equals("created_by")) {

					currentNode.addTag(key, val);
                }
			}

		} else if (mode == MODE_WAY) {
			if (qName.equals("nd")) {
				long id = Long.parseLong(attributes.getValue("ref"));
				currentWay.addNode(id);
			} else if (qName.equals("tag")) {
				String key = attributes.getValue("k");
				String val = attributes.getValue("v");
				currentWay.addTag(key, val);
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
//				if (currentNode != null)
//					converter.convertNode(currentNode);
//				currentNodeId = 0;
				callbacks.addNode(currentNode);
				currentNode = null;
			}

		} else if (mode == MODE_WAY) {
			if (qName.equals("way")) {
				mode = 0;
				currentWay = null;
				// Process the way.
//				converter.convertWay(currentWay);
			}
		}
	}


	public void fatalError(SAXParseException e) throws SAXException {
		System.err.println("Error at line " + e.getLineNumber() + ", col "
				+ e.getColumnNumber());
		super.fatalError(e);
	}

	public void setCallbacks(MapCollector dbWriter) {
		callbacks = dbWriter;
	}
}