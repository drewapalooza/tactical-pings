/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

/** Only these original bundled assets can be requested by a received ping. */
public enum PingSound
{
	FALLING("Falling warning", "flee.wav"),
	RISING("Rising chime", "go.wav"),
	STRIKES("Triple strike", "attack.wav"),
	SPARKLE("Bright double chime", "use.wav"),
	BELL("Bell", "bell.wav"),
	SONAR("Sonar", "sonar.wav"),
	CHIRP("Chirp", "chirp.wav"),
	CHORD("Soft chord", "chord.wav"),
	SILENT("Silent", null);

	private final String label;
	private final String resource;

	PingSound(String label, String resource)
	{
		this.label = label;
		this.resource = resource;
	}

	public String getResource()
	{
		return resource;
	}

	static PingSound fromWire(String name)
	{
		for (PingSound sound : values())
		{
			if (sound.name().equals(name))
			{
				return sound;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
