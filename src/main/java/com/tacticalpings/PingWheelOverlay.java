/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PingWheelOverlay extends Overlay
{
	@Inject
	private Client client;
	@Inject
	private PingInput input;
	@Inject
	private TacticalPingsConfig config;

	@Inject
	PingWheelOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PingInput.Wheel wheel = input.getWheel();
		if (wheel != null)
		{
			Point cursor = input.getCursor();
			int inset = PingWheelLayout.radius(wheel.count) + 6;
			int x = Math.max(inset, Math.min(client.getCanvasWidth() - inset, wheel.origin.x));
			int y = Math.max(inset, Math.min(client.getCanvasHeight() - inset, wheel.origin.y));
			paint(graphics, x, y, PingWheelLayout.choose(cursor.x - wheel.origin.x,
				cursor.y - wheel.origin.y, wheel.count, wheel.quick), wheel.count, config);
		}
		else if (input.isArmed())
		{
			Point cursor = input.getCursor();
			paintArmed(graphics, cursor, client.getCanvasWidth(), client.getCanvasHeight());
		}
		return null;
	}

	private static void paintArmed(Graphics2D graphics, Point cursor, int canvasWidth, int canvasHeight)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
			String text = "Ping ready: click & drag";
			int width = g.getFontMetrics().stringWidth(text) + 20;
			int x = Math.max(4, Math.min(canvasWidth - width - 4, cursor.x + 18));
			int y = Math.max(4, Math.min(canvasHeight - 30, cursor.y + 20));
			g.setColor(new Color(12, 16, 22, 230));
			g.fillRoundRect(x, y, width, 26, 8, 8);
			g.setColor(new Color(100, 170, 225, 180));
			g.drawRoundRect(x, y, width, 26, 8, 8);
			g.setColor(new Color(230, 238, 246));
			g.drawString(text, x + 10, y + 17);
		}
		finally
		{
			g.dispose();
		}
	}

	static void paint(Graphics2D graphics, int x, int y, PingType selected)
	{
		paint(graphics, x, y, PingSlot.fromType(selected), 4, new TacticalPingsConfig() {});
	}

	static void paint(Graphics2D graphics, int x, int y, PingSlot selected, int requested, TacticalPingsConfig config)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			int count = PingWheelLayout.count(requested);
			int radius = PingWheelLayout.radius(count);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(12, 16, 22, 225));
			g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
			g.setColor(new Color(135, 150, 165, 75));
			g.setStroke(new BasicStroke(1));
			g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
			for (int i = 0; i < count; i++)
			{
				PingSlot slot = PingSlot.values()[i];
				PingStyle style = PingStyle.fromConfig(config, slot);
				if (slot == selected)
				{
					g.setColor(new Color(style.getColor().getRed(), style.getColor().getGreen(), style.getColor().getBlue(), 38));
					g.fill(new Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2,
						90 - i * 360.0 / count, -360.0 / count, Arc2D.PIE));
					g.setColor(style.getColor());
					g.setStroke(new BasicStroke(2));
					g.draw(new Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2,
						90 - i * 360.0 / count, -360.0 / count, Arc2D.OPEN));
				}
				if (count > 1)
				{
					double boundary = -Math.PI / 2 + i * Math.PI * 2 / count;
					g.setColor(new Color(135, 150, 165, 65));
					g.setStroke(new BasicStroke(1));
					g.draw(new Line2D.Double(x + Math.cos(boundary) * 19, y + Math.sin(boundary) * 19,
						x + Math.cos(boundary) * radius, y + Math.sin(boundary) * radius));
				}
				double angle = PingWheelLayout.angle(i, count);
				double distance = count == 1 ? 0 : radius * 0.64;
				int px = x + (int) Math.round(Math.cos(angle) * distance);
				int py = y + (int) Math.round(Math.sin(angle) * distance);
				PingPainter.glyph(g, style, px, py - 4, 11, slot == selected ? 1 : 0.65);
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
				g.setColor(slot == selected ? style.getColor() : new Color(175, 183, 190));
				String label = wheelText(g, style.getText(), count > 4 ? 60 : 78);
				g.drawString(label, px - g.getFontMetrics().stringWidth(label) / 2, py + 17);
			}
			if (count > 1)
			{
				g.setColor(new Color(12, 16, 22));
				g.fill(new Ellipse2D.Double(x - 18, y - 18, 36, 36));
				g.setColor(new Color(135, 150, 165, 75));
				g.setStroke(new BasicStroke(1));
				g.draw(new Ellipse2D.Double(x - 18, y - 18, 36, 36));
				g.setColor(new Color(185, 193, 202));
				g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
			}
		}
		finally
		{
			g.dispose();
		}
	}

	private static String wheelText(Graphics2D g, String text, int maxWidth)
	{
		if (g.getFontMetrics().stringWidth(text) <= maxWidth)
		{
			return text;
		}
		int end = text.length();
		while (end > 0 && g.getFontMetrics().stringWidth(text.substring(0, end) + "\u2026") > maxWidth)
		{
			end = text.offsetByCodePoints(end, -1);
		}
		return text.substring(0, end) + "\u2026";
	}
}
