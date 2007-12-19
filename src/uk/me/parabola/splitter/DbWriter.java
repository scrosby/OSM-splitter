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
 * Create date: Dec 19, 2007
 */
package uk.me.parabola.splitter;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.bind.tuple.TupleBinding;

/**
 * @author Steve Ratcliffe
 */
public class DbWriter implements MapCollector {
	private OsmDatabase database;

	private TupleBinding longBinder = TupleBinding.getPrimitiveBinding(Long.class);
	private TupleBinding intBinder = TupleBinding.getPrimitiveBinding(Integer.class);

	public DbWriter(OsmDatabase db) {
		this.database = db;
	}

	public void addNode(OsmNode node) {
		Database db = database.getNodeDb();

		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();

		longBinder.objectToEntry(Long.valueOf(node.getId()), key);
		intBinder.objectToEntry(Integer.valueOf(1), value);

		try {
			db.put(null, key, value);
		} catch (DatabaseException e) {
			throw new WriteException(e);
		}
	}
}
