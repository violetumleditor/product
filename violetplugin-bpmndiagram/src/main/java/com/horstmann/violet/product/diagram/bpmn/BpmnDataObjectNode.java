package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A BPMN data object: information an activity needs or produces (a document, an order...), drawn as a page with a folded corner,
 * its name below it.
 */
public class BpmnDataObjectNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), WIDTH, HEIGHT);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D b = getBounds();
        Path2D.Double page = new Path2D.Double();
        page.moveTo(b.getX(), b.getY());
        page.lineTo(b.getMaxX() - FOLD, b.getY());
        page.lineTo(b.getMaxX(), b.getY() + FOLD);
        page.lineTo(b.getMaxX(), b.getMaxY());
        page.lineTo(b.getX(), b.getMaxY());
        page.closePath();
        return page;
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
        Rectangle2D b = getBounds();
        Path2D.Double fold = new Path2D.Double();
        fold.moveTo(b.getMaxX() - FOLD, b.getY());
        fold.lineTo(b.getMaxX() - FOLD, b.getY() + FOLD);
        fold.lineTo(b.getMaxX(), b.getY() + FOLD);
        g2.draw(fold);
        DRAWING.drawLabelBelow(g2, getName(), b, getTextColor());
        g2.setColor(oldColor);
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the data
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the data, drawn below it
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
    public BpmnDataObjectNode clone()
    {
        BpmnDataObjectNode cloned = (BpmnDataObjectNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    private static final BpmnDrawing DRAWING = new BpmnDrawing();

    private static final int WIDTH = 36;
    private static final int HEIGHT = 50;
    private static final int FOLD = 10;

}
