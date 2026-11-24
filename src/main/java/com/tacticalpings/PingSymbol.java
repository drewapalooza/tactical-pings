/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

/** Stable names are shared over the party relay; never use enum ordinals. */
public enum PingSymbol
{
	EXCLAMATION("Exclamation point"),
	ARROW("Arrow"),
	CROSSHAIR("Crosshair"),
	DIAMOND("Diamond"),
	QUESTION("Question mark"),
	FLAG("Flag"),
	STAR("Star"),
	SHIELD("Shield");

	private final String label;

	PingSymbol(String label)
	{
		this.label = label;
	}

	static PingSymbol fromWire(String name)
	{
		for (PingSymbol symbol : values())
		{
			if (symbol.name().equals(name))
			{
				return symbol;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
