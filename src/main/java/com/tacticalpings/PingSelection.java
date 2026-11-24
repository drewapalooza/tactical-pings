/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import net.runelite.api.coords.WorldPoint;

public final class PingSelection
{
	private final WorldPoint point;
	private final PingOutline outline;

	public PingSelection(WorldPoint point, PingOutline outline)
	{
		this.point = point;
		this.outline = outline;
	}

	public WorldPoint getPoint()
	{
		return point;
	}

	public PingOutline getOutline()
	{
		return outline;
	}
}
