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
 * A BPMN event: a circle of standard size, whose outline tells when it happens in the process (start, intermediate, end), with
 * a marker telling what triggers it and its name below it.
 */
public abstract class BpmnEventNode extends EllipticalNode
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
        getTrigger().drawMarker(g2, markerArea, isThrowing(), getBackgroundColor(), lineWidth);
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
     * @return true for an event that throws its trigger (end event), whose marker is filled, false for one that catches it
     */
    protected boolean isThrowing()
    {
        return false;
    }

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

    /**
     * Sets the trigger property value.
     *
     * @param newValue what triggers the event
     */
    public void setTrigger(EventTrigger newValue)
    {
        this.trigger = newValue;
    }

    /**
     * Gets the trigger property value.
     *
     * @return what triggers the event, none by default
     */
    public EventTrigger getTrigger()
    {
        if (this.trigger == null)
        {
            this.trigger = EventTrigger.NONE;
        }
        return this.trigger;
    }

    @Override
    public BpmnEventNode clone()
    {
        BpmnEventNode cloned = (BpmnEventNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    private EventTrigger trigger;

    private static final BpmnDrawing DRAWING = new BpmnDrawing();

    private static final int DIAMETER = 36;

    private static final double MARKER_INSET_RATIO = 0.22;

}
