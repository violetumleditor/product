package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A BPMN sequence flow: the order of the steps inside a pool, a solid line ending with a filled arrow. Its middle label holds its
 * condition, out of a gateway.
 */
public class BpmnSequenceFlowEdge extends SegmentedLineEdge
{

    @Override
    public LineStyle getLineStyle()
    {
        return LineStyle.SOLID;
    }

    @Override
    public ArrowHead getStartArrowHead()
    {
        return FlowType.CONDITIONAL.equals(getFlowType()) ? ArrowHead.DIAMOND : ArrowHead.NONE;
    }

    @Override
    public ArrowHead getEndArrowHead()
    {
        return ArrowHead.BLACK_TRIANGLE;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        if (!FlowType.DEFAULT.equals(getFlowType()))
        {
            return;
        }
        List<Point2D> points = getPoints();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(getBorderColor());
        g2.setStroke(getLineStyle().getStroke(getBorderWidth()));
        getFlowType().drawDefaultMarker(g2, points.get(0), points.get(1), getBorderWidth());
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Sets the flowType property value.
     *
     * @param newValue the kind of flow
     */
    public void setFlowType(FlowType newValue)
    {
        this.flowType = newValue;
    }

    /**
     * Gets the flowType property value.
     *
     * @return the kind of flow, normal by default
     */
    public FlowType getFlowType()
    {
        if (this.flowType == null)
        {
            this.flowType = FlowType.NORMAL;
        }
        return this.flowType;
    }

    private FlowType flowType;

}
