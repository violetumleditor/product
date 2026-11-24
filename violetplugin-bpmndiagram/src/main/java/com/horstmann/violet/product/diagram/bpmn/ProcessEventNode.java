package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.EllipticalNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A BPMN event: a circle of standard size, whose outline tells when it happens in the process (start, end), with a marker
 * telling its type (what triggers a start event, what an end event produces) and its name below it.
 */
public abstract class ProcessEventNode extends EllipticalNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), DIAMETER, DIAMETER);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        return new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public Rectangle2D getOutsideLabelBounds()
    {
        return DRAWING.getLabelAreaBelow(getName(), getBounds());
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        float lineWidth = DRAWING.getLineWidth(g2);
        Rectangle2D bounds = getBounds();
        Ellipse2D circle = new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g2.setColor(getBackgroundColor());
        g2.fill(circle);
        g2.setColor(getBorderColor());
        drawOutline(g2, circle, lineWidth);
        g2.setStroke(oldStroke);
        double inset = bounds.getWidth() * MARKER_INSET_RATIO;
        Rectangle2D markerArea = new Rectangle2D.Double(bounds.getX() + inset, bounds.getY() + inset, bounds.getWidth() - 2
                * inset, bounds.getHeight() - 2 * inset);
        g2.setColor(getTextColor());
        drawMarker(g2, markerArea, lineWidth);
        DRAWING.drawLabelBelow(g2, getName(), bounds, getTextColor());
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws the outline of the circle, which tells the kind of event
     *
     * @param g2 graphics, with the border color and the outline stroke set
     * @param circle the circle of the event
     * @param lineWidth the width of the outline stroke
     */
    protected abstract void drawOutline(Graphics2D g2, Ellipse2D circle, float lineWidth);

    /**
     * Draws the marker of the type of event
     *
     * @param g2 graphics, its color set to the text color
     * @param area the square inside the circle
     * @param lineWidth the width of the outline stroke
     */
    protected abstract void drawMarker(Graphics2D g2, Rectangle2D area, float lineWidth);

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the event
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the event, drawn below it
     */
    public MultiLineString getName()
    {
        if (this.name == null)
        {
            this.name = new MultiLineString();
        }
        return this.name;
    }

    @Override
    public ProcessEventNode clone()
    {
        ProcessEventNode cloned = (ProcessEventNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    private static final ProcessDrawing DRAWING = new ProcessDrawing();

    private static final int DIAMETER = 36;

    private static final double MARKER_INSET_RATIO = 0.22;

}
