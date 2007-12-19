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


/**
 * @author Steve Ratcliffe
 */
public class Main {
	public static void main(String[] args) {
		Main m = new Main();
		m.start();
	}

	private void start() {
		OsmDatabase db = new OsmDatabase();

		OsmXmlHandler xmlHandler = new OsmXmlHandler();
		xmlHandler.setCallbacks(new DbWriter(db));
	}
}
