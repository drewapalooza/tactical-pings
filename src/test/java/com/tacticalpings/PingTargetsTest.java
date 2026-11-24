/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.ItemLayer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PingTargetsTest
{
	private static final WorldPoint POINT = new WorldPoint(3250, 3250, 0);
	private final Client client = mock(Client.class);
	private final WorldView view = mock(WorldView.class);
	private final ModelOutlineRenderer renderer = mock(ModelOutlineRenderer.class);
	private final IndexedObjectSet<NPC> npcs = mock(IndexedObjectSet.class);
	private final IndexedObjectSet<Player> players = mock(IndexedObjectSet.class);
	private final Tile[][][] tiles = new Tile[4][104][104];
	private PingTargets targets;
	private PingTargetResolver resolver;

	@Before
	public void setup()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(view);
		when(view.getId()).thenReturn(WorldView.TOPLEVEL);
		when(view.isTopLevel()).thenReturn(true);
		when(view.getBaseX()).thenReturn(3200);
		when(view.getBaseY()).thenReturn(3200);
		when(view.getSizeX()).thenReturn(104);
		when(view.getSizeY()).thenReturn(104);
		doReturn(npcs).when(view).npcs();
		doReturn(players).when(view).players();
		Scene scene = mock(Scene.class);
		when(view.getScene()).thenReturn(scene);
		when(scene.getTiles()).thenReturn(tiles);
		Player local = mock(Player.class);
		when(local.getWorldView()).thenReturn(view);
		when(client.getLocalPlayer()).thenReturn(local);
		Injector injector = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(Client.class).toInstance(client);
				bind(ModelOutlineRenderer.class).toProvider(() -> renderer);
			}
		});
		targets = injector.getInstance(PingTargets.class);
		resolver = injector.getInstance(PingTargetResolver.class);
	}

	@Test
	public void manuallyClickedNpcHasMatchingPulsingOutlineAndSharedIdentity()
	{
		NPC npc = npc(7, new Rectangle(100, 100, 40, 40));
		PingSelection selected = targets.fromMenu(npcMenu(npc));
		assertSame(npc, selected.getOutline().actor);
		assertSame(npc, targets.resolve(selected.getOutline().getTarget(), POINT).actor);
		ActivePing ping = new ActivePing(PingType.ATTACK, POINT, 0, 1600, selected.getOutline());
		targets.draw(ping, 0.25, false);
		targets.draw(ping, 0.5, false);
		ArgumentCaptor<Color> colors = ArgumentCaptor.forClass(Color.class);
		ArgumentCaptor<Integer> widths = ArgumentCaptor.forClass(Integer.class);
		verify(renderer, times(2)).drawOutline(eq((Actor) npc), widths.capture(), colors.capture(), eq(2));
		assertEquals(PingType.ATTACK.getColor().getRed(), colors.getValue().getRed());
		assertTrue(colors.getAllValues().get(0).getAlpha() > colors.getAllValues().get(1).getAlpha());
		assertTrue(widths.getAllValues().get(0) > widths.getAllValues().get(1));
		clearInvocations(renderer);
		targets.draw(ping, 1, false);
		verify(renderer, never()).drawOutline(any(Actor.class), anyInt(), any(), anyInt());
	}

	@Test
	public void reusedNpcIndexCannotInheritOldOutline()
	{
		NPC original = npc(7, new Rectangle(100, 100, 40, 40));
		PingSelection selected = targets.fromMenu(npcMenu(original));
		assertTrue(targets.present(selected.getOutline(), POINT));
		npc(7, new Rectangle(100, 100, 40, 40));
		assertFalse(targets.present(selected.getOutline(), POINT));
		targets.draw(new ActivePing(PingType.GO, POINT, 0, 1600, selected.getOutline()), 0.25, false);
		verify(renderer, never()).drawOutline(any(Actor.class), anyInt(), any(), anyInt());
	}

	@Test
	public void clickedFriendlyPlayerRequiresSameNameAndIndex()
	{
		Player player = mock(Player.class);
		when(player.getId()).thenReturn(17);
		when(player.getName()).thenReturn("A Friend");
		when(player.getWorldLocation()).thenReturn(POINT);
		when(player.getWorldView()).thenReturn(view);
		when(players.byIndex(17)).thenReturn(player);
		MenuEntry entry = menu(MenuAction.PLAYER_FIRST_OPTION, 17);
		when(entry.getPlayer()).thenReturn(player);
		PingSelection selected = targets.fromMenu(entry);
		assertEquals("a friend", selected.getOutline().getTarget().getName());
		assertSame(player, targets.resolve(selected.getOutline().getTarget(), POINT).actor);
		when(player.getName()).thenReturn("Someone Else");
		assertNull(targets.resolve(selected.getOutline().getTarget(), POINT));
	}

	@Test
	public void clickedWorldObjectStopsOutliningAfterDespawn()
	{
		Tile tile = tile();
		GameObject object = mock(GameObject.class);
		when(object.getId()).thenReturn(1234);
		when(tile.getGameObjects()).thenReturn(new GameObject[] {object});
		PingSelection selected = targets.fromMenu(menu(MenuAction.GAME_OBJECT_FIRST_OPTION, 1234));
		assertNotNull(selected);
		assertSame(object, targets.resolve(selected.getOutline().getTarget(), POINT).object);
		targets.draw(new ActivePing(PingType.USE, POINT, 0, 1600, selected.getOutline()), 0.25, false);
		verify(renderer).drawOutline(eq((net.runelite.api.TileObject) object), anyInt(), any(), eq(2));
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		assertFalse(targets.present(selected.getOutline(), POINT));
		assertNull(targets.resolve(selected.getOutline().getTarget(), POINT));
	}

	@Test
	public void droppedItemMustActuallyBeInRenderedItemLayer()
	{
		Tile tile = tile();
		ItemLayer layer = mock(ItemLayer.class);
		TileItem item = mock(TileItem.class);
		when(item.getId()).thenReturn(4151);
		when(tile.getItemLayer()).thenReturn(layer);
		when(tile.getGroundItems()).thenReturn(Collections.singletonList(item));
		MenuEntry entry = menu(MenuAction.GROUND_ITEM_THIRD_OPTION, 4151);
		assertNull(targets.fromMenu(entry));
		when(layer.getTop()).thenReturn(item);
		PingSelection selected = targets.fromMenu(entry);
		assertNotNull(selected);
		targets.draw(new ActivePing(PingType.GO, POINT, 0, 1600, selected.getOutline()), 0.25, false);
		verify(renderer).drawOutline(eq(layer), eq(item), anyInt(), any(), eq(2));
		when(tile.getGroundItems()).thenReturn(Collections.emptyList());
		assertFalse(targets.present(selected.getOutline(), POINT));
	}

	@Test
	public void pressCaptureKeepsClickedEntityWhenLiveHoverAlreadyMoved()
	{
		NPC first = npc(7, new Rectangle(100, 100, 40, 40));
		PingSelection pressed = targets.fromMenu(npcMenu(first));
		NPC second = npc(8, new Rectangle(200, 200, 40, 40));
		MenuEntry secondMenu = npcMenu(second);
		when(client.getMenuEntries()).thenReturn(new MenuEntry[] {secondMenu});
		assertSame(pressed, resolver.captureAtPress(new java.awt.Point(110, 110), pressed));
	}

	@Test
	public void malformedTargetIdentityIsRejected()
	{
		assertFalse(new PingTarget(null, 1, 0, null).isValid());
		assertFalse(new PingTarget(PingTarget.Kind.NPC, 1, -1, null).isValid());
		assertFalse(new PingTarget(PingTarget.Kind.GAME_OBJECT, -1, 0, null).isValid());
		assertFalse(new PingTarget(PingTarget.Kind.PLAYER, 0, 17, "<img=1>bad").isValid());
		assertNull(targets.resolve(new PingTarget(PingTarget.Kind.NPC, 999, 7, null), POINT));
	}

	private NPC npc(int index, Rectangle hull)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(100);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getWorldLocation()).thenReturn(POINT);
		when(npc.getWorldView()).thenReturn(view);
		when(npc.getConvexHull()).thenReturn(hull);
		when(npcs.byIndex(index)).thenReturn(npc);
		return npc;
	}

	private MenuEntry npcMenu(NPC npc)
	{
		MenuEntry entry = menu(MenuAction.NPC_SECOND_OPTION, npc.getIndex());
		when(entry.getNpc()).thenReturn(npc);
		return entry;
	}

	private MenuEntry menu(MenuAction action, int id)
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(action);
		when(entry.getWorldViewId()).thenReturn(WorldView.TOPLEVEL);
		when(entry.getIdentifier()).thenReturn(id);
		when(entry.getParam0()).thenReturn(50);
		when(entry.getParam1()).thenReturn(50);
		return entry;
	}

	private Tile tile()
	{
		Tile tile = mock(Tile.class);
		when(tile.getWorldLocation()).thenReturn(POINT);
		tiles[0][50][50] = tile;
		return tile;
	}
}
