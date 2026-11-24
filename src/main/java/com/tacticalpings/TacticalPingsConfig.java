/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(TacticalPingsConfig.GROUP)
public interface TacticalPingsConfig extends Config
{
	String GROUP = "tacticalpings";

	@ConfigSection(name = "Wheel & timing", description = "Wheel size, quick click and lifetime", position = 0, closedByDefault = false)
	String WHEEL_SECTION = "wheel";

	@ConfigSection(name = "Controls", description = "Mouse, keyboard and right-click controls", position = 1, closedByDefault = true)
	String CONTROLS_SECTION = "controls";

	@ConfigSection(name = "Party & sound", description = "Sharing, receiving, designs and audio", position = 2, closedByDefault = true)
	String PARTY_SECTION = "party";

	@ConfigSection(name = "Visuals", description = "Minimap, labels, outlines and reduced motion", position = 3, closedByDefault = true)
	String VISUALS_SECTION = "visuals";


	@ConfigItem(keyName = "inputMethod", name = "Ping input", position = 0, section = CONTROLS_SECTION,
		description = "Tap mouse button 4/5 to prime one ping, then left-click and drag to choose and release to send. Keyboard mode uses a held modifier plus left click. A quick left click sends your Quick-click ping.")
	default PingActivation inputMethod()
	{
		return PingActivation.KEYBOARD;
	}

	@ConfigItem(keyName = "wheelKey", name = "Ping wheel key", position = 1, section = CONTROLS_SECTION,
		description = "Keyboard input only: hold this key and left-click a scene or minimap tile. Ignored when using mouse button 4/5.")
	default Keybind wheelKey()
	{
		return Keybind.ALT;
	}

	@ConfigItem(keyName = "menuEntries", name = "Right-click pings", position = 2, section = CONTROLS_SECTION,
		description = "Add the enabled ping slots to the ground right-click menu")
	default boolean menuEntries()
	{
		return true;
	}

	@ConfigItem(keyName = "sharePings", name = "Share with party", position = 3, section = PARTY_SECTION,
		description = "Send your clicked tile/target, type, color, symbol, text, bundled sound choice, world, instance/template coordinates, timestamp and random ping ID through RuneLite's party relay. Player targets include the clicked player's name and index.")
	default boolean sharePings()
	{
		return true;
	}

	@ConfigItem(keyName = "receivePings", name = "Receive party pings", position = 4, section = PARTY_SECTION,
		description = "Display Ayra's Pings from current RuneLite party members in the same world, plane and loaded scene")
	default boolean receivePings()
	{
		return true;
	}

	@Range(min = 800, max = 60000)
	@ConfigItem(keyName = "durationMillis", name = "Duration (ms)", position = 5, section = WHEEL_SECTION,
		description = "Lifetime from 800 ms to 60,000 ms (one minute); long pings appear promptly, hold, then return and fade")
	default int durationMillis()
	{
		return 1600;
	}

