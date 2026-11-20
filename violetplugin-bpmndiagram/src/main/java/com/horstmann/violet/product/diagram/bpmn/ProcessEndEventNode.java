package com.horstmann.violet.product.diagram.bpmn;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.bpmn.type.EventResultType;

/**
 * Where a path of the process ends: a thick circle. No sequence flow leaves it, and its marker is filled, as it throws its
 * result (message sent, error...).
 */
public class ProcessEndEventNode extends ProcessEventNode
{

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && e.getEndNode() != this && !(e instanceof ProcessSequenceFlowEdge);
    }

    @Override
    protected void drawOutline(Graphics2D g2, Ellipse2D circle, float lineWidth)
    {
        float thickWidth = lineWidth * THICKNESS_RATIO;
        double inset = thickWidth / 2;
        g2.setStroke(new BasicStroke(thickWidth));
        g2.draw(new Ellipse2D.Double(circle.getX() + inset, circle.getY() + inset, circle.getWidth() - 2 * inset, circle
                .getHeight() - 2 * inset));
    }

    @Override
    protected void drawMarker(Graphics2D g2, Rectangle2D area, float lineWidth)
    {
        getResult().drawMarker(g2, area, true, getBackgroundColor(), lineWidth);
    }

    /**
     * Sets the result property value.
     *
     * @param newValue what the event produces
     */
    public void setResult(EventResultType newValue)
    {
        this.result = newValue;
    }

    /**
     * Gets the result property value.
     *
     * @return what the event produces, none by default
     */
    public EventResultType getResult()
    {
        if (this.result == null)
        {
            this.result = EventResultType.NONE;
        }
        return this.result;
    }

    @Override
    public ProcessEndEventNode clone()
    {
        return (ProcessEndEventNode) super.clone();
    }

    private EventResultType result;

    private static final float THICKNESS_RATIO = 3;

}
