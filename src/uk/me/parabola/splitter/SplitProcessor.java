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

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import uk.me.parabola.splitter.Relation.Member;

/**
 * Splits a map into multiple areas.
 */
class SplitProcessor implements MapProcessor {

	private final SplitIntMap coords = new SplitIntMap();
	private final SplitIntMap ways = new SplitIntMap();
	private final IntObjMap<long[]> bigWays = new IntObjMap<long[]>();

	private final OSMWriter[] writers;
	private final BlockingQueue<Element>[] writerInputQueues;
	private final BlockingQueue<InputQueueInfo> writerInputQueue;
	private final ArrayList<Thread> workerThreads;

	private int currentNodeAreaSet;
	private BitSet currentWayAreaSet;
	private BitSet currentRelAreaSet;
	
	private final int maxThreads;

	TreeMap<Integer,ArrayList<OSMWriter>> writermap = new TreeMap<Integer,ArrayList<OSMWriter>>();
	HashMap<OSMWriter,Integer> writerToID = new HashMap<OSMWriter,Integer>();
	
	void makeWriterMap() {
		for (int i = 0 ; i < writers.length ; i++) {
			writerToID.put(writers[i],i);
		}
		
		for (OSMWriter w : writers) {
			writermap.put(w.getExtendedBounds().getMinLat(),new ArrayList<OSMWriter>());
			writermap.put(w.getExtendedBounds().getMaxLat(),new ArrayList<OSMWriter>());
		}
		for (OSMWriter w: writers) {
			int minlat = w.getExtendedBounds().getMinLat();
			int maxlat = w.getExtendedBounds().getMaxLat();
			for (Integer i = minlat ; i != null && i <= maxlat;  i = writermap.higherKey(i)) {
				writermap.get(i).add(w);
			}
		}

	}

	SplitProcessor(OSMWriter[] writers, int maxThreads) {
		this.writers = writers;
		makeWriterMap();
		this.maxThreads = maxThreads;
		this.writerInputQueue = new ArrayBlockingQueue<InputQueueInfo>(writers.length); 
		this.writerInputQueues = new BlockingQueue[writers.length];
		for (int i = 0; i < writerInputQueues.length;i++) {
			writerInputQueues[i] = new ArrayBlockingQueue<Element>(NO_ELEMENTS);
			writerInputQueue.add(new InputQueueInfo(this.writers[i], writerInputQueues[i]));
		}

		currentWayAreaSet = new BitSet(writers.length);
		currentRelAreaSet = new BitSet(writers.length);
		
		int noOfWorkerThreads = this.maxThreads - 1;
		workerThreads = new ArrayList<Thread>(noOfWorkerThreads);
		for (int i = 0; i < noOfWorkerThreads; i++) {
			Thread worker = new Thread(new OSMWriterWorker());
			worker.setName("worker-" + i);
			workerThreads.add(worker);
			worker.start();
		}
	}

	@Override
	public boolean isStartNodeOnly() {
		return false;
	}

	@Override
	public void boundTag(Area bounds) {
	}

