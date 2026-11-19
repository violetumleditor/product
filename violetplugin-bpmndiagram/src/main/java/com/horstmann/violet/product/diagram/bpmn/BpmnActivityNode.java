package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A BPMN activity: a rounded rectangle with its name inside, which grows with the name.
 */
public abstract class BpmnActivityNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D nameBounds = getName().getBounds();
        Point2D location = getLocation();
        double width = Math.max(nameBounds.getWidth() + 2 * HORIZONTAL_PADDING, DEFAULT_WIDTH);
        double height = Math.max(nameBounds.getHeight() + 2 * VERTICAL_PADDING, DEFAULT_HEIGHT);
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), width, height);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        return new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ARC_SIZE, ARC_SIZE);
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
        getName().draw(g2, bounds);
        float lineWidth = DRAWING.getLineWidth(g2);
        if (this.isToolPrototype)
        {
            // Markers enlarged around the corner of the node, else they vanish in the small tool icon
            AffineTransform oldTransform = g2.getTransform();
            g2.translate(bounds.getX(), bounds.getY());
            g2.scale(TOOL_ICON_MARKER_ZOOM, TOOL_ICON_MARKER_ZOOM);
            drawMarkers(g2, new Rectangle2D.Double(0, 0, bounds.getWidth() / TOOL_ICON_MARKER_ZOOM, bounds.getHeight()
                    / TOOL_ICON_MARKER_ZOOM), (float) (lineWidth / TOOL_ICON_MARKER_ZOOM));
            g2.setTransform(oldTransform);
        }
        else
        {
            drawMarkers(g2, bounds, lineWidth);
        }
        g2.setColor(oldColor);
    }

    /**
     * Turns this node into a tool bar prototype, whose markers are drawn larger
     */
    public void enableToolIcon()
    {
        this.isToolPrototype = true;
    }

    /**
     * Draws what tells the kind of activity
     *
     * @param g2 graphics, its color set to the text color
     * @param bounds the bounds of the activity
     * @param lineWidth the width of the outline
     */
    protected abstract void drawMarkers(Graphics2D g2, Rectangle2D bounds, float lineWidth);

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the activity
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the activity
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
    public BpmnActivityNode clone()
    {
        BpmnActivityNode cloned = (BpmnActivityNode) super.clone();
        cloned.name = getName().clone();
        cloned.isToolPrototype = false;
        return cloned;
    }

    private MultiLineString name;

    private transient boolean isToolPrototype;

    private static final double TOOL_ICON_MARKER_ZOOM = 2.2;

    private static final BpmnDrawing DRAWING = new BpmnDrawing();

    private static final int ARC_SIZE = 20;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 80;
    private static final int HORIZONTAL_PADDING = 22;
    private static final int VERTICAL_PADDING = 10;

}
