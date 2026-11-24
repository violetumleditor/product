package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.text.View;

/**
 * Shows a text pane on the diagram, transparent, exactly as the diagram draws the same text: the pane is laid out in the units
 * of the graph, whatever the zoom, then painted through the same scale as the diagram. So its lines are broken and placed alike
 * at any zoom, which a bigger font wouldn't give (its sizes would be rounded to the pixels of the screen at each line). The
 * mouse events are taken here and given to the pane in its own coordinates.
 */
public class ZoomedTextBox extends JComponent {

    public ZoomedTextBox(DiagramTextPane pane) {
        this.pane = pane;
        pane.setOpaque(false);
        pane.setDoubleBuffered(false);
        pane.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        setLayout(null);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        add(pane);
        MouseAdapter mouseForwarder = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forward(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                forward(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                forward(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                forward(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                forward(e);
            }
        };
        addMouseListener(mouseForwarder);
        addMouseMotionListener(mouseForwarder);
    }

    /**
     * @return the room the text takes without breaking its lines, in the units of the graph, margins excluded
     */
    public Dimension getTextSize() {
        View rootView = this.pane.getUI().getRootView(this.pane);
        int width = (int) Math.ceil(rootView.getPreferredSpan(View.X_AXIS)) + CARET_ROOM;
        rootView.setSize(width, Integer.MAX_VALUE);
        int height = (int) Math.ceil(rootView.getPreferredSpan(View.Y_AXIS));
        return new Dimension(width, height);
    }

    /**
     * Places the text on the diagram.
     *
     * @param x left of the text
     * @param y top of the text
     * @param width of the text
     * @param height of the text
     * @param textToGraph from the place above to the graph: a translation, or a rotation too for a vertical text
     * @param zoom of the diagram
     */
    public void place(double x, double y, int width, int height, AffineTransform textToGraph, double zoom) {
        this.pane.setBounds(0, 0, width + 2 * MARGIN, height + 2 * MARGIN);
        this.pane.doLayout();
        AffineTransform paneToDiagram = AffineTransform.getScaleInstance(zoom, zoom);
        paneToDiagram.concatenate(textToGraph);
        paneToDiagram.translate(x - MARGIN, y - MARGIN);
        Rectangle bounds = paneToDiagram.createTransformedShape(this.pane.getBounds()).getBounds();
        // A place on the graph may fall between two pixels
        this.paneToBox = AffineTransform.getTranslateInstance(-bounds.x, -bounds.y);
        this.paneToBox.concatenate(paneToDiagram);
        setBounds(bounds);
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.transform(this.paneToBox);
            this.pane.paint(g2);
        } finally {
            g2.dispose();
        }
        g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(OUTLINE_COLOR);
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 4, 4));
        } finally {
            g2.dispose();
        }
    }

    /**
     * @param rectangle in the pane
     * @return the rectangle it is painted in
     */
    Rectangle toBox(Rectangle rectangle) {
        return this.paneToBox.createTransformedShape(rectangle).getBounds();
    }

    private void forward(MouseEvent e) {
        Point2D point;
        try {
            point = this.paneToBox.inverseTransform(e.getPoint(), null);
        } catch (NoninvertibleTransformException ex) {
            return;
        }
        int x = (int) Math.round(point.getX());
        int y = (int) Math.round(point.getY());
        this.pane.dispatchEvent(new MouseEvent(this.pane, e.getID(), e.getWhen(), e.getModifiersEx(), x, y, e.getClickCount(),
                e.isPopupTrigger(), e.getButton()));
    }

    private final DiagramTextPane pane;

    /** From the pane, laid out in the units of the graph, to the pixels of this box: the zoom, and the rotation if any */
    private AffineTransform paneToBox = new AffineTransform();

    /** Between the text and the outline, also the room of the caret at the ends of a line */
    private static final int MARGIN = 3;

    /** So that the rounding of the widths never breaks a line */
    private static final int CARET_ROOM = 2;

    private static final Color OUTLINE_COLOR = new Color(124, 92, 220);
}
