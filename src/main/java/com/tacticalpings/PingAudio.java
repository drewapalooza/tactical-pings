/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PingAudio
{
	private static final Logger log = LoggerFactory.getLogger(PingAudio.class);
	@Inject
	private AudioPlayer audioPlayer;
	@Inject
	private ScheduledExecutorService executor;
	@Inject
	private TacticalPingsConfig config;
	private final AtomicLong generation = new AtomicLong();
	private volatile boolean running;

	public void start()
	{
		generation.incrementAndGet();
		running = true;
	}

	public void stop()
	{
		running = false;
		generation.incrementAndGet();
	}

	public void invalidate()
	{
		generation.incrementAndGet();
	}

	public void play(PingType type)
	{
		int volume = Math.max(0, Math.min(100, config.volume()));
		if (!running || !config.sounds() || volume == 0)
		{
			return;
		}
		long token = generation.get();
		float gain = (float) (20 * Math.log10(volume / 100.0));
		executor.execute(() ->
		{
			if (!running || generation.get() != token || !config.sounds() || config.volume() == 0)
			{
				return;
			}
			try
			{
				audioPlayer.play(PingAudio.class, type.name().toLowerCase(Locale.ROOT) + ".wav", gain);
			}
			catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException ex)
			{
				log.debug("Unable to play Ayra's Pings chime", ex);
			}
		});
	}
}
