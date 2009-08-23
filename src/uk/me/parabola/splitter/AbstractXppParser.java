package uk.me.parabola.splitter;

import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
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

	protected void setReader(Reader reader) throws XmlPullParserException {
		parser.setInput(reader);
	}

	protected String getAttr(String name) {
		return parser.getAttributeValue(null, name);
	}

	protected int getIntAttr(String name) {
		return Integer.parseInt(parser.getAttributeValue(null, name));
	}

	protected void parse() throws IOException, XmlPullParserException {
		boolean done = false;
		int eventType = parser.getEventType();
		do {
			if (eventType == XmlPullParser.START_TAG) {
				done = startElement(parser.getName());
			} else if (eventType == XmlPullParser.END_TAG) {
				endElement(parser.getName());
			}
		}
		while (!done && (eventType = parser.next()) != XmlPullParser.END_DOCUMENT);
	}

	abstract protected boolean startElement(String name) throws XmlPullParserException;

	abstract protected void endElement(String name) throws XmlPullParserException;
}
