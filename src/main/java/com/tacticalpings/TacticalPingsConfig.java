/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(TacticalPingsConfig.GROUP)
public interface TacticalPingsConfig extends Config
{
	String GROUP = "tacticalpings";

	@ConfigItem(keyName = "inputMethod", name = "Ping input", position = 0,
		description = "Tap mouse button 4/5 to prime one ping, then left-click and drag to choose and release to send. Keyboard mode uses a held modifier plus left click. A quick left click means Go.")
	default PingActivation inputMethod()
	{
		return PingActivation.KEYBOARD;
	}

	@ConfigItem(keyName = "wheelKey", name = "Ping wheel key", position = 1,
		description = "Keyboard input only: hold this key and left-click a scene or minimap tile. Ignored when using mouse button 4/5.")
	default Keybind wheelKey()
	{
		return Keybind.ALT;
	}

	@ConfigItem(keyName = "menuEntries", name = "Right-click pings", position = 2,
		description = "Add four manual ping choices to the ground right-click menu")
	default boolean menuEntries()
	{
		return true;
	}

	@ConfigItem(keyName = "sharePings", name = "Share with party", position = 3,
		description = "Send your clicked tile/target, ping type, world, instance/template coordinates, timestamp and random ping ID through RuneLite's party relay. Player targets include the clicked player's name and index.")
	default boolean sharePings()
	{
		return true;
	}

	@ConfigItem(keyName = "receivePings", name = "Receive party pings", position = 4,
		description = "Display Ayra's Pings from current RuneLite party members in the same world, plane and loaded scene")
	default boolean receivePings()
	{
		return true;
	}

	@Range(min = 800, max = 3000)
	@ConfigItem(keyName = "durationMillis", name = "Duration (ms)", position = 5,
		description = "Time for the ripple to expand, return to the clicked tile, and fade")
	default int durationMillis()
	{
		return 1600;
	}

	@ConfigItem(keyName = "sounds", name = "Ping chimes", position = 6,
		description = "Play a distinct chime for each ping type")
	default boolean sounds()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "volume", name = "Chime volume", position = 7,
		description = "Independent ping volume; zero mutes all ping chimes")
	default int volume()
	{
		return 60;
	}

	@ConfigItem(keyName = "minimap", name = "Minimap ripples", position = 8,
		description = "Show small matching ripples on the visible minimap")
	default boolean minimap()
	{
		return true;
	}

	@ConfigItem(keyName = "labels", name = "Scene labels", position = 9,
		description = "Show Flee, Go, Attack or Use beneath the scene symbol")
	default boolean labels()
	{
		return false;
	}

	@ConfigItem(keyName = "reducedMotion", name = "Reduced motion", position = 10,
		description = "Use a stationary tile and ring with a gentle fade instead of an expanding ripple")
	default boolean reducedMotion()
	{
		return false;
	}

	@ConfigItem(keyName = "targetOutlines", name = "Clicked-target outlines", position = 11,
		description = "Pulse the border of a manually pinged NPC, player, world object or visible dropped item in the ping color")
	default boolean targetOutlines()
	{
		return true;
	}
}
