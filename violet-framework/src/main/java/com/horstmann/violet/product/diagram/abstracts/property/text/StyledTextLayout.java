package com.horstmann.violet.product.diagram.abstracts.property.text;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
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
     * @param lineJustifications the alignment of each line, null for a line that has none of its own
     * @param defaultSize the font size of the pieces that have none
     * @param isUnderlined true to underline the whole text
     */
    public StyledTextLayout(List<List<TextRun>> textLines, List<Integer> lineJustifications, float defaultSize,
            boolean isUnderlined)
    {
        this.textLines = textLines;
        this.lineJustifications = lineJustifications;
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
     * @param defaultJustification of the lines that have none of their own, one of {@link MultiLineString.Justification}
     */
    public void draw(Graphics2D g2, Rectangle2D r, int defaultJustification)
    {
        Color defaultColor = g2.getColor();
        Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Whole pixels, so that the underlines stay sharp
        double y = r.getY() + Math.round((r.getHeight() - getBounds().getHeight()) / 2);
        for (Line line : getLines())
        {
            int justification = line.justification() == null ? defaultJustification : line.justification();
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
            for (int i = 0; i < this.textLines.size(); i++)
            {
                this.lines.add(createLine(this.textLines.get(i), this.lineJustifications.get(i)));
            }
        }
        return this.lines;
    }

    /**
     * @param runs the pieces of text of a line
     * @param justification of the line, null if it has none of its own
     * @return their shapes, placed from the start of the line (x = 0) on its baseline (y = 0)
     */
    private Line createLine(List<TextRun> runs, Integer justification)
    {
        float lineSize = runs.isEmpty() ? this.defaultSize : 0;
        for (TextRun run : runs)
        {
            lineSize = Math.max(lineSize, getSize(run));
        }
        double height = getFonts().getLineHeight(lineSize);
        double baseline = getFonts().getBaseline(lineSize);

        List<Piece> pieces = new ArrayList<Piece>();
        double x = 0;
        for (TextRun run : runs)
        {
            TextStyle style = run.style();
            float size = getSize(run);
            double runStart = x;
            for (GlyphVector glyphs : getFonts().createGlyphs(style.font(), style.bold(), style.italic(), size, run.text()))
            {
                pieces.add(new Piece(glyphs.getOutline((float) x, 0), style.color()));
                x += getFonts().getWidth(glyphs);
            }
            if (style.underlined() || this.isUnderlined)
            {
                double offset = Math.max(1, Math.round(size * UNDERLINE_OFFSET));
                double thickness = Math.max(1, Math.round(size * UNDERLINE_THICKNESS));
                pieces.add(new Piece(new Rectangle2D.Double(runStart, offset, x - runStart, thickness), style.color()));
            }
        }
        return new Line(pieces, x, height, baseline, justification);
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
     * A line of text ready to be drawn.
     *
     * @param pieces its shapes, from the start of the line (x = 0), on its baseline (y = 0)
     * @param width of the line
     * @param height of the line
     * @param baseline distance from the top of the line to its baseline
     * @param justification of the line, null if it has none of its own
     */
    private record Line(List<Piece> pieces, double width, double height, double baseline, Integer justification)
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

    /** Height of a line, for a font size of 1: the line spacing the diagrams had so far (also the one of the text editor) */
    public static final double LINE_HEIGHT = 4.0 / 3.0;
    /** Distance from the baseline to the top of the underline, for a font size of 1 */
    private static final double UNDERLINE_OFFSET = 0.11;
    /** Thickness of the underline, for a font size of 1 */
    private static final double UNDERLINE_THICKNESS = 0.07;

    private final List<List<TextRun>> textLines;
    private final List<Integer> lineJustifications;
    private final float defaultSize;
    private final boolean isUnderlined;
    private List<Line> lines;
}
