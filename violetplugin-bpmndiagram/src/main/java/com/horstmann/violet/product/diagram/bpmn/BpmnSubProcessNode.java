package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * A BPMN collapsed sub-process: an activity detailed elsewhere (in another diagram), marked by a "+" at the bottom.
 */
public class BpmnSubProcessNode extends BpmnActivityNode
{

    @Override
    protected void drawMarkers(Graphics2D g2, Rectangle2D bounds, float lineWidth)
    {
        Rectangle2D box = new Rectangle2D.Double(bounds.getCenterX() - MARKER_SIZE / 2.0, bounds.getMaxY() - MARKER_SIZE
                - MARKER_MARGIN, MARKER_SIZE, MARKER_SIZE);
        g2.draw(box);
        double inset = MARKER_SIZE * 0.25;
        g2.draw(new Line2D.Double(box.getX() + inset, box.getCenterY(), box.getMaxX() - inset, box.getCenterY()));
        g2.draw(new Line2D.Double(box.getCenterX(), box.getY() + inset, box.getCenterX(), box.getMaxY() - inset));
    }

    @Override
    public BpmnSubProcessNode clone()
    {
        return (BpmnSubProcessNode) super.clone();
    }

    private static final int MARKER_SIZE = 14;
    private static final int MARKER_MARGIN = 4;

}
