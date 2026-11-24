/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.CameraFocusableEntity;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises the real minimap projection, active-widget selection and clipped renderer. */
public class MinimapPingOverlayTest
{
	private static final Rectangle MAP = new Rectangle(610, 20, 150, 150);
	private static final WorldPoint ORIGIN = new WorldPoint(3250, 3250, 0);
	private final Client client = mock(Client.class);
	private final WorldView view = mock(WorldView.class);
	private final TacticalPingsPlugin plugin = mock(TacticalPingsPlugin.class);
	private MinimapPingOverlay overlay;
	private PingTargetResolver resolver;

	@Before
	public void setup()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(view);
		when(view.getId()).thenReturn(WorldView.TOPLEVEL);
		when(view.isTopLevel()).thenReturn(true);
		when(view.getBaseX()).thenReturn(3200);
		when(view.getBaseY()).thenReturn(3200);
		when(view.getSizeX()).thenReturn(104);
		when(view.getSizeY()).thenReturn(104);
		LocalPoint local = LocalPoint.fromWorld(view, ORIGIN);
		CameraFocusableEntity focus = mock(CameraFocusableEntity.class);
		when(focus.getCameraFocus()).thenReturn(local);
		when(client.getCameraFocusEntity()).thenReturn(focus);
		when(client.getMinimapZoom()).thenReturn(4.0);
		Player player = mock(Player.class);
		when(player.getLocalLocation()).thenReturn(local);
		when(player.getWorldView()).thenReturn(view);
		when(client.getLocalPlayer()).thenReturn(player);
		when(plugin.canShow(any())).thenReturn(true);
		setPing(ORIGIN);
		Injector injector = Guice.createInjector(new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(Client.class).toInstance(client);
				bind(TacticalPingsPlugin.class).toProvider(() -> plugin);
				bind(TacticalPingsConfig.class).toInstance(new TacticalPingsConfig() {});
			}
		});
		resolver = injector.getInstance(PingTargetResolver.class);
		overlay = injector.getInstance(MinimapPingOverlay.class);
	}

	@Test
	public void fixedModeDrawsIndicator()
	{
		layout(false, 0, WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
		assertVisibleAndClipped();
	}

	@Test
	public void resizableModernDrawsIndicatorUsingVisibleWidget()
	{
		layout(true, 1, WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
		assertVisibleAndClipped();
	}

	@Test
	public void resizableClassicDrawsIndicatorUsingVisibleWidget()
	{
		layout(true, 0, WidgetInfo.RESIZABLE_MINIMAP_STONES_DRAW_AREA);
		assertVisibleAndClipped();
	}

	@Test
	public void rotatedZoomedMinimapProjectsMarkerAndAcceptsMouseTarget()
	{
		layout(true, 1, WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
		when(client.getMinimapZoom()).thenReturn(8.0);
		when(client.getCameraYawTarget()).thenReturn(4096);
		WorldPoint target = ORIGIN.dx(4);
		setPing(target);
		Point screen = Perspective.localToMinimap(client, LocalPoint.fromWorld(view, target));
		assertNotNull(screen);
		assertEquals(685, screen.getX());
		assertEquals(127, screen.getY());
		assertEquals(target, resolver.resolve(screen));
		assertTrue(paintedPixels(render(), new Rectangle(screen.getX() - 6, screen.getY() - 6, 12, 12)) > 5);
	}

	@Test
	public void disabledMinimapDoesNotDraw()
	{
		layout(true, 1, WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
		when(client.getVarbitValue(VarbitID.MINIMAP_STATE)).thenReturn(2);
		assertEquals(0, paintedPixels(render(), new Rectangle(0, 0, 800, 300)));
	}

	private void layout(boolean resized, int arrangement, WidgetInfo active)
	{
		when(client.isResized()).thenReturn(resized);
		when(client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT)).thenReturn(arrangement);
		for (WidgetInfo info : new WidgetInfo[] {WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA,
			WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA, WidgetInfo.RESIZABLE_MINIMAP_STONES_DRAW_AREA})
		{
			Widget widget = mock(Widget.class);
			when(widget.isHidden()).thenReturn(info != active);
			when(widget.getBounds()).thenReturn(MAP);
			when(widget.getCanvasLocation()).thenReturn(new Point(MAP.x, MAP.y));
			when(widget.getWidth()).thenReturn(MAP.width);
			when(widget.getHeight()).thenReturn(MAP.height);
			when(client.getWidget(info)).thenReturn(widget);
			// Perspective still uses this enum internally; production code uses the packed ID.
			when(client.getWidget(info.getId())).thenReturn(widget);
		}
	}

	private void setPing(WorldPoint target)
	{
		when(plugin.getActive()).thenAnswer(call -> Collections.singletonList(
			new ActivePing(PingType.FLEE, target, System.nanoTime() - 700_000_000L, 3000)));
	}

	private void assertVisibleAndClipped()
	{
		assertEquals(MAP, resolver.minimapBounds());
		BufferedImage image = render();
		assertTrue("No minimap indicator was rendered", paintedPixels(image, MAP) > 20);
		assertEquals(paintedPixels(image, MAP), paintedPixels(image, new Rectangle(0, 0, 800, 300)));
	}

	private BufferedImage render()
	{
		BufferedImage image = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		overlay.render(graphics);
		graphics.dispose();
		return image;
	}

	private int paintedPixels(BufferedImage image, Rectangle bounds)
	{
		int count = 0;
		for (int x = bounds.x; x < bounds.x + bounds.width; x++)
		{
			for (int y = bounds.y; y < bounds.y + bounds.height; y++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					count++;
				}
			}
		}
		return count;
	}
}
