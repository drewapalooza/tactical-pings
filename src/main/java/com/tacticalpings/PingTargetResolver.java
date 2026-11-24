/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@Singleton
public class PingTargetResolver
{
	@Inject
	private Client client;
	@Inject
	private PingTargets targets;

	public WorldPoint resolve(Point mouse)
	{
		PingSelection selection = resolveSelection(mouse);
		return selection == null ? null : selection.getPoint();
	}

	public PingSelection resolveSelection(Point mouse)
	{
		WorldView view = client.getTopLevelWorldView();
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen() || view == null
			|| client.getLocalPlayer() == null || !client.getLocalPlayer().getWorldView().isTopLevel())
		{
			return null;
		}
		Widget worldMap = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (worldMap != null && !worldMap.isHidden())
		{
			return null;
		}
		Rectangle map = minimapBounds();
		if (map != null && map.contains(mouse.getX(), mouse.getY()))
		{
			if (!minimapClip(map).contains(mouse.getX(), mouse.getY()))
			{
				return null;
			}
			LocalPoint origin = client.getLocalPlayer().getLocalLocation();
			Point a = Perspective.localToMinimap(client, origin);
			Point b = Perspective.localToMinimap(client, origin.plus(512, 0));
			Point c = Perspective.localToMinimap(client, origin.plus(0, 512));
			if (a == null || b == null || c == null)
			{
				return null;
			}
			Point2D offset = invert(mouse.getX() - a.getX(), mouse.getY() - a.getY(),
				b.getX() - a.getX(), b.getY() - a.getY(), c.getX() - a.getX(), c.getY() - a.getY());
			if (offset == null)
			{
				return null;
			}
			LocalPoint local = origin.plus((int) Math.round(offset.getX() * 512), (int) Math.round(offset.getY() * 512));
			return local.isInScene() ? new PingSelection(WorldPoint.fromLocal(client, local), null) : null;
		}
		boolean ground = false;
		MenuEntry[] entries = client.getMenuEntries();
		for (int i = entries.length - 1; i >= 0; i--)
		{
			MenuEntry entry = entries[i];
			if (entry == null)
			{
				continue;
			}
			PingSelection entity = targets.fromMenu(entry);
			if (entity != null)
			{
				return entity;
			}
			ground |= entry.getType() == MenuAction.WALK && entry.getWorldViewId() == WorldView.TOPLEVEL;
		}
		Tile tile = view.getSelectedSceneTile();
		return ground && tile != null ? new PingSelection(tile.getWorldLocation(), null) : null;
	}

	/** Resolve the immutable mouse-down position, even if dragging advanced the live hover. */
	public PingSelection captureAtPress(java.awt.Point mouse, PingSelection previous)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return null;
		}
		Rectangle map = minimapBounds();
		if (map != null && map.contains(mouse))
		{
			return resolveSelection(new Point(mouse.x, mouse.y));
		}
		PingSelection current = resolveSelection(new Point(mouse.x, mouse.y));
		if (current == null)
		{
			return null;
		}
		if (hits(current, mouse))
		{
			return current;
		}
		if (hits(previous, mouse))
		{
			return previous;
		}
		// A rapid mouse move can precede the next client tick. Pick the nearby ground
		// at the saved down position instead of requiring the user to pause.
		PingSelection anchor = previous != null ? previous : current;
		if (anchor == null)
		{
			return null;
		}
		WorldView view = client.getTopLevelWorldView();
		LocalPoint center = LocalPoint.fromWorld(view, anchor.getPoint());
		if (center == null)
		{
			return null;
		}
		for (int radius = 0; radius <= 8; radius++)
		{
			for (int dx = -radius; dx <= radius; dx++)
			{
				for (int dy = -radius; dy <= radius; dy++)
				{
					if (Math.max(Math.abs(dx), Math.abs(dy)) != radius)
					{
						continue;
					}
					LocalPoint sample = center.plus(dx * 128, dy * 128);
					if (!sample.isInScene())
					{
						continue;
					}
					Polygon tile = Perspective.getCanvasTilePoly(client, sample);
					if (tile != null && tile.contains(mouse))
					{
						return new PingSelection(WorldPoint.fromLocal(client, sample), null);
					}
				}
			}
		}
		return null;
	}

	private boolean hits(PingSelection selection, java.awt.Point mouse)
	{
		if (selection == null)
		{
			return false;
		}
		if (selection.getOutline() != null)
		{
			return selection.getOutline().contains(mouse);
		}
		LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), selection.getPoint());
		Polygon tile = local == null ? null : Perspective.getCanvasTilePoly(client, local);
		return tile != null && tile.contains(mouse);
	}

	public Rectangle minimapBounds()
	{
		// Respect game-disabled/black minimaps, including Barrows and cutscenes.
		if (client.getVarbitValue(VarbitID.MINIMAP_STATE) != 0)
		{
			return null;
		}
		int widgetId = !client.isResized() ? InterfaceID.Toplevel.MINIMAP
			: client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
			? InterfaceID.ToplevelPreEoc.MINIMAP : InterfaceID.ToplevelOsrsStretch.MINIMAP;
		Widget widget = client.getWidget(widgetId);
		return widget == null || widget.isHidden() ? null : widget.getBounds();
	}

	public static Ellipse2D minimapClip(Rectangle bounds)
	{
		double radius = Math.min(bounds.width, bounds.height) / 2.0 - 7;
		return new Ellipse2D.Double(bounds.getCenterX() - radius, bounds.getCenterY() - radius, radius * 2, radius * 2);
	}

	/** Inverse of the minimap's projected basis; handles zoom and camera rotation. */
	static Point2D invert(double x, double y, double ax, double ay, double bx, double by)
	{
		double determinant = ax * by - ay * bx;
		return Math.abs(determinant) < 0.001 ? null
			: new Point2D.Double((x * by - y * bx) / determinant, (ax * y - ay * x) / determinant);
	}
}
