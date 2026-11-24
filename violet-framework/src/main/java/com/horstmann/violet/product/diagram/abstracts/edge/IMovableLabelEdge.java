package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An edge whose texts can be dragged by the user (MoveEdgeLabelBehavior). Each text is named after its String property. It keeps
 * its offset from its automatic place, which follows the edge; the offset is a property too, named after the text (see
 * {@link #getLabelOffsetPropertyName(String)}), so that a move can be undone.
 */
public interface IMovableLabelEdge extends IEdge
{
    /**
     * @param aPoint a point on the graph
     * @return the name of the text of this edge under the point, or null; never inside the nodes the edge links, which keep their
     *         clicks
     */
    String findLabel(Point2D aPoint);

    /**
     * @param labelName the name of a text of this edge
     * @return its bounds on the graph at its automatic place, or null if this text is empty
     */
    Rectangle2D getDefaultLabelBounds(String labelName);

    /**
     * @param labelName the name of a text of this edge
     * @return its bounds on the graph, moved by the user if it was, or null if it is empty
     */
    Rectangle2D getLabelBounds(String labelName);

    /**
     * @param labelName the name of a text of this edge
     * @return the point of the edge this text belongs to
     */
    Point2D getLabelAnchor(String labelName);

    /**
     * @param labelName the name of a text of this edge
     * @return the offset from its automatic place, or null if this text is there
     */
    Point2D.Double getLabelOffset(String labelName);

    /**
     * @param labelName the name of a text of this edge
     * @param offset the offset from its automatic place, or null to put it back there
     */
    void setLabelOffset(String labelName, Point2D.Double offset);

    /**
     * @param labelName the name of a text of this edge
     * @return the name of the bean property holding its offset (for an undoable property edit)
     */
    default String getLabelOffsetPropertyName(String labelName)
    {
        return labelName + "Offset";
    }
}
