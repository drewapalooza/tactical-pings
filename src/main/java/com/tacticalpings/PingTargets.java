/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.Text;

@Singleton
public class PingTargets
{
	@Inject
	private Client client;
	@Inject
	private ModelOutlineRenderer renderer;

	public PingSelection fromMenu(MenuEntry entry)
	{
		if (entry == null || entry.getType() == null)
		{
			return null;
		}
		WorldView view = client.getTopLevelWorldView();
		if (entry.getWorldViewId() != WorldView.TOPLEVEL || view == null)
		{
			return null;
		}
		Actor actor = null;
		switch (entry.getType())
		{
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case EXAMINE_NPC:
			case ITEM_USE_ON_NPC:
			case WIDGET_TARGET_ON_NPC:
				actor = entry.getNpc();
				break;
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGHTH_OPTION:
			case ITEM_USE_ON_PLAYER:
			case WIDGET_TARGET_ON_PLAYER:
				actor = entry.getPlayer();
				break;
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case EXAMINE_OBJECT:
			case ITEM_USE_ON_GAME_OBJECT:
			case WIDGET_TARGET_ON_GAME_OBJECT:
				return objectAt(tileAtScene(entry.getParam0(), entry.getParam1()), entry.getIdentifier(), null);
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case EXAMINE_ITEM_GROUND:
			case ITEM_USE_ON_GROUND_ITEM:
			case WIDGET_TARGET_ON_GROUND_ITEM:
				return itemAt(tileAtScene(entry.getParam0(), entry.getParam1()), entry.getIdentifier());
			default:
				return null;
		}
		if (actor == null || actor.getWorldView() != view || actor.isDead())
		{
			return null;
		}
		PingTarget target = actor instanceof NPC
			? new PingTarget(PingTarget.Kind.NPC, ((NPC) actor).getId(), ((NPC) actor).getIndex(), null)
			: new PingTarget(PingTarget.Kind.PLAYER, 0, ((Player) actor).getId(), normalize(actor.getName()));
		return target.isValid() ? new PingSelection(actor.getWorldLocation(), new PingOutline(target, view, actor, null, null, null)) : null;
	}

	public PingOutline resolve(PingTarget target, WorldPoint point)
	{
		if (target == null || !target.isValid())
		{
			return null;
		}
		WorldView view = client.getTopLevelWorldView();
		if (view == null || point.getPlane() != view.getPlane())
		{
			return null;
		}
		if (target.getKind() == PingTarget.Kind.NPC || target.getKind() == PingTarget.Kind.PLAYER)
		{
			Actor actor;
			if (target.getKind() == PingTarget.Kind.NPC)
			{
				NPC npc = view.npcs().byIndex(target.getIndex());
				actor = npc != null && npc.getId() == target.getId() ? npc : null;
			}
			else
			{
				Player player = view.players().byIndex(target.getIndex());
				actor = player != null && target.getName().equals(normalize(player.getName())) ? player : null;
			}
			if (actor == null || actor.isDead() || actor.getWorldView() != view
				|| actor.getWorldLocation().distanceTo(point) > 32)
			{
				return null;
			}
			return new PingOutline(target, view, actor, null, null, null);
		}
		Tile tile = tileAt(point);
		PingSelection selection = target.getKind() == PingTarget.Kind.GROUND_ITEM
			? itemAt(tile, target.getId()) : objectAt(tile, target.getId(), target.getKind());
		return selection == null ? null : selection.getOutline();
	}

	public boolean present(PingOutline outline, WorldPoint point)
	{
		return outline != null && outline.view == client.getTopLevelWorldView()
			&& (outline.tile == null || tileAt(point) == outline.tile) && outline.present();
	}

	public void draw(ActivePing ping, double progress, boolean reducedMotion)
	{
		PingOutline outline = ping.getOutline();
		double pulse = reducedMotion ? 0.5 : 0.5 - 0.5 * Math.cos(progress * Math.PI * 4);
		int alpha = (int) (245 * PingAnimation.opacity(progress) * (0.5 + pulse * 0.5));
		if (alpha <= 0 || !present(outline, ping.getPoint()))
		{
			return;
		}
		Color color = ping.getType().getColor();
		outline.draw(renderer, new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha), 2 + (int) Math.round(pulse * 2));
	}

	private PingSelection objectAt(Tile tile, int id, PingTarget.Kind required)
	{
		if (tile == null)
		{
			return null;
		}
		TileObject selected = null;
		PingTarget.Kind kind = null;
		TileObject[] singles = {tile.getWallObject(), tile.getDecorativeObject(), tile.getGroundObject()};
		PingTarget.Kind[] kinds = {PingTarget.Kind.WALL_OBJECT, PingTarget.Kind.DECORATIVE_OBJECT, PingTarget.Kind.GROUND_OBJECT};
		for (int i = 0; i < singles.length; i++)
		{
			if (singles[i] != null && singles[i].getId() == id && (required == null || required == kinds[i]))
			{
				if (selected != null)
				{
					return null;
				}
				selected = singles[i];
				kind = kinds[i];
			}
		}
		if ((required == null || required == PingTarget.Kind.GAME_OBJECT) && tile.getGameObjects() != null)
		{
			for (GameObject object : tile.getGameObjects())
			{
				if (object != null && object.getId() == id)
				{
					if (selected != null && selected != object)
					{
						return null;
					}
					selected = object;
					kind = PingTarget.Kind.GAME_OBJECT;
				}
			}
		}
		if (selected == null)
		{
			return null;
		}
		PingTarget target = new PingTarget(kind, id, 0, null);
		return new PingSelection(tile.getWorldLocation(), new PingOutline(target, client.getTopLevelWorldView(), null, tile, selected, null));
	}

	private PingSelection itemAt(Tile tile, int id)
	{
		if (tile == null || tile.getGroundItems() == null || tile.getItemLayer() == null)
		{
			return null;
		}
		for (TileItem item : tile.getGroundItems())
		{
			if (item.getId() == id && (tile.getItemLayer().getBottom() == item
				|| tile.getItemLayer().getMiddle() == item || tile.getItemLayer().getTop() == item))
			{
				PingTarget target = new PingTarget(PingTarget.Kind.GROUND_ITEM, id, 0, null);
				return new PingSelection(tile.getWorldLocation(), new PingOutline(target, client.getTopLevelWorldView(), null, tile, null, item));
			}
		}
		return null;
	}

	private Tile tileAt(WorldPoint point)
	{
		WorldView view = client.getTopLevelWorldView();
		LocalPoint local = view == null ? null : LocalPoint.fromWorld(view, point);
		return local == null ? null : tileAtScene(local.getSceneX(), local.getSceneY());
	}

	private Tile tileAtScene(int x, int y)
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null || x < 0 || y < 0 || x >= view.getSizeX() || y >= view.getSizeY())
		{
			return null;
		}
		Tile[][][] tiles = view.getScene().getTiles();
		return tiles[view.getPlane()][x][y];
	}

	private static String normalize(String name)
	{
		return name == null ? "" : Text.toJagexName(name).toLowerCase(Locale.ROOT);
	}
}
