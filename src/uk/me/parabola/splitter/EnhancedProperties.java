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
 * Create date: May 17, 2008
 */
package uk.me.parabola.splitter;

import java.util.Properties;

/**
 * Wrapper that behaves as an enhanced properties class that has getProperty
 * calls for many different data types.
 *
 * @author Steve Ratcliffe
 */
public class EnhancedProperties {
	private final Properties props;

	public EnhancedProperties(Properties props) {
		this.props = props;
	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public String getProperty(String key, String def) {
		return props.getProperty(key, def);
	}

	public int getProperty(String key, int def) {
		try {
			String s = props.getProperty(key);
			return s == null ? def : Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public boolean getProperty(String key, boolean def) {
		String s = props.getProperty(key);
		if (s != null) {
			char c = s.toLowerCase().charAt(0);
			if (c == '1' || c == 'y' || c == 't')
				return true;
			else
				return false;
		}
		return def;
	}
}
