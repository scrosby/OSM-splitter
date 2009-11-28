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

package uk.me.parabola.splitter.disk;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

/**
 * Buffers the output then writes it all at once with a 2-byte length prefix
 * every time next() is called.
 * <p/>
 * This is similar to a {@code DataOutputStream} but offers better
 * performance (more specialised buffering, no synchronisation).
 *
 * @author Chris Miller
 */
public class LengthPrefixOutputStream extends OutputStream {

	private OutputStream out;
	private byte[] buf;
	private int index = 2;

	public LengthPrefixOutputStream(OutputStream out) {
		this(out, 4096);
	}

	public LengthPrefixOutputStream(OutputStream out, int bufferSize) {
		this.out = new BufferedOutputStream(out, 8192);
		buf = new byte[bufferSize];
	}

	public void writeByte(int b) throws IOException {
		ensureCapacity(1);
		buf[index++] = (byte) b;
	}

	public final void writeShort(int v) throws IOException {
		ensureCapacity(2);
		buf[index++] = (byte) (v >>> 8);
		buf[index++] = (byte) v;
	}

	public final void writeInt(int v) throws IOException {
		ensureCapacity(4);
		buf[index++] = (byte) (v >>> 24);
		buf[index++] = (byte) (v >>> 16);
		buf[index++] = (byte) (v >>> 8);
		buf[index++] = (byte) v;
	}

	public final void writeLong(long v) throws IOException {
		ensureCapacity(8);
		buf[index++] = (byte) (v >>> 56);
		buf[index++] = (byte) (v >>> 48);
		buf[index++] = (byte) (v >>> 40);
		buf[index++] = (byte) (v >>> 32);
		buf[index++] = (byte) (v >>> 24);
		buf[index++] = (byte) (v >>> 16);
		buf[index++] = (byte) (v >>> 8);
		buf[index++] = (byte) v;
	}

	public final void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToLongBits(v));
	}

	/**
	 * This has been lifted from DataOutputStream. We inline it here rather than
	 * delegate to DOS because that way we can avoid creating a temporary buffer
	 * for the UTF-8 chars.
	 */
	public void writeUTF(CharSequence str) throws IOException {
		int strlen = str.length();
		int utflen = 0;
		int c;

		for (int i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		if (utflen > 65535)
			throw new UTFDataFormatException("encoded string too long: " + utflen + " bytes");

		ensureCapacity(utflen + 2);
		buf[index++] = (byte) (utflen >>> 8);
		buf[index++] = (byte) utflen;

		int i;
		for (i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if (!((c >= 0x0001) && (c <= 0x007F)))
				break;
			buf[index++] = (byte) c;
		}

		for (; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				buf[index++] = (byte) c;

			} else if (c > 0x07FF) {
				buf[index++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				buf[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				buf[index++] = (byte) (0x80 | c & 0x3F);
			} else {
				buf[index++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				buf[index++] = (byte) (0x80 | c & 0x3F);
			}
		}
	}

	@Override
	public void write(int b) throws IOException {
		ensureCapacity(1);
		buf[index++] = (byte) b;
	}

	@Override
	public void write(byte[] b) throws IOException {
		ensureCapacity(b.length);
		System.arraycopy(b, 0, buf, index, b.length);
		index += b.length;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureCapacity(len);
		System.arraycopy(b, off, buf, this.index, len);
		this.index += len;
	}

	public void next() throws IOException {
		// Write the length prefix
		index -= 2;

		if (index > 0xFFFF) {
			// Write a marker so we know the length is held in an int rather than a short
			byte[] temp = new byte[4];
			temp[0] = (byte) 0xFF;
			temp[1] = (byte) 0xFF;
			temp[2] = (byte) (index >>> 24);
			temp[3] = (byte) (index >>> 16);
			out.write(temp);
			buf[0] = (byte) (index >>> 8);
			buf[1] = (byte) index;
		} else {
			buf[0] = (byte) (index >>> 8);
			buf[1] = (byte) index;
		}
		// Dump out the whole buffer
		out.write(buf, 0, index + 2);
		// Reset the length
		index = 2;
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	private void ensureCapacity(int len) {
		if (buf.length < this.index + len) {
			// We need to grow our buffer
			byte[] temp = new byte[(this.index + len) * 3 / 2];
			System.arraycopy(buf, 0, temp, 0, buf.length);
			buf = temp;
		}
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