	private void relationNode(int id, String role) {
		{
			int set = coords.get(id);
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
					if (val == 0)
						break;
					// val - 1 because the areas held in 'ways' are in the range 1-255
					currentRelAreaSet.set(val - 1);
				}
			}
		}
	}

	private void relationWay(int id, String role) {
		{
			long[] bigSet;
			int set = ways.get(id);
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
					if (val == 0)
						break;
					// val - 1 because the areas held in 'ways' are in the range 1-255
					currentRelAreaSet.set(val - 1);
				}
			} else if ((bigSet = bigWays.get(id)) != null) {
				// Copy bits from bigSet to currentRelAreaSet
				for (int i = 0; i < bigSet.length; i++) {
					for (int j = 0; j < 64; j++) {
						if ((bigSet[i] & (1L << (j % 64))) != 0) {
							currentRelAreaSet.set(i * 64 + j);
						}
					}
				}
			}
		}
	}

	@Override
	public void processNode(Node n) {
		try {
			writeNode(n);
			currentNodeAreaSet = 0;
		} catch (IOException e) {
			throw new RuntimeException("failed to write node " + n.getId(), e);
		}
	}

	@Override
	public void processWay(Way w) {

		for (int id: w.getRefs().asArray()) {
			// Get the list of areas that the node is in.  A node may be in
			// more than one area because of overlap.
			int set = coords.get(id);

			// add the list of areas to the currentWayAreaSet
			if (set != 0) {
				int mask = 0xff;
				for (int slot = 0; slot < 4; slot++, mask <<= 8) {
					int val = (set & mask) >>> (slot * 8);
				if (val == 0)
					break;
				currentWayAreaSet.set(val - 1);
				}
			}
		}
		
		try {
			writeWay(w);
			currentWayAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write way " + w.getId(), e);
		}
	}

	@Override
	public void processRelation(Relation r) {
		try {
			for (Member mem : r.getMembers()) {
				String role = mem.getRole();
				int id = mem.getRef();
				if (mem.getType().equals("node")) {
					relationNode(id,role);
				} else if (mem.getType().equals("way")){
					relationWay(id,role);
				}			
			}
			
			writeRelation(r);
			currentRelAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write relation " + r.getId(), e);
		}
	}

	@Override
	public void endMap() {
		for (int i = 0; i < writerInputQueues.length; i++) {
			try {
				writerInputQueues[i].put(STOP_ELEMENT);
			} catch (InterruptedException e) {
				throw new RuntimeException("Failed to add the stop element for worker thread " + i, e);
			}
		}
		for (Thread workerThread : workerThreads) {
			try {
				workerThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException("Failed to join for thread " + workerThread.getName(), e);
			}
		}
		for (OSMWriter writer : writers) {
			writer.finishWrite();
		}
	}

	private void writeNode(Node currentNode) throws IOException {
		Entry<Integer, ArrayList<OSMWriter>> entry = writermap.floorEntry(currentNode.getMapLat());
		if (entry == null)
			return;
		for (OSMWriter w : entry.getValue()) {
			int n = writerToID.get(w);
			boolean found = w.nodeBelongsToThisArea(currentNode); 
			if (found) {
				if (maxThreads > 1) {
					addToWorkingQueue(n, currentNode);
				} else {
					w.write(currentNode);
				}
				if (currentNodeAreaSet == 0) {
					currentNodeAreaSet = n + 1;
				} else {
					currentNodeAreaSet = addToSet(currentNodeAreaSet, n + 1, currentNode.getId());
				}
			}
		}
		// Only remember the node if it's in one or more of the areas we care about
		if (currentNodeAreaSet != 0) {
			coords.put(currentNode.getId(), currentNodeAreaSet);
		}
	}

	private boolean seenWay;

	private void writeWay(Way currentWay) throws IOException {
		if (!seenWay) {
			seenWay = true;
			System.out.println("Writing ways " + new Date());
		}
		if (!currentWayAreaSet.isEmpty()) {
			if (currentWayAreaSet.cardinality() <= 4) {
				// this way falls into 4 or less areas (the normal case). Store these areas in the ways map
				int set = 0;
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					if (maxThreads > 1) {
						addToWorkingQueue(n, currentWay);
					} else {
						writers[n].write(currentWay);
					}
					// add one to the area so we're in the range 1-255. This is because we treat 0 as the
					// equivalent of a null
					set = set << 8 | (n + 1);
				}
				ways.put(currentWay.getId(), set);
			} else {
				// this way falls into 5 or more areas. Convert the currentWayAreaSet into a long[] and store
				// these areas in the bigWays map
				long[] set = new long[currentWayAreaSet.size() / 64];
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					if (maxThreads > 1) {
						addToWorkingQueue(n, currentWay);
					} else {
						writers[n].write(currentWay);
					}
					set[n / 64] |= 1L << (n % 64);
				}
				bigWays.put(currentWay.getId(), set);
			}
		}
	}

	private boolean seenRel;

	private void writeRelation(Relation currentRelation) throws IOException {
		if (!seenRel) {
			seenRel = true;
			System.out.println("Writing relations " + new Date());
		}
		for (int n = currentRelAreaSet.nextSetBit(0); n >= 0; n = currentRelAreaSet.nextSetBit(n + 1)) {
			// if n is out of bounds, then something has gone wrong
			if (maxThreads > 1) {
				addToWorkingQueue(n, currentRelation);
			} else {
				writers[n].write(currentRelation);
			}
		}
	}

	private int addToSet(int set, int v, int id) {
		int val = v;
		for (int mask = 0xff; mask != 0; mask <<= 8) {
			int setval = set & mask;
			if (setval == 0) {
				return set | val;
			} else if (setval == val) {
				return set;
			}
			val <<= 8;
		}
		// it was not added
		System.err.println("Node " + id + " in too many areas. Already in areas 0x" + Integer.toHexString(set) + ", trying to add area 0x" + Integer.toHexString(v));
		return set;
	}

	private void addToWorkingQueue(int writerNumber, Element element) {
		try {
			writerInputQueues[writerNumber].put(element);
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to write node " + element.getId() + " to worker thread " + writerNumber, e);
		}
	}

	private static class InputQueueInfo {
		private final OSMWriter writer;
		private final BlockingQueue<Element> inputQueue;

		public InputQueueInfo(OSMWriter writer, BlockingQueue<Element> inputQueue) {
      this.writer = writer;
			this.inputQueue = inputQueue;
		}
	}

	private static final Element STOP_ELEMENT = new Element();

	public static final int NO_ELEMENTS = 1000;

	private class OSMWriterWorker implements Runnable {

		public void processElement(Element element, OSMWriter writer) throws IOException {
			if (element instanceof Node) {
				writer.write((Node) element);
			} else if (element instanceof Way) {
				writer.write((Way) element);
			} else if (element instanceof Relation) {
				writer.write((Relation) element);
			}
		}

		@Override
		public void run() {
			boolean finished = false;
			while (!finished) {
				InputQueueInfo workPackage = writerInputQueue.poll();
				if (workPackage==null) {
					finished=true;
				} else {
					while (!workPackage.inputQueue.isEmpty()) {
						Element element =null;
						try {
							element = workPackage.inputQueue.poll();
							if (element == null) {
								writerInputQueue.put(workPackage);
								workPackage=null;
								break;
							} else if (element == STOP_ELEMENT) {
								workPackage=null;
								System.out.println("Thread " + Thread.currentThread().getName() + " has finished");
								// this writer is finished
								break;
							} else {
								processElement(element, workPackage.writer);
							}
							
						} catch (InterruptedException e) {
							throw new RuntimeException("Thread " + Thread.currentThread().getName() + " failed to get next element", e);
						} catch (IOException e) {
							throw new RuntimeException("Thread " + Thread.currentThread().getName() + " failed to write element " + element.getId() + '(' + element.getClass().getSimpleName() + ')', e);
						}
					}
					if (workPackage != null) {
						try {
							writerInputQueue.put(workPackage);
						} catch (InterruptedException e) {
							throw new RuntimeException("Thread " + Thread.currentThread().getName() + " failed to return work package", e);
						}
					}
				}
			}
		}
	}
}