package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.bpmn.type.EventTriggerType;
import com.horstmann.violet.product.diagram.bpmn.type.FlowType;

/**
 * A BPMN sequence flow: the order of the steps inside a pool, a solid line ending with a filled arrow. Its middle label holds its
 * condition, out of a gateway. A timer flow (a scheduled step) shows a clock where it starts, as a timer event
 * put on the flow, out of the way of its middle label.
 */
public class ProcessSequenceFlowEdge extends SegmentedLineEdge
{

    @Override
    public LineStyle getLineStyle()
    {
        return LineStyle.SOLID;
    }

    @Override
    public ArrowHead getStartArrowHead()
    {
        return ArrowHead.NONE;
    }

    /**
     * Automatic flows prefer a single bend (HV or VH), see {@link ProcessRouteChooser}
     */
    @Override
    public Direction getDirection(INode node)
    {
        Direction direction = new ProcessRouteChooser(this).getDirection(node);
        return direction != null ? direction : super.getDirection(node);
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
        if (!FlowType.TIMER.equals(getFlowType()))
        {
            return;
        }
        List<Point2D> points = getPoints();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(getBorderColor());
        g2.setStroke(getLineStyle().getStroke(getBorderWidth()));
        double size = CLOCK_SIZE * Math.max(1, getBorderWidth() * 0.75);
        Point2D center = getPointAlong(points, size / 2 + CLOCK_GAP);
        Rectangle2D area = new Rectangle2D.Double(center.getX() - size / 2, center.getY() - size / 2, size, size);
        EventTriggerType.TIMER.drawMarker(g2, area, Color.WHITE, getBorderWidth());
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * @param points the points of the line, at least two
     * @param distance from the start of the line
     * @return the point at this distance along the line, or halfway along a shorter line
     */
    private Point2D getPointAlong(List<Point2D> points, double distance)
    {
        double length = 0;
        for (int i = 1; i < points.size(); i++)
        {
            length += points.get(i - 1).distance(points.get(i));
        }
        double remaining = Math.min(distance, length / 2);
        for (int i = 1; i < points.size(); i++)
        {
            Point2D from = points.get(i - 1);
            Point2D to = points.get(i);
            double segment = from.distance(to);
            if (segment > 0 && remaining <= segment)
            {
                double ratio = remaining / segment;
                return new Point2D.Double(from.getX() + (to.getX() - from.getX()) * ratio, from.getY() + (to.getY() - from.getY())
                        * ratio);
            }
            remaining -= segment;
        }
        return points.get(points.size() - 1);
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

    /**
     * Side of the square holding the clock of a timer flow
     */
    private static final double CLOCK_SIZE = 30;

    /**
     * Space between the start of a timer flow and its clock
     */
    private static final double CLOCK_GAP = 4;

}
