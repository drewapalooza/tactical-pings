/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Point;
import java.util.List;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.ItemLayer;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/** Captures one local entity reference. It never retargets a reused NPC/player index. */
public final class PingOutline
{
	final PingTarget target;
	final WorldView view;
	final Actor actor;
	final Tile tile;
	final TileObject object;
	final TileItem item;
	final ItemLayer layer;

	PingOutline(PingTarget target, WorldView view, Actor actor, Tile tile, TileObject object, TileItem item)
	{
		this.target = target;
		this.view = view;
		this.actor = actor;
		this.tile = tile;
		this.object = object;
		this.item = item;
		layer = item == null ? null : tile.getItemLayer();
	}

	public PingTarget getTarget()
	{
		return target;
	}

	public boolean contains(Point mouse)
	{
		Shape shape = actor != null ? actor.getConvexHull() : object != null ? object.getClickbox()
			: layer != null ? layer.getCanvasTilePoly() : null;
		return shape != null && shape.contains(mouse);
	}

	boolean present()
	{
		if (actor != null)
		{
			return !actor.isDead() && actor.getWorldView() == view
				&& (!(actor instanceof NPC) || ((NPC) actor).getId() == target.getId())
				&& (target.getKind() == PingTarget.Kind.NPC
				? view.npcs().byIndex(target.getIndex()) == actor
				: view.players().byIndex(target.getIndex()) == actor);
		}
		if (item != null)
		{
			List<TileItem> items = tile.getGroundItems();
			return layer != null && layer == tile.getItemLayer() && items != null
				&& items.stream().anyMatch(candidate -> candidate == item)
				&& (layer.getBottom() == item || layer.getMiddle() == item || layer.getTop() == item);
		}
		if (object == tile.getWallObject() || object == tile.getDecorativeObject() || object == tile.getGroundObject())
		{
			return object != null;
		}
		if (tile.getGameObjects() != null)
		{
			for (TileObject candidate : tile.getGameObjects())
			{
				if (candidate == object)
				{
					return true;
				}
			}
		}
		return false;
	}

	void draw(ModelOutlineRenderer renderer, Color color, int width)
	{
		if (actor != null)
		{
			renderer.drawOutline(actor, width, color, 2);
		}
		else if (item != null)
		{
			renderer.drawOutline(layer, item, width, color, 2);
		}
		else
		{
			renderer.drawOutline(object, width, color, 2);
		}
	}
}
