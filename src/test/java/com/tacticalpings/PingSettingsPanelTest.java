/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PingSettingsPanelTest
{
	@Test
	public void countChangesImmediatelyShowOnlyTheEnabledEditorsAndQuickChoices() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			f.count.set(5);
			PingSettingsPanel panel = f.create();
			assertEquals(5, find(panel, "enabledPings", JPanel.class).getComponentCount());
			assertEquals(5, find(panel, "quickPing", JComboBox.class).getItemCount());
			find(panel, "pingCount", JSpinner.class).setValue(2);
			assertEquals(2, find(panel, "enabledPings", JPanel.class).getComponentCount());
			assertEquals(2, find(panel, "quickPing", JComboBox.class).getItemCount());
			find(panel, "pingCount", JSpinner.class).setValue(8);
			assertEquals(8, find(panel, "enabledPings", JPanel.class).getComponentCount());
			panel.close();
		});
	}

	@Test
	public void disablingSlotsPreservesTheirSavedTextAndRestoresItWhenReenabled() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			f.count.set(8);
			PingSettingsPanel panel = f.create();
			JTextField text = find(panel, "ping8Text", JTextField.class);
			text.setText("Keep this design!");
			text.postActionEvent();
			find(panel, "pingCount", JSpinner.class).setValue(3);
			assertEquals(3, find(panel, "enabledPings", JPanel.class).getComponentCount());
			find(panel, "pingCount", JSpinner.class).setValue(8);
			assertEquals("Keep this design!", find(panel, "ping8Text", JTextField.class).getText());
			panel.close();
		});
	}

	@Test
	public void sixtySecondsIsSavedAsSixtyThousandMilliseconds() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			PingSettingsPanel panel = f.create();
			find(panel, "durationMillis", JSpinner.class).setValue(60.0);
			assertEquals(60000, f.values.get("durationMillis"));
			assertEquals(60.0, find(panel, "durationMillis", JSpinner.class).getValue());
			panel.close();
		});
	}

	@Test
	public void soundChangesPreviewOnceAndRefreshingDoesNotPlayOrWriteSettings() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			PingSettingsPanel panel = f.create();
			panel.refresh();
			verify(f.manager, never()).setConfiguration(anyString(), anyString(), any(Object.class));
			verify(f.audio, never()).play(any());
			find(panel, "fleeSound", JComboBox.class).setSelectedItem(PingSound.BELL);
			assertEquals(PingSound.BELL, f.values.get("fleeSound"));
			verify(f.audio).play(PingSound.BELL);
			panel.close();
		});
	}

	@Test
	public void textEditorKeepsUnicodeWithinTheSharedLimitAndClosedPanelCannotSave() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			f.count.set(8);
			PingSettingsPanel panel = f.create();
			JTextField text = find(panel, "ping8Text", JTextField.class);
			text.setText("🚩".repeat(65));
			assertEquals("🚩".repeat(64), text.getText());
			panel.close();
			text.postActionEvent();
			assertTrue(f.values.isEmpty());
		});
	}

	@Test
	public void nativeSettingsUseFourAccordionsAndNoEmptyPingSections()
	{
		ConfigManager manager = mock(ConfigManager.class);
		TacticalPingsConfig config = new TacticalPingsConfig() {};
		when(manager.getConfigDescriptor(config)).thenCallRealMethod();
		ConfigDescriptor descriptor = manager.getConfigDescriptor(config);
		assertEquals(4, descriptor.getSections().size());
		assertEquals(32, descriptor.getItems().stream().filter(item -> item.getItem().hidden()).count());
		assertEquals(60000, descriptor.getItems().stream().filter(item -> item.getItem().keyName().equals("durationMillis"))
			.findFirst().get().getRange().max());
	}

	@Test
	public void accordionHeaderTracksItsExpandedState() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			Fixture f = new Fixture();
			PingSettingsPanel panel = f.create();
			JButton header = find(panel, "accordion:Ping 1", JButton.class);
			assertTrue(header.getText().startsWith("+ "));
			header.doClick();
			assertTrue(header.getText(), header.getText().startsWith("− "));
			panel.refresh();
			assertTrue(header.getText(), header.getText().startsWith("− "));
			header.doClick();
			assertTrue(header.getText().startsWith("+ "));
			panel.close();
		});
	}

	static <T extends Component> T find(Container parent, String name, Class<T> type)
	{
		T result = search(parent, name, type);
		assertNotNull(name, result);
		return result;
	}

	private static <T extends Component> T search(Container parent, String name, Class<T> type)
	{
		for (Component child : parent.getComponents())
		{
			if (name.equals(child.getName()) && type.isInstance(child))
			{
				return type.cast(child);
			}
			if (child instanceof Container)
			{
				T nested = search((Container) child, name, type);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static final class Fixture
	{
		private final AtomicInteger count = new AtomicInteger(4);
		private final Map<String, Object> values = new HashMap<>();
		private final TacticalPingsConfig config = mock(TacticalPingsConfig.class, CALLS_REAL_METHODS);
		private final ConfigManager manager = mock(ConfigManager.class);
		private final PingAudio audio = mock(PingAudio.class);

		private Fixture()
		{
			when(config.pingCount()).thenAnswer(call -> count.get());
			when(config.durationMillis()).thenAnswer(call -> values.getOrDefault("durationMillis", 1600));
			when(config.ping8Text()).thenAnswer(call -> values.getOrDefault("ping8Text", "Defend"));
			when(config.fleeSound()).thenAnswer(call -> values.getOrDefault("fleeSound", PingSound.FALLING));
			doAnswer(call ->
			{
				String key = call.getArgument(1);
				Object value = call.getArgument(2);
				values.put(key, value);
				if ("pingCount".equals(key))
				{
					count.set((Integer) value);
				}
				return null;
			}).when(manager).setConfiguration(eq(TacticalPingsConfig.GROUP), anyString(), any(Object.class));
		}

		private PingSettingsPanel create()
		{
			return new PingSettingsPanel(config, manager, mock(ColorPickerManager.class), audio);
		}
	}
}
