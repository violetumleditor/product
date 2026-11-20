package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.bpmn.type.EventTriggerType;

/**
 * Where a process starts: a thin circle. Nothing flows into it (see {@link ProcessDiagramGraph#connect}) and it sends no message.
 */
public class ProcessStartEventNode extends ProcessEventNode
{

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && e.getEndNode() != this && !(e instanceof ProcessMessageFlowEdge);
    }

    @Override
    protected void drawOutline(Graphics2D g2, Ellipse2D circle, float lineWidth)
    {
        g2.draw(circle);
    }

    @Override
    protected void drawMarker(Graphics2D g2, Rectangle2D area, float lineWidth)
    {
        getTrigger().drawMarker(g2, area, getBackgroundColor(), lineWidth);
    }

    /**
     * Sets the trigger property value.
     *
     * @param newValue what triggers the event
     */
    public void setTrigger(EventTriggerType newValue)
    {
        this.trigger = newValue;
    }

    /**
     * Gets the trigger property value.
     *
     * @return what triggers the event, none by default
     */
    public EventTriggerType getTrigger()
    {
        if (this.trigger == null)
        {
            this.trigger = EventTriggerType.NONE;
        }
        return this.trigger;
    }

    @Override
    public ProcessStartEventNode clone()
    {
        return (ProcessStartEventNode) super.clone();
    }

    private EventTriggerType trigger;

}
