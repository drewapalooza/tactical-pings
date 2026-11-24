/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import static org.mockito.Mockito.mock;

/** Offline capture of the actual Swing settings panel, with no client login. */
public final class PingSettingsPreview
{
	public static void main(String[] args) throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			for (String type : new String[] {"ComboBox", "TextField", "FormattedTextField", "Spinner", "Button"})
			{
				UIManager.put(type + ".background", new Color(44, 50, 60));
				UIManager.put(type + ".foreground", new Color(235, 240, 247));
			}
			BufferedImage image = new BufferedImage(830, 1010, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(17, 21, 26));
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.setColor(new Color(235, 240, 247));
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
			g.drawString("A CLEANER SETTINGS PAGE", 28, 42);
			g.setColor(new Color(163, 176, 191));
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
			g.drawString("Grouped controls · only active pings · up to 60 seconds", 28, 69);
			int[] counts = {5, 2, 8};
			for (int i = 0; i < counts.length; i++)
			{
				final int count = counts[i];
				TacticalPingsConfig config = new TacticalPingsConfig()
				{
					@Override public int pingCount() { return count; }
					@Override public int durationMillis() { return 60000; }
				};
				PingSettingsPanel panel = new PingSettingsPanel(config, mock(ConfigManager.class),
					mock(ColorPickerManager.class), mock(PingAudio.class));
				if (i == 1)
				{
					expand(panel, "+ Ping 1");
				}
				panel.setSize(245, panel.getPreferredSize().height);
				layout(panel);
				Graphics2D capture = (Graphics2D) g.create(28 + 270 * i, 100, 245, 880);
				panel.printAll(capture);
				capture.dispose();
				panel.close();
			}
			g.dispose();
			try
			{
				ImageIO.write(image, "png", new File("build/previews/settings.png"));
			}
			catch (java.io.IOException ex)
			{
				throw new IllegalStateException(ex);
			}
		});
	}

	private static void expand(Container parent, String prefix)
	{
		for (Component child : parent.getComponents())
		{
			if (child instanceof JButton && ((JButton) child).getText().startsWith(prefix))
			{
				((JButton) child).doClick();
				return;
			}
			if (child instanceof Container)
			{
				expand((Container) child, prefix);
			}
		}
	}

	private static void layout(Container parent)
	{
		parent.doLayout();
		for (Component child : parent.getComponents())
		{
			if (child instanceof Container)
			{
				layout((Container) child);
			}
		}
	}
}
