package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A BPMN data store: where the process reads or keeps information beyond its own life (a database, a register...), drawn as a
 * cylinder, its name below it.
 */
public class BpmnDataStoreNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), SIZE, SIZE);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D b = getBounds();
        Area cylinder = new Area(new Rectangle2D.Double(b.getX(), b.getY() + LID / 2, b.getWidth(), b.getHeight() - LID));
        cylinder.add(new Area(new Ellipse2D.Double(b.getX(), b.getY(), b.getWidth(), LID)));
        cylinder.add(new Area(new Ellipse2D.Double(b.getX(), b.getMaxY() - LID, b.getWidth(), LID)));
        return cylinder;
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
        // The rim of the lid, then two rings below it
        for (int i = 0; i < 3; i++)
        {
            double y = b.getY() + i * RING_GAP;
            g2.draw(new Arc2D.Double(b.getX(), y, b.getWidth(), LID, 180, 180, Arc2D.OPEN));
        }
        DRAWING.drawLabelBelow(g2, getName(), b, getTextColor());
        g2.setColor(oldColor);
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the store
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the store, drawn below it
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
    public BpmnDataStoreNode clone()
    {
        BpmnDataStoreNode cloned = (BpmnDataStoreNode) super.clone();
        cloned.name = getName().clone();
        return cloned;
    }

    private MultiLineString name;

    private static final BpmnDrawing DRAWING = new BpmnDrawing();

    private static final int SIZE = 50;
    private static final double LID = 14;
    private static final double RING_GAP = 4;

}
