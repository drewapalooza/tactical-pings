/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import net.runelite.api.coords.WorldPoint;

public final class ActivePing
{
	private final PingType type;
	private final WorldPoint point;
	private final long startedNanos;
	private final long durationNanos;
	private final PingOutline outline;

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis)
	{
		this(type, point, startedNanos, durationMillis, null);
	}

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis, PingOutline outline)
	{
		this.type = type;
		this.point = point;
		this.startedNanos = startedNanos;
		durationNanos = durationMillis * 1_000_000L;
		this.outline = outline;
	}

	public PingOutline getOutline()
	{
		return outline;
	}

	public PingType getType()
	{
		return type;
	}

	public WorldPoint getPoint()
	{
		return point;
	}

	public double progress(long nowNanos)
	{
		return Math.max(0, Math.min(1, (double) (nowNanos - startedNanos) / durationNanos));
	}
}
