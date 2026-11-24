package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.theme.FlatLightTheme;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;

/**
 * A color chosen again loses its background, then gets it back
 */
class ColorToggleTest
{

    @BeforeAll
    static void installTheme()
    {
        FlatLightTheme theme = new FlatLightTheme();
        new ThemeManager()
        {
            @Override
            public ITheme getTheme()
            {
                return theme;
            }
        };
    }

    @Test
    void shouldToggleTheBackgroundOfTheCurrentColor()
    {
        ColorToolsBarPanel panel = new ColorToolsBarPanel();
        List<ColorChoice> applied = new ArrayList<ColorChoice>();
        panel.addColorChoiceChangeListener(applied::add);
        ColorChoice red = ColorChoice.PASTEL_RED;
        ColorChoice blue = ColorChoice.PASTEL_BLUE;
        panel.toggleColorChoice(red);
        panel.toggleColorChoice(red);
        panel.toggleColorChoice(red);
        panel.toggleColorChoice(red);
        panel.toggleColorChoice(blue);
        assertEquals(Arrays.asList(red, red.withoutBackground(), red, red.withoutBackground(), blue), applied);
    }

    @Test
    void shouldGiveItsBackgroundToAnElementShownWithItsBorderOnly()
    {
        ColorToolsBarPanel panel = new ColorToolsBarPanel();
        panel.setShownColorChoice(ColorChoice.PASTEL_RED.withoutBackground());
        panel.toggleColorChoice(ColorChoice.PASTEL_RED);
        assertEquals(ColorChoice.PASTEL_RED, panel.getShownColorChoice());
    }

}
