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
			int x = Math.max(106, Math.min(client.getCanvasWidth() - 106, wheel.origin.x));
			int y = Math.max(100, Math.min(client.getCanvasHeight() - 100, wheel.origin.y));
			paint(graphics, x, y, PingInput.choose(cursor.x - wheel.origin.x, cursor.y - wheel.origin.y));
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
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(12, 16, 22, 225));
			g.fill(new Ellipse2D.Double(x - 86, y - 86, 172, 172));
			g.setColor(new Color(135, 150, 165, 75));
			g.setStroke(new BasicStroke(1));
			g.draw(new Ellipse2D.Double(x - 86, y - 86, 172, 172));
			int[][] positions = {{0, -54}, {54, 0}, {0, 54}, {-54, 0}};
			PingType[] types = PingType.values();
			for (int i = 0; i < types.length; i++)
			{
				PingType type = types[i];
				int px = x + positions[i][0];
				int py = y + positions[i][1];
				if (type == selected)
				{
					g.setColor(new Color(type.getColor().getRed(), type.getColor().getGreen(), type.getColor().getBlue(), 40));
					g.fill(new Ellipse2D.Double(px - 25, py - 24, 50, 48));
				}
				PingPainter.glyph(g, type, px, py - 4, 10, type == selected ? 1 : 0.65);
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
				g.setColor(type == selected ? type.getColor() : new Color(175, 183, 190));
				g.drawString(type.getLabel(), px - g.getFontMetrics().stringWidth(type.getLabel()) / 2, py + 17);
			}
			g.setColor(new Color(185, 193, 202));
			g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
		}
		finally
		{
			g.dispose();
		}
	}
}
