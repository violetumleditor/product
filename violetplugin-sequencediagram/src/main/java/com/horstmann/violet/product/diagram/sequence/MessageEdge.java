package com.horstmann.violet.product.diagram.sequence;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * A call or a return
 */
public abstract class MessageEdge extends SegmentedLineEdge
{
    @Override
    public boolean isTransitionPointsSupported()
    {
        return false;
    }

    @Override
    public ArrayList<Point2D> getPoints()
    {
        return new ArrayList<>(List.of(getStartNode().getConnectionPoint(this), getEndNode().getConnectionPoint(this)));
    }

    @Override
    public Line2D getConnectionPoints()
    {
        ArrayList<Point2D> points = getPoints();
        return new Line2D.Double(points.get(0), points.get(points.size() - 1));
    }

    /**
     * Remembers the nodes this edge linked when removed
     */
    void rememberRemoval()
    {
        this.removedStartNode = getStartNode();
        this.removedEndNode = getEndNode();
    }

    /**
     * @return true if this edge, formerly removed, links the same nodes again (undo): it is accepted as it was then, whatever the
     *         rules (a return may be put back before its call, an older file may have edges that are refused now)
     */
    boolean isPutBack()
    {
        return this.removedStartNode != null && this.removedStartNode == getStartNode() && this.removedEndNode == getEndNode();
    }

    private transient INode removedStartNode;

    private transient INode removedEndNode;
}
