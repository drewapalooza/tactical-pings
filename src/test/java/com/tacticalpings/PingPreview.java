/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Offline developer tool only. No screenshot or game assets are shipped. */
public final class PingPreview
{
	public static void main(String[] args) throws Exception
	{
		File directory = new File("build/previews");
		if (!directory.isDirectory() && !directory.mkdirs())
		{
			throw new IllegalStateException("Cannot create preview directory");
		}
		BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(17, 21, 26));
		g.fillRect(0, 0, 1280, 820);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
		g.setColor(new Color(235, 240, 247));
		g.drawString("TACTICAL PINGS", 38, 48);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		g.setColor(new Color(155, 169, 184));
		g.drawString("Live Java renderer · circular ground ripple · distinct symbols", 38, 74);
		for (int i = 0; i < 4; i++)
		{
			PingType type = PingType.values()[i];
			int left = 30 + (i % 2) * 420;
			int top = 100 + (i / 2) * 270;
			Graphics2D panel = (Graphics2D) g.create(left, top, 400, 250);
			drawFloor(panel, 200, 143);
			panel.setColor(type.getColor());
			panel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
			panel.drawString(type.getLabel().toUpperCase(java.util.Locale.ROOT), 18, 30);
			PingPainter.paint(panel, (x, y) -> project(x, y, 200, 143), type, 0.44, false, false);
			panel.dispose();
		}
		PingWheelOverlay.paint(g, 1070, 267, PingType.FLEE);
		g.setColor(new Color(225, 233, 243));
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		g.drawString("ALT + CLICK + DRAG", 973, 409);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		g.setColor(new Color(155, 169, 184));
		g.drawString("Quick click: Go", 973, 438);
		g.drawString("Release: send ping", 973, 462);
		g.drawString("Escape / right-click: cancel", 973, 486);
		g.drawString("RuneLite Party: shared pings", 973, 530);
		double[] times = {0.12, 0.34, 0.52, 0.72, 0.88};
		String[] labels = {"APPEAR", "EXPAND", "TURN", "RETURN", "DISSIPATE"};
		for (int i = 0; i < times.length; i++)
		{
			final int cx = 142 + i * 249;
			final int cy = 714;
			PingPainter.paint(g, (x, y) -> new Point2D.Double(cx + x * 34 - y * 24, cy + x * 16 + y * 22),
				PingType.GO, times[i], false, false);
			g.setColor(new Color(155, 169, 184));
			g.drawString(labels[i], cx - g.getFontMetrics().stringWidth(labels[i]) / 2, 790);
		}
		g.dispose();
		ImageIO.write(image, "png", new File(directory, "renderer.png"));
		BufferedImage icon = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig = icon.createGraphics();
		ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		PingPainter.paint(ig, (x, y) -> new Point2D.Double(24 + x * 13 - y * 8, 24 + x * 5 + y * 9),
			PingType.GO, 0.46, false, false);
		ig.dispose();
		ImageIO.write(icon, "png", new File(directory, "icon.png"));
	}

	private static Point2D project(double x, double y, double cx, double cy)
	{
		return new Point2D.Double(cx + x * 43 - y * 34, cy + x * 23 + y * 28);
	}

	private static void drawFloor(Graphics2D g, int cx, int cy)
	{
		g.setColor(new Color(38, 40, 37));
		g.fillRect(0, 0, 400, 250);
		for (int x = -8; x < 8; x++)
		{
			for (int y = -8; y < 8; y++)
			{
				Path2D tile = new Path2D.Double();
				double[][] corners = {{x - 0.48, y - 0.48}, {x + 0.48, y - 0.48}, {x + 0.48, y + 0.48}, {x - 0.48, y + 0.48}};
				for (int i = 0; i < 4; i++)
				{
					Point2D point = project(corners[i][0], corners[i][1], cx, cy);
					if (i == 0)
					{
						tile.moveTo(point.getX(), point.getY());
					}
					else
					{
						tile.lineTo(point.getX(), point.getY());
					}
				}
				tile.closePath();
				int shade = 48 + Math.floorMod(x * 17 + y * 11, 12);
				g.setColor(new Color(shade, shade + 1, shade - 4));
				g.fill(tile);
			}
		}
	}
}
