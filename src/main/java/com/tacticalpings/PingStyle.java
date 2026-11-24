/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;

/** Immutable design snapshot, kept for the entire ping lifetime. */
public final class PingStyle
{
	public static final int MAX_TEXT_LENGTH = 64;
	private final Color color;
	private final PingSymbol symbol;
	private final PingSound sound;
	private final String text;

	public PingStyle(Color color, PingSymbol symbol, PingSound sound, String text)
	{
		if (color == null || symbol == null || sound == null || !validText(text))
		{
			throw new IllegalArgumentException("Invalid ping design");
		}
		this.color = new Color(color.getRGB() & 0xffffff);
		this.symbol = symbol;
		this.sound = sound;
		this.text = text;
	}

	public static PingStyle defaults(PingType type)
	{
		switch (type)
		{
			case FLEE:
				return new PingStyle(type.getColor(), PingSymbol.EXCLAMATION, PingSound.FALLING, type.getLabel());
			case ATTACK:
				return new PingStyle(type.getColor(), PingSymbol.CROSSHAIR, PingSound.STRIKES, type.getLabel());
			case USE:
				return new PingStyle(type.getColor(), PingSymbol.DIAMOND, PingSound.SPARKLE, type.getLabel());
			default:
				return new PingStyle(type.getColor(), PingSymbol.ARROW, PingSound.RISING, type.getLabel());
		}
	}

	public static PingStyle fromConfig(TacticalPingsConfig config, PingType type)
	{
		return fromConfig(config, PingSlot.fromType(type));
	}

	public static PingStyle fromConfig(TacticalPingsConfig config, PingSlot slot)
	{
		switch (slot)
		{
			case ONE:
				return configured(slot, config.fleeColor(), config.fleeSymbol(), config.fleeSound(), config.fleeText());
			case THREE:
				return configured(slot, config.attackColor(), config.attackSymbol(), config.attackSound(), config.attackText());
			case FOUR:
				return configured(slot, config.useColor(), config.useSymbol(), config.useSound(), config.useText());
			case FIVE:
				return configured(slot, config.ping5Color(), config.ping5Symbol(), config.ping5Sound(), config.ping5Text());
			case SIX:
				return configured(slot, config.ping6Color(), config.ping6Symbol(), config.ping6Sound(), config.ping6Text());
			case SEVEN:
				return configured(slot, config.ping7Color(), config.ping7Symbol(), config.ping7Sound(), config.ping7Text());
			case EIGHT:
				return configured(slot, config.ping8Color(), config.ping8Symbol(), config.ping8Sound(), config.ping8Text());
			default:
				return configured(slot, config.goColor(), config.goSymbol(), config.goSound(), config.goText());
		}
	}

	private static PingStyle configured(PingSlot slot, Color color, PingSymbol symbol, PingSound sound, String text)
	{
		PingStyle fallback = slot.getDefaultStyle();
		return new PingStyle(color == null ? fallback.color : color, symbol == null ? fallback.symbol : symbol,
			sound == null ? fallback.sound : sound, text == null ? fallback.text : cleanText(text));
	}

	/** Single-line plain Unicode text; no invisible controls or unpaired surrogates. */
	static boolean validText(String text)
	{
		if (text == null || text.length() > MAX_TEXT_LENGTH * 2
			|| text.codePointCount(0, text.length()) > MAX_TEXT_LENGTH)
		{
			return false;
		}
		return text.codePoints().allMatch(PingStyle::printable);
	}

	private static boolean printable(int codePoint)
	{
		int category = Character.getType(codePoint);
		return category != Character.CONTROL && category != Character.FORMAT
			&& category != Character.SURROGATE && category != Character.LINE_SEPARATOR
			&& category != Character.PARAGRAPH_SEPARATOR;
	}

	static String cleanText(String text)
	{
		StringBuilder result = new StringBuilder();
		// Bound processing even if a configuration file was edited outside RuneLite.
		text.codePoints().limit(MAX_TEXT_LENGTH * 4L).filter(PingStyle::printable)
			.limit(MAX_TEXT_LENGTH).forEach(result::appendCodePoint);
		return result.toString();
	}

	public Color getColor()
	{
		return color;
	}

	public PingSymbol getSymbol()
	{
		return symbol;
	}

	public PingSound getSound()
	{
		return sound;
	}

	public String getText()
	{
		return text;
	}

	public String getMenuText()
	{
		StringBuilder escaped = new StringBuilder();
		text.codePoints().forEach(codePoint ->
		{
			if (codePoint == '<')
			{
				escaped.append("<lt>");
			}
			else if (codePoint == '>')
			{
				escaped.append("<gt>");
			}
			else
			{
				escaped.appendCodePoint(codePoint);
			}
		});
		return escaped.toString();
	}
}
