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
 * Create date: 20-Dec-2008
 */
package uk.me.parabola.splitter;

/**
 * A miniture set of integers all held in a single int variable.
 *
 * @author Steve Ratcliffe
 */
public class MiniIntSet {
	private int values;

	public MiniIntSet(int values) {
		this.values = values;
	}

	/**
	 * Add to the set.  In reality the vast number of nodes and ways are only
	 * in the one area and so the other cases don't have to be exceptionally
	 * quick.
	 * @param val The byte sized value to add.
	 */
	public void add(int val) {
		assert (val & ~0xff) == 0;
		if (values == val) {
			// nothing to do
		} else if (values == 0) {
			values = val;
		} else {

		}
	}
}
