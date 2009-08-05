/**
 * Copyright © 2009 Chris Miller
 */
package uk.me.parabola.splitter;

import java.io.Reader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Base functionality for an XPP based XML parser
 */
public abstract class AbstractXppParser {
	private final XmlPullParser parser;

	public AbstractXppParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
		parser = factory.newPullParser();
	}

	public XmlPullParser getParser() {
		return parser;
	}

	public String getAttr(String name) {
		return parser.getAttributeValue(null, name);
	}

	public void setReader(Reader reader) throws XmlPullParserException {
		parser.setInput(reader);
	}

	public void parse() throws IOException, XmlPullParserException {
		boolean done = false;
		int eventType = parser.getEventType();
		do {
			if (eventType == XmlPullParser.START_TAG) {
				done = startElement();
			} else if (eventType == XmlPullParser.END_TAG) {
				endElement();
			}
		}
		while (!done && (eventType = parser.next()) != XmlPullParser.END_DOCUMENT);
	}

	abstract protected boolean startElement() throws XmlPullParserException;

	abstract protected void endElement() throws XmlPullParserException;
}
