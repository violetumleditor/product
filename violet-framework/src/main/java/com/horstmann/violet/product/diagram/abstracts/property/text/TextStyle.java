package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Color;

/**
 * The style of a piece of text of a {@link MultiLineString}, as read from its HTML.
 *
 * @param bold true for a bold text
 * @param italic true for an italic text
 * @param underlined true for an underlined text
 * @param size the font size, 0 for the default size of the string
 * @param color the text color, null for the color of the graphics the string is drawn on
 */
public record TextStyle(boolean bold, boolean italic, boolean underlined, float size, Color color)
{
    /** The style of a text without any formatting */
    public static final TextStyle PLAIN = new TextStyle(false, false, false, 0, null);

    public TextStyle withBold(boolean newValue)
    {
        return new TextStyle(newValue, italic, underlined, size, color);
    }

    public TextStyle withItalic(boolean newValue)
    {
        return new TextStyle(bold, newValue, underlined, size, color);
    }

    public TextStyle withUnderlined(boolean newValue)
    {
        return new TextStyle(bold, italic, newValue, size, color);
    }

    public TextStyle withSize(float newValue)
    {
        return new TextStyle(bold, italic, underlined, newValue, color);
    }

    public TextStyle withColor(Color newValue)
    {
        return new TextStyle(bold, italic, underlined, size, newValue);
    }
}
