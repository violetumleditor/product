package com.horstmann.violet.product.diagram.abstracts;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An element (edge, or node with a text outside) whose texts can be dragged by the user (MoveLabelBehavior). Each text is named
 * after its property. It keeps its offset from its automatic place, which follows the element; the offset is a property too, named after the text (see
 * {@link #getLabelOffsetPropertyName(String)}), so that a move can be undone.
 */
public interface IMovableLabelElement
{
    /**
     * @param aPoint a point on the graph
     * @return the name of the text of this element a press at the point takes, or null
     */
    String findLabel(Point2D aPoint);

    /**
     * @param labelName the name of a text of this element
     * @return its bounds on the graph at its automatic place, or null if this text is empty
     */
    Rectangle2D getDefaultLabelBounds(String labelName);

    /**
     * @param labelName the name of a text of this element
     * @return its bounds on the graph, moved by the user if it was, or null if it is empty
     */
    Rectangle2D getLabelBounds(String labelName);

    /**
     * @param labelName the name of a text of this element
     * @return the point of the element this text belongs to
     */
    Point2D getLabelAnchor(String labelName);

    /**
     * @param labelName the name of a text of this element
     * @return the offset from its automatic place, or null if this text is there
     */
    Point2D.Double getLabelOffset(String labelName);

    /**
     * @param labelName the name of a text of this element
     * @param offset the offset from its automatic place, or null to put it back there
     */
    void setLabelOffset(String labelName, Point2D.Double offset);

    /**
     * @param labelName the name of a text of this element
     * @return the name of the bean property holding its offset (for an undoable property edit)
     */
    default String getLabelOffsetPropertyName(String labelName)
    {
        return labelName + "Offset";
    }
}
