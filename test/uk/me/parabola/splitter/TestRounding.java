package uk.me.parabola.splitter;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the rounding up/down utility methods.
 */
public class TestRounding {
	@Test
	public void testPositiveRoundingDown() {
		for (int i = 0; i < 50000; i += 19) {
			testRoundDown(i, 11, i / 2048 * 2048);
		}
		testRoundDown(0x1d5842, 11, 0x1d5800);
		testRoundDown(0x2399a, 11, 0x23800);
		testRoundDown(0x23800, 11, 0x23800);
		testRoundDown(0x237f0, 11, 0x23000);
	}

	@Test
	public void testPositiveRoundingUp() {
		for (int i = 0; i < 50000; i += 19) {
			testRoundUp(i, 11, (i + 2047) / 2048 * 2048);
		}
		testRoundUp(0x1e7faa, 11, 0x1e8000);
		testRoundUp(0x1e7801, 11, 0x1e8000);
		testRoundUp(0x1e7800, 11, 0x1e7800);
		testRoundUp(0x1e70aa, 11, 0x1e7800);
		testRoundUp(0x1e77ff, 11, 0x1e7800);
	}

	@Test
	public void testNegativeRoundingDown() {
		testRoundDown(0xffcbba86, 11, 0xffcbb800);
		testRoundDown(0xffcbbfff, 11, 0xffcbb800);
		testRoundDown(0xffcbb801, 11, 0xffcbb800);
		testRoundDown(0xffcbb7ff, 11, 0xffcbb000);
	}

	@Test
	public void testNegativeRoundingUp() {
		testRoundUp(0xffcbba86, 11, 0xffcbc000);
		testRoundUp(0xffcbbfff, 11, 0xffcbc000);
		testRoundUp(0xffcbb801, 11, 0xffcbc000);
		testRoundUp(0xffcbb7ff, 11, 0xffcbb800);
		testRoundUp(Integer.MIN_VALUE + 1234, 11, 0x80000800);
	}

	private void testRoundDown(int value, int shift, int outcome) {
		Assert.assertEquals(Utils.roundDown(value, shift), outcome, "Before: " + Integer.toHexString(value) +
						", After: " + Integer.toHexString(Utils.roundDown(value, shift)));
	}

	private void testRoundUp(int value, int shift, int outcome) {
		Assert.assertEquals(Utils.roundUp(value, shift), outcome, "Before: " + Integer.toHexString(value) +
						", After: " + Integer.toHexString(Utils.roundUp(value, shift)));
	}
}