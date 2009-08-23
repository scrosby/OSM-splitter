/**
 * Chris Miller
 */
package uk.me.parabola.splitter;

/**
 * Maintains a list of int primitives.
 */
public class IntList {
	private static final int DEFAULT_BUFFER_SIZE = 10;

	private int[] data;
	private int size;

	public IntList() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public IntList(int initialSize) {
		data = new int[initialSize];
	}

	public void add(int value) {
		ensureCapacity();
		data[size++] = value;
	}

	public int get(int i) {
		return data[i];
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	private void ensureCapacity() {
		if (size == data.length - 1) {
			int[] temp = data;
			data = new int[size * 3 / 2 + 1];
			System.arraycopy(temp, 0, data, 0, size);
		}
	}
}