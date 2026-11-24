package com.horstmann.violet.product.diagram.bpmn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * Drawing shared by the BPMN nodes
 */
final class ProcessDrawing
{

    /**
     * @param g2 graphics whose stroke is the outline of the node: the graph sets it to the node's thickness, and the tool bar to
     *            one pixel whatever the scale of its icon
     * @return the width of the outline
     */
    float getLineWidth(Graphics2D g2)
    {
        Stroke stroke = g2.getStroke();
        if (stroke instanceof BasicStroke)
        {
            return ((BasicStroke) stroke).getLineWidth();
        }
        return 1;
    }

    /**
     * The label of an event is centered below it, as BPMN tools do: the node keeps its standard size whatever its text
     *
     * @return where this label is drawn unless the user moved it, or null if it is empty
     */
    Rectangle2D getLabelAreaBelow(MultiLineString label, Rectangle2D shapeBounds)
    {
        Rectangle2D textBounds = label.getBounds();
        if (textBounds.getWidth() <= 0)
        {
            return null;
        }
        return new Rectangle2D.Double(shapeBounds.getCenterX() - textBounds.getWidth() / 2, shapeBounds.getMaxY()
                + LABEL_GAP, textBounds.getWidth(), textBounds.getHeight());
    }

    /**
     * Draws the label of a small node outside it
     *
     * @param g2 graphics
     * @param label the text
     * @param area where it is drawn, or null if it is empty
     * @param color the color of the text
     */
    void drawLabel(Graphics2D g2, MultiLineString label, Rectangle2D area, Color color)
    {
        if (area == null)
        {
            return;
        }
        Color oldColor = g2.getColor();
        g2.setColor(color);
        label.draw(g2, area);
        g2.setColor(oldColor);
    }

    /**
     * Draws a text turned a quarter left, bottom to top, centered in a vertical band (title of a pool or a lane)
     *
     * @param g2 graphics, its color set
     * @param text the text
     * @param band the band
     */
    void drawVerticalText(Graphics2D g2, MultiLineString text, Rectangle2D band)
    {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(band.getCenterX(), band.getCenterY());
        g2.rotate(-Math.PI / 2);
        text.draw(g2, new Rectangle2D.Double(-band.getHeight() / 2, -band.getWidth() / 2, band.getHeight(), band.getWidth()));
        g2.setTransform(oldTransform);
    }

    private static final int LABEL_GAP = 4;

}
