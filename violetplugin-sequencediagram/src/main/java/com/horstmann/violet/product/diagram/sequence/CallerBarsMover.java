package com.horstmann.violet.product.diagram.sequence;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Lives as long as the drag of one bar. When a called activation bar is moved, the bar of the other lifeline calling it moves too, instead of being resized, and so on up
 * to the first caller. A nested caller moves with the whole bar it is in. A caller that has other callees only moves as far as it
 * keeps its length without moving them, then it is resized.
 */
class CallerBarsMover
{
    /**
     * @param graph the graph
     * @param draggedBar the bar being dragged
     * @param drawnBounds the bounds on graph of all bars, as they were just before the drag
     */
    CallerBarsMover(SequenceDiagramGraph graph, ActivationBarNode draggedBar, Map<ActivationBarNode, Rectangle2D> drawnBounds)
    {
        this.draggedBar = draggedBar;
        this.draggedBarStartY = drawnBounds.get(draggedBar).getY();
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (!(anEdge instanceof CallEdge) || !(anEdge.getStartNode() instanceof ActivationBarNode)
                    || !(anEdge.getEndNode() instanceof ActivationBarNode))
            {
                continue;
            }
            ActivationBarNode caller = (ActivationBarNode) anEdge.getStartNode();
            ActivationBarNode callee = (ActivationBarNode) anEdge.getEndNode();
            if (!drawnBounds.containsKey(caller) || !drawnBounds.containsKey(callee))
            {
                continue;
            }
            if (caller.getImplicitParameter() == callee.getImplicitParameter())
            {
                continue;
            }
            ActivationBarNode root = getRoot(caller);
            if (!isInside(draggedBar, root) && !isInside(caller, draggedBar))
            {
                this.callsByRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(new Call(callee, drawnBounds.get(caller), drawnBounds.get(callee)));
                this.startLocations.put(root, copyOf(root.getLocation()));
            }
        }
    }

    ActivationBarNode getDraggedBar()
    {
        return this.draggedBar;
    }

    /**
     * Places the callers again, from where they were at the beginning of the drag
     */
    void follow()
    {
        Map<INode, Double> dyByNode = new HashMap<>();
        double askedY = this.draggedBar.getParent().getLocationOnGraph().getY() + this.draggedBar.getAskedLocation().getY();
        dyByNode.put(this.draggedBar, askedY - this.draggedBarStartY);
        // A caller may be called too: again until nothing moves
        boolean isChanged = true;
        for (int i = 0; isChanged && i <= this.callsByRoot.size(); i++)
        {
            isChanged = false;
            for (Map.Entry<ActivationBarNode, List<Call>> entry : this.callsByRoot.entrySet())
            {
                double dy = getCallerMove(entry.getValue(), dyByNode);
                Double formerDy = dyByNode.put(entry.getKey(), dy);
                isChanged |= dy != (formerDy == null ? 0 : formerDy);
            }
        }
        for (ActivationBarNode aRoot : this.callsByRoot.keySet())
        {
            double dy = dyByNode.getOrDefault(aRoot, 0d);
            if (dy != this.appliedMoves.getOrDefault(aRoot, 0d))
            {
                Point2D start = this.startLocations.get(aRoot);
                aRoot.setLocation(new Point2D.Double(start.getX(), start.getY() + dy));
                this.appliedMoves.put(aRoot, dy);
            }
        }
    }

    /**
     * The caller follows its bottom, i.e. its lowest callee, but stays above each of its callees
     */
    private double getCallerMove(List<Call> calls, Map<INode, Double> dyByNode)
    {
        double formerBottom = Double.NEGATIVE_INFINITY;
        double newBottom = Double.NEGATIVE_INFINITY;
        double maxMove = Double.POSITIVE_INFINITY;
        for (Call aCall : calls)
        {
            double dy = getMove(aCall.callee, dyByNode);
            formerBottom = Math.max(formerBottom, aCall.calleeBottom);
            newBottom = Math.max(newBottom, aCall.calleeBottom + dy);
            double slack = Math.max(0, aCall.calleeTop - aCall.callerTop - ActivationBarNode.CALL_YGAP / 2);
            maxMove = Math.min(maxMove, slack + dy);
        }
        return Math.min(newBottom - formerBottom, maxMove);
    }

    /** A bar moves with the bars it is in */
    private double getMove(INode bar, Map<INode, Double> dyByNode)
    {
        double dy = 0;
        for (INode aNode = bar; aNode instanceof ActivationBarNode; aNode = aNode.getParent())
        {
            dy += dyByNode.getOrDefault(aNode, 0d);
        }
        return dy;
    }

    private ActivationBarNode getRoot(ActivationBarNode bar)
    {
        ActivationBarNode root = bar;
        while (root.getParent() instanceof ActivationBarNode)
        {
            root = (ActivationBarNode) root.getParent();
        }
        return root;
    }

    private boolean isInside(INode node, INode ancestor)
    {
        for (INode aNode = node; aNode != null; aNode = aNode.getParent())
        {
            if (aNode == ancestor)
            {
                return true;
            }
        }
        return false;
    }

    private Point2D copyOf(Point2D p)
    {
        return new Point2D.Double(p.getX(), p.getY());
    }

    /** A call as it was at the beginning of the move, on the graph */
    private static class Call
    {
        Call(ActivationBarNode callee, Rectangle2D callerBounds, Rectangle2D calleeBounds)
        {
            this.callee = callee;
            this.callerTop = callerBounds.getY();
            this.calleeTop = calleeBounds.getY();
            this.calleeBottom = calleeBounds.getMaxY();
        }

        private final ActivationBarNode callee;
        private final double callerTop;
        private final double calleeTop;
        private final double calleeBottom;
    }

    private final ActivationBarNode draggedBar;

    private final double draggedBarStartY;

    /** Calls to other lifelines of each bar put on a lifeline, its nested bars included */
    private final Map<ActivationBarNode, List<Call>> callsByRoot = new LinkedHashMap<>();

    private final Map<ActivationBarNode, Point2D> startLocations = new HashMap<>();

    private final Map<ActivationBarNode, Double> appliedMoves = new HashMap<>();
}
