/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.client.audio.AudioPlayer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PingAudioTest
{
	private final TacticalPingsConfig config = mock(TacticalPingsConfig.class);
	private final AudioPlayer player = mock(AudioPlayer.class);
	private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
	private final List<Runnable> queued = new ArrayList<>();
	private PingAudio audio;

	@Before
	public void setup()
	{
		when(config.sounds()).thenReturn(true);
		when(config.volume()).thenReturn(50);
		doAnswer(call ->
		{
			queued.add(call.getArgument(0));
			return null;
		}).when(executor).execute(any(Runnable.class));
		audio = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(TacticalPingsConfig.class).toInstance(config);
				bind(AudioPlayer.class).toInstance(player);
				bind(ScheduledExecutorService.class).toInstance(executor);
			}
		}).getInstance(PingAudio.class);
		audio.start();
	}

	@Test
	public void selectedLibrarySoundUsesTheListenersVolume() throws Exception
	{
		audio.play(PingSound.BELL);
		assertEquals(1, queued.size());
		queued.get(0).run();
		verify(player).play(PingAudio.class, "bell.wav", (float) (20 * Math.log10(0.5)));
	}

	@Test
	public void silentMutedAndZeroVolumeNeverQueuePlayback()
	{
		audio.play(PingSound.SILENT);
		when(config.volume()).thenReturn(0);
		audio.play(PingSound.BELL);
		when(config.volume()).thenReturn(50);
		when(config.sounds()).thenReturn(false);
		audio.play(PingSound.CHORD);
		assertEquals(0, queued.size());
	}

	@Test
	public void volumeChangesBeforePlaybackUseTheLatestPreference() throws Exception
	{
		audio.play(PingSound.BELL);
		when(config.volume()).thenReturn(20);
		queued.get(0).run();
		verify(player).play(PingAudio.class, "bell.wav", (float) (20 * Math.log10(0.2)));
	}

	@Test
	public void mutingOrStoppingBeforeQueuedPlaybackCancelsIt() throws Exception
	{
		audio.play(PingSound.SONAR);
		when(config.sounds()).thenReturn(false);
		queued.get(0).run();
		when(config.sounds()).thenReturn(true);
		audio.play(PingSound.CHIRP);
		audio.stop();
		queued.get(1).run();
		verify(player, never()).play(any(Class.class), anyString(), anyFloat());
	}
}
