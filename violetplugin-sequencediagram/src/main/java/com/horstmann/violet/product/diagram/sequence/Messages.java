package com.horstmann.violet.product.diagram.sequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * The calls and returns of a diagram, as they are when this object is created, and what they tell: a bar runs during its caller and
 * during the bar it is nested in.
 */
class Messages
{
    Messages(IGraph graph)
    {
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getStartNode() instanceof ActivationBarNode)
            {
                this.calls.add((CallEdge) anEdge);
            }
            else if (anEdge instanceof ReturnEdge)
            {
                this.returns.add((ReturnEdge) anEdge);
            }
        }
    }

    /**
     * @return the calls of bars to bars, and the creations of lifelines
     */
    List<CallEdge> getCalls()
    {
        return this.calls;
    }

    /**
     * @return the bars that call a bar, or create a lifeline
     */
    List<INode> getCallers(INode callee)
    {
        return this.calls.stream().filter(c -> c.getEndNode() == callee).map(CallEdge::getStartNode).toList();
    }

    /**
     * @return the bar that calls a bar, or creates a lifeline, null if none
     */
    ActivationBarNode getCaller(INode callee)
    {
        return getBars(getCallers(callee)).stream().findFirst().orElse(null);
    }

    /**
     * @return the bars called by a bar and the lifelines it creates
     */
    List<INode> getCallees(INode caller)
    {
        return this.calls.stream().filter(c -> c.getStartNode() == caller).map(CallEdge::getEndNode).toList();
    }

    List<ActivationBarNode> getCalleesOnOtherLifelines(INode caller)
    {
        return getBars(getCallees(caller)).stream()
                .filter(c -> c.getImplicitParameter() != ((ActivationBarNode) caller).getImplicitParameter()).toList();
    }

    /**
     * @return the bars from which a return goes to the caller
     */
    List<ActivationBarNode> getReturningBars(INode caller)
    {
        return getBars(this.returns.stream().filter(r -> r.getEndNode() == caller).map(ReturnEdge::getStartNode).toList());
    }

    /**
     * @return the bars running during the given one, directly or not, itself included: its length includes theirs
     */
    Set<ActivationBarNode> getInnerBars(ActivationBarNode bar)
    {
        return this.innerBars.computeIfAbsent(bar, b -> collect(b, n -> {
            List<INode> inner = new ArrayList<>(n.getChildren());
            inner.addAll(getCallees(n));
            return inner;
        }));
    }

    /**
     * @return the bars the given one runs during, directly or not, itself included
     */
    Set<ActivationBarNode> getOuterBars(ActivationBarNode bar)
    {
        return collect(bar, n -> {
            List<INode> outer = new ArrayList<>(getCallers(n));
            outer.add(n.getParent());
            return outer;
        });
    }

    /**
     * @return true if one bar runs during the other, directly or not: they may lie side by side
     */
    boolean isRunningTogether(ActivationBarNode bar1, ActivationBarNode bar2)
    {
        return getInnerBars(bar1).contains(bar2) || getInnerBars(bar2).contains(bar1);
    }

    /**
     * @return true if the node is the other one or is nested in it
     */
    boolean isInside(INode node, INode ancestor)
    {
        INode aNode = node;
        while (aNode != null && aNode != ancestor)
        {
            aNode = aNode.getParent();
        }
        return aNode != null;
    }

    private Set<ActivationBarNode> collect(ActivationBarNode bar, Function<INode, List<INode>> next)
    {
        Set<ActivationBarNode> result = new LinkedHashSet<>();
        Deque<ActivationBarNode> remaining = new ArrayDeque<>(List.of(bar));
        while (!remaining.isEmpty())
        {
            ActivationBarNode aBar = remaining.pop();
            if (result.add(aBar))
            {
                remaining.addAll(getBars(next.apply(aBar)));
            }
        }
        return result;
    }

    /**
     * @return the given nodes that are bars
     */
    List<ActivationBarNode> getBars(Collection<INode> nodes)
    {
        return nodes.stream().filter(ActivationBarNode.class::isInstance).map(ActivationBarNode.class::cast).toList();
    }

    private final List<CallEdge> calls = new ArrayList<>();

    private final List<ReturnEdge> returns = new ArrayList<>();

    private final Map<ActivationBarNode, Set<ActivationBarNode>> innerBars = new HashMap<>();
}
