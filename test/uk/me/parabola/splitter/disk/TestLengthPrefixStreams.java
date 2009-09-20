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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class TestLengthPrefixStreams {
	@Test
	public void testOutput() throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		LengthPrefixOutputStream os = new LengthPrefixOutputStream(result, 2);  // 2 so we test the buffer expansion
		os.writeShort(0xABCD);
		os.writeInt(0x12345678);
		os.writeInt(0xAABBCCDD);
		os.writeUTF("Testing");
		os.next();
		os.writeShort(0x9988);
		os.writeByte(240);
		os.next();
		os.close();

		byte[] bytes = result.toByteArray();

		// Test to see if the byte array contains what we'd expect
		Assert.assertEquals(bytes, new byte[] {
			0x00, 0x13,                 // length prefix
			(byte) 0xAB, (byte) 0xCD,   // 0xABCD
			0x12, 0x34, 0x56, 0x78,     // 0x12345678
			(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD,    // 0xAABBCCDD
			0x00, 0x07, 'T', 'e', 's', 't', 'i', 'n', 'g',         // "Testing"
			0x00, 0x03,                 // length prefix
			(byte) 0x99, (byte) 0x88,   // 0x9988
			(byte) 240,                 // 240
		},
		"Resultant byte array does not match expected result");
	}

	@Test
	public void testinput() throws IOException {
		byte[] buf = new byte[] {
						0x00, 0x13,								 // length prefix
						(byte) 0xAB, (byte) 0xCD,	 // 0xABCD
						0x12, 0x34, 0x56, 0x78,		 // 0x12345678
						(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD,		// 0xAABBCCDD
						0x00, 0x07, 'T', 'e', 's', 't', 'i', 'n', 'g',				 // "Testing"
						0x00, 0x04,								 // length prefix
						(byte) 0x99, (byte) 0x88,	 // 0x9988
						(byte) 240,								 // 240
						(byte) 240,								 // 240
		};
		ByteArrayInputStream result = new ByteArrayInputStream(buf);
		LengthPrefixInputStream is = new LengthPrefixInputStream(result, 2);  // 2 so we test the buffer expansion

		Assert.assertEquals(is.next(), true, "Premature end of stream reached");
		Assert.assertEquals(is.remaining(), 0x13, "Unexpected number of bytes remaining in this segment");
		Assert.assertEquals(is.readUnsignedShort(), 0xABCD);
		Assert.assertEquals(is.readInt(), 0x12345678);
		Assert.assertEquals(is.readInt(), 0xAABBCCDD);
		Assert.assertEquals(is.readUTF(), "Testing");
		Assert.assertEquals(is.remaining(), 0x00, "Unexpected number of bytes remaining in this segment");
		Assert.assertEquals(is.next(), true, "Premature end of stream reached");
		Assert.assertEquals(is.readShort(), (short) 0x9988);
		Assert.assertEquals(is.readUnsignedByte(), 240);
		Assert.assertEquals(is.readByte(), (byte) 240);
		Assert.assertEquals(is.remaining(), 0x00, "Unexpected number of bytes remaining in this segment");
		Assert.assertEquals(is.next(), false, "Expected to reach the end of stream but didn't");
	}
}