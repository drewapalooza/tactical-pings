/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

public final class PingAnimation
{
	private PingAnimation()
	{
	}

	/** Radius in tiles, with zero velocity at the turn and at the center. */
	public static double radius(double progress)
	{
		double t = clamp(progress);
		return 1.35 * (t < 0.52 ? smooth(t / 0.52) : 1 - smooth((t - 0.52) / 0.48));
	}

	public static double opacity(double progress)
	{
		double t = clamp(progress);
		return smooth(t / 0.07) * (1 - smooth((t - 0.68) / 0.32));
	}

	public static double smooth(double value)
	{
		double t = clamp(value);
		return t * t * (3 - 2 * t);
	}

	private static double clamp(double value)
	{
		return Math.max(0, Math.min(1, value));
	}
}
