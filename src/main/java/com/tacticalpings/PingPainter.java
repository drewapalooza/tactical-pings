/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/** Shared by the live overlay and the offline animation preview. */
public final class PingPainter
{
	@FunctionalInterface
	public interface Projection
	{
		Point2D project(double tileX, double tileY);
	}

	private PingPainter()
	{
	}

	public static void paint(Graphics2D graphics, Projection projection, PingType type,
		double progress, boolean reducedMotion, boolean label)
	{
		paint(graphics, projection, PingStyle.defaults(type), progress, reducedMotion, label);
	}

	public static void paint(Graphics2D graphics, Projection projection, PingStyle style,
		double progress, boolean reducedMotion, boolean label)
	{
		paint(graphics, projection, style, progress, reducedMotion, label, null);
	}

	public static void paint(Graphics2D graphics, Projection projection, PingStyle style,
		double progress, boolean reducedMotion, boolean label, String senderName)
	{
		double opacity = PingAnimation.opacity(progress);
		Point2D center = projection.project(0, 0);
		if (center == null || opacity <= 0)
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color color = style.getColor();
			Path2D tile = tile(projection);
			if (tile != null)
			{
				g.setColor(alpha(color, 27 * opacity));
				g.fill(tile);
				glow(g, tile, color, opacity);
			}
			double radius = reducedMotion ? 0.8 : PingAnimation.radius(progress);
			Path2D ring = ring(projection, radius);
			if (ring != null)
			{
				glow(g, ring, color, opacity * 0.82);
			}
			if (!reducedMotion && progress > 0.10)
			{
				Path2D echo = ring(projection, PingAnimation.radius(progress - 0.10));
				if (echo != null)
				{
					g.setStroke(new BasicStroke(1));
					g.setColor(alpha(color, 65 * opacity));
					g.draw(echo);
				}
			}
			Point2D edge = projection.project(0.5, 0.5);
			double size = edge == null ? 11 : Math.max(7, Math.min(15, center.distance(edge) * 0.37));
			glyph(g, style, center.getX(), center.getY(), size, opacity);
			int labelY = (int) (center.getY() + size + 16);
			if (label && !style.getText().isEmpty())
			{
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
				centeredText(g, style.getText(), color, (int) center.getX(), labelY, opacity);
				labelY += 14;
			}
			if (senderName != null && !senderName.isEmpty())
			{
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
				centeredText(g, senderName, new Color(225, 233, 243), (int) center.getX(), labelY, opacity);
			}
		}
		finally
		{
			g.dispose();
		}
	}

	private static void centeredText(Graphics2D g, String text, Color color, int centerX, int y, double opacity)
	{
		int x = centerX - g.getFontMetrics().stringWidth(text) / 2;
		g.setColor(alpha(Color.BLACK, 200 * opacity));
		g.drawString(text, x + 1, y + 1);
		g.setColor(alpha(color, 240 * opacity));
		g.drawString(text, x, y);
	}

	public static void glyph(Graphics2D g, PingType type, double x, double y, double size, double opacity)
	{
		glyph(g, PingStyle.defaults(type), x, y, size, opacity);
	}

	public static void glyph(Graphics2D g, PingStyle style, double x, double y, double size, double opacity)
	{
		Graphics2D icon = (Graphics2D) g.create();
		try
		{
			icon.translate(x, y);
			icon.scale(size / 12, size / 12);
			Path2D path = new Path2D.Double();
			switch (style.getSymbol())
			{
				case EXCLAMATION:
					path.moveTo(0, -9);
					path.lineTo(0, 3);
					break;
				case ARROW:
					path.moveTo(0, -9);
					path.lineTo(0, 8);
					path.moveTo(-6, 2);
					path.lineTo(0, 8);
					path.lineTo(6, 2);
					break;
				case CROSSHAIR:
					path.append(new Ellipse2D.Double(-7, -7, 14, 14), false);
					path.moveTo(-11, 0);
					path.lineTo(-4, 0);
					path.moveTo(4, 0);
					path.lineTo(11, 0);
					path.moveTo(0, -11);
					path.lineTo(0, -4);
					path.moveTo(0, 4);
					path.lineTo(0, 11);
					break;
				case DIAMOND:
					path.moveTo(0, -9);
					path.lineTo(9, 0);
					path.lineTo(0, 9);
					path.lineTo(-9, 0);
					path.closePath();
					break;
				case QUESTION:
					path.moveTo(-6, -5);
					path.curveTo(-6, -13, 8, -12, 7, -4);
					path.curveTo(7, 0, 0, 0, 0, 4);
					break;
				case FLAG:
					path.moveTo(-6, 10);
					path.lineTo(-6, -10);
					path.lineTo(8, -10);
					path.lineTo(4, -4);
					path.lineTo(8, 2);
					path.lineTo(-6, 2);
					break;
				case STAR:
					for (int i = 0; i < 10; i++)
					{
						double angle = -Math.PI / 2 + i * Math.PI / 5;
						double radius = i % 2 == 0 ? 10 : 4.5;
						if (i == 0)
						{
							path.moveTo(Math.cos(angle) * radius, Math.sin(angle) * radius);
						}
						else
						{
							path.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius);
						}
					}
					path.closePath();
					break;
				case SHIELD:
					path.moveTo(0, -10);
					path.lineTo(9, -6);
					path.curveTo(9, 4, 6, 7, 0, 11);
					path.curveTo(-6, 7, -9, 4, -9, -6);
					path.closePath();
					break;
			}
			icon.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			icon.setColor(alpha(Color.BLACK, 130 * opacity));
			icon.draw(path);
			icon.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			icon.setColor(alpha(style.getColor().brighter(), 255 * opacity));
			icon.draw(path);
			if (style.getSymbol() == PingSymbol.EXCLAMATION || style.getSymbol() == PingSymbol.CROSSHAIR
				|| style.getSymbol() == PingSymbol.QUESTION)
			{
				double dotY = style.getSymbol() == PingSymbol.CROSSHAIR ? 0 : 8;
				icon.fill(new Ellipse2D.Double(-1.6, dotY - 1.6, 3.2, 3.2));
			}
		}
		finally
		{
			icon.dispose();
		}
	}

	private static Path2D tile(Projection projection)
	{
		Path2D path = new Path2D.Double();
		double[][] corners = {{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}};
		for (int i = 0; i < corners.length; i++)
		{
			Point2D point = projection.project(corners[i][0], corners[i][1]);
			if (point == null)
			{
				return null;
			}
			if (i == 0)
			{
				path.moveTo(point.getX(), point.getY());
			}
			else
			{
				path.lineTo(point.getX(), point.getY());
			}
		}
		path.closePath();
		return path;
	}

	private static Path2D ring(Projection projection, double radius)
	{
		Path2D path = new Path2D.Double();
		for (int i = 0; i < 64; i++)
		{
			double angle = i * Math.PI * 2 / 64;
			Point2D point = projection.project(Math.cos(angle) * radius, Math.sin(angle) * radius);
			if (point == null)
			{
				return null;
			}
			if (i == 0)
			{
				path.moveTo(point.getX(), point.getY());
			}
			else
			{
				path.lineTo(point.getX(), point.getY());
			}
		}
		path.closePath();
		return path;
	}

	private static void glow(Graphics2D g, Shape shape, Color color, double opacity)
	{
		g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(alpha(color, 16 * opacity));
		g.draw(shape);
		g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(alpha(color, 42 * opacity));
		g.draw(shape);
		g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(alpha(color, 230 * opacity));
		g.draw(shape);
	}

	private static Color alpha(Color color, double alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.max(0, Math.min(255, alpha)));
	}
}
