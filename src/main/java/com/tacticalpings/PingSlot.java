/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;

/** Slot identity is independent of the sender's wheel size and cosmetic design. */
public enum PingSlot
{
	ONE(PingType.FLEE),
	TWO(PingType.GO),
	THREE(PingType.ATTACK),
	FOUR(PingType.USE),
	FIVE(PingType.GO),
	SIX(PingType.GO),
	SEVEN(PingType.GO),
	EIGHT(PingType.GO);

	private final PingType legacyType;

	PingSlot(PingType legacyType)
	{
		this.legacyType = legacyType;
	}

	public PingType getLegacyType()
	{
		return legacyType;
	}

	public PingStyle getDefaultStyle()
	{
		switch (this)
		{
			case FIVE:
				return new PingStyle(new Color(192, 135, 255), PingSymbol.FLAG, PingSound.BELL, "Group");
			case SIX:
				return new PingStyle(new Color(255, 167, 75), PingSymbol.QUESTION, PingSound.SONAR, "Help");
			case SEVEN:
				return new PingStyle(new Color(88, 232, 221), PingSymbol.STAR, PingSound.CHIRP, "Focus");
			case EIGHT:
				return new PingStyle(new Color(235, 240, 250), PingSymbol.SHIELD, PingSound.CHORD, "Defend");
			default:
				return PingStyle.defaults(legacyType);
		}
	}

	public static PingSlot fromType(PingType type)
	{
		return values()[type.ordinal()];
	}

	@Override
	public String toString()
	{
		return "Ping " + (ordinal() + 1);
	}
}
