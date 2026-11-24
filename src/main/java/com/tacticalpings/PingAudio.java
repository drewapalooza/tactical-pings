/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.io.IOException;
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

	public void play(PingSound sound)
	{
		int volume = Math.max(0, Math.min(100, config.volume()));
		if (!running || !config.sounds() || volume == 0 || sound == null || sound == PingSound.SILENT)
		{
			return;
		}
		long token = generation.get();
		executor.execute(() ->
		{
			int playbackVolume = Math.max(0, Math.min(100, config.volume()));
			if (!running || generation.get() != token || !config.sounds() || playbackVolume == 0)
			{
				return;
			}
			try
			{
				float gain = (float) (20 * Math.log10(playbackVolume / 100.0));
				audioPlayer.play(PingAudio.class, sound.getResource(), gain);
			}
			catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException ex)
			{
				log.debug("Unable to play Ayra's Pings chime", ex);
			}
		});
	}
}
