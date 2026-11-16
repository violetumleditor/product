package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * The drawing of a {@link MultiLineString}: its lines of styled text turned once into shapes (the outlines of the characters),
 * then filled like any other shape of the diagram.
 * <p>
 * Nothing here comes from Swing, the look and feel, the system fonts or the graphics drawn on: the font is bundled, the
 * characters are measured out of any graphics and the line heights are computed. So a text has the same size and the same
 * shape on every computer, in the browser, at any zoom and in the exports.
 */
public class StyledTextLayout
{
    /**
     * @param textLines the lines of the text, each as pieces of one style
     * @param defaultSize the font size of the pieces that have none
     * @param isUnderlined true to underline the whole text
     */
    public StyledTextLayout(List<List<TextRun>> textLines, float defaultSize, boolean isUnderlined)
    {
        this.textLines = textLines;
        this.defaultSize = defaultSize;
        this.isUnderlined = isUnderlined;
    }

    /**
     * @return the room the text takes, in whole pixels, with its top left corner at (0,0); empty for a text without character
     */
    public Rectangle2D getBounds()
    {
        double width = 0;
        double height = 0;
        boolean hasText = false;
        for (Line line : getLines())
        {
            width = Math.max(width, line.width());
            height += line.height();
            hasText = hasText || !line.pieces().isEmpty();
        }
        return hasText ? new Rectangle2D.Double(0, 0, Math.ceil(width), height) : new Rectangle2D.Double();
    }

    /**
     * Draws the text in a rectangle: centered vertically, each line aligned horizontally. The pieces without a color of their
     * own take the current color of the graphics.
     *
     * @param g2 the graphics context
     * @param r the rectangle
     * @param justification one of {@link MultiLineString.Justification}
     */
    public void draw(Graphics2D g2, Rectangle2D r, int justification)
    {
        Color defaultColor = g2.getColor();
        Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Whole pixels, so that the underlines stay sharp
        double y = r.getY() + Math.round((r.getHeight() - getBounds().getHeight()) / 2);
        for (Line line : getLines())
        {
            double x = r.getX();
            if (justification == MultiLineString.Justification.CENTER)
            {
                x += (r.getWidth() - line.width()) / 2;
            }
            else if (justification == MultiLineString.Justification.RIGHT)
            {
                x += r.getWidth() - line.width();
            }
            g2.translate(x, y + line.baseline());
            for (Piece piece : line.pieces())
            {
                g2.setColor(piece.color() == null ? defaultColor : piece.color());
                g2.fill(piece.shape());
            }
            g2.translate(-x, -(y + line.baseline()));
            y += line.height();
        }
        g2.setColor(defaultColor);
        if (antialiasing != null)
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        }
    }

    private List<Line> getLines()
    {
        if (this.lines == null)
        {
            this.lines = new ArrayList<Line>();
            for (List<TextRun> runs : this.textLines)
            {
                this.lines.add(createLine(runs));
            }
        }
        return this.lines;
    }

    /**
     * @param runs the pieces of text of a line
     * @return their shapes, placed from the start of the line (x = 0) on its baseline (y = 0)
     */
    private Line createLine(List<TextRun> runs)
    {
        float lineSize = runs.isEmpty() ? this.defaultSize : 0;
        for (TextRun run : runs)
        {
            lineSize = Math.max(lineSize, getSize(run));
        }
        double height = Math.ceil(lineSize * LINE_HEIGHT);
        LineMetrics metrics = getFonts().getFont(false, false, lineSize).getLineMetrics("", getMeasuringContext());
        double textHeight = metrics.getAscent() + metrics.getDescent();
        double baseline = Math.round((height - textHeight) / 2 + metrics.getAscent());

        List<Piece> pieces = new ArrayList<Piece>();
        double x = 0;
        for (TextRun run : runs)
        {
            TextStyle style = run.style();
            float size = getSize(run);
            double runStart = x;
            for (GlyphVector glyphs : createGlyphs(run.text(), style.bold(), style.italic(), size))
            {
                pieces.add(new Piece(glyphs.getOutline((float) x, 0), style.color()));
                x += glyphs.getLogicalBounds().getWidth();
            }
            if (style.underlined() || this.isUnderlined)
            {
                double offset = Math.max(1, Math.round(size * UNDERLINE_OFFSET));
                double thickness = Math.max(1, Math.round(size * UNDERLINE_THICKNESS));
                pieces.add(new Piece(new Rectangle2D.Double(runStart, offset, x - runStart, thickness), style.color()));
            }
        }
        return new Line(pieces, x, height, baseline);
    }

    /**
     * @param text of one style
     * @param isBold true for a bold text
     * @param isItalic true for an italic text
     * @param size of the font
     * @return its characters, in the bundled font, or in a system font for those this one doesn't have
     */
    private List<GlyphVector> createGlyphs(String text, boolean isBold, boolean isItalic, float size)
    {
        Font font = getFonts().getFont(isBold, isItalic, size);
        Font fallbackFont = null;
        List<GlyphVector> result = new ArrayList<GlyphVector>();
        int start = 0;
        while (start < text.length())
        {
            boolean isDisplayable = font.canDisplay(text.codePointAt(start));
            int end = start;
            while (end < text.length() && font.canDisplay(text.codePointAt(end)) == isDisplayable)
            {
                end += Character.charCount(text.codePointAt(end));
            }
            if (!isDisplayable && fallbackFont == null)
            {
                fallbackFont = getFonts().getFallbackFont(isBold, isItalic, size);
            }
            Font partFont = isDisplayable ? font : fallbackFont;
            result.add(partFont.createGlyphVector(getMeasuringContext(), text.substring(start, end)));
            start = end;
        }
        return result;
    }

    private float getSize(TextRun run)
    {
        return run.style().size() > 0 ? run.style().size() : this.defaultSize;
    }

    private DiagramTextFonts getFonts()
    {
        return DiagramTextFonts.getInstance();
    }

    /**
     * @return how the characters are measured and outlined: always the same way, whatever the graphics they are drawn on
     */
    private FontRenderContext getMeasuringContext()
    {
        if (this.measuringContext == null)
        {
            this.measuringContext = new FontRenderContext(null, true, true);
        }
        return this.measuringContext;
    }

    /**
     * A line of text ready to be drawn.
     *
     * @param pieces its shapes, from the start of the line (x = 0), on its baseline (y = 0)
     * @param width of the line
     * @param height of the line
     * @param baseline distance from the top of the line to its baseline
     */
    private record Line(List<Piece> pieces, double width, double height, double baseline)
    {
    }

    /**
     * Some characters of a line, or their underline.
     *
     * @param shape to fill
     * @param color to fill it with, null for the color of the graphics
     */
    private record Piece(Shape shape, Color color)
    {
    }

    /** Height of a line, for a font size of 1: the line spacing the diagrams had so far */
    private static final double LINE_HEIGHT = 4.0 / 3.0;
    /** Distance from the baseline to the top of the underline, for a font size of 1 */
    private static final double UNDERLINE_OFFSET = 0.11;
    /** Thickness of the underline, for a font size of 1 */
    private static final double UNDERLINE_THICKNESS = 0.07;

    private final List<List<TextRun>> textLines;
    private final float defaultSize;
    private final boolean isUnderlined;
    private List<Line> lines;
    private FontRenderContext measuringContext;
}
