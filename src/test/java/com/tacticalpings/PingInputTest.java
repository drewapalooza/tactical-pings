/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PingInputTest
{
	private final Canvas canvas = new Canvas();
	private final KeyManager keys = mock(KeyManager.class);
	private final TacticalPingsPlugin plugin = mock(TacticalPingsPlugin.class);
	private final ClientThread clientThread = mock(ClientThread.class);
	private final PingTargetResolver resolver = mock(PingTargetResolver.class);
	private final List<Runnable> clientCalls = new ArrayList<>();
	private boolean deferClientCalls;
	private final WorldPoint tile = new WorldPoint(3200, 3200, 0);
	private PingInput input;
	private KeyListener hotkey;
	private PingActivation activation = PingActivation.KEYBOARD;
	private Keybind binding = Keybind.ALT;

	@Before
	public void setup()
	{
		doAnswer(call ->
		{
			if (deferClientCalls)
			{
				clientCalls.add(call.getArgument(0, Runnable.class));
			}
			else
			{
				call.getArgument(0, Runnable.class).run();
			}
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		when(resolver.captureAtPress(any(), any())).thenAnswer(call -> call.getArgument(1));
		input = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(TacticalPingsConfig.class).toInstance(new TacticalPingsConfig()
				{
					@Override
					public PingActivation inputMethod()
					{
						return activation;
					}

					@Override
					public Keybind wheelKey()
					{
						return binding;
					}
				});
				bind(TacticalPingsPlugin.class).toProvider(() -> plugin);
				bind(ClientThread.class).toProvider(() -> clientThread);
				bind(KeyManager.class).toInstance(keys);
				bind(MouseManager.class).toProvider(() -> mock(MouseManager.class));
				bind(PingTargetResolver.class).toProvider(() -> resolver);
			}
		}).getInstance(PingInput.class);
		input.start();
		ArgumentCaptor<KeyListener> capture = ArgumentCaptor.forClass(KeyListener.class);
		verify(keys, org.mockito.Mockito.times(2)).registerKeyListener(capture.capture());
		hotkey = capture.getAllValues().get(0);
		hotkey.keyPressed(new KeyEvent(canvas, KeyEvent.KEY_PRESSED, 1, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED));
		input.updateHover(tile, new Point(100, 100));
	}

	@Test
	public void dragConsumesGameClickAndSendsExactlyOneChosenPing()
	{
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(press);
		assertTrue(press.isConsumed());
		MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, 100, 50, MouseEvent.BUTTON1);
		input.mouseReleased(release);
		assertTrue(release.isConsumed());
		verify(plugin).sendPing(tile, PingType.FLEE);
		MouseEvent click = mouse(MouseEvent.MOUSE_CLICKED, 100, 50, MouseEvent.BUTTON1);
		input.mouseClicked(click);
		assertTrue(click.isConsumed());
		assertNull(input.getWheel());
	}

	@Test
	public void escapeCancelsWithoutForwardingRelease()
	{
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.keyPressed(new KeyEvent(canvas, KeyEvent.KEY_PRESSED, 1, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
		MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON1);
		input.mouseReleased(release);
		assertTrue(release.isConsumed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void rightClickCancels()
	{
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON3));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON3));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON1));
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void focusLossDisarmsAndCancels()
	{
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.focusLost();
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON1));
		verify(plugin, never()).sendPing(any(), any());
		assertNull(input.getWheel());
	}

	@Test
	public void staleHoverNeverPingsWrongTileOrWalks()
	{
		when(resolver.captureAtPress(any(), any())).thenReturn(null);
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 180, 180, MouseEvent.BUTTON1);
		input.mousePressed(press);
		assertTrue(press.isConsumed());
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 180, 180, MouseEvent.BUTTON1));
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void immediateDragAndReleaseBeforeNextTickUsesMouseDownTarget()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		deferClientCalls = true;
		WorldPoint clicked = tile.dx(3);
		when(resolver.captureAtPress(any(), any())).thenReturn(new PingSelection(clicked, null));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 180, 180, MouseEvent.BUTTON1));
		assertNotNull(input.getWheel());
		input.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 180, 120, MouseEvent.NOBUTTON));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 180, 120, MouseEvent.BUTTON1));
		verify(plugin, never()).sendPing(any(), any());
		for (Runnable call : clientCalls)
		{
			call.run();
		}
		verify(resolver).captureAtPress(org.mockito.ArgumentMatchers.eq(new java.awt.Point(180, 180)), any());
		verify(plugin).sendPing(clicked, PingType.FLEE);
	}

	@Test
	public void clickedEntityStaysSelectedWhenDragMovesAcrossAnotherEntity()
	{
		PingOutline outline = new PingOutline(new PingTarget(PingTarget.Kind.NPC, 123, 7, null), null, null, null, null, null);
		PingSelection selected = new PingSelection(tile, outline);
		when(resolver.captureAtPress(any(), any())).thenReturn(selected);
		input.updateHoverSelection(selected, new Point(100, 100));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.updateHover(tile.dx(5), new Point(150, 100));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 150, 100, MouseEvent.BUTTON1));
		verify(plugin).sendSelection(selected, PingType.GO);
	}

	@Test
	public void mouseButtonFourOnlyPrimesThenLeftDragSendsOnce()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		assertTrue(input.isArmed());
		assertNull(input.getWheel());
		verify(plugin, never()).sendPing(any(), any());
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(press);
		assertTrue(press.isConsumed());
		assertFalse(input.isArmed());
		MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, 50, 100, MouseEvent.BUTTON1);
		input.mouseReleased(release);
		assertTrue(release.isConsumed());
		verify(plugin).sendPing(tile, PingType.USE);
		MouseEvent click = mouse(MouseEvent.MOUSE_CLICKED, 50, 100, MouseEvent.BUTTON1);
		input.mouseClicked(click);
		assertTrue(click.isConsumed());
		input.updateHover(tile, new Point(100, 100));
		MouseEvent next = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(next);
		assertFalse(next.isConsumed());
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON1));
		verify(plugin, org.mockito.Mockito.times(1)).sendPing(any(), any());
	}

	@Test
	public void mouseButtonFivePrimesThenLeftDragUsesTileFromLeftClick()
	{
		mouseMode(PingActivation.MOUSE_5);
		prime(5);
		input.updateHover(tile.dx(4), new Point(160, 160));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 160, 160, MouseEvent.BUTTON1));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 160, 210, MouseEvent.BUTTON1));
		verify(plugin).sendPing(tile.dx(4), PingType.ATTACK);
	}

	@Test
	public void primedQuickLeftClickMeansGo()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON1));
		verify(plugin).sendPing(tile, PingType.GO);
	}

	@Test
	public void mouseModePreservesLeftClickOtherSideButtonAndKeyboardBindings()
	{
		mouseMode(PingActivation.MOUSE_4);
		binding = new Keybind(KeyEvent.VK_F4, 0);
		KeyEvent key = new KeyEvent(canvas, KeyEvent.KEY_PRESSED, 1, 0, KeyEvent.VK_F4, KeyEvent.CHAR_UNDEFINED);
		hotkey.keyPressed(key);
		assertFalse(key.isConsumed());
		MouseEvent left = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(left);
		assertFalse(left.isConsumed());
		MouseEvent otherSide = sideMouse(100, 100, 5);
		input.mousePressed(otherSide);
		verify(otherSide, never()).consume();
		assertNull(input.getWheel());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void releasingSideButtonDuringLeftDragDoesNotSendPing()
	{
		mouseMode(PingActivation.MOUSE_5);
		input.mousePressed(sideMouse(100, 100, 5));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1));
		input.mouseReleased(sideMouse(100, 50, 5));
		verify(plugin, never()).sendPing(any(), any());
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 100, 50, MouseEvent.BUTTON1));
		verify(plugin).sendPing(tile, PingType.FLEE);
	}

	@Test
	public void escapeCancelsSideButtonPing()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		input.keyPressed(new KeyEvent(canvas, KeyEvent.KEY_PRESSED, 1, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
		assertFalse(input.isArmed());
		input.updateHover(tile, new Point(100, 100));
		MouseEvent left = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(left);
		assertFalse(left.isConsumed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void secondSideButtonPressCancelsPriming()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		prime(4);
		assertFalse(input.isArmed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void rightClickCancelsPrimingWithoutOpeningGameMenu()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON3);
		input.mousePressed(press);
		assertTrue(press.isConsumed());
		MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, 100, 100, MouseEvent.BUTTON3);
		input.mouseReleased(release);
		assertTrue(release.isConsumed());
		assertFalse(input.isArmed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void canPrimeOverInterfaceThenChooseGroundTile()
	{
		mouseMode(PingActivation.MOUSE_4);
		input.updateHover(null, new Point(100, 100));
		prime(4);
		assertTrue(input.isArmed());
		input.updateHover(tile, new Point(160, 160));
		input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 160, 160, MouseEvent.BUTTON1));
		input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, 160, 160, MouseEvent.BUTTON1));
		verify(plugin).sendPing(tile, PingType.GO);
	}

	@Test
	public void interfaceClickCancelsPrimingAndKeepsInterfaceAction()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		input.updateHover(null, new Point(100, 100));
		MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1);
		input.mousePressed(press);
		assertFalse(press.isConsumed());
		assertFalse(input.isArmed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void focusLossAndLeavingCanvasCancelPriming()
	{
		mouseMode(PingActivation.MOUSE_4);
		prime(4);
		input.focusLost();
		assertFalse(input.isArmed());
		prime(4);
		assertTrue(input.isArmed());
		input.mouseExited(mouse(MouseEvent.MOUSE_EXITED, 100, 100, MouseEvent.NOBUTTON));
		assertFalse(input.isArmed());
		verify(plugin, never()).sendPing(any(), any());
	}

	@Test
	public void gameDisabledMinimapIsNeverDrawnOrUsed()
	{
		Client client = mock(Client.class);
		when(client.getVarbitValue(VarbitID.MINIMAP_STATE)).thenReturn(2);
		PingTargetResolver resolver = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(Client.class).toInstance(client);
			}
		}).getInstance(PingTargetResolver.class);
		assertNull(resolver.minimapBounds());
		verify(client, never()).getWidget(anyInt());
	}

	private MouseEvent mouse(int id, int x, int y, int button)
	{
		return new MouseEvent(canvas, id, 1, InputEvent.ALT_DOWN_MASK, x, y, 1, false, button);
	}

	private void mouseMode(PingActivation selected)
	{
		input.focusLost();
		activation = selected;
		input.updateHover(tile, new Point(100, 100));
	}

	private MouseEvent sideMouse(int x, int y, int button)
	{
		// Avoid depending on the test machine's physical mouse having five buttons.
		MouseEvent event = mock(MouseEvent.class);
		when(event.getButton()).thenReturn(button);
		when(event.getPoint()).thenReturn(new java.awt.Point(x, y));
		return event;
	}

	private void prime(int button)
	{
		MouseEvent press = sideMouse(100, 100, button);
		input.mousePressed(press);
		verify(press).consume();
		MouseEvent release = sideMouse(100, 100, button);
		input.mouseReleased(release);
		verify(release).consume();
		MouseEvent click = sideMouse(100, 100, button);
		input.mouseClicked(click);
		verify(click).consume();
	}
}
