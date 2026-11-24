/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Offline asset authoring tool; never part of the plugin's source set or JAR. */
public final class GenerateChimes
{
	public static void main(String[] args) throws Exception
	{
		File directory = new File("src/main/resources/com/tacticalpings");
		if (!directory.isDirectory() && !directory.mkdirs())
		{
			throw new IllegalStateException("Cannot create chime directory");
		}
		write(directory, "flee", new double[] {880, 659.255}, new double[] {0, 0.14}, 0.16);
		write(directory, "go", new double[] {523.251, 783.991}, new double[] {0, 0.12}, 0.22);
		write(directory, "attack", new double[] {659.255, 659.255, 1046.502}, new double[] {0, 0.09, 0.18}, 0.11);
		write(directory, "use", new double[] {987.767, 1318.510}, new double[] {0, 0.08}, 0.30);
		write(directory, "bell", new double[] {783.991, 1567.982}, new double[] {0, 0}, 0.40);
		write(directory, "sonar", new double[] {391.995, 391.995}, new double[] {0, 0.20}, 0.30);
		write(directory, "chirp", new double[] {783.991, 1046.502, 1567.982}, new double[] {0, 0.06, 0.12}, 0.14);
		write(directory, "chord", new double[] {523.251, 659.255, 783.991}, new double[] {0, 0, 0}, 0.42);
	}

	private static void write(File directory, String name, double[] frequencies, double[] starts, double decay) throws Exception
	{
		int rate = 44100;
		int samples = (int) (rate * (starts[starts.length - 1] + 0.48));
		double[] signal = new double[samples];
		double peak = 0;
		for (int i = 0; i < samples; i++)
		{
			double time = i / (double) rate;
			for (int note = 0; note < frequencies.length; note++)
			{
				double t = time - starts[note];
				if (t >= 0)
				{
					double envelope = Math.min(1, t / 0.006) * Math.exp(-t * 6 / decay);
					double phase = Math.PI * 2 * frequencies[note] * t;
					signal[i] += envelope * (Math.sin(phase) + 0.22 * Math.sin(phase * 2.003) + 0.08 * Math.sin(phase * 3.997));
				}
			}
			peak = Math.max(peak, Math.abs(signal[i]));
		}
		byte[] bytes = new byte[samples * 2];
		for (int i = 0; i < samples; i++)
		{
			short sample = (short) (signal[i] / peak * 0.65 * Short.MAX_VALUE);
			bytes[2 * i] = (byte) sample;
			bytes[2 * i + 1] = (byte) (sample >> 8);
		}
		AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
		try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes), format, samples))
		{
			AudioSystem.write(stream, AudioFileFormat.Type.WAVE, new File(directory, name + ".wav"));
		}
	}
}