	@ConfigItem(keyName = "sounds", name = "Ping chimes", position = 6, section = PARTY_SECTION,
		description = "Play a distinct chime for each ping type")
	default boolean sounds()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "volume", name = "Chime volume", position = 7, section = PARTY_SECTION,
		description = "Independent ping volume; zero mutes all ping chimes")
	default int volume()
	{
		return 60;
	}

	@ConfigItem(keyName = "minimap", name = "Minimap ripples", position = 8, section = VISUALS_SECTION,
		description = "Show small matching ripples on the visible minimap")
	default boolean minimap()
	{
		return true;
	}

	@ConfigItem(keyName = "labels", name = "Scene labels", position = 9, section = VISUALS_SECTION,
		description = "Show the ping's text beneath its scene symbol, including shared custom text")
	default boolean labels()
	{
		return true;
	}

	@ConfigItem(keyName = "reducedMotion", name = "Reduced motion", position = 10, section = VISUALS_SECTION,
		description = "Use a stationary tile and ring with a gentle fade instead of an expanding ripple")
	default boolean reducedMotion()
	{
		return false;
	}

	@ConfigItem(keyName = "targetOutlines", name = "Clicked-target outlines", position = 11, section = VISUALS_SECTION,
		description = "Pulse the border of a manually pinged NPC, player, world object or visible dropped item in the ping color")
	default boolean targetOutlines()
	{
		return true;
	}

	@ConfigItem(keyName = "sharedDesigns", name = "Use shared designs", position = 12, section = PARTY_SECTION,
		description = "Use the sender's color, symbol, text and bundled sound choice. Disable to use your own slot designs for incoming pings. Volume, mute, labels and reduced motion always use your settings.")
	default boolean sharedDesigns()
	{
		return true;
	}

	@Range(min = 1, max = 8)
	@ConfigItem(keyName = "pingCount", name = "Ping count", position = 13, section = WHEEL_SECTION,
		description = "Enable 1 to 8 slots. Customize active designs in the Ayra's Pings sidebar; inactive slots keep their saved designs.")
	default int pingCount()
	{
		return 4;
	}

	@ConfigItem(keyName = "quickPing", name = "Quick-click ping", position = 14, section = WHEEL_SECTION,
		description = "Slot sent when releasing without dragging. If this slot is disabled, Ping 1 is used.")
	default PingSlot quickPing()
	{
		return PingSlot.TWO;
	}

	String FLEE_SECTION = "flee";

	@ConfigItem(keyName = "fleeColor", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color fleeColor()
	{
		return PingType.FLEE.getColor();
	}

	@ConfigItem(keyName = "fleeSymbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol fleeSymbol()
	{
		return PingSymbol.EXCLAMATION;
	}

	@ConfigItem(keyName = "fleeText", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String fleeText()
	{
		return "Flee";
	}

	@ConfigItem(keyName = "fleeSound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound fleeSound()
	{
		return PingSound.FALLING;
	}

	String GO_SECTION = "go";

	@ConfigItem(keyName = "goColor", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color goColor()
	{
		return PingType.GO.getColor();
	}

	@ConfigItem(keyName = "goSymbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol goSymbol()
	{
		return PingSymbol.ARROW;
	}

	@ConfigItem(keyName = "goText", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String goText()
	{
		return "Go";
	}

	@ConfigItem(keyName = "goSound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound goSound()
	{
		return PingSound.RISING;
	}

	String ATTACK_SECTION = "attack";

	@ConfigItem(keyName = "attackColor", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color attackColor()
	{
		return PingType.ATTACK.getColor();
	}

	@ConfigItem(keyName = "attackSymbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol attackSymbol()
	{
		return PingSymbol.CROSSHAIR;
	}

	@ConfigItem(keyName = "attackText", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String attackText()
	{
		return "Attack";
	}

	@ConfigItem(keyName = "attackSound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound attackSound()
	{
		return PingSound.STRIKES;
	}

	String USE_SECTION = "use";

	@ConfigItem(keyName = "useColor", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color useColor()
	{
		return PingType.USE.getColor();
	}

	@ConfigItem(keyName = "useSymbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol useSymbol()
	{
		return PingSymbol.DIAMOND;
	}

	@ConfigItem(keyName = "useText", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String useText()
	{
		return "Use";
	}

	@ConfigItem(keyName = "useSound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound useSound()
	{
		return PingSound.SPARKLE;
	}

	String PING5_SECTION = "ping5";

	@ConfigItem(keyName = "ping5Color", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color ping5Color()
	{
		return PingSlot.FIVE.getDefaultStyle().getColor();
	}

	@ConfigItem(keyName = "ping5Symbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol ping5Symbol()
	{
		return PingSlot.FIVE.getDefaultStyle().getSymbol();
	}

	@ConfigItem(keyName = "ping5Text", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String ping5Text()
	{
		return PingSlot.FIVE.getDefaultStyle().getText();
	}

	@ConfigItem(keyName = "ping5Sound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound ping5Sound()
	{
		return PingSlot.FIVE.getDefaultStyle().getSound();
	}

	String PING6_SECTION = "ping6";

	@ConfigItem(keyName = "ping6Color", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color ping6Color()
	{
		return PingSlot.SIX.getDefaultStyle().getColor();
	}

	@ConfigItem(keyName = "ping6Symbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol ping6Symbol()
	{
		return PingSlot.SIX.getDefaultStyle().getSymbol();
	}

	@ConfigItem(keyName = "ping6Text", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String ping6Text()
	{
		return PingSlot.SIX.getDefaultStyle().getText();
	}

	@ConfigItem(keyName = "ping6Sound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound ping6Sound()
	{
		return PingSlot.SIX.getDefaultStyle().getSound();
	}

	String PING7_SECTION = "ping7";

	@ConfigItem(keyName = "ping7Color", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color ping7Color()
	{
		return PingSlot.SEVEN.getDefaultStyle().getColor();
	}

	@ConfigItem(keyName = "ping7Symbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol ping7Symbol()
	{
		return PingSlot.SEVEN.getDefaultStyle().getSymbol();
	}

	@ConfigItem(keyName = "ping7Text", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String ping7Text()
	{
		return PingSlot.SEVEN.getDefaultStyle().getText();
	}

	@ConfigItem(keyName = "ping7Sound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound ping7Sound()
	{
		return PingSlot.SEVEN.getDefaultStyle().getSound();
	}

	String PING8_SECTION = "ping8";

	@ConfigItem(keyName = "ping8Color", name = "Color", description = "Choose the shared RGB color", hidden = true, position = 0)
	default Color ping8Color()
	{
		return PingSlot.EIGHT.getDefaultStyle().getColor();
	}

	@ConfigItem(keyName = "ping8Symbol", name = "Symbol", description = "Choose a symbol from the bundled library", hidden = true, position = 1)
	default PingSymbol ping8Symbol()
	{
		return PingSlot.EIGHT.getDefaultStyle().getSymbol();
	}

	@ConfigItem(keyName = "ping8Text", name = "Text", description = "Shared plain text, up to 64 Unicode characters; leave empty for no label", hidden = true, position = 2)
	default String ping8Text()
	{
		return PingSlot.EIGHT.getDefaultStyle().getText();
	}

	@ConfigItem(keyName = "ping8Sound", name = "Sound", description = "Choose an original bundled chime or Silent; listeners control their own volume", hidden = true, position = 3)
	default PingSound ping8Sound()
	{
		return PingSlot.EIGHT.getDefaultStyle().getSound();
	}


}
