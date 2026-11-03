package com.horstmann.violet.workspace.sidebar.colortools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class ColorToolsBarPanelTest
{
    private static final double MINIMUM_CONTRAST_RATIO = 4.5;

    @Test
    void textPaletteOffersTheDefaultTextColorAndBlack()
    {
        assertTrue(ColorToolsBarPanel.TEXT_COLOR_LIST.contains(ColorToolsBarPanel.DEFAULT_TEXT_COLOR));
        assertTrue(ColorToolsBarPanel.TEXT_COLOR_LIST.contains(Color.BLACK));
    }

    @Test
    void everyTextColorIsReadableOnWhite()
    {
        for (Color aColor : ColorToolsBarPanel.TEXT_COLOR_LIST)
        {
            double ratio = contrastRatio(aColor, Color.WHITE);
            assertTrue(ratio >= MINIMUM_CONTRAST_RATIO, aColor + " has a ratio of " + ratio);
        }
    }

    private static double contrastRatio(Color first, Color second)
    {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05) / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double luminance(Color color)
    {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel)
    {
        double value = channel / 255.0;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
