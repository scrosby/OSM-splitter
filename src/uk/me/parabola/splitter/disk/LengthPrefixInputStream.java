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

import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

import uk.me.parabola.splitter.Utils;

/**
 * Reads in a 'unit' of data at a time into a buffer every time next() is called.
 * Each unit has a two-byte prefix indicating how many bytes long the unit is.
 * Every time next() is called, the next unit will be read in.
 * <p/>
 * This is similar to a {@code DataInputStream} but offers better
 * performance (more specialised buffering, no synchronisation).
 *
 * @author Chris Miller
 */
public class LengthPrefixInputStream extends InputStream {

	private InputStream in;
	private byte[] buf;
	private int index;
	private int maxLen;
	private long bytesRead;

	public LengthPrefixInputStream(InputStream in) {
		this(in, 4096);
	}

	public LengthPrefixInputStream(InputStream in, int bufferSize) {
		this.in = in;
		buf = new byte[bufferSize];
	}

	public int remaining() {
		return maxLen - index;
	}

	public byte readByte() throws IOException {
		ensureData(1);
		return buf[index++];
	}

	public int readUnsignedByte() throws IOException {
		ensureData(1);
		return buf[index++] & 0xff;
	}

	public short readShort() throws IOException {
		ensureData(2);
		return (short) ((buf[index++] << 8) | (buf[index++] & 0xff));
	}

	public int readUnsignedShort() throws IOException {
		ensureData(2);
		return (buf[index++] & 0xff) << 8 | (buf[index++] & 0xff);
	}

	public int readInt() throws IOException {
		ensureData(4);
		return buf[index++] << 24 | (buf[index++] & 0xff) << 16 | (buf[index++] & 0xff) << 8 | (buf[index++] & 0xff);
	}

	public long readLong() throws IOException {
		ensureData(8);
		return (long) buf[index++] << 56 |
						((long) buf[index++] & 0xff) << 48 |
						((long) buf[index++] & 0xff) << 40 |
						((long) buf[index++] & 0xff) << 32 |
						((long) buf[index++] & 0xff) << 24 |
						((long) buf[index++] & 0xff) << 16 |
						((long) buf[index++] & 0xff) << 8 |
						buf[index++] & 0xff;
	}

	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * This has been lifted from DataInputStream. We inline it here rather than
	 * delegate to DIS because that way we can avoid creating a temporary buffer
	 * for the UTF-8 chars.
	 */
	public String readUTF() throws IOException {
		int utflen = readUnsignedShort();
		ensureData(utflen);
		char[] chararr = new char[utflen];

		int c, char2, char3;
		int count = 0;
		int chararr_count = 0;

		while (count < utflen) {
			c = (int) buf[index + count] & 0xff;
			if (c > 127) break;
			count++;
			chararr[chararr_count++] = (char) c;
		}

		while (count < utflen) {
			c = (int) buf[index + count] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxxxxxx*/
				count++;
				chararr[chararr_count++] = (char) c;
				break;
			case 12:
			case 13:
				/* 110x xxxx   10xx xxxx*/
				count += 2;
				if (count > utflen)
					throw new UTFDataFormatException("malformed input: partial character at end");
				char2 = (int) buf[index + count - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException("malformed input around byte " + count);
				chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14:
				/* 1110 xxxx  10xx xxxx  10xx xxxx */
				count += 3;
				if (count > utflen)
					throw new UTFDataFormatException("malformed input: partial character at end");
				char2 = (int) buf[index + count - 2];
				char3 = (int) buf[index + count - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException("malformed input around byte " + (count - 1));
				chararr[chararr_count++] = (char) ((c & 0x0F) << 12 | (char2 & 0x3F) << 6 | (char3 & 0x3F));
				break;
			default:
				/* 10xx xxxx,  1111 xxxx */
				throw new UTFDataFormatException("malformed input around byte " + count);
			}
		}
		index += count;
		// The number of chars produced may be less than utflen
		return new String(chararr, 0, chararr_count);
	}

	private byte[] lengthBuf = new byte[2];

	public boolean next() throws IOException {
		// Read the length prefix
		int len = 0;
		while (len < 2) {
			int read = in.read(lengthBuf, len, 2 - len);
			if (read < 0) {
				if (len > 0) {
					displayByteBuffer();
					System.out.println("Unexpected EOF reached while reading segment length. Only " + len + " of 2 bytes read. Total bytes read " + Utils.format(bytesRead));
				}
				return false;
			}
			len += read;
			bytesRead += read;
		}
		maxLen = (lengthBuf[0] & 0xFF) << 8 | (lengthBuf[1] & 0xFF);
		if (maxLen == 0xFFFF) {
			// we've hit a marker that indicates the length is held in an int rather than a short
			byte[] temp = new byte[4];
			len = 0;
			while (len < 4) {
				int read = in.read(temp, len, 4 - len);
				if (read < 0) {
					displayByteBuffer();
					System.out.println("Unexpected EOF reached while reading segment length. Only " + len + " of 4 bytes read. Total bytes read " + Utils.format(bytesRead));
					return false;
				}
				len += read;
				bytesRead += read;
			}
			maxLen = temp[0] << 24 | (temp[1] & 0xff) << 16 | (temp[2] & 0xff) << 8 | (temp[3] & 0xff);
		}
		index = 0;

		// Make sure the buffer has enough room for this segment of data
		if (buf.length < index + maxLen) {
			buf = new byte[index + maxLen];
		}
		// Read in the whole buffer
		len = 0;
		while (len < maxLen) {
			int read = in.read(buf, index + len, maxLen - len);
			if (read < 0) {
				// We've hit the EOF - this shouldn't happen!
				System.out.println("Unexpected EOF reached while loading segment. Expected " + maxLen + " bytes, only read " + len + ". Total bytes read " + Utils.format(bytesRead));
				System.out.println("Here's the buffer that was read:");
				maxLen = len;
				displayByteBuffer();
				return false;
			}
			len += read;
			bytesRead += read;
		}
		return true;
	}

	private void ensureData(int len) throws IOException {
		if (index + len > maxLen) {
			displayByteBuffer(); // useful for debugging
			throw new IOException("Attempt was made to read " + len + " bytes when there are only " + (maxLen - index) + " remaining (from " + maxLen + " total in this segment)");
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = Math.min(len, maxLen - index);
		System.arraycopy(buf, index, b, off, result);
		index += result;
		return result;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	private static final char[] HEX_CHARS = new char[] {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	private static final int BYTES_PER_ROW = 32;

	private void displayByteBuffer() {
		char[] ascii = new char[BYTES_PER_ROW];
		for (int i = 0; i < maxLen; i++) {
			System.out.print(HEX_CHARS[(buf[i] & 0xFF) >> 4]);
			System.out.print(HEX_CHARS[buf[i] & 0x0F]);
			if (i % 8 == 7)
				System.out.print("  ");
			else
				System.out.print(' ');

			char c = (char) buf[i];
			if (c < 32 || c == 127)
				c = '.';
			ascii[i % BYTES_PER_ROW] = c;

			if (i % BYTES_PER_ROW == BYTES_PER_ROW - 1)
				System.out.println(ascii);
		}
		int missing = (BYTES_PER_ROW - (maxLen % BYTES_PER_ROW) % BYTES_PER_ROW);
		for (int i = 0; i < missing; i++)
			System.out.print("   ");
		for (int i = 0; i < missing / 8; i++)
			System.out.print(' ');
		System.out.println(ascii);
	}
}