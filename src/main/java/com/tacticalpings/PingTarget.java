/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

/** Optional manual target identity; coordinates are supplied by the enclosing ping. */
public final class PingTarget
{
	public enum Kind
	{
		NPC,
		PLAYER,
		GAME_OBJECT,
		WALL_OBJECT,
		DECORATIVE_OBJECT,
		GROUND_OBJECT,
		GROUND_ITEM
	}

	private final Kind kind;
	private final int id;
	private final int index;
	private final String name;

	public PingTarget(Kind kind, int id, int index, String name)
	{
		this.kind = kind;
		this.id = id;
		this.index = index;
		this.name = name;
	}

	public Kind getKind()
	{
		return kind;
	}

	public int getId()
	{
		return id;
	}

	public int getIndex()
	{
		return index;
	}

	public String getName()
	{
		return name;
	}

	public boolean isValid()
	{
		if (kind == null || id < 0 || id > 1_000_000)
		{
			return false;
		}
		if (kind == Kind.PLAYER)
		{
			return id == 0 && index >= 0 && index < 2048 && name != null
				&& name.length() <= 12 && name.matches("[a-z0-9 _-]{1,12}");
		}
		return name == null && (kind == Kind.NPC ? index >= 0 && index < 65536 : index == 0);
	}
}
