/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import java.awt.event.MouseEvent;

public enum PingActivation
{
	KEYBOARD("Keyboard + left click", MouseEvent.BUTTON1),
	MOUSE_4("Mouse button 4", 4),
	MOUSE_5("Mouse button 5", 5);

	private final String label;
	private final int mouseButton;

	PingActivation(String label, int mouseButton)
	{
		this.label = label;
		this.mouseButton = mouseButton;
	}

	public int getMouseButton()
	{
		return mouseButton;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
