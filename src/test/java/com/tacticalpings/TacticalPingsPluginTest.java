/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TacticalPingsPluginTest
{
	private static final WorldPoint TILE = new WorldPoint(3225, 3228, 0);
	private final Client client = mock(Client.class);
	private final WorldView view = mock(WorldView.class);
	private final ClientThread clientThread = mock(ClientThread.class);
	private final PartyService party = mock(PartyService.class);
	private final WSClient wsClient = mock(WSClient.class);
	private final PingAudio audio = mock(PingAudio.class);
	private final PingTargets targets = mock(PingTargets.class);
	private final TacticalPingsPlugin plugin = new TacticalPingsPlugin();
	private final TacticalPingsConfig config = mock(TacticalPingsConfig.class);

	@Before
	public void setup()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(view);
		when(client.getWorld()).thenReturn(301);
		when(view.getId()).thenReturn(WorldView.TOPLEVEL);
		when(view.isTopLevel()).thenReturn(true);
		when(view.getBaseX()).thenReturn(3200);
		when(view.getBaseY()).thenReturn(3200);
		when(view.getSizeX()).thenReturn(104);
		when(view.getSizeY()).thenReturn(104);
		when(config.durationMillis()).thenReturn(1600);
		when(config.sharePings()).thenReturn(true);
		when(config.receivePings()).thenReturn(true);
		when(config.menuEntries()).thenReturn(true);
		when(party.isInParty()).thenReturn(true);
		PartyMember member = mock(PartyMember.class);
		when(member.getMemberId()).thenReturn(7L);
		when(party.getMemberById(7L)).thenReturn(member);
		doAnswer(call ->
		{
			call.getArgument(0, Runnable.class).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(Client.class).toInstance(client);
				bind(ClientThread.class).toInstance(clientThread);
				bind(PartyService.class).toInstance(party);
				bind(WSClient.class).toInstance(wsClient);
				bind(TacticalPingsConfig.class).toInstance(config);
				bind(PingAudio.class).toProvider(() -> audio);
				bind(PingTargets.class).toProvider(() -> targets);
				bind(PingInput.class).toProvider(() -> mock(PingInput.class));
				bind(PingTargetResolver.class).toProvider(() -> mock(PingTargetResolver.class));
				bind(OverlayManager.class).toProvider(() -> mock(OverlayManager.class));
				bind(ScenePingOverlay.class).toProvider(() -> mock(ScenePingOverlay.class));
				bind(MinimapPingOverlay.class).toProvider(() -> mock(MinimapPingOverlay.class));
				bind(PingWheelOverlay.class).toProvider(() -> mock(PingWheelOverlay.class));
			}
		}).injectMembers(plugin);
		plugin.startUp();
	}

	@After
	public void teardown()
	{
		plugin.shutDown();
	}

	@Test
	public void localFeedbackIsImmediateAndServerEchoIsDeduplicated()
	{
		plugin.sendPing(TILE, PingType.FLEE);
		assertEquals(1, plugin.getActive().size());
		verify(audio).play(PingType.FLEE);
		ArgumentCaptor<TacticalPingMessage> capture = ArgumentCaptor.forClass(TacticalPingMessage.class);
		verify(party).send(capture.capture());
		TacticalPingMessage message = capture.getValue();
		assertEquals(TILE, message.getPoint());
		assertEquals(TILE, message.getTemplate());
		message.setMemberId(7);
		plugin.onTacticalPingMessage(message);
		assertEquals(1, plugin.getActive().size());
		verify(audio, times(1)).play(PingType.FLEE);
	}

	@Test
	public void membershipWorldPlaneInstanceAndTemplateMustMatch()
	{
		TacticalPingMessage stranger = message(301, TILE, false, TILE);
		stranger.setMemberId(99);
		plugin.onTacticalPingMessage(stranger);
		plugin.onTacticalPingMessage(message(302, TILE, false, TILE));
		plugin.onTacticalPingMessage(message(301, TILE.dz(1), false, TILE.dz(1)));
		plugin.onTacticalPingMessage(message(301, TILE, true, TILE));
		plugin.onTacticalPingMessage(message(301, TILE, false, TILE.dx(8)));
		plugin.onTacticalPingMessage(message(301, new WorldPoint(6000, 6000, 0), false, TILE));
		assertTrue(plugin.getActive().isEmpty());
		verify(audio, never()).play(any());
		plugin.onTacticalPingMessage(message(301, TILE, false, TILE));
		assertEquals(1, plugin.getActive().size());
	}

	@Test
	public void localModeWorksWithoutPartyOrNetwork()
	{
		when(party.isInParty()).thenReturn(false);
		plugin.sendPing(TILE, PingType.GO);
		assertEquals(1, plugin.getActive().size());
		verify(party, never()).send(any());
		verify(party, never()).changeParty(any());
	}

	@Test
	public void sharingAndReceivingCanBeDisabledIndependently()
	{
		when(config.receivePings()).thenReturn(false);
		plugin.onTacticalPingMessage(message(301, TILE, false, TILE));
		assertTrue(plugin.getActive().isEmpty());
		when(config.sharePings()).thenReturn(false);
		plugin.sendPing(TILE, PingType.USE);
		assertEquals(1, plugin.getActive().size());
		verify(party, never()).send(any());
	}

	@Test
	public void sceneLoadingClearsMarkersAndShutdownUnregistersWireType()
	{
		plugin.sendPing(TILE, PingType.ATTACK);
		GameStateChanged changed = new GameStateChanged();
		changed.setGameState(GameState.LOADING);
		plugin.onGameStateChanged(changed);
		assertTrue(plugin.getActive().isEmpty());
		plugin.shutDown();
		clearInvocations(audio);
		plugin.onTacticalPingMessage(message(301, TILE, false, TILE));
		assertTrue(plugin.getActive().isEmpty());
		verify(audio, never()).play(any());
		verify(wsClient).unregisterMessage(TacticalPingMessage.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void menuFreezesSelectedTileAndKeepsOriginalLeftClick()
	{
		Tile tile = mock(Tile.class);
		when(tile.getWorldLocation()).thenReturn(TILE);
		when(view.getSelectedSceneTile()).thenReturn(tile);
		List<Consumer<MenuEntry>> callbacks = new ArrayList<>();
		when(client.createMenuEntry(anyInt())).thenAnswer(call ->
		{
			assertEquals(1, (int) call.getArgument(0));
			MenuEntry entry = mock(MenuEntry.class, org.mockito.Answers.RETURNS_SELF);
			doAnswer(onClick ->
			{
				callbacks.add(onClick.getArgument(0));
				return entry;
			}).when(entry).onClick(any());
			return entry;
		});
		MenuEntry walk = mock(MenuEntry.class);
		when(walk.getType()).thenReturn(MenuAction.WALK);
		when(walk.getWorldViewId()).thenReturn(WorldView.TOPLEVEL);
		plugin.onMenuEntryAdded(new MenuEntryAdded(walk));
		assertEquals(4, callbacks.size());
		when(tile.getWorldLocation()).thenReturn(TILE.dx(5));
		callbacks.get(0).accept(walk);
		assertEquals(TILE, plugin.getActive().get(0).getPoint());
	}

	private TacticalPingMessage message(int world, WorldPoint point, boolean instance, WorldPoint template)
	{
		TacticalPingMessage message = new TacticalPingMessage(PingType.GO, world, point, instance, template, System.currentTimeMillis());
		message.setMemberId(7);
		return message;
	}

	@Test
	public void sendsManualTargetIdentityWithLocalPing()
	{
		PingTarget target = new PingTarget(PingTarget.Kind.NPC, 100, 7, null);
		PingOutline outline = new PingOutline(target, view, null, null, null, null);
		when(targets.present(outline, TILE)).thenReturn(true);
		plugin.sendSelection(new PingSelection(TILE, outline), PingType.ATTACK);
		ArgumentCaptor<TacticalPingMessage> capture = ArgumentCaptor.forClass(TacticalPingMessage.class);
		verify(party).send(capture.capture());
		assertEquals(target, capture.getValue().getTarget());
		assertEquals(outline, plugin.getActive().get(0).getOutline());
	}

	@Test
	public void partyTargetIsResolvedOnceAndStoredWithReceivedPing()
	{
		TacticalPingMessage message = message(301, TILE, false, TILE);
		PingTarget target = new PingTarget(PingTarget.Kind.GAME_OBJECT, 1234, 0, null);
		message.setTarget(target);
		PingOutline outline = new PingOutline(target, view, null, null, null, null);
		when(targets.resolve(target, TILE)).thenReturn(outline);
		plugin.onTacticalPingMessage(message);
		assertEquals(outline, plugin.getActive().get(0).getOutline());
		verify(targets).resolve(target, TILE);
	}
}
