package com.horstmann.violet.product.diagram.sequence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Tells which calls make sense. A bar runs during the bar calling it and during the bar it is nested in; calls never make it run
 * during itself (loop). Two bars of a same lifeline only run at the same time if one is nested in the other.
 */
class CallRules
{
    CallRules(IGraph graph)
    {
        this.messages = new Messages(graph);
    }

    /**
     * A bar is called once, and neither the caller nor a bar running at that time may be a bar of the callee's lifeline (nor of those
     * of the bars running during the callee) unless the latter is nested in it
     */
    boolean isCallAcceptable(ActivationBarNode caller, ActivationBarNode callee)
    {
        if (caller == callee || !this.messages.getCallers(callee).isEmpty() || isPlacedFrom(caller, callee))
        {
            return false;
        }
        Set<ActivationBarNode> innerBars = this.messages.getInnerBars(callee);
        return this.messages.getOuterBars(caller).stream()
                .allMatch(outer -> innerBars.stream().allMatch(inner -> isRunningTogetherPossible(outer, inner)));
    }

    private boolean isRunningTogetherPossible(ActivationBarNode outerBar, ActivationBarNode innerBar)
    {
        LifelineNode lifeline = outerBar.getImplicitParameter();
        boolean isSameLifeline = lifeline != null && lifeline == innerBar.getImplicitParameter();
        return innerBar != outerBar && (!isSameLifeline || this.messages.isInside(innerBar, outerBar));
    }

    /**
     * A lifeline is created once, and not by a bar that depends on it
     */
    boolean isCreationAcceptable(ActivationBarNode creator, LifelineNode created)
    {
        return this.messages.getCallers(created).isEmpty() && !isPlacedFrom(creator, created);
    }

    /**
     * A return only goes back to the bar that called
     */
    boolean isReturnAcceptable(INode callee, INode caller)
    {
        return this.messages.getCallers(callee).contains(caller);
    }

    /**
     * @return the innermost bar of the lifeline that runs during the caller's call, in which a call to this lifeline nests its new
     *         bar; null if there is none
     */
    ActivationBarNode getRunningBar(ActivationBarNode caller, LifelineNode lifeline)
    {
        ActivationBarNode runningBar = null;
        for (ActivationBarNode anOuterBar : this.messages.getOuterBars(caller))
        {
            if (anOuterBar.getImplicitParameter() == lifeline
                    && (runningBar == null || this.messages.isInside(anOuterBar, runningBar)))
            {
                runningBar = anOuterBar;
            }
        }
        return runningBar;
    }

    /**
     * @return true if the place of the node comes from the one of the origin: a bar is below its caller, in its parent, and a created
     *         lifeline is beside its creator
     */
    private boolean isPlacedFrom(INode node, INode origin)
    {
        Set<INode> visited = new HashSet<>();
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
                remaining.addAll(this.messages.getCallers(aNode));
                if (aNode.getParent() != null)
                {
                    remaining.add(aNode.getParent());
                }
            }
        }
        return false;
    }

    private final Messages messages;
}
