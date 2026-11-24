/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

/** Shared geometry for selection and painting: clockwise sectors start at twelve o'clock. */
public final class PingWheelLayout
{
	private PingWheelLayout()
	{
	}

	public static int count(int requested)
	{
		return Math.max(1, Math.min(8, requested));
	}

	public static PingSlot choose(double dx, double dy, int requested, PingSlot quick)
	{
		int count = count(requested);
		if (dx * dx + dy * dy < 18 * 18)
		{
			return quick != null && quick.ordinal() < count ? quick : PingSlot.ONE;
		}
		double angle = Math.atan2(dx, -dy);
		if (angle < 0)
		{
			angle += Math.PI * 2;
		}
		int index = Math.min(count - 1, (int) (angle * count / (Math.PI * 2)));
		return PingSlot.values()[index];
	}

	public static double angle(int index, int count)
	{
		return -Math.PI / 2 + (index + 0.5) * Math.PI * 2 / count(count);
	}

	public static int radius(int count)
	{
		return count(count) > 4 ? 128 : 100;
	}
}
