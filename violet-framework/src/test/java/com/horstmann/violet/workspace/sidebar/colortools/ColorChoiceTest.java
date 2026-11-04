package com.horstmann.violet.workspace.sidebar.colortools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ColorChoiceTest
{
    @Test
    void textPaletteOffersTheDefaultTextColorAndBlack()
    {
        assertTrue(ColorChoice.TEXT_COLOR_LIST.contains(ColorChoice.DEFAULT_TEXT_COLOR));
        assertTrue(ColorChoice.TEXT_COLOR_LIST.contains(Color.BLACK));
    }

    @Test
    void defaultTextColorIsThePastelGreyBorder()
    {
        assertEquals(ColorChoice.PASTEL_GREY.getBorderColor(), ColorChoice.DEFAULT_TEXT_COLOR);
    }

    @Test
    void textPaletteMatchesTheColorChoiceBorders()
    {
        List<Color> borders = new ArrayList<Color>();
        for (ColorChoice aChoice : ColorChoice.PALETTE)
        {
            borders.add(aChoice.getBorderColor());
        }
        assertEquals(borders, ColorChoice.TEXT_COLOR_LIST);
    }
}
