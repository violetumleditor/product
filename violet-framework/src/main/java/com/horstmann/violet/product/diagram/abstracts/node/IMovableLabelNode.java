package com.horstmann.violet.product.diagram.abstracts.node;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.IMovableLabelElement;

/**
 * A node that draws its name outside its bounds, where the user can drag it (MoveLabelBehavior). The name keeps its offset from
 * its automatic place, so it follows the node.
 */
public interface IMovableLabelNode extends INode, IMovableLabelElement
{
    /** The only text of the node, after its property */
    String NAME = "name";

    /**
     * @return the bounds of the name on the graph at its automatic place, or null if it is empty
     */
    Rectangle2D getDefaultNameBounds();

    /**
     * @return the offset of the name from its automatic place, or null if it is there
     */
    Point2D.Double getNameOffset();

    /**
     * @param newValue the offset of the name from its automatic place, or null to put it back there
     */
    void setNameOffset(Point2D.Double newValue);

    /**
     * @return the shape of the node, without its name
     */
    Shape getShape();

    @Override
    default Rectangle2D getOutsideLabelBounds()
    {
        Rectangle2D bounds = getDefaultNameBounds();
        Point2D offset = getNameOffset();
        if (bounds == null || offset == null)
        {
            return bounds;
        }
        return new Rectangle2D.Double(bounds.getX() + offset.getX(), bounds.getY() + offset.getY(), bounds.getWidth(),
                bounds.getHeight());
    }

    /**
     * The shape of the node keeps its presses, unless the user moved the name upon it; another node upon the name keeps its own
     */
    @Override
    default String findLabel(Point2D aPoint)
    {
        Rectangle2D bounds = getOutsideLabelBounds();
        if (bounds == null || !bounds.contains(aPoint))
        {
            return null;
        }
        if (getNameOffset() == null && getShape().contains(aPoint))
        {
            return null;
        }
        return getGraph().findNode(aPoint) == this ? NAME : null;
    }

    @Override
    default Rectangle2D getDefaultLabelBounds(String labelName)
    {
        return getDefaultNameBounds();
    }

    @Override
    default Rectangle2D getLabelBounds(String labelName)
    {
        return getOutsideLabelBounds();
    }

    @Override
    default Point2D getLabelAnchor(String labelName)
    {
        Rectangle2D bounds = getBounds();
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    @Override
    default Point2D.Double getLabelOffset(String labelName)
    {
        return getNameOffset();
    }

    @Override
    default void setLabelOffset(String labelName, Point2D.Double offset)
    {
        setNameOffset(offset);
    }
}
