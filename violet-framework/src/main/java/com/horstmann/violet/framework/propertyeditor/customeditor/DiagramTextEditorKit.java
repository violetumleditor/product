package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.DiagramTextFonts;
import com.horstmann.violet.product.diagram.abstracts.property.text.StyledTextLayout;

/**
 * The HTML editor kit of the text of a {@link MultiLineString}: it shows the text as the diagram draws it. Same font (the
 * bundled one, not the one the HTML names), same sizes (a point is a pixel, as on a diagram at 100 %, where Swing would
 * otherwise make a pixel 1.3 point), same characters: measured and drawn as {@link StyledTextLayout} does, as filled outlines
 * with fractional widths. Swing would draw them hinted, with widths rounded to the pixels of the screen, a few percent
 * narrower on a scaled screen. Same line height and baseline too, whether lines are separated by a line break or by a
 * paragraph. The text written back is the same as with the standard kit.
 */
class DiagramTextEditorKit extends HTMLEditorKit {

    @Override
    public Document createDefaultDocument() {
        // As the standard kit does, with a style sheet of our own
        StyleSheet styles = new DiagramStyleSheet();
        styles.addStyleSheet(getStyleSheet());
        HTMLDocument document = new HTMLDocument(styles);
        document.setParser(getParser());
        document.setAsynchronousLoadPriority(4);
        document.setTokenThreshold(100);
        return document;
    }

    @Override
    public ViewFactory getViewFactory() {
        return getDiagramViewFactory();
    }

    private ViewFactory getDiagramViewFactory() {
        if (this.diagramViewFactory == null) {
            this.diagramViewFactory = new DiagramViewFactory();
        }
        return this.diagramViewFactory;
    }

    private transient ViewFactory diagramViewFactory;

    /**
     * Gives the bundled font of the diagrams, in the style and the size the HTML asks for.
     */
    private static class DiagramStyleSheet extends StyleSheet {
        @Override
        public Font getFont(AttributeSet attributes) {
            Font font = super.getFont(attributes);
            return DiagramTextFonts.getInstance().getFont(font.isBold(), font.isItalic(), font.getSize2D());
        }
    }

    /**
     * Creates the views of the standard kit, the pieces of text measured and drawn as on the diagrams.
     */
    private static class DiagramViewFactory extends HTMLFactory {
        @Override
        public View create(Element element) {
            View view = super.create(element);
            if (view instanceof GlyphView) {
                ((GlyphView) view).setGlyphPainter(getGlyphPainter());
            }
            return view;
        }

        private GlyphPainter getGlyphPainter() {
            if (this.glyphPainter == null) {
                this.glyphPainter = new DiagramGlyphPainter();
            }
            return this.glyphPainter;
        }

        private GlyphPainter glyphPainter;
    }

    /**
     * Measures and draws the characters of a piece of text as {@link StyledTextLayout} does: outlines of the glyphs, measured
     * out of any graphics, in a line 4/3 of the font size high, the baseline placed alike. It holds no state, so one serves all
     * the pieces.
     */
    private static class DiagramGlyphPainter extends GlyphPainter {

        @Override
        public float getSpan(GlyphView view, int p0, int p1, TabExpander expander, float x) {
            return (float) getWidth(view, p0, p1);
        }

        @Override
        public float getHeight(GlyphView view) {
            return (float) getLineHeight(view.getFont());
        }

        @Override
        public float getAscent(GlyphView view) {
            return (float) getBaseline(view.getFont());
        }

        @Override
        public float getDescent(GlyphView view) {
            Font font = view.getFont();
            return (float) (getLineHeight(font) - getBaseline(font));
        }

        @Override
        public void paint(GlyphView view, Graphics g, Shape allocation, int p0, int p1) {
            Rectangle bounds = allocation.getBounds();
            double x = bounds.getX() + getWidth(view, view.getStartOffset(), p0);
            double y = bounds.getY() + getBaseline(view.getFont());
            Graphics2D g2 = (Graphics2D) g;
            Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // In the color the view has set on the graphics: the text color, or the one of a selected text
            g2.fill(createGlyphs(view.getFont(), getText(view, p0, p1)).getOutline((float) x, (float) y));
            if (antialiasing != null) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
            }
        }

        @Override
        public Shape modelToView(GlyphView view, int position, Position.Bias bias, Shape allocation)
                throws BadLocationException {
            if (position < view.getStartOffset() || position > view.getEndOffset()) {
                throw new BadLocationException("Not in the view", position);
            }
            Rectangle bounds = allocation.getBounds();
            int x = bounds.x + (int) Math.round(getWidth(view, view.getStartOffset(), position));
            return new Rectangle(x, bounds.y, 0, bounds.height);
        }

        @Override
        public int viewToModel(GlyphView view, float x, float y, Shape allocation, Position.Bias[] biasReturn) {
            Rectangle bounds = allocation.getBounds();
            biasReturn[0] = Position.Bias.Forward;
            int start = view.getStartOffset();
            int end = view.getEndOffset();
            double previousWidth = 0;
            for (int position = start + 1; position <= end; position++) {
                double width = getWidth(view, start, position);
                // Nearest boundary between two characters
                if (x < bounds.x + (previousWidth + width) / 2) {
                    return position - 1;
                }
                previousWidth = width;
            }
            return end;
        }

        @Override
        public int getBoundedPosition(GlyphView view, int p0, float x, float length) {
            int end = view.getEndOffset();
            int position = p0;
            while (position < end && getWidth(view, p0, position + 1) <= length) {
                position++;
            }
            return position;
        }

        private double getWidth(GlyphView view, int p0, int p1) {
            if (p1 <= p0) {
                return 0;
            }
            return createGlyphs(view.getFont(), getText(view, p0, p1)).getLogicalBounds().getWidth();
        }

        private String getText(GlyphView view, int p0, int p1) {
            Segment text = view.getText(p0, p1);
            // A tab would have no glyph: the diagram has none either, the text pane shows it as a space
            return text.toString().replace('\t', ' ');
        }

        private GlyphVector createGlyphs(Font font, String text) {
            return font.createGlyphVector(getMeasuringContext(), text);
        }

        /**
         * @param font of a piece of text
         * @return the height of its line, as on the diagrams
         */
        private double getLineHeight(Font font) {
            return Math.ceil(font.getSize2D() * StyledTextLayout.LINE_HEIGHT);
        }

        /**
         * @param font of a piece of text
         * @return the distance from the top of its line to its baseline, as on the diagrams: the characters centered in the
         *         line
         */
        private double getBaseline(Font font) {
            LineMetrics metrics = font.getLineMetrics("", getMeasuringContext());
            double textHeight = metrics.getAscent() + metrics.getDescent();
            return Math.round((getLineHeight(font) - textHeight) / 2 + metrics.getAscent());
        }

        /**
         * @return how the characters are measured and outlined, as on the diagrams: whatever the graphics they are drawn on
         */
        private FontRenderContext getMeasuringContext() {
            if (this.measuringContext == null) {
                this.measuringContext = new FontRenderContext(null, true, true);
            }
            return this.measuringContext;
        }

        private FontRenderContext measuringContext;
    }
}
