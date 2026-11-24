package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fonts of the texts drawn on the diagrams. They are bundled with the application instead of taken from the system or the
 * look and feel, so that a text takes the same room and the same shape on every computer and in the browser. The text editor
 * shows them too, so that a text looks there as on the diagram.
 */
public final class DiagramTextFonts
{
    private DiagramTextFonts()
    {
    }

    /**
     * @return the fonts, loaded once for all the texts
     */
    public static DiagramTextFonts getInstance()
    {
        return INSTANCE;
    }

    /**
     * @param font one of the bundled fonts
     * @param bold true for the bold face
     * @param italic true for the italic face
     * @param size in points
     * @return the bundled font. Its size is the one asked for, even when its characters are enlarged
     */
    public Font getFont(DiagramFont font, boolean bold, boolean italic, float size)
    {
        return getFace(font, getStyle(bold, italic)).deriveFont(size);
    }

    /**
     * @param font one of the bundled fonts
     * @param bold true for a bold text
     * @param italic true for an italic text
     * @param size in points
     * @param text of one style
     * @return its characters, in the font, else in the standard one or in a system font for those it doesn't have
     */
    public List<GlyphVector> createGlyphs(DiagramFont font, boolean bold, boolean italic, float size, String text)
    {
        Font[] candidates = { getFont(font, bold, italic, size), getFont(DiagramFont.STANDARD, bold, italic, size),
                getSystemFace(getStyle(bold, italic)).deriveFont(size) };
        List<GlyphVector> result = new ArrayList<GlyphVector>();
        int start = 0;
        while (start < text.length())
        {
            Font partFont = findFont(candidates, text.codePointAt(start));
            int end = start;
            while (end < text.length() && findFont(candidates, text.codePointAt(end)) == partFont)
            {
                end += Character.charCount(text.codePointAt(end));
            }
            result.add(partFont.createGlyphVector(getMeasuringContext(), text.substring(start, end)));
            start = end;
        }
        return result;
    }

    /**
     * @param glyphs of a piece of text
     * @return its width: where the next character starts, as the logical bounds of slanted characters are larger
     */
    public double getWidth(GlyphVector glyphs)
    {
        return glyphs.getGlyphPosition(glyphs.getNumGlyphs()).getX();
    }

    /**
     * @param size of a line of text
     * @return the distance from the top of the line to its baseline: the one of the standard font, whatever the fonts of the
     *         line, centered in the line
     */
    public double getBaseline(float size)
    {
        LineMetrics metrics = getFont(DiagramFont.STANDARD, false, false, size).getLineMetrics("", getMeasuringContext());
        double textHeight = metrics.getAscent() + metrics.getDescent();
        return Math.round((getLineHeight(size) - textHeight) / 2 + metrics.getAscent());
    }

    /**
     * @param size of a line of text
     * @return the height of the line
     */
    public double getLineHeight(float size)
    {
        return Math.ceil(size * StyledTextLayout.LINE_HEIGHT);
    }

    private Font findFont(Font[] candidates, int character)
    {
        for (Font candidate : candidates)
        {
            if (candidate.canDisplay(character))
            {
                return candidate;
            }
        }
        return candidates[candidates.length - 1];
    }

    private int getStyle(boolean bold, boolean italic)
    {
        return (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);
    }

    private synchronized Font getFace(DiagramFont font, int style)
    {
        String key = font.name() + style;
        Font face = getFaces().get(key);
        if (face == null)
        {
            face = loadFace(font, style);
            getFaces().put(key, face);
        }
        return face;
    }

    private Font loadFace(DiagramFont font, int style)
    {
        boolean isItalic = (style & Font.ITALIC) != 0;
        String resource = font.getResource((style & Font.BOLD) != 0, isItalic);
        Font face;
        try (InputStream in = getClass().getResourceAsStream(resource))
        {
            face = Font.createFont(Font.TRUETYPE_FONT, in);
        }
        catch (Exception | LinkageError e)
        {
            // Better a text in another font than no text at all
            System.err.println("Unable to load the font " + resource + ": " + e);
            return getSystemFace(style);
        }
        double slant = isItalic && !font.hasItalicFaces() ? ITALIC_SLANT : 0;
        if (font.getScale() == 1 && slant == 0)
        {
            return face;
        }
        // A transform leaves the size of the font as it is, so the height of the lines too
        return face.deriveFont(new AffineTransform(font.getScale(), 0, -slant * font.getScale(), font.getScale(), 0, 0));
    }

    private Font getSystemFace(int style)
    {
        return new Font(Font.SANS_SERIF, style, 1);
    }

    private Map<String, Font> getFaces()
    {
        if (this.faces == null)
        {
            this.faces = new HashMap<String, Font>();
        }
        return this.faces;
    }

    /**
     * @return how the characters are measured and outlined: always the same way, whatever the graphics they are drawn on
     */
    private synchronized FontRenderContext getMeasuringContext()
    {
        if (this.measuringContext == null)
        {
            this.measuringContext = new FontRenderContext(null, true, true);
        }
        return this.measuringContext;
    }

    /** Horizontal shift of a slanted character, for a height of 1 */
    private static final double ITALIC_SLANT = 0.21;

    private static final DiagramTextFonts INSTANCE = new DiagramTextFonts();

    private Map<String, Font> faces;
    private FontRenderContext measuringContext;
}
