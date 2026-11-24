/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.util.UUID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.messages.PartyMemberMessage;

/** Versioned, bounded payload. The relay supplies the inherited memberId. */
public class TacticalPingMessage extends PartyMemberMessage
{
	private int version;
	private String id;
	private String type;
	private int world;
	private int x;
	private int y;
	private int plane;
	private boolean instanced;
	private int templateX;
	private int templateY;
	private int templatePlane;
	private long sentAt;
	private PingTarget target;

	public TacticalPingMessage()
	{
		// Gson requires a no-argument constructor; absent fields remain invalid.
	}

	public TacticalPingMessage(PingType type, int world, WorldPoint point,
		boolean instanced, WorldPoint template, long sentAt)
	{
		version = 1;
		id = UUID.randomUUID().toString();
		this.type = type.name();
		this.world = world;
		x = point.getX();
		y = point.getY();
		plane = point.getPlane();
		this.instanced = instanced;
		templateX = template.getX();
		templateY = template.getY();
		templatePlane = template.getPlane();
		this.sentAt = sentAt;
	}

	public boolean isValid(long now)
	{
		return version == 1 && id != null && id.length() == 36
			&& id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
			&& getType() != null && world > 0 && world <= 65535
			&& validPoint(x, y, plane) && validPoint(templateX, templateY, templatePlane)
			&& (target == null || target.isValid())
			&& sentAt >= now - 10000 && sentAt <= now + 30000;
	}

	public PingTarget getTarget()
	{
		return target;
	}

	public void setTarget(PingTarget target)
	{
		this.target = target;
	}

	private static boolean validPoint(int x, int y, int plane)
	{
		return x >= 0 && x < 16384 && y >= 0 && y < 16384 && plane >= 0 && plane < 4;
	}

	public String getId()
	{
		return id;
	}

	public PingType getType()
	{
		return PingType.fromWire(type);
	}

	public int getWorld()
	{
		return world;
	}

	public WorldPoint getPoint()
	{
		return new WorldPoint(x, y, plane);
	}

	public WorldPoint getTemplate()
	{
		return new WorldPoint(templateX, templateY, templatePlane);
	}

	public boolean isInstanced()
	{
		return instanced;
	}
}
