/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.Provides;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Ayra's Pings",
	description = "Share manual flee, go, attack and use tile pings with your RuneLite party",
	tags = {"party", "ping", "tile", "team", "communication", "ripple"}
)
public class TacticalPingsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(TacticalPingsPlugin.class);
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private PartyService party;
	@Inject
	private WSClient wsClient;
	@Inject
	private TacticalPingsConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ScenePingOverlay sceneOverlay;
	@Inject
	private MinimapPingOverlay minimapOverlay;
	@Inject
	private PingWheelOverlay wheelOverlay;
	@Inject
	private PingInput input;
	@Inject
	private PingAudio audio;
	@Inject
	private PingTargetResolver resolver;
	@Inject
	private PingTargets targets;
	private final PingStore store = new PingStore();
	private final Map<MenuEntry, PingType> pingMenus = new IdentityHashMap<>();
	private volatile boolean running;
	private final AtomicLong lifecycle = new AtomicLong();

	@Provides
	TacticalPingsConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(TacticalPingsConfig.class);
	}

	@Override
	protected void startUp()
	{
		running = true;
		lifecycle.incrementAndGet();
		store.clear();
		pingMenus.clear();
		wsClient.registerMessage(TacticalPingMessage.class);
		overlayManager.add(sceneOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(wheelOverlay);
		audio.start();
		input.start();
	}

	@Override
	protected void shutDown()
	{
		running = false;
		pingMenus.clear();
		long token = lifecycle.incrementAndGet();
		input.stop();
		audio.stop();
		wsClient.unregisterMessage(TacticalPingMessage.class);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(wheelOverlay);
		clientThread.invoke(() ->
		{
			if (!running && lifecycle.get() == token)
			{
				store.clear();
			}
		});
	}

	List<ActivePing> getActive()
	{
		return store.getActive();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		store.prune(System.nanoTime());
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		input.updateHoverSelection(resolver.resolveSelection(mouse), mouse);
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event)
	{
		if (!event.isFocused())
		{
			input.focusLost();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			lifecycle.incrementAndGet();
			audio.invalidate();
			store.clear();
			input.cancel();
		}
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		lifecycle.incrementAndGet();
		audio.invalidate();
		clientThread.invoke(() ->
		{
			store.clear();
			input.cancel();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TacticalPingsConfig.GROUP.equals(event.getGroup()))
		{
			input.focusLost();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!running || !config.menuEntries() || event.getType() != MenuAction.WALK.getId()
			|| event.getMenuEntry().getWorldViewId() != WorldView.TOPLEVEL)
		{
			return;
		}
		WorldView view = client.getTopLevelWorldView();
		Tile tile = view == null ? null : view.getSelectedSceneTile();
		if (tile == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		// Freeze the menu's tile; moving the mouse over a menu must not move the ping.
		WorldPoint point = tile.getWorldLocation();
		pingMenus.clear();
		for (PingType type : PingType.values())
		{
			// Below Walk here: ordinary left-click must retain its original action.
			MenuEntry pingEntry = client.createMenuEntry(1)
				.setOption("Ping")
				.setTarget(ColorUtil.wrapWithColorTag(type.getLabel(), type.getColor()))
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> sendPing(point, type));
			pingMenus.put(pingEntry, type);
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!running || pingMenus.isEmpty())
		{
			return;
		}
		MenuEntry[] entries = event.getMenuEntries();
		PingSelection selected = null;
		for (int i = entries.length - 1; i >= 0 && selected == null; i--)
		{
			selected = targets.fromMenu(entries[i]);
		}
		if (selected == null)
		{
			return;
		}
		PingSelection frozen = selected;
		for (MenuEntry entry : entries)
		{
			PingType type = pingMenus.get(entry);
			if (type != null)
			{
				entry.onClick(clicked -> sendSelection(frozen, type));
			}
		}
	}

	void sendPing(WorldPoint point, PingType type)
	{
		sendSelection(new PingSelection(point, null), type);
	}

	void sendSelection(PingSelection selection, PingType type)
	{
		WorldPoint point = selection.getPoint();
		if (!running || !canShow(point))
		{
			return;
		}
		WorldView view = client.getTopLevelWorldView();
		LocalPoint local = LocalPoint.fromWorld(view, point);
		WorldPoint template = WorldPoint.fromLocalInstance(client, local, point.getPlane());
		TacticalPingMessage message = new TacticalPingMessage(type, client.getWorld(), point,
			view.isInstance(), template, System.currentTimeMillis());
		PingOutline outline = targets.present(selection.getOutline(), point) ? selection.getOutline() : null;
		message.setTarget(outline == null ? null : outline.getTarget());
		PartyMember self = party.getLocalMember();
		long memberId = self == null ? 0 : self.getMemberId();
		if (!accept(message, memberId, outline))
		{
			return;
		}
		if (config.sharePings() && party.isInParty())
		{
			try
			{
				party.send(message);
			}
			catch (IllegalStateException ex)
			{
				// A disconnect cannot prevent the immediate local marker and chime.
				log.debug("Party connection unavailable for Ayra's Pings", ex);
			}
		}
	}

	@Subscribe
	public void onTacticalPingMessage(TacticalPingMessage message)
	{
		long token = lifecycle.get();
		long partyId = party.getPartyId();
		clientThread.invoke(() ->
		{
			if (!running || token != lifecycle.get() || partyId != party.getPartyId()
				|| !config.receivePings() || !party.isInParty()
				|| message == null || !message.isValid(System.currentTimeMillis())
				|| party.getMemberById(message.getMemberId()) == null
				|| message.getWorld() != client.getWorld() || !canShow(message.getPoint()))
			{
				return;
			}
			WorldView view = client.getTopLevelWorldView();
			if (view.isInstance() != message.isInstanced())
			{
				return;
			}
			LocalPoint local = LocalPoint.fromWorld(view, message.getPoint());
			WorldPoint template = WorldPoint.fromLocalInstance(client, local, message.getPoint().getPlane());
			if (!template.equals(message.getTemplate()))
			{
				return;
			}
			accept(message, message.getMemberId(), targets.resolve(message.getTarget(), message.getPoint()));
		});
	}

	boolean canShow(WorldPoint point)
	{
		WorldView view = client.getTopLevelWorldView();
		return point != null && client.getGameState() == GameState.LOGGED_IN && view != null
			&& point.getPlane() == view.getPlane() && LocalPoint.fromWorld(view, point) != null;
	}

	private boolean accept(TacticalPingMessage message, long memberId, PingOutline outline)
	{
		if (!store.add(message.getId(), memberId, message.getType(), message.getPoint(),
			System.nanoTime(), Math.max(800, Math.min(3000, config.durationMillis())), outline))
		{
			return false;
		}
		audio.play(message.getType());
		return true;
	}
}
