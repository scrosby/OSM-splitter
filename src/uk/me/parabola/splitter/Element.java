/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 or
 *  version 3 as published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 16-Dec-2008
 */
package uk.me.parabola.splitter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Steve Ratcliffe
 */
public class Element {
	private Map<String, String> tags;
	protected final String stringId;

	public Element(String stringId) {
		this.stringId = stringId;
	}

	public int getId() {
		return Integer.parseInt(stringId);
	}
	
	public void addTag(String key, String value) {
		if (key.equals("created_by"))
			return;
		
		if (tags == null)
			tags = new HashMap<String, String>(8);
		tags.put(key, value);
	}

	public boolean hasTags() {
		return tags != null && !tags.isEmpty();
	}

	public Iterator<Map.Entry<String,String>> tagsIterator() {
		return tags.entrySet().iterator();
	}

	public String getStringId() {
		return stringId;
	}
}
