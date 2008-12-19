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
 * Create date: 19-Dec-2008
 */
package uk.me.parabola.splitter;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author Steve Ratcliffe
 */
public class SplitIntMap {
	private static final int NMAPS = 4;
	public static final int MASK = 0x3;

	private static final int INIT_CAP = 100000;
	private static final float LOAD = 0.9f;

	private Int2IntOpenHashMap[] maps = new Int2IntOpenHashMap[NMAPS];

	public SplitIntMap() {
		for (int i = 0; i < NMAPS; i++) {
			maps[i] = new Int2IntOpenHashMap(INIT_CAP, LOAD);
			maps[i].growthFactor(4);
		}
	}


	public void put(int key, int value) {
		maps[key & MASK].put(key, value);
	}

	public int size() {
		int size = 0;
		for (int i = 0; i < NMAPS; i++) {
			size += maps[i].size();
		}
		return size;
	}

	public ObjectIterator<Int2IntMap.Entry> fastIterator() {
		return new NormalObjectIterator();
	}

	private class NormalObjectIterator implements ObjectIterator<Int2IntMap.Entry> {
		private Int2IntMap.Entry entry;

		private ObjectIterator[] iterators = new ObjectIterator[NMAPS];

		private int currentMap;

		private NormalObjectIterator() {
			for (int i = 0; i < NMAPS; i++) {
				iterators[i] = maps[i].int2IntEntrySet().fastIterator();
			}
		}

		public int skip(int n) {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			// All done
			if (currentMap >= NMAPS)
				return false;

			// easy case:
			if (iterators[currentMap].hasNext())
				return true;

			// Else step to the next one and try it
			currentMap++;
			return hasNext();
		}

		public Int2IntMap.Entry next() {
			return (Int2IntMap.Entry) iterators[currentMap].next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
