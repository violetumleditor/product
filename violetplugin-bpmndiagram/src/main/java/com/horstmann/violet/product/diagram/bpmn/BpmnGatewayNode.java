package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A BPMN gateway: a diamond of standard size where the flow splits or merges, its marker telling how (exclusive, parallel,
 * inclusive, on an event), its name (usually a question) above its right side. Flows leave and reach it by its vertices.
 */
public class BpmnGatewayNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), SIZE, SIZE);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        Rectangle2D b = getBounds();
        Direction d = e.getDirection(this).getNearestCardinalDirection();
        if (Direction.NORTH.equals(d))
        {
            return new Point2D.Double(b.getCenterX(), b.getMaxY());
        }
        if (Direction.SOUTH.equals(d))
        {
            return new Point2D.Double(b.getCenterX(), b.getMinY());
        }
        if (Direction.EAST.equals(d))
        {
            return new Point2D.Double(b.getMinX(), b.getCenterY());
        }
        if (Direction.WEST.equals(d))
        {
            return new Point2D.Double(b.getMaxX(), b.getCenterY());
        }
        return new Point2D.Double(b.getCenterX(), b.getCenterY());
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && e.getEndNode() != this;
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D b = getBounds();
        Path2D.Double diamond = new Path2D.Double();
        diamond.moveTo(b.getX(), b.getCenterY());
        diamond.lineTo(b.getCenterX(), b.getY());
        diamond.lineTo(b.getMaxX(), b.getCenterY());
        diamond.lineTo(b.getCenterX(), b.getMaxY());
        diamond.closePath();
        return diamond;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        Color oldColor = g2.getColor();
        Shape shape = getShape();
        g2.setColor(getBackgroundColor());
        g2.fill(shape);
        g2.setColor(getBorderColor());
        g2.draw(shape);
        Rectangle2D bounds = getBounds();
        g2.setColor(getTextColor());
        getGatewayType().drawMarker(g2, bounds, DRAWING.getLineWidth(g2));
        DRAWING.drawLabelAboveRight(g2, getName(), bounds, getTextColor());
        g2.setColor(oldColor);
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the gateway
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the gateway, usually the question it answers, drawn above its right side
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
     * Sets the gatewayType property value.
     *
     * @param newValue the kind of gateway
     */
    public void setGatewayType(GatewayType newValue)
    {
        this.gatewayType = newValue;
    }

    /**
     * Gets the gatewayType property value.
     *
     * @return the kind of gateway, exclusive by default
     */
    public GatewayType getGatewayType()
    {
        if (this.gatewayType == null)
        {
            this.gatewayType = GatewayType.EXCLUSIVE;
        }
        return this.gatewayType;
    }

    @Override
    public BpmnGatewayNode clone()
    {
        BpmnGatewayNode cloned = (BpmnGatewayNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    private GatewayType gatewayType;

    private static final BpmnDrawing DRAWING = new BpmnDrawing();

    private static final int SIZE = 50;

}
