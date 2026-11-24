package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A BPMN message flow: a message exchanged by two pools, a dashed line starting with a small circle and ending with an open
 * arrow.
 */
public class ProcessMessageFlowEdge extends SegmentedLineEdge
{

    @Override
    public LineStyle getLineStyle()
    {
        return LineStyle.DASHED;
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
        return ArrowHead.TRIANGLE;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        List<Point2D> points = getPoints();
        Point2D start = points.get(0);
        Point2D next = points.get(1);
        double length = start.distance(next);
        if (length < 1)
        {
            return;
        }
        double radius = CIRCLE_RADIUS + getBorderWidth();
        double cx = start.getX() + (next.getX() - start.getX()) / length * radius;
        double cy = start.getY() + (next.getY() - start.getY()) / length * radius;
        Ellipse2D circle = new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius);
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        // White inside, as the open arrow heads
        g2.setColor(Color.WHITE);
        g2.fill(circle);
        g2.setColor(getBorderColor());
        g2.setStroke(LineStyle.SOLID.getStroke(getBorderWidth()));
        g2.draw(circle);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private static final double CIRCLE_RADIUS = 3;

}
