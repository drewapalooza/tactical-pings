/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PingSenderTest
{
	@Test
	public void namesAreBoundedPlainTextAndUnknownNamesAreOmitted()
	{
		assertEquals("", ActivePing.cleanSenderName(null));
		assertEquals("", ActivePing.cleanSenderName("<unknown>"));
		assertEquals("", ActivePing.cleanSenderName("x".repeat(257)));
		assertEquals("Dawn Rune", ActivePing.cleanSenderName(" <img=1>Dawn\u00a0Rune\u202e\n "));
		assertEquals("x".repeat(32), ActivePing.cleanSenderName("x".repeat(64)));
		assertFalse(new TacticalPingsConfig() {}.senderNames());
	}

	@Test
	public void senderIsDrawnBelowTextOrBelowSymbolWhenTextIsDisabled()
	{
		BufferedImage original = render(true, null, 0.44);
		BufferedImage withName = render(true, "Dawn Rune", 0.44);
		assertTrue(different(original, withName, 90, 115));
		assertFalse(different(original, withName, 0, 86));
		assertTrue(different(render(false, null, 0.44), render(false, "Dawn Rune", 0.44), 72, 96));
		assertFalse(different(render(true, null, 1), render(true, "Dawn Rune", 1), 0, 128));
	}

	private static BufferedImage render(boolean labels, String sender, double progress)
	{
		BufferedImage image = new BufferedImage(160, 128, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		PingPainter.paint(g, (x, y) -> new Point2D.Double(80 + x * 10 - y * 6, 60 + x * 4 + y * 6),
			PingStyle.defaults(PingType.GO), progress, false, labels, sender);
		g.dispose();
		return image;
	}

	private static boolean different(BufferedImage first, BufferedImage second, int top, int bottom)
	{
		for (int y = top; y < bottom; y++)
		{
			for (int x = 0; x < first.getWidth(); x++)
			{
				if (first.getRGB(x, y) != second.getRGB(x, y))
				{
					return true;
				}
			}
		}
		return false;
	}
}
