/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.util.HotkeyListener;

@Singleton
public class PingInput extends MouseAdapter implements KeyListener
{
	@Inject
	private TacticalPingsConfig config;
	@Inject
	private TacticalPingsPlugin plugin;
	@Inject
	private ClientThread clientThread;
	@Inject
	private KeyManager keyManager;
	@Inject
	private MouseManager mouseManager;
	@Inject
	private PingTargetResolver resolver;

	private volatile boolean held;
	private volatile boolean armed;
	private volatile boolean captured;
	private int suppressedReleases;
	private int suppressedClicks;
	private volatile Point cursor = new Point();
	private volatile Hover hover;
	private volatile Wheel wheel;
	private final AtomicLong generation = new AtomicLong();
	private final HotkeyListener hotkey = new HotkeyListener(() ->
		config.inputMethod() == PingActivation.KEYBOARD ? config.wheelKey() : Keybind.NOT_SET)
	{
		@Override
		public void hotkeyPressed()
		{
			held = true;
		}

		@Override
		public void hotkeyReleased()
		{
			held = false;
		}
	};

	public void start()
	{
		keyManager.registerKeyListener(hotkey);
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(0, this);
	}

	public void stop()
	{
		keyManager.unregisterKeyListener(hotkey);
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(this);
		cancel();
		held = false;
		captured = false;
		suppressedReleases = 0;
		suppressedClicks = 0;
	}

	public void updateHover(WorldPoint target, net.runelite.api.Point mouse)
	{
		updateHoverSelection(target == null ? null : new PingSelection(target, null), mouse);
	}

	public void updateHoverSelection(PingSelection target, net.runelite.api.Point mouse)
	{
		hover = target == null ? null : new Hover(target);
	}

	public Wheel getWheel()
	{
		return wheel;
	}

	public boolean isArmed()
	{
		return armed;
	}

	public Point getCursor()
	{
		return cursor;
	}

	public void cancel()
	{
		generation.incrementAndGet();
		armed = false;
		wheel = null;
		hover = null;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		int button = event.getButton();
		suppressedClicks &= ~(1 << button);
		if (captured)
		{
			cancel();
			consumePress(event);
			return event;
		}
		PingActivation activation = config.inputMethod();
		if (activation != PingActivation.KEYBOARD && button == activation.getMouseButton())
		{
			cursor = event.getPoint();
			if (armed)
			{
				cancel();
			}
			else
			{
				armed = true;
			}
			consumePress(event);
			return event;
		}
		if (armed && button == MouseEvent.BUTTON3)
		{
			cancel();
			consumePress(event);
			return event;
		}
		if (button == MouseEvent.BUTTON1 && (armed || (activation == PingActivation.KEYBOARD && held)))
		{
			// The side button only primes. This click selects the actual tile, once.
			armed = false;
			Hover target = hover;
			if (target == null)
			{
				return event;
			}
			captured = true;
			cursor = event.getPoint();
			long token = generation.incrementAndGet();
			Wheel started = new Wheel(event.getPoint(), token);
			wheel = started;
			// Queue capture before release. The wheel responds immediately, even when
			// press, drag and release all arrive before the next client tick.
			clientThread.invoke(() ->
			{
				if (generation.get() == token)
				{
					started.selection = resolver.captureAtPress(started.origin, target.target);
				}
			});
			consumePress(event);
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		int buttonMask = 1 << event.getButton();
		if ((suppressedReleases & buttonMask) != 0)
		{
			suppressedReleases &= ~buttonMask;
			event.consume();
		}
		if (captured)
		{
			event.consume();
			if (event.getButton() == MouseEvent.BUTTON1)
			{
				captured = false;
				Wheel completed = wheel;
				wheel = null;
				Point release = event.getPoint();
				if (completed != null)
				{
					clientThread.invoke(() ->
					{
						if (generation.get() == completed.generation)
						{
							PingSelection selection = completed.selection;
							PingType type = choose(release.x - completed.origin.x, release.y - completed.origin.y);
							if (selection != null)
							{
								if (selection.getOutline() == null)
								{
									plugin.sendPing(selection.getPoint(), type);
								}
								else
								{
									plugin.sendSelection(selection, type);
								}
							}
						}
					});
				}
			}
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		int buttonMask = 1 << event.getButton();
		if ((suppressedClicks & buttonMask) != 0 || captured)
		{
			event.consume();
			suppressedClicks &= ~buttonMask;
		}
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		cursor = event.getPoint();
		if (captured || suppressedReleases != 0)
		{
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		cursor = event.getPoint();
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		if (captured || armed)
		{
			cancel();
		}
		return event;
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.VK_ESCAPE && (captured || armed))
		{
			cancel();
			event.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
	}

	@Override
	public void focusLost()
	{
		hotkey.focusLost();
		held = false;
		cancel();
		captured = false;
		suppressedReleases = 0;
		suppressedClicks = 0;
	}

	private void consumePress(MouseEvent event)
	{
		int buttonMask = 1 << event.getButton();
		suppressedReleases |= buttonMask;
		suppressedClicks |= buttonMask;
		event.consume();
	}

	public static PingType choose(double dx, double dy)
	{
		if (dx * dx + dy * dy < 18 * 18)
		{
			return PingType.GO;
		}
		return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? PingType.GO : PingType.USE)
			: (dy > 0 ? PingType.ATTACK : PingType.FLEE);
	}

	public static final class Wheel
	{
		volatile PingSelection selection;
		final Point origin;
		final long generation;

		Wheel(Point origin, long generation)
		{
			this.origin = origin;
			this.generation = generation;
		}
	}

	private static final class Hover
	{
		private final PingSelection target;

		private Hover(PingSelection target)
		{
			this.target = target;
		}
	}
}
