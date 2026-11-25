/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/** Own-plugin settings only. All Swing state and updates are confined to the EDT. */
public class PingSettingsPanel extends PluginPanel
{
	private static final Color BACKGROUND = new Color(24, 28, 34);
	private static final Color CARD = new Color(34, 39, 47);
	private static final Color MUTED = new Color(163, 176, 191);
	private final TacticalPingsConfig config;
	private final ConfigManager manager;
	private final ColorPickerManager pickers;
	private final PingAudio audio;
	private final JPanel slots = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
	private final Map<PingSlot, SlotEditor> editors = new EnumMap<>(PingSlot.class);
	private final List<Runnable> refreshers = new ArrayList<>();
	private final JComboBox<PingSlot> quick = new JComboBox<>();
	private boolean updating;
	private boolean active = true;
	private int visibleCount;
	private RuneliteColorPicker picker;

	@Inject
	PingSettingsPanel(TacticalPingsConfig config, ConfigManager manager, ColorPickerManager pickers, PingAudio audio)
	{
		this.config = config;
		this.manager = manager;
		this.pickers = pickers;
		this.audio = audio;
		setBackground(BACKGROUND);
		setLayout(new DynamicGridLayout(0, 1, 0, 6));
		JLabel title = new JLabel("Ayra's Pings");
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
		title.setForeground(Color.WHITE);
		title.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
		add(title);

		Accordion wheel = new Accordion("Wheel & timing", true);
		add(wheel);
		row(wheel.body, "Ping count", integer("pingCount", config::pingCount, 1, 8, 1));
		JSpinner duration = new JSpinner(new SpinnerNumberModel(1.6, 0.8, 60.0, 0.1));
		duration.setName("durationMillis");
		duration.setEditor(new JSpinner.NumberEditor(duration, "0.0"));
		duration.addChangeListener(e -> save("durationMillis", (int) Math.round(((Number) duration.getValue()).doubleValue() * 1000)));
		refreshers.add(() -> duration.setValue(Math.max(800, Math.min(60000, config.durationMillis())) / 1000.0));
		row(wheel.body, "Duration (sec)", duration);
		quick.setName("quickPing");
		quick.addActionListener(e -> save("quickPing", quick.getSelectedItem()));
		row(wheel.body, "Quick click", quick);

		Accordion controls = new Accordion("Controls", false);
		add(controls);
		row(controls.body, "Ping input", choice("inputMethod", PingActivation.values(), config::inputMethod));
		JButton hotkey = new JButton();
		hotkey.setName("wheelKey");
		boolean[] listening = {false};
		hotkey.addActionListener(e ->
		{
			listening[0] = true;
			hotkey.setText("Press a key...");
			hotkey.requestFocusInWindow();
		});
		hotkey.setFocusTraversalKeysEnabled(false);
		hotkey.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (!listening[0])
				{
					return;
				}
				e.consume();
				listening[0] = false;
				if (e.getKeyCode() != KeyEvent.VK_ESCAPE)
				{
					save("wheelKey", e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						? Keybind.NOT_SET : new Keybind(e));
				}
				refresh();
			}
		});
		hotkey.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				listening[0] = false;
				refresh();
			}
		});
		refreshers.add(() ->
		{
			hotkey.setEnabled(config.inputMethod() == PingActivation.KEYBOARD);
			if (!listening[0])
			{
				hotkey.setText(config.wheelKey().toString());
			}
		});
		hotkey.setToolTipText("Click, then press a key. Escape cancels; Delete clears.");
		row(controls.body, "Wheel key", hotkey);
		check(controls.body, "Right-click pings", "menuEntries", config::menuEntries);

		Accordion party = new Accordion("Party & sound", false);
		add(party);
		check(party.body, "Share with party", "sharePings", config::sharePings);
		check(party.body, "Receive party pings", "receivePings", config::receivePings);
		check(party.body, "Use shared designs", "sharedDesigns", config::sharedDesigns);
		check(party.body, "Ping chimes", "sounds", config::sounds);
		row(party.body, "Volume (%)", integer("volume", config::volume, 0, 100, 5));
		JLabel disclosure = new JLabel("<html><body style='width:180px'>Shares your clicked tile/target, text and design with your RuneLite party.</body></html>");
		disclosure.setForeground(MUTED);
		party.body.add(disclosure);

		Accordion visuals = new Accordion("Visuals", false);
		add(visuals);
		check(visuals.body, "Minimap ripples", "minimap", config::minimap);
		check(visuals.body, "Scene labels", "labels", config::labels);
		check(visuals.body, "Sender names", "senderNames", config::senderNames);
		check(visuals.body, "Clicked-target outlines", "targetOutlines", config::targetOutlines);
		check(visuals.body, "Reduced motion", "reducedMotion", config::reducedMotion);
		JLabel heading = new JLabel("YOUR PINGS");
		heading.setBorder(BorderFactory.createEmptyBorder(12, 2, 3, 0));
		heading.setForeground(MUTED);
		add(heading);
		slots.setName("enabledPings");
		slots.setOpaque(false);
		add(slots);
		refresh();
	}

	@Override
	public void onActivate()
	{
		refresh();
	}

	public void refresh()
	{
		assert SwingUtilities.isEventDispatchThread();
		if (!active)
		{
			return;
		}
		updating = true;
		try
		{
			refreshers.forEach(Runnable::run);
			int count = PingWheelLayout.count(config.pingCount());
			if (count != visibleCount)
			{
				slots.removeAll();
				quick.removeAllItems();
				for (int i = 0; i < count; i++)
				{
					PingSlot slot = PingSlot.values()[i];
					SlotEditor editor = editors.computeIfAbsent(slot, SlotEditor::new);
					slots.add(editor.card);
					quick.addItem(slot);
				}
				visibleCount = count;
			}
			PingSlot selected = config.quickPing();
			quick.setSelectedItem(selected != null && selected.ordinal() < count ? selected : PingSlot.ONE);
			for (int i = 0; i < count; i++)
			{
				editors.get(PingSlot.values()[i]).refresh();
			}
		}
		finally
		{
			updating = false;
		}
		revalidate();
		repaint();
	}

	public void close()
	{
		active = false;
		if (picker != null)
		{
			picker.dispose();
			picker = null;
		}
	}

	private void save(String key, Object value)
	{
		if (!updating && active && value != null)
		{
			manager.setConfiguration(TacticalPingsConfig.GROUP, key, value);
			refresh();
		}
	}

	private JSpinner integer(String key, Supplier<Integer> getter, int min, int max, int step)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
		spinner.setName(key);
		spinner.addChangeListener(e -> save(key, spinner.getValue()));
		refreshers.add(() -> spinner.setValue(Math.max(min, Math.min(max, getter.get()))));
		return spinner;
	}

	private <T> JComboBox<T> choice(String key, T[] values, Supplier<T> getter)
	{
		JComboBox<T> combo = new JComboBox<>(values);
		combo.setName(key);
		combo.addActionListener(e -> save(key, combo.getSelectedItem()));
		refreshers.add(() -> combo.setSelectedItem(getter.get()));
		return combo;
	}

	private void check(JPanel parent, String text, String key, BooleanSupplier getter)
	{
		JCheckBox box = new JCheckBox(text);
		box.setName(key);
		box.setOpaque(false);
		box.setForeground(Color.WHITE);
		if ("sharePings".equals(key))
		{
			box.setToolTipText("Shares clicked tile/target (including a selected player's name/index), world/instance coordinates, slot/design/text, timestamp and random ping ID through RuneLite's party relay.");
		}
		box.addActionListener(e -> save(key, box.isSelected()));
		refreshers.add(() -> box.setSelected(getter.getAsBoolean()));
		parent.add(box);
	}

	private static void row(JPanel parent, String text, JComponent control)
	{
		row(parent, text, control, 88);
	}

	private static void row(JPanel parent, String text, JComponent control, int labelWidth)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel label = new JLabel(text);
		label.setForeground(new Color(218, 225, 235));
		Font fieldFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		label.setFont(fieldFont);
		label.setPreferredSize(new Dimension(labelWidth, 28));
		label.setLabelFor(control);
		control.getAccessibleContext().setAccessibleName(text);
		control.setFont(fieldFont);
		control.setPreferredSize(new Dimension(115, 28));
		row.setPreferredSize(new Dimension(labelWidth + 121, 28));
		row.add(label, BorderLayout.WEST);
		row.add(control, BorderLayout.CENTER);
		parent.add(row);
	}

	private final class SlotEditor
	{
		private final PingSlot slot;
		private final Accordion card;
		private final ColorJButton color;
		private final JComboBox<PingSymbol> symbol = new JComboBox<>(PingSymbol.values());
		private final JComboBox<PingSound> sound = new JComboBox<>(PingSound.values());
		private final JTextField text = new JTextField();

		private SlotEditor(PingSlot slot)
		{
			this.slot = slot;
			card = new Accordion(slot.toString(), false);
			card.setName("slot" + (slot.ordinal() + 1));
			color = new ColorJButton("", slot.getDefaultStyle().getColor());
			color.addActionListener(e ->
			{
				picker = pickers.create(PingSettingsPanel.this, color.getColor(), slot + " color", true);
				picker.setOnClose(selected -> save(slot.getConfigPrefix() + "Color", selected));
				picker.setVisible(true);
			});
			row(card.body, "Color", color, 48);
			symbol.setName(slot.getConfigPrefix() + "Symbol");
			symbol.addActionListener(e -> save(slot.getConfigPrefix() + "Symbol", symbol.getSelectedItem()));
			row(card.body, "Symbol", symbol, 48);
			text.setName(slot.getConfigPrefix() + "Text");
			text.setToolTipText("Your text, up to 64 characters. Enter or leave the field to save.");
			((AbstractDocument) text.getDocument()).setDocumentFilter(new DocumentFilter()
			{
				@Override
				public void insertString(FilterBypass fb, int offset, String value, AttributeSet attrs) throws BadLocationException
				{
					replace(fb, offset, 0, value, attrs);
				}

				@Override
				public void replace(FilterBypass fb, int offset, int length, String value, AttributeSet attrs) throws BadLocationException
				{
					String previous = fb.getDocument().getText(0, fb.getDocument().getLength());
					String before = previous.substring(0, offset);
					String after = previous.substring(offset + length);
					String retained = before + after;
					int available = Math.max(0, PingStyle.MAX_TEXT_LENGTH - retained.codePointCount(0, retained.length()));
					String inserted = PingStyle.cleanText(value == null ? "" : value);
					int end = inserted.offsetByCodePoints(0, Math.min(available, inserted.codePointCount(0, inserted.length())));
					inserted = inserted.substring(0, end);
					String proposed = before + inserted + after;
					if (PingStyle.validText(proposed))
					{
						fb.replace(offset, length, inserted, attrs);
					}
					else
					{
						// A selection through half of a surrogate pair must not leave invalid text.
						fb.replace(0, previous.length(), PingStyle.cleanText(proposed), attrs);
					}
				}
			});
			text.addActionListener(e -> save(slot.getConfigPrefix() + "Text", text.getText()));
			text.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					save(slot.getConfigPrefix() + "Text", text.getText());
				}
			});
			row(card.body, "Text", text, 48);
			sound.setName(slot.getConfigPrefix() + "Sound");
			sound.addActionListener(e ->
			{
				if (!updating && active)
				{
					save(slot.getConfigPrefix() + "Sound", sound.getSelectedItem());
					audio.play((PingSound) sound.getSelectedItem());
				}
			});
			row(card.body, "Sound", sound, 48);
		}

		private void refresh()
		{
			PingStyle style = PingStyle.fromConfig(config, slot);
			card.header.setText((card.body.isVisible() ? "− " : "+ ") + slot + " · " + style.getText());
			card.header.setForeground(style.getColor());
			card.header.setIcon(new Icon()
			{
				@Override
				public int getIconWidth()
				{
					return 24;
				}
				@Override
				public int getIconHeight()
				{
					return 24;
				}
				@Override
				public void paintIcon(Component c, Graphics g, int x, int y)
				{
					PingPainter.glyph((Graphics2D) g, style, x + 12, y + 12, 8, 1);
				}
			});
			color.setColor(style.getColor());
			color.setText(String.format("#%06X", style.getColor().getRGB() & 0xffffff));
			symbol.setSelectedItem(style.getSymbol());
			sound.setSelectedItem(style.getSound());
			if (!text.hasFocus())
			{
				text.setText(style.getText());
			}
		}
	}

	private static final class Accordion extends JPanel
	{
		private final JButton header;
		private final JPanel body = new JPanel(new DynamicGridLayout(0, 1, 0, 7));

		private Accordion(String title, boolean expanded)
		{
			super(new BorderLayout());
			setBackground(CARD);
			setBorder(BorderFactory.createLineBorder(new Color(49, 57, 68)));
			header = new JButton((expanded ? "− " : "+ ") + title);
			header.setName("accordion:" + title);
			header.setHorizontalAlignment(SwingConstants.LEFT);
			header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			header.setForeground(Color.WHITE);
			header.setBackground(CARD);
			header.setContentAreaFilled(false);
			header.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 6));
			header.setFocusPainted(true);
			body.setBackground(CARD);
			body.setBorder(BorderFactory.createEmptyBorder(1, 8, 10, 8));
			body.setVisible(expanded);
			header.addActionListener(e ->
			{
				body.setVisible(!body.isVisible());
				header.setText((body.isVisible() ? "− " : "+ ") + header.getText().substring(2));
				revalidate();
			});
			add(header, BorderLayout.NORTH);
			add(body, BorderLayout.CENTER);
		}
	}
}
