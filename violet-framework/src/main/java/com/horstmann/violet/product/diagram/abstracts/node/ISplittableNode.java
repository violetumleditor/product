package com.horstmann.violet.product.diagram.abstracts.node;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * A node split into parts by separators (the lanes of a BPMN pool...) that the user moves by dragging them (see
 * MoveSeparatorBehavior), which changes the size of the parts on both sides of the separator.
 * <p>
 * A separator is a line, horizontal or vertical: a horizontal one moves up and down, a vertical one left and right.
 */
public interface ISplittableNode extends INode
{

    /**
     * @return the separators, in graph coordinates, each a horizontal or vertical line; empty when the node isn't split
     */
    List<Line2D> getSeparators();

    /**
     * Moves a separator toward a point: along the y axis for a horizontal separator, along the x axis for a vertical one. The node
     * may limit the move (a minimum size for the parts...).
     *
     * @param index of the separator, as in {@link #getSeparators()}
     * @param location the wished location, in graph coordinates
     */
    void moveSeparator(int index, Point2D location);

    /**
     * @return the positions of the separators, as saved in the file, so that a move can be undone; null when never moved
     */
    String getSeparatorPositions();

    /**
     * @param separatorPositions positions formerly given by {@link #getSeparatorPositions()}
     */
    void setSeparatorPositions(String separatorPositions);

}
