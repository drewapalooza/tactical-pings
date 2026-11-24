/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Client-thread owned. Memory and incoming visual/audio work are bounded. */
public final class PingStore
{
	private static final long MEMBER_INTERVAL = 450_000_000L;
	private static final long GLOBAL_INTERVAL = 120_000_000L;
	private final List<ActivePing> active = new ArrayList<>();
	private final Map<String, Long> seen = new LinkedHashMap<>();
	private final Map<Long, Long> memberTimes = new LinkedHashMap<>();
	private Long lastAccepted;

	public boolean add(String id, long memberId, PingType type, WorldPoint point,
		long nowNanos, int durationMillis)
	{
		return add(id, memberId, type, point, nowNanos, durationMillis, null);
	}

	public boolean add(String id, long memberId, PingType type, WorldPoint point,
		long nowNanos, int durationMillis, PingOutline outline)
	{
		prune(nowNanos);
		Long memberTime = memberTimes.get(memberId);
		if (seen.containsKey(id) || (memberTime != null && nowNanos - memberTime < MEMBER_INTERVAL)
			|| (lastAccepted != null && nowNanos - lastAccepted < GLOBAL_INTERVAL))
		{
			return false;
		}
		seen.put(id, nowNanos);
		memberTimes.put(memberId, nowNanos);
		trim(seen, 256);
		trim(memberTimes, 64);
		lastAccepted = nowNanos;
		if (active.size() >= 24)
		{
			active.remove(0);
		}
		active.add(new ActivePing(type, point, nowNanos, durationMillis, outline));
		return true;
	}

	public List<ActivePing> getActive()
	{
		return active;
	}

	public void prune(long nowNanos)
	{
		active.removeIf(ping -> ping.progress(nowNanos) >= 1);
		seen.values().removeIf(time -> nowNanos - time > 60_000_000_000L);
		memberTimes.values().removeIf(time -> nowNanos - time > 60_000_000_000L);
	}

	public void clear()
	{
		active.clear();
		seen.clear();
		memberTimes.clear();
		lastAccepted = null;
	}

	private static void trim(Map<?, ?> map, int limit)
	{
		while (map.size() > limit)
		{
			Iterator<?> iterator = map.keySet().iterator();
			iterator.next();
			iterator.remove();
		}
	}
}
