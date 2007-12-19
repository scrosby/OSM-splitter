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

import java.io.File;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * In Java DB an environment is what you would usually call a database and
 * what they call a database is more similar to a table in other contexts.
 *
 * In any case a Database is just like a persistant hashmap, there is a key and
 * a value.
 * 
 * @author Steve Ratcliffe
 */
public class OsmDatabase {
	private Environment env;

	private Database nodeDb;
	private Database waydb;

	private boolean writeable;

	public void init(boolean write) {
		writeable = write;

		EnvironmentConfig config = new EnvironmentConfig();
		config.setAllowCreate(write);
		try {
			env = new Environment(new File("db"), config);
			DatabaseConfig dbconf = new DatabaseConfig();
			dbconf.setAllowCreate(write);
			dbconf.setDeferredWrite(true);

			nodeDb = env.openDatabase(null, "nodes", dbconf);
			waydb = env.openDatabase(null, "ways", dbconf);

		} catch (DatabaseException e) {
			System.err.println("Could not open database environment " + e);
			System.exit(1);
		}
	}

	public void sync() throws DatabaseException {
		if (!writeable)
			return;

		nodeDb.sync();
		waydb.sync();
	}

	public void close() {
		if (env == null)
			return;

		try {
			nodeDb.close();
			waydb.close();

			env.close();
		} catch (DatabaseException e) {
			// ignore
		}
	}

	public Database getNodeDb() {
		return nodeDb;
	}
}
