/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;

public final class ActivePing
{
	private final PingType type;
	private final PingStyle style;
	private final WorldPoint point;
	private final long startedNanos;
	private final long durationNanos;
	private final PingOutline outline;
	private final String senderName;

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis)
	{
		this(type, point, startedNanos, durationMillis, null);
	}

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis, PingOutline outline)
	{
		this(type, point, startedNanos, durationMillis, outline, PingStyle.defaults(type));
	}

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis,
		PingOutline outline, PingStyle style)
	{
		this(type, point, startedNanos, durationMillis, outline, style, null);
	}

	public ActivePing(PingType type, WorldPoint point, long startedNanos, int durationMillis,
		PingOutline outline, PingStyle style, String senderName)
	{
		this.type = type;
		this.style = style;
		this.point = point;
		this.startedNanos = startedNanos;
		durationNanos = durationMillis * 1_000_000L;
		this.outline = outline;
		this.senderName = cleanSenderName(senderName);
	}

	static String cleanSenderName(String name)
	{
		if (name == null || name.length() > 256)
		{
			return "";
		}
		String clean = PingStyle.cleanText(Text.removeTags(name).replace('\u00a0', ' ')).trim();
		int end = clean.offsetByCodePoints(0, Math.min(32, clean.codePointCount(0, clean.length())));
		return clean.substring(0, end);
	}

	public String getSenderName()
	{
		return senderName;
	}

	public PingOutline getOutline()
	{
		return outline;
	}

	public PingType getType()
	{
		return type;
	}

	public PingStyle getStyle()
	{
		return style;
	}

	public WorldPoint getPoint()
	{
		return point;
	}

	public double progress(long nowNanos)
	{
		return Math.max(0, Math.min(1, (double) (nowNanos - startedNanos) / durationNanos));
	}

	/** Long lifetimes keep a prompt entrance and exit instead of a minute-long slow fade-in. */
	public double visualProgress(long nowNanos)
	{
		if (durationNanos <= 3_000_000_000L)
		{
			return progress(nowNanos);
		}
		long age = Math.max(0, nowNanos - startedNanos);
		long transition = 800_000_000L;
		if (age < transition)
		{
			return 0.52 * age / transition;
		}
		long remaining = durationNanos - age;
		return remaining > transition ? 0.52 : Math.min(1, 0.52 + 0.48 * (1 - (double) remaining / transition));
	}
}
