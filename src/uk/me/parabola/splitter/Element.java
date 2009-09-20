/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.splitter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Steve Ratcliffe
 */
public class Element {
	private Map<String, String> tags = new HashMap<String, String>(8);
	private int id;

	protected void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void reset() {
		this.id = 0;
		tags.clear();
	}

	public void addTag(String key, String value) {
		if (key.equals("created_by"))
			return;
		tags.put(key, value);
	}

	public boolean hasTags() {
		return tags != null && !tags.isEmpty();
	}

	public Iterator<Map.Entry<String, String>> tagsIterator() {
		return tags.entrySet().iterator();
	}
}
