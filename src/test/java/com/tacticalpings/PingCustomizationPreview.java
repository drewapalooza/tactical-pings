/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Offline examples drawn by the same wheel and ripple painters as the plugin. */
public final class PingCustomizationPreview
{
	public static void main(String[] args) throws Exception
	{
		BufferedImage image = new BufferedImage(1280, 900, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(17, 21, 26));
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.setColor(new Color(235, 240, 247));
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
		g.drawString("AYRA'S PINGS", 32, 48);
		g.setColor(new Color(155, 169, 184));
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
		g.drawString("Choose 1–8 pings · shared designs · your own words", 32, 79);
		int[] counts = {1, 2, 4, 8};
		TacticalPingsConfig defaults = new TacticalPingsConfig() {};
		for (int i = 0; i < counts.length; i++)
		{
			int x = 160 + 320 * i;
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
			g.setColor(new Color(235, 240, 247));
			centered(g, counts[i] + (counts[i] == 1 ? " PING" : " PINGS"), x, 142);
			PingWheelOverlay.paint(g, x, 304, PingSlot.ONE, counts[i], defaults);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
			g.setColor(new Color(155, 169, 184));
			String[] captions = {"One action", "Left / right halves", "Four quadrants", "Two per quadrant"};
			centered(g, captions[i], x, 465);
		}
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		g.setColor(new Color(235, 240, 247));
		g.drawString("YOUR DESIGNS · OPTIONAL SENDER NAMES", 32, 530);
		PingStyle[] designs = {
			new PingStyle(new Color(192, 135, 255), PingSymbol.FLAG, PingSound.BELL, "Stack here"),
			new PingStyle(new Color(255, 167, 75), PingSymbol.QUESTION, PingSound.SONAR, "Need help?"),
			new PingStyle(new Color(88, 232, 221), PingSymbol.STAR, PingSound.CHIRP, "Focus boss"),
			new PingStyle(new Color(105, 175, 255), PingSymbol.SHIELD, PingSound.CHORD, "Hold position")
		};
		String[] senders = {"Ayra", "Dawn Rune", "Oak Sage", "Lunar Fox"};
		for (int i = 0; i < designs.length; i++)
		{
			Graphics2D panel = (Graphics2D) g.create(20 + i * 320, 558, 300, 245);
			PingPreview.drawFloor(panel, 150, 130);
			PingPainter.paint(panel, (x, y) -> new Point2D.Double(150 + x * 43 - y * 34, 130 + x * 23 + y * 28),
				designs[i], 0.44, false, true, senders[i]);
			panel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
			panel.setColor(new Color(220, 227, 235));
			centered(panel, "Sound: " + designs[i].getSound(), 150, 224);
			panel.dispose();
		}
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
		g.setColor(new Color(155, 169, 184));
		centered(g, "Color picker  ·  8 symbols  ·  8 chimes + Silent  ·  Custom text", 640, 858);
		g.dispose();
		ImageIO.write(image, "png", new File("build/previews/customization.png"));
	}

	private static void centered(Graphics2D g, String text, int x, int y)
	{
		g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
	}
}
