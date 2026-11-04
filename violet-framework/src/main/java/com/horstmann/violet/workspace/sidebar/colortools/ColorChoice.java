package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A background + border color pair, and the palette offered to the user.
 */
public class ColorChoice
{

    // Source : http://www.tinygorilla.com/Easter_eggs/pallatehex.html
    // http://www.colorhexa.com/
    public static final ColorChoice DEFAULT = new ColorChoice(Color.WHITE, new Color(191,191,191));
    public static final ColorChoice PASTEL_GREY = new ColorChoice(new Color(242,242,242), new Color(91,91,91));
    public static final ColorChoice PASTEL_RED = new ColorChoice(new Color(251,199,183), new Color(246,132,98));
    public static final ColorChoice PASTEL_RED_ORANGE = new ColorChoice(new Color(252,212,190), new Color(248,157,105));
    public static final ColorChoice PASTEL_YELLOW_ORANGE = new ColorChoice(new Color(254,227,198), new Color(253,186,113));
    public static final ColorChoice PASTEL_YELLOW = new ColorChoice(new Color(255,252,212), new Color(255,222,154));
    public static final ColorChoice PASTEL_PEA_GREEN = new ColorChoice(new Color(224,238,203), new Color(185,217,136));
    public static final ColorChoice PASTEL_YELLOW_GREEN = new ColorChoice(new Color(204,231,201), new Color(145,203,138));
    public static final ColorChoice PASTEL_GREEN = new ColorChoice(new Color(179,223,196), new Color(112,194,143));
    public static final ColorChoice PASTEL_GREEN_CYAN = new ColorChoice(new Color(190,230,228), new Color(123,205,200));
    public static final ColorChoice PASTEL_CYAN = new ColorChoice(new Color(193,235,251), new Color(110,207,246));
    public static final ColorChoice PASTEL_CYAN_BLUE = new ColorChoice(new Color(196,214,237), new Color(126,167,216));
    public static final ColorChoice PASTEL_BLUE = new ColorChoice(new Color(196,203,230), new Color(132,147,202));
    public static final ColorChoice PASTEL_BLUE_VIOLET = new ColorChoice(new Color(195,191,222), new Color(136,130,190));
    public static final ColorChoice PASTEL_VIOLET = new ColorChoice(new Color(208,196,223), new Color(161,135,190));
    public static final ColorChoice PASTEL_VIOLET_MAGENTA = new ColorChoice(new Color(223,200,224), new Color(188,141,191));
    public static final ColorChoice PASTEL_MAGENTA = new ColorChoice(new Color(250,208,227), new Color(241,131,180));
    public static final ColorChoice PASTEL_MAGENTA_RED = new ColorChoice(new Color(251,207,210), new Color(244,129,135));
    public static final ColorChoice PASTEL_BLACK = new ColorChoice(new Color(201,201,201), new Color(0,0,0));

    /** Color choices offered in the side bar, in display order */
    public static final List<ColorChoice> PALETTE = Collections.unmodifiableList(Arrays.asList(
            DEFAULT,
            PASTEL_GREY,
            PASTEL_RED,
            PASTEL_RED_ORANGE,
            PASTEL_YELLOW_ORANGE,
            PASTEL_YELLOW,
            PASTEL_PEA_GREEN,
            PASTEL_YELLOW_GREEN,
            PASTEL_GREEN,
            PASTEL_GREEN_CYAN,
            PASTEL_CYAN,
            PASTEL_CYAN_BLUE,
            PASTEL_BLUE,
            PASTEL_BLUE_VIOLET,
            PASTEL_VIOLET,
            PASTEL_VIOLET_MAGENTA,
            PASTEL_MAGENTA,
            PASTEL_MAGENTA_RED,
            PASTEL_BLACK));

    public static final Color DEFAULT_TEXT_COLOR = PASTEL_GREY.getBorderColor();

    /** Text colors offered to the user : the border colors of the palette, in the same order */
    public static final List<Color> TEXT_COLOR_LIST = Collections.unmodifiableList(borderColorsOf(PALETTE));

    private final Color backgroundColor;

    private final Color borderColor;

    public ColorChoice(Color backgroundColor, Color borderColor)
    {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    public Color getBorderColor()
    {
        return borderColor;
    }

    private static List<Color> borderColorsOf(List<ColorChoice> choices)
    {
        List<Color> borderColors = new ArrayList<Color>();
        for (ColorChoice aChoice : choices)
        {
            borderColors.add(aChoice.getBorderColor());
        }
        return borderColors;
    }

}
