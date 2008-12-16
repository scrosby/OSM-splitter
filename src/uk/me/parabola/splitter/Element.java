/*
 * Copyright (C) 2008 Steve Ratcliffe
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
 * Create date: 16-Dec-2008
 */
package uk.me.parabola.splitter;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author Steve Ratcliffe
 */
public class Element {
	private Object2ObjectArrayMap<String, String> tags;
	protected String stringId;

	public Element(String stringId) {
		this.stringId = stringId;
	}

	public void addTag(String key, String value) {
		if (key.equals("created_by"))
			return;
		
		if (tags == null)
			tags = new Object2ObjectArrayMap<String, String>(8);
		tags.put(key, value);
	}

	public boolean hasTags() {
		return tags != null && !tags.isEmpty();
	}

	public ObjectIterator<Object2ObjectMap.Entry<String,String>> tagsIterator() {
		return tags.object2ObjectEntrySet().fastIterator();
	}

	public String getStringId() {
		return stringId;
	}
}
