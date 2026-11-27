package com.horstmann.violet.product.diagram.abstracts.node;

import java.awt.geom.Point2D;

/**
 * A node that the user can turn around its center (a free text): with the selection tool, a round handle drawn off its top turns
 * it ({@code RotateNodeBehavior}), and its properties dialog offers the angle.
 * <p>
 * Its bounds are those of the turned node, so they still start at its location, as everywhere else; the node itself is centered
 * in them.
 * </p>
 */
public interface IRotatableNode extends INode
{
    /**
     * @return the angle, in degrees, clockwise on screen, from 0 included to 360 excluded; null for 0
     */
    Double getRotation();

    /**
     * Turns the node around its center, which stays where it is
     *
     * @param degrees the angle, clockwise on screen; any value, brought back from 0 to 360; null or 0 for none
     */
    void setRotation(Double degrees);

    /**
     * @return the width of the node before it is turned
     */
    double getUnrotatedWidth();

    /**
     * @return the height of the node before it is turned
     */
    double getUnrotatedHeight();

    /**
     * @return the center of the node on the graph, around which it turns
     */
    default Point2D getRotationCenter()
    {
        Point2D location = getLocationOnGraph();
        return new Point2D.Double(location.getX() + getBounds().getWidth() / 2, location.getY() + getBounds().getHeight() / 2);
    }

    /**
     * @return the angle in degrees, 0 when none
     */
    default double getRotationDegrees()
    {
        Double rotation = getRotation();
        return rotation == null ? 0 : rotation;
    }
}
