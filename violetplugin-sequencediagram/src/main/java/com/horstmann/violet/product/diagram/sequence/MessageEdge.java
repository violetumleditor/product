package com.horstmann.violet.product.diagram.sequence;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * A call or a return
 */
public abstract class MessageEdge extends SegmentedLineEdge
{
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
