/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;

public enum PingType
{
	FLEE("Flee", new Color(255, 215, 65)),
	GO("Go", new Color(109, 244, 93)),
	ATTACK("Attack", new Color(255, 91, 96)),
	USE("Use", new Color(82, 178, 255));

	private final String label;
	private final Color color;

	PingType(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	public String getLabel()
	{
		return label;
	}

	public Color getColor()
	{
		return color;
	}

	public static PingType fromWire(String value)
	{
		for (PingType type : values())
		{
			if (type.name().equals(value))
			{
				return type;
			}
		}
		return null;
	}
}
