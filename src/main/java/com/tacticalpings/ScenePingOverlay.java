/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class ScenePingOverlay extends Overlay
{
	@Inject
	private Client client;
	@Inject
	private TacticalPingsPlugin plugin;
	@Inject
	private TacticalPingsConfig config;
	@Inject
	private PingTargets targets;

	@Inject
	ScenePingOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = System.nanoTime();
		for (ActivePing ping : plugin.getActive())
		{
			if (!plugin.canShow(ping.getPoint()))
			{
				continue;
			}
			LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), ping.getPoint());
			PingPainter.paint(graphics, (x, y) ->
			{
				LocalPoint sample = local.plus((int) Math.round(x * 128), (int) Math.round(y * 128));
				Point point = Perspective.localToCanvas(client, sample, ping.getPoint().getPlane(), 2);
				return point == null ? null : new Point2D.Double(point.getX(), point.getY());
			}, ping.getStyle(), ping.progress(now), config.reducedMotion(), config.labels());
			if (config.targetOutlines())
			{
				targets.draw(ping, ping.progress(now), config.reducedMotion());
			}
		}
		return null;
	}
}
