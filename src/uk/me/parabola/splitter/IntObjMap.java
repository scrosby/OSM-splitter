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
 * Create date: 08-Dec-2008
 */
package uk.me.parabola.splitter;

public class IntObjMap<V> {
	private static final int INIT_SIZE = 1 << 16;

	private int size;
	private int[] keys;
	private V[] values;

	private int capacity;

	private int targetSize;
	private final float loadFactor;

	private static final int OFF = 7;

	public IntObjMap() {
		this(INIT_SIZE, 0.9f);
	}

	public IntObjMap(int initCap, float load) {
		keys = new int[initCap];
		values = (V[]) new Object[initCap];
		capacity = initCap;

		loadFactor = load;
		targetSize = (int) (initCap * load);
		assert targetSize > 0;
	}


	public int size() {
		return size;
	}

	public V get(int key) {
		Integer ind = keyPos(key);
		if (ind == null)
			return null;

		return values[ind];
	}

	public V put(int key, V value) {
		ensureSpace();

		int ind = keyPos(key);
		keys[ind] = key;

		V old = values[ind];
		if (old == null)
			size++;
		values[ind] = value;

		return old;
	}

	private void ensureSpace() {
		while (size + 1 >= targetSize) {
			int ncap = capacity * 3 / 2;
			targetSize = (int) (ncap * loadFactor);

			int[] okey = keys;
			V[] oval = values;

			size = 0;
			keys = new int[ncap];
			values = (V[]) new Object[ncap];
			capacity = ncap;
			for (int i = 0; i < okey.length; i++) {
				int k = okey[i];
				if (k != 0)
					put(k, oval[i]);
			}
		}
		assert size < capacity;
	}

	private int keyPos(int key) {
		int k = key & (capacity - 1);

		int h1 = keys[k];
		if (h1 != 0 && h1 != key) {
			for (int k2 = k+OFF; ; k2+= OFF) {
				if (k2 >= capacity) {
					k2 -= capacity;
				}
				int fk = keys[k2];
				if (fk == 0 || fk == key) {
					return k2;
				}
			}
		}
		return k;
	}
}