/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Color;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PingStyleTest
{
	private final Gson gson = new Gson();
	private static final WorldPoint TILE = new WorldPoint(3200, 3200, 0);

	@Test
	public void everySlotAndLibraryChoiceRoundTripsWithLiteralUnicodeText()
	{
		for (PingSlot slot : PingSlot.values())
		{
			for (PingSound sound : PingSound.values())
			{
				PingStyle design = new PingStyle(new Color(0x1234ab), PingSymbol.values()[slot.ordinal()], sound, "Stack <here> — café! ⚑");
				TacticalPingMessage outgoing = message(slot);
				outgoing.setStyle(design);
				String json = gson.toJson(outgoing);
				assertTrue(json.length() < 1024);
				TacticalPingMessage incoming = gson.fromJson(json, TacticalPingMessage.class);
				assertTrue(incoming.isValid(100000));
				assertEquals(slot, incoming.getSlot());
				assertStyle(design, incoming.getStyle());
			}
		}
	}

	@Test
	public void oldMessagesKeepOriginalDesignAndNewFieldsDoNotChangeBaseProtocol()
	{
		TacticalPingMessage old = new TacticalPingMessage(PingType.ATTACK, 301, TILE, false, TILE, 100000);
		assertStyle(PingStyle.defaults(PingType.ATTACK), gson.fromJson(gson.toJson(old), TacticalPingMessage.class).getStyle());
		assertEquals(PingSlot.THREE, old.getSlot());
		JsonObject json = gson.toJsonTree(message(PingSlot.EIGHT)).getAsJsonObject();
		assertEquals(1, json.get("version").getAsInt());
		assertEquals("GO", json.get("type").getAsString());
		json.remove("slot");
		json.remove("style");
		assertTrue(gson.fromJson(json, TacticalPingMessage.class).isValid(100000));
	}

	@Test
	public void unknownAssetsAndMalformedDesignsFallBackWithoutLosingThePing()
	{
		TacticalPingMessage outgoing = message(PingSlot.THREE);
		outgoing.setStyle(PingSlot.EIGHT.getDefaultStyle());
		String[] fields = {"rgb", "symbol", "sound", "text"};
		Object[] invalid = {-1, "NEW_UNKNOWN_SYMBOL", "https://example.invalid/sound.wav", "x".repeat(65)};
		for (int i = 0; i < fields.length; i++)
		{
			JsonObject json = gson.toJsonTree(outgoing).getAsJsonObject();
			if (invalid[i] instanceof Number)
			{
				json.getAsJsonObject("style").addProperty(fields[i], (Number) invalid[i]);
			}
			else
			{
				json.getAsJsonObject("style").addProperty(fields[i], (String) invalid[i]);
			}
			TacticalPingMessage received = gson.fromJson(json, TacticalPingMessage.class);
			assertTrue(received.isValid(100000));
			assertStyle(PingStyle.defaults(PingType.ATTACK), received.getStyle());
		}
	}

	@Test
	public void textIsBoundedByCodePointsAndMenuMarkupIsEscaped()
	{
		String unicode = "🚩".repeat(64);
		assertTrue(PingStyle.validText(unicode));
		assertFalse(PingStyle.validText(unicode + "x"));
		assertFalse(PingStyle.validText("hidden\u202etext"));
		assertFalse(PingStyle.validText("line\nfeed"));
		assertFalse(PingStyle.validText("broken\ud800"));
		assertEquals(unicode, PingStyle.cleanText(unicode + "excess"));
		PingStyle style = new PingStyle(Color.GREEN, PingSymbol.FLAG, PingSound.BELL, "<col=ff0000>Go</col>");
		assertEquals("<lt>col=ff0000<gt>Go<lt>/col<gt>", style.getMenuText());
		assertEquals("<col=ff0000>Go</col>", style.getText());
	}

	@Test
	public void invalidSlotNumbersAndMismatchedLegacyActionsAreRejected()
	{
		for (int slot : new int[] {-1, 8, 999})
		{
			JsonObject json = gson.toJsonTree(message(PingSlot.ONE)).getAsJsonObject();
			json.addProperty("slot", slot);
			assertFalse(gson.fromJson(json, TacticalPingMessage.class).isValid(100000));
		}
		JsonObject json = gson.toJsonTree(message(PingSlot.ONE)).getAsJsonObject();
		json.addProperty("slot", 7);
		assertFalse(gson.fromJson(json, TacticalPingMessage.class).isValid(100000));
	}

	@Test
	public void defaultConfigurationResolvesAllEightDesigns()
	{
		TacticalPingsConfig config = new TacticalPingsConfig() {};
		for (PingSlot slot : PingSlot.values())
		{
			assertStyle(slot.getDefaultStyle(), PingStyle.fromConfig(config, slot));
		}
		assertTrue(config.sharedDesigns());
		assertTrue(config.labels());
	}

	private TacticalPingMessage message(PingSlot slot)
	{
		return new TacticalPingMessage(slot, 301, TILE, false, TILE, 100000);
	}

	static void assertStyle(PingStyle expected, PingStyle actual)
	{
		assertEquals(expected.getColor(), actual.getColor());
		assertEquals(expected.getSymbol(), actual.getSymbol());
		assertEquals(expected.getSound(), actual.getSound());
		assertEquals(expected.getText(), actual.getText());
	}
}
