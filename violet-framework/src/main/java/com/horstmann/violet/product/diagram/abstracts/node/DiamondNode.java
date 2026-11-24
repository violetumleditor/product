package com.horstmann.violet.product.diagram.abstracts.node;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A diamond of standard size where a flow splits or merges (BPMN gateway, decision of an activity diagram), its name (usually a
 * question) above its right side, where the user can drag it. Edges leave and reach it by its vertices.
 */
public abstract class DiamondNode extends RectangularNode implements IMovableLabelNode
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

    /**
     * The name starts against the upper right edge of the diamond: an edge often leaves its bottom vertex, and a name below would
     * hide it
     */
    @Override
    public Rectangle2D getDefaultNameBounds()
    {
        Rectangle2D textBounds = getName().getBounds();
        if (textBounds.getWidth() <= 0)
        {
            return null;
        }
        Rectangle2D b = getBounds();
        return new Rectangle2D.Double(b.getCenterX() + b.getWidth() / 4, b.getY() + b.getHeight() / 4 - textBounds.getHeight(),
                textBounds.getWidth(), textBounds.getHeight());
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
        g2.setColor(getTextColor());
        drawMarker(g2);
        Rectangle2D nameBounds = getOutsideLabelBounds();
        if (nameBounds != null)
        {
            g2.setColor(getTextColor());
            getName().draw(g2, nameBounds);
        }
        g2.setColor(oldColor);
    }

    /**
     * Draws the marker inside the diamond, none by default
     *
     * @param g2 graphics, its color set to the color of the text
     */
    protected void drawMarker(Graphics2D g2)
    {
        // An empty diamond
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the node
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the node, usually the question it answers, drawn above its right side
     */
    public MultiLineString getName()
    {
        if (this.name == null)
        {
            this.name = new MultiLineString();
        }
        return this.name;
    }

    @Override
    public Point2D.Double getNameOffset()
    {
        return this.nameOffset;
    }

    @Override
    public void setNameOffset(Point2D.Double newValue)
    {
        this.nameOffset = newValue;
    }

    @Override
    public DiamondNode clone()
    {
        DiamondNode cloned = (DiamondNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    /** Offset of the name from its automatic place, null when it is there (not saved then) */
    private Point2D.Double nameOffset;

    private static final int SIZE = 50;

}
