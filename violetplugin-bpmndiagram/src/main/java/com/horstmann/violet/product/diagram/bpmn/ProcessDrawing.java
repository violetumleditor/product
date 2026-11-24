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
     * Draws the label of a small node (event, gateway, data) centered below it, as BPMN tools do: the node keeps its standard
     * size whatever its text
     *
     * @param g2 graphics
     * @param label the text
     * @param shapeBounds the bounds of the node
     * @param color the color of the text
     */
    void drawLabelBelow(Graphics2D g2, MultiLineString label, Rectangle2D shapeBounds, Color color)
    {
        Rectangle2D area = getLabelAreaBelow(label, shapeBounds);
        if (area != null)
        {
            drawLabel(g2, label, area, color);
        }
    }

    /**
     * @return where this label is drawn, or null if it is empty
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
     * Draws the label of a gateway above its right side, ending against its upper right edge: a flow often leaves its bottom
     * vertex, and a label below would hide it
     *
     * @param g2 graphics
     * @param label the text
     * @param shapeBounds the bounds of the gateway
     * @param color the color of the text
     */
    void drawLabelAboveRight(Graphics2D g2, MultiLineString label, Rectangle2D shapeBounds, Color color)
    {
        Rectangle2D area = getLabelAreaAboveRight(label, shapeBounds);
        if (area != null)
        {
            drawLabel(g2, label, area, color);
        }
    }

    /**
     * @return where this label is drawn, or null if it is empty
     */
    Rectangle2D getLabelAreaAboveRight(MultiLineString label, Rectangle2D shapeBounds)
    {
        Rectangle2D textBounds = label.getBounds();
        if (textBounds.getWidth() <= 0)
        {
            return null;
        }
        return new Rectangle2D.Double(shapeBounds.getCenterX() + shapeBounds.getWidth() / 4, shapeBounds.getY()
                + shapeBounds.getHeight() / 4 - textBounds.getHeight(), textBounds.getWidth(), textBounds.getHeight());
    }

    private void drawLabel(Graphics2D g2, MultiLineString label, Rectangle2D area, Color color)
    {
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
