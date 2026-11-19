package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;

/**
 * Where a process starts: a thin circle. Nothing flows into it (see {@link BpmnDiagramGraph#connect}) and it sends no message.
 */
public class BpmnStartEventNode extends BpmnEventNode
{

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && e.getEndNode() != this && !(e instanceof BpmnMessageFlowEdge);
    }

    @Override
    protected void drawOutline(Graphics2D g2, Ellipse2D circle, float lineWidth)
    {
        g2.draw(circle);
    }

    @Override
    public BpmnStartEventNode clone()
    {
        return (BpmnStartEventNode) super.clone();
    }

}
