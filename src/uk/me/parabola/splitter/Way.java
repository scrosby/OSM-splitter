/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  as published by the Free Software Foundation.
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

/**
 * @author Steve Ratcliffe
 */
public class Way extends Element {
	private final IntList refs = new IntList(10);

	public void set(int id) {
		setId(id);
	}

	@Override
	public void reset() {
		super.reset();
		refs.clear();
	}

	public void addRef(int ref) {
		refs.add(ref);
	}

	public IntList getRefs() {
		return refs;
	}
}
