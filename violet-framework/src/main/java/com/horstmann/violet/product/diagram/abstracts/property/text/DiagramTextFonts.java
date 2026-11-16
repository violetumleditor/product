package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Font;
import java.io.InputStream;

/**
 * The fonts of the texts drawn on the diagrams. They are bundled with the application instead of taken from the system or the
 * look and feel, so that a text takes the same room and the same shape on every computer and in the browser.
 */
final class DiagramTextFonts
{
    private DiagramTextFonts()
    {
    }

    /**
     * @return the fonts, loaded once for all the texts
     */
    static DiagramTextFonts getInstance()
    {
        return INSTANCE;
    }

    /**
     * @param bold true for the bold face
     * @param italic true for the italic face
     * @param size in points
     * @return the bundled font
     */
    Font getFont(boolean bold, boolean italic, float size)
    {
        return getFace(getStyle(bold, italic)).deriveFont(size);
    }

    /**
     * @param bold true for the bold face
     * @param italic true for the italic face
     * @param size in points
     * @return the system font to use for the characters the bundled font can't display
     */
    Font getFallbackFont(boolean bold, boolean italic, float size)
    {
        return getSystemFace(getStyle(bold, italic)).deriveFont(size);
    }

    private int getStyle(boolean bold, boolean italic)
    {
        return (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);
    }

    private synchronized Font getFace(int style)
    {
        if (this.faces[style] == null)
        {
            this.faces[style] = loadFace(style);
        }
        return this.faces[style];
    }

    private Font loadFace(int style)
    {
        try (InputStream in = getClass().getResourceAsStream(FACE_RESOURCES[style]))
        {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        }
        catch (Exception | LinkageError e)
        {
            // Better a text in another font than no text at all
            System.err.println("Unable to load the font " + FACE_RESOURCES[style] + ": " + e);
            return getSystemFace(style);
        }
    }

    private Font getSystemFace(int style)
    {
        return new Font(Font.SANS_SERIF, style, 1);
    }

    /** Indexed by font style: plain, bold, italic, bold and italic */
    private static final String[] FACE_RESOURCES = { "/fonts/Roboto-Regular.ttf", "/fonts/Roboto-Bold.ttf",
            "/fonts/Roboto-Italic.ttf", "/fonts/Roboto-BoldItalic.ttf" };

    /** After the resources, which its fields need */
    private static final DiagramTextFonts INSTANCE = new DiagramTextFonts();

    private final Font[] faces = new Font[FACE_RESOURCES.length];
}
