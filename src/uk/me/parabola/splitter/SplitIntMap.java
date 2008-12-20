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
 * An int/int map that is split into several smaller maps.  This is to avoid the problem where
 * rehashing a map temporarily consumes twice as much memory as is actually required.
 * We only ever have to rehash one of the sub maps at a time and therefore much less
 * temporary space is required.
 *
 * It is all a balance though, there is an overhead in each map because of the load factor.
 * Also it is going to be slower overall.  So splitting into 4 seems about right.
 * 
 * @author Steve Ratcliffe
 */
public class SplitIntMap {
	private static final int NMAPS = 4;
	public static final int MASK = 0x3;

	private static final int INIT_CAP = 100000;
	private static final float LOAD = 0.9f;

	private int size;

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

	public int get(int key) {
		return maps[key & MASK].get(key);
	}

	public int size() {
		if (this.size != 0)
			return size;

		int size = 0;
		for (int i = 0; i < NMAPS; i++) {
			size += maps[i].size();
		}
		return size;
	}

	private void fixSize() {
		this.size = size();
	}

	public ObjectIterator<Int2IntMap.Entry> fastIterator() {
		return new NormalObjectIterator();
	}

	/**
	 * The deleting iterator takes case of the case where you are transfering the entries from
	 * one map to another.  Once they are read, they are no longer needed and so the map
	 * can be freed.  This avoids avoids using double memory when splitting the areas.
	 * @return
	 */
	public ObjectIterator<Int2IntMap.Entry> fastDeletingIterator() {
		return new NormalObjectIterator(true);
	}

	/**
	 * Trim the map down to its minimum size.  This can be used when we are not going to
	 * add to the map any more to reduce the overhead of having serveral sub-maps.
	 */
	public void trim() {
		for (int i = 0; i < NMAPS; i++) {
			maps[i].trim();
		}
	}

	/**
	 * Iterates over all the sub-maps.
	 */
	private class NormalObjectIterator implements ObjectIterator<Int2IntMap.Entry> {

		private boolean deleteAfter;

		private ObjectIterator[] iterators = new ObjectIterator[NMAPS];

		private int currentMap;

		private NormalObjectIterator(boolean deleteAfter) {
			if (deleteAfter)
				fixSize();
			
			this.deleteAfter = deleteAfter;
			for (int i = 0; i < NMAPS; i++) {
				iterators[i] = maps[i].int2IntEntrySet().fastIterator();
			}
		}

		private NormalObjectIterator() {
			this(false);
		}

		public int skip(int n) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Note that you have to call this for the call to next() to be correct
		 * and so it doesn' properly follow the contract for hasNext() on the
		 * regular Iterator.
		 */
		public boolean hasNext() {
			// All done
			if (currentMap >= NMAPS)
				return false;

			// easy case:
			if (iterators[currentMap].hasNext())
				return true;

			// Else step to the next one and try it
			if (deleteAfter) {
				iterators[currentMap] = null;
				maps[currentMap] = null;
			}
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
