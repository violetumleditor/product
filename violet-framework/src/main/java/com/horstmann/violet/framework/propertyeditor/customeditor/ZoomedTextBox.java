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
import java.awt.geom.Rectangle2D;
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
     * @param x left of the text on the graph
     * @param y top of the text on the graph
     * @param width of the text, in the units of the graph
     * @param height of the text, in the units of the graph
     * @param newZoom of the diagram
     */
    public void place(double x, double y, int width, int height, double newZoom) {
        this.zoom = newZoom;
        this.pane.setBounds(0, 0, width + 2 * MARGIN, height + 2 * MARGIN);
        this.pane.doLayout();
        Rectangle2D exact = new Rectangle2D.Double((x - MARGIN) * newZoom, (y - MARGIN) * newZoom,
                this.pane.getWidth() * newZoom, this.pane.getHeight() * newZoom);
        Rectangle bounds = exact.getBounds();
        // A place on the graph may fall between two pixels
        this.offsetX = exact.getX() - bounds.x;
        this.offsetY = exact.getY() - bounds.y;
        setBounds(bounds);
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(this.offsetX, this.offsetY);
            g2.scale(this.zoom, this.zoom);
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
        return new Rectangle2D.Double(this.offsetX + rectangle.x * this.zoom, this.offsetY + rectangle.y * this.zoom,
                rectangle.width * this.zoom, rectangle.height * this.zoom).getBounds();
    }

    private void forward(MouseEvent e) {
        int x = (int) Math.round((e.getX() - this.offsetX) / this.zoom);
        int y = (int) Math.round((e.getY() - this.offsetY) / this.zoom);
        this.pane.dispatchEvent(new MouseEvent(this.pane, e.getID(), e.getWhen(), e.getModifiersEx(), x, y, e.getClickCount(),
                e.isPopupTrigger(), e.getButton()));
    }

    private final DiagramTextPane pane;

    private double zoom = 1;

    private double offsetX;

    private double offsetY;

    /** Between the text and the outline, also the room of the caret at the ends of a line */
    private static final int MARGIN = 3;

    /** So that the rounding of the widths never breaks a line */
    private static final int CARET_ROOM = 2;

    private static final Color OUTLINE_COLOR = new Color(124, 92, 220);
}
