package com.horstmann.violet.framework.swingextension;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.Icon;

/**
 * Outline icon drawn from a shape defined on a 24x24 grid, so it stays sharp on any display.
 */
public class ShapeIcon implements Icon
{

    /**
     * Creates an icon which follows the foreground color of its component
     *
     * @param shape on a 24x24 grid
     * @param size in pixels
     */
    public ShapeIcon(Shape shape, int size)
    {
        this(shape, size, null);
    }

    /**
     * @param shape on a 24x24 grid
     * @param size in pixels
     * @param color outline color
     */
    public ShapeIcon(Shape shape, int size, Color color)
    {
        this(shape, size, color, false);
    }

    /**
     * @param shape on a 24x24 grid
     * @param size in pixels
     * @param color icon color
     * @param isFilled true to fill the shape, false to draw its outline
     */
    public ShapeIcon(Shape shape, int size, Color color, boolean isFilled)
    {
        this.shape = shape;
        this.size = size;
        this.color = color;
        this.isFilled = isFilled;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(this.color != null ? this.color : c.getForeground());
        g2.translate(x, y);
        g2.scale(this.size / GRID_SIZE, this.size / GRID_SIZE);
        if (this.isFilled)
        {
            g2.fill(this.shape);
        }
        else
        {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(this.shape);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth()
    {
        return this.size;
    }

    @Override
    public int getIconHeight()
    {
        return this.size;
    }

    private static final double GRID_SIZE = 24.0;

    private final Shape shape;
    private final int size;
    private final Color color;
    private final boolean isFilled;

}
