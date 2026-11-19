package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Something that happens during a process (a delay, a message received...): a double circle.
 */
public class BpmnIntermediateEventNode extends BpmnEventNode
{

    @Override
    protected void drawOutline(Graphics2D g2, Ellipse2D circle, float lineWidth)
    {
        g2.draw(circle);
        double gap = RING_GAP * lineWidth;
        g2.draw(new Ellipse2D.Double(circle.getX() + gap, circle.getY() + gap, circle.getWidth() - 2 * gap, circle.getHeight() - 2
                * gap));
    }

    @Override
    public BpmnIntermediateEventNode clone()
    {
        return (BpmnIntermediateEventNode) super.clone();
    }

    private static final double RING_GAP = 3;

}
