/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.geom.Point2D;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PingModelTest
{
	private static final WorldPoint TILE = new WorldPoint(3200, 3200, 0);

	@Test
	public void rippleExpandsAndReturnsContinuously()
	{
		assertEquals(0, PingAnimation.radius(0), 0.00001);
		assertEquals(1.35, PingAnimation.radius(0.52), 0.00001);
		assertEquals(0, PingAnimation.radius(1), 0.00001);
		double previous = 0;
		for (int i = 0; i <= 520; i++)
		{
			double radius = PingAnimation.radius(i / 1000.0);
			assertTrue(radius >= previous);
			previous = radius;
		}
		for (int i = 521; i <= 1000; i++)
		{
			double radius = PingAnimation.radius(i / 1000.0);
			assertTrue(radius <= previous);
			previous = radius;
		}
		assertEquals(0, PingAnimation.opacity(0), 0);
		assertEquals(0, PingAnimation.opacity(1), 0);
		assertEquals(1, PingAnimation.opacity(0.5), 0);
	}

	@Test
	public void monotonicLifetimeIgnoresWallClock()
	{
		ActivePing ping = new ActivePing(PingType.GO, TILE, 500_000_000L, 1600);
		assertEquals(0, ping.progress(400_000_000L), 0);
		assertEquals(0.5, ping.progress(1_300_000_000L), 0);
		assertEquals(1, ping.progress(9_000_000_000L), 0);
	}

	@Test
	public void minuteLongPingAppearsPromptlyHoldsAndExpiresAtItsFullLifetime()
	{
		PingStore store = new PingStore();
		assertTrue(store.add("long", 1, PingType.GO, TILE, 0, 60000));
		ActivePing ping = store.getActive().get(0);
		assertTrue(PingAnimation.opacity(ping.visualProgress(100_000_000L)) > 0.9);
		assertEquals(0.52, ping.visualProgress(1_000_000_000L), 0.00001);
		assertEquals(0.52, ping.visualProgress(30_000_000_000L), 0.00001);
		assertTrue(ping.visualProgress(59_600_000_000L) > 0.52);
		store.prune(59_999_000_000L);
		assertEquals(1, store.getActive().size());
		assertEquals(1, ping.visualProgress(60_000_000_000L), 0);
		store.prune(60_000_000_000L);
		assertTrue(store.getActive().isEmpty());
	}

	@Test
	public void deduplicationRateLimitsAndExpiry()
	{
		PingStore store = new PingStore();
		assertTrue(store.add("one", 1, PingType.FLEE, TILE, 0, 1600));
		assertFalse(store.add("one", 1, PingType.FLEE, TILE, 500_000_000L, 1600));
		assertFalse(store.add("two", 1, PingType.FLEE, TILE, 200_000_000L, 1600));
		assertFalse(store.add("two", 2, PingType.GO, TILE, 100_000_000L, 1600));
		assertTrue(store.add("two", 2, PingType.GO, TILE, 200_000_000L, 1600));
		assertTrue(store.add("three", 1, PingType.ATTACK, TILE, 500_000_000L, 1600));
		assertEquals(3, store.getActive().size());
		store.prune(2_100_000_000L);
		assertTrue(store.getActive().isEmpty());
		store.clear();
		assertTrue(store.add("one", 1, PingType.USE, TILE, 0, 1600));
	}

	@Test
	public void rejectsMalformedUnknownAndStaleWireData()
	{
		// Test-only serializer; production uses RuneLite's injected transport serializer.
		Gson gson = new Gson();
		TacticalPingMessage original = new TacticalPingMessage(PingType.ATTACK, 301, TILE, false, TILE, 100000);
		String json = gson.toJson(original);
		assertTrue(gson.fromJson(json, TacticalPingMessage.class).isValid(100000));
		String[] fields = {"version", "id", "type", "world", "x", "y", "plane", "templatePlane", "sentAt"};
		Object[] invalid = {2, "invalid", "AUTO_ATTACK", 0, -1, 16384, 4, -1, 1};
		for (int i = 0; i < fields.length; i++)
		{
			JsonObject object = gson.fromJson(json, JsonObject.class);
			if (invalid[i] instanceof Number)
			{
				object.addProperty(fields[i], (Number) invalid[i]);
			}
			else
			{
				object.addProperty(fields[i], (String) invalid[i]);
			}
			assertFalse(fields[i], gson.fromJson(object, TacticalPingMessage.class).isValid(100000));
		}
		assertFalse(new TacticalPingMessage().isValid(100000));
		assertNull(PingType.fromWire(null));
	}

	@Test
	public void wheelDirectionsAndDeadzone()
	{
		assertEquals(PingSlot.TWO, PingWheelLayout.choose(0, 0, 4, PingSlot.TWO));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(0, -40, 4, PingSlot.TWO));
		assertEquals(PingSlot.TWO, PingWheelLayout.choose(40, 0, 4, PingSlot.TWO));
		assertEquals(PingSlot.THREE, PingWheelLayout.choose(0, 40, 4, PingSlot.TWO));
		assertEquals(PingSlot.FOUR, PingWheelLayout.choose(-40, 0, 4, PingSlot.TWO));
	}

	@Test
	public void minimapInverseHandlesRotatedScaledBasis()
	{
		Point2D result = PingTargetResolver.invert(14, -2, 4, 4, 6, -6);
		assertEquals(1.5, result.getX(), 0.0001);
		assertEquals(4.0 / 3, result.getY(), 0.0001);
		assertNull(PingTargetResolver.invert(5, 5, 1, 1, 2, 2));
	}

	@Test
	public void optionalTargetRoundTripsAndMalformedTargetIsRejected()
	{
		Gson gson = new Gson();
		TacticalPingMessage original = new TacticalPingMessage(PingType.ATTACK, 301, TILE, false, TILE, 100000);
		original.setTarget(new PingTarget(PingTarget.Kind.PLAYER, 0, 17, "a friend"));
		TacticalPingMessage copy = gson.fromJson(gson.toJson(original), TacticalPingMessage.class);
		assertTrue(copy.isValid(100000));
		assertEquals("a friend", copy.getTarget().getName());
		copy.setTarget(new PingTarget(PingTarget.Kind.PLAYER, 0, 99999, "a friend"));
		assertFalse(copy.isValid(100000));
	}
}
