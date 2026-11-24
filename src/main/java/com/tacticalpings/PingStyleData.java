/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;

/** Optional protocol-1 extension. Malformed or newer designs fall back to the original action. */
final class PingStyleData
{
	private Integer rgb;
	private String symbol;
	private String sound;
	private String text;

	PingStyleData()
	{
		// Missing fields remain invalid when read by RuneLite's transport serializer.
	}

	PingStyleData(PingStyle style)
	{
		rgb = style.getColor().getRGB() & 0xffffff;
		symbol = style.getSymbol().name();
		sound = style.getSound().name();
		text = style.getText();
	}

	PingStyle resolve(PingType type)
	{
		PingSymbol resolvedSymbol = PingSymbol.fromWire(symbol);
		PingSound resolvedSound = PingSound.fromWire(sound);
		if (rgb == null || rgb < 0 || rgb > 0xffffff || resolvedSymbol == null || resolvedSound == null
			|| !PingStyle.validText(text))
		{
			return PingStyle.defaults(type);
		}
		return new PingStyle(new Color(rgb), resolvedSymbol, resolvedSound, text);
	}
}
