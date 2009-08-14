/*
 * Copyright (C) 2006 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * Author: Steve Ratcliffe
 * Create date: 03-Dec-2006
 */
package uk.me.parabola.splitter;

import java.text.NumberFormat;

/**
 * Some miscellaneous functions that are used within the .img code.
 *
 * @author Steve Ratcliffe
 */
public class Utils {

	private static final NumberFormat FORMATTER = NumberFormat.getIntegerInstance();

	public static String format(int number) {
		return FORMATTER.format(number);
	}

	public static String format(long number) {
		return FORMATTER.format(number);
	}

	public static double toDegrees(int val) {
		return (double) val / ((1 << 24) / 360.0);
	}

	/**
	 * A map unit is an integer value that is 1/(2^24) degrees of latitude or
	 * longitude.
	 *
	 * @param l The lat or long as decimal degrees.
	 * @return An integer value in map units.
	 */
	public static int toMapUnit(double l) {
		double DELTA = 0.000001; // TODO check if we really mean this
		if (l > 0)
			return (int) ((l + DELTA) * (1 << 24)/360);
		else
			return (int) ((l - DELTA) * (1 << 24)/360);
	}
	
	public static double toRadians(int latitude) {
		return toDegrees(latitude) * Math.PI / 180;
	}

	/**
	 * Rounds an integer down to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round down.
	 * @param shift the power of two to round down to.
	 * @return the rounded integer.
	 */
	public static int roundDown(int val, int shift) {
		return val >>> shift << shift;
	}

	/**
	 * Rounds an integer up to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round up.
	 * @param shift the power of two to round up to.
	 * @return the rounded integer.
	 */
	public static int roundUp(int val, int shift) {
		return (val + (1 << shift) - 1) >>> shift << shift;
	}

	/**
	 * Rounds an integer up or down to the nearest multiple of {@code 2^shift}.
	 * Works with both positive and negative integers.
	 * @param val the integer to round.
	 * @param shift the power of two to round to.
	 * @return the rounded integer.
	 */
	public static int round(int val, int shift) {
		return (val + (1 << (shift - 1))) >>> shift << shift;
	}

	/**
	 * @param n the integer to test
	 * @return {@code true} if the integer is a power of two, {@code false} otherwise.
	 */
	public static boolean isPowerOfTwo(int n) {
		return ((n & (n - 1)) == 0) && n > 0;
	}
}