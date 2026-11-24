/* Copyright (c) 2026, Drew. SPDX-License-Identifier: BSD-2-Clause */
package com.tacticalpings;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PingWheelLayoutTest
{
	@Test
	public void everySectorCenterAndInteriorMatchesItsPaintedSlotForAllCounts()
	{
		for (int count = 1; count <= 8; count++)
		{
			for (int index = 0; index < count; index++)
			{
				for (double fraction : new double[] {0.001, 0.25, 0.5, 0.75, 0.999})
				{
					double angle = -Math.PI / 2 + (index + fraction) * Math.PI * 2 / count;
					assertEquals(PingSlot.values()[index], PingWheelLayout.choose(Math.cos(angle) * 100,
						Math.sin(angle) * 100, count, PingSlot.TWO));
				}
				double center = PingWheelLayout.angle(index, count);
				assertEquals(PingSlot.values()[index], PingWheelLayout.choose(Math.cos(center) * 100,
					Math.sin(center) * 100, count, PingSlot.TWO));
			}
		}
	}

	@Test
	public void twoUsesVerticalSplitAndFourUsesQuadrants()
	{
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(80, -80, 2, null));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(80, 80, 2, null));
		assertEquals(PingSlot.TWO, PingWheelLayout.choose(-80, -80, 2, null));
		assertEquals(PingSlot.TWO, PingWheelLayout.choose(-80, 80, 2, null));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(80, -80, 4, null));
		assertEquals(PingSlot.TWO, PingWheelLayout.choose(80, 80, 4, null));
		assertEquals(PingSlot.THREE, PingWheelLayout.choose(-80, 80, 4, null));
		assertEquals(PingSlot.FOUR, PingWheelLayout.choose(-80, -80, 4, null));
	}

	@Test
	public void countAndQuickClickAreBoundedAndNeverChooseADisabledSlot()
	{
		assertEquals(1, PingWheelLayout.count(-1));
		assertEquals(8, PingWheelLayout.count(100));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(0, 0, 1, PingSlot.TWO));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(0, 0, 4, PingSlot.EIGHT));
		assertEquals(PingSlot.EIGHT, PingWheelLayout.choose(1, 1, 8, PingSlot.EIGHT));
		assertEquals(PingSlot.ONE, PingWheelLayout.choose(-80, 80, 1, null));
	}
}
