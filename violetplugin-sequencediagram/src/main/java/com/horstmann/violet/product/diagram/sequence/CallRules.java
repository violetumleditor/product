package com.horstmann.violet.product.diagram.sequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Tells which calls make sense. A bar runs during the bar calling it and during the bar it is nested in; calls never make it run
 * during itself (loop). Two bars of a same lifeline only run at the same time if one is nested in the other.
 */
class CallRules
{
    CallRules(IGraph graph)
    {
        this.graph = graph;
    }

    /**
     * A bar is called once, and neither the caller nor a bar running at that time may be a bar of the callee's lifeline (nor of those
     * of the bars running during the callee) unless the latter is nested in it
     */
    boolean isCallAcceptable(ActivationBarNode caller, ActivationBarNode callee)
    {
        if (caller == callee || !getCallers(callee).isEmpty() || isPlacedFrom(caller, callee))
        {
            return false;
        }
        Set<INode> innerBars = collect(callee, true);
        for (INode anOuterBar : collect(caller, false))
        {
            LifelineNode lifeline = ((ActivationBarNode) anOuterBar).getImplicitParameter();
            for (INode anInnerBar : innerBars)
            {
                boolean isSameLifeline = lifeline != null && lifeline == ((ActivationBarNode) anInnerBar).getImplicitParameter();
                if (anInnerBar == anOuterBar || (isSameLifeline && !isNestedIn(anInnerBar, anOuterBar)))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A lifeline is created once, and not by a bar that depends on it
     */
    boolean isCreationAcceptable(ActivationBarNode creator, LifelineNode created)
    {
        return getCallers(created).isEmpty() && !isPlacedFrom(creator, created);
    }

    /**
     * @return the innermost bar of the lifeline that runs during the caller's call, in which a call to this lifeline nests its new
     *         bar; null if there is none
     */
    ActivationBarNode getRunningBar(ActivationBarNode caller, LifelineNode lifeline)
    {
        ActivationBarNode runningBar = null;
        for (INode anOuterBar : collect(caller, false))
        {
            if (((ActivationBarNode) anOuterBar).getImplicitParameter() == lifeline
                    && (runningBar == null || isNestedIn(anOuterBar, runningBar)))
            {
                runningBar = (ActivationBarNode) anOuterBar;
            }
        }
        return runningBar;
    }

    /**
     * @param isInner true for the bars running during the given one, false for the ones it runs during
     * @return these bars, directly or not, the given one included
     */
    private Set<INode> collect(ActivationBarNode bar, boolean isInner)
    {
        Set<INode> result = new LinkedHashSet<>();
        Deque<INode> remaining = new ArrayDeque<>(List.of(bar));
        while (!remaining.isEmpty())
        {
            INode aNode = remaining.pop();
            if (!(aNode instanceof ActivationBarNode) || !result.add(aNode))
            {
                continue;
            }
            if (isInner)
            {
                remaining.addAll(aNode.getChildren());
                remaining.addAll(getCallees(aNode));
            }
            else
            {
                addIfPresent(remaining, aNode.getParent());
                remaining.addAll(getCallers(aNode));
            }
        }
        return result;
    }

    /**
     * @return true if the place of the node comes from the one of the origin: a bar is below its caller, in its parent, and a created
     *         lifeline is beside its creator
     */
    private boolean isPlacedFrom(INode node, INode origin)
    {
        Set<INode> visited = new LinkedHashSet<>();
        Deque<INode> remaining = new ArrayDeque<>(List.of(node));
        while (!remaining.isEmpty())
        {
            INode aNode = remaining.pop();
            if (aNode == origin)
            {
                return true;
            }
            if (visited.add(aNode))
            {
                addIfPresent(remaining, aNode.getParent());
                remaining.addAll(getCallers(aNode));
            }
        }
        return false;
    }

    private void addIfPresent(Deque<INode> nodes, INode node)
    {
        if (node != null)
        {
            nodes.add(node);
        }
    }

    private boolean isNestedIn(INode node, INode ancestor)
    {
        for (INode aNode = node.getParent(); aNode != null; aNode = aNode.getParent())
        {
            if (aNode == ancestor)
            {
                return true;
            }
        }
        return false;
    }

    private List<INode> getCallers(INode callee)
    {
        List<INode> callers = new ArrayList<>();
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getEndNode() == callee)
            {
                callers.add(anEdge.getStartNode());
            }
        }
        return callers;
    }

    private List<INode> getCallees(INode caller)
    {
        List<INode> callees = new ArrayList<>();
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getStartNode() == caller)
            {
                callees.add(anEdge.getEndNode());
            }
        }
        return callees;
    }

    private final IGraph graph;
}
