package com.horstmann.violet.product.diagram.bpmn;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;

/**
 * Where a path of the process ends: a thick circle. No sequence flow leaves it, and its marker is filled, as it throws its
 * trigger (message sent, error...).
 */
public class BpmnEndEventNode extends BpmnEventNode
{

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && e.getEndNode() != this && !(e instanceof BpmnSequenceFlowEdge);
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
    protected boolean isThrowing()
    {
        return true;
    }

    @Override
    public BpmnEndEventNode clone()
    {
        return (BpmnEndEventNode) super.clone();
    }

    private static final float THICKNESS_RATIO = 3;

}
