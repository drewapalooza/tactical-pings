/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Client;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PluginWiringTest
{
	@Test
	public void realOverlayInputAndAudioGraphStartsAndStops() throws Exception
	{
		ClientThread thread = mock(ClientThread.class);
		doAnswer(call ->
		{
			call.getArgument(0, Runnable.class).run();
			return null;
		}).when(thread).invoke(any(Runnable.class));
		KeyManager keys = mock(KeyManager.class);
		MouseManager mouse = mock(MouseManager.class);
		OverlayManager overlays = mock(OverlayManager.class);
		WSClient ws = mock(WSClient.class);
		net.runelite.client.ui.ClientToolbar toolbar = mock(net.runelite.client.ui.ClientToolbar.class);
		PingSettingsPanel settings = mock(PingSettingsPanel.class);
		TacticalPingsPlugin plugin = new TacticalPingsPlugin();
		Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(TacticalPingsPlugin.class).toInstance(plugin);
				bind(TacticalPingsConfig.class).toInstance(new TacticalPingsConfig() {});
				bind(Client.class).toInstance(mock(Client.class));
				bind(ClientThread.class).toProvider(() -> thread);
				bind(PartyService.class).toProvider(() -> mock(PartyService.class));
				bind(WSClient.class).toProvider(() -> ws);
				bind(KeyManager.class).toProvider(() -> keys);
				bind(MouseManager.class).toProvider(() -> mouse);
				bind(OverlayManager.class).toProvider(() -> overlays);
				bind(net.runelite.client.ui.ClientToolbar.class).toInstance(toolbar);
				bind(PingSettingsPanel.class).toProvider(() -> settings);
				bind(AudioPlayer.class).toProvider(() -> mock(AudioPlayer.class));
				bind(ScheduledExecutorService.class).toInstance(mock(ScheduledExecutorService.class));
			}
		});
		plugin.startUp();
		javax.swing.SwingUtilities.invokeAndWait(() -> {});
		org.mockito.ArgumentCaptor<net.runelite.client.ui.NavigationButton> navigation =
			org.mockito.ArgumentCaptor.forClass(net.runelite.client.ui.NavigationButton.class);
		verify(toolbar).addNavigation(navigation.capture());
		org.junit.Assert.assertSame(settings, navigation.getValue().getPanel());
		verify(overlays, times(3)).add(any(Overlay.class));
		verify(keys, times(2)).registerKeyListener(any());
		verify(mouse).registerMouseListener(anyInt(), any());
		verify(ws).registerMessage(TacticalPingMessage.class);
		plugin.shutDown();
		javax.swing.SwingUtilities.invokeAndWait(() -> {});
		verify(toolbar).removeNavigation(navigation.getValue());
		verify(settings).close();
		verify(overlays, times(3)).remove(any(Overlay.class));
		verify(keys, times(2)).unregisterKeyListener(any());
		verify(mouse).unregisterMouseListener(any());
		verify(ws).unregisterMessage(TacticalPingMessage.class);
	}
}
