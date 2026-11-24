/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChimeResourceTest
{
	@Test
	public void allLibraryChimesAreBundledReadableAndNotClipped() throws Exception
	{
		Set<Integer> hashes = new HashSet<>();
		for (PingSound sound : PingSound.values())
		{
			if (sound == PingSound.SILENT)
			{
				continue;
			}
			try (InputStream resource = PingAudio.class.getResourceAsStream(sound.getResource()))
			{
				assertNotNull(sound.name(), resource);
				try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(resource)))
				{
					assertEquals(44100, audio.getFormat().getSampleRate(), 0);
					assertEquals(1, audio.getFormat().getChannels());
					assertEquals(16, audio.getFormat().getSampleSizeInBits());
					byte[] samples = audio.readAllBytes();
					assertTrue(samples.length > 10000 && samples.length < 88200);
					int peak = 0;
					for (int i = 0; i < samples.length; i += 2)
					{
						short sample = (short) ((samples[i] & 255) | (samples[i + 1] << 8));
						peak = Math.max(peak, Math.abs((int) sample));
					}
					assertTrue(peak > 5000 && peak < 30000);
					assertTrue(hashes.add(java.util.Arrays.hashCode(samples)));
				}
			}
		}
		assertEquals(8, hashes.size());
	}
}
