/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MinimapPingOverlay extends Overlay
{
	@Inject
	private Client client;
	@Inject
	private TacticalPingsPlugin plugin;
	@Inject
	private TacticalPingsConfig config;
	@Inject
	private PingTargetResolver resolver;

	@Inject
	MinimapPingOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Rectangle bounds = resolver.minimapBounds();
		if (!config.minimap() || bounds == null)
		{
			return null;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.clip(PingTargetResolver.minimapClip(bounds));
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			long now = System.nanoTime();
			for (ActivePing ping : plugin.getActive())
			{
				if (!plugin.canShow(ping.getPoint()))
				{
					continue;
				}
				LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), ping.getPoint());
				Point point = Perspective.localToMinimap(client, local);
				if (point == null)
				{
					continue;
				}
				double t = ping.progress(now);
				double radius = config.reducedMotion() ? 9 : 3 + 10 * PingAnimation.radius(t);
				g.setColor(new java.awt.Color(ping.getStyle().getColor().getRed(), ping.getStyle().getColor().getGreen(),
					ping.getStyle().getColor().getBlue(), (int) (230 * PingAnimation.opacity(t))));
				g.setStroke(new BasicStroke(1.5f));
				g.draw(new Ellipse2D.Double(point.getX() - radius, point.getY() - radius, radius * 2, radius * 2));
				PingPainter.glyph(g, ping.getStyle(), point.getX(), point.getY(), 5, PingAnimation.opacity(t));
			}
		}
		finally
		{
			g.dispose();
		}
		return null;
	}
}
