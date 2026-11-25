package com.horstmann.violet.product.diagram.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Keeps a space between the activation bars of a same parent, lifeline or activation bar: from top to bottom, each one is moved down
 * below the previous ones. Only a shift is computed, the location asked by the user doesn't change, so a bar goes back to its place
 * once there is room again. A bar that runs during another one (called by it, directly or not) may lie beside it.
 * <p>
 * Likewise, a nested bar is moved below a return that would reach it, or a call that would leave from it, instead of the bar it is
 * in. But if it calls a bar above the one called, the latter is moved below the nested bar.
 * <p>
 * A bar during which a lifeline is created always comes before its siblings using this lifeline: below them, it would move the
 * lifeline down, hence lengthen them, hence go down again, endlessly. If the orders of several lifelines contradict each other
 * otherwise, the bars follow the order of the calls, the same on all lifelines.
 */
class ActivationBarSpacing
{
    /** Vertical space between two bars */
    static final double GAP = ActivationBarNode.CALL_YGAP;

    /** Vertical space between a call or a return and the bars nested in the one it leaves or reaches */
    private static final double MESSAGE_GAP = GAP / 2;

    ActivationBarSpacing(SequenceDiagramGraph graph)
    {
        this.graph = graph;
    }

    void apply()
    {
        for (INode aNode : this.graph.getAllNodes())
        {
            if (aNode instanceof ActivationBarNode)
            {
                ActivationBarNode bar = (ActivationBarNode) aNode;
                this.bars.add(bar);
                bar.setSiblingShift(0);
                if (bar.getParent() != null)
                {
                    this.barsByParent.computeIfAbsent(bar.getParent(), k -> new ArrayList<>()).add(bar);
                }
            }
        }
        this.barsByParent.entrySet().removeIf(e -> e.getValue().size() < 2 && !isLinkedToOtherLifelines(e.getKey()));
        if (this.barsByParent.isEmpty())
        {
            return;
        }
        for (ActivationBarNode aBar : this.bars)
        {
            Set<ActivationBarNode> result = new HashSet<>();
            collectInnerBars(aBar, result);
            this.innerBars.put(aBar, result);
        }
        for (ActivationBarNode aBar : this.bars)
        {
            this.creators.put(aBar, findCreators(aBar));
        }
        if (separateAll(null))
        {
            return;
        }
        resetShifts();
        Map<ActivationBarNode, Integer> callOrder = getCallOrder();
        if (!separateAll(callOrder))
        {
            resetShifts(); // no possible place: better bars that overlap than endless ones
        }
    }

    private void resetShifts()
    {
        for (ActivationBarNode aBar : this.bars)
        {
            aBar.setSiblingShift(0);
        }
    }

    /**
     * Moving a bar may move the ones it calls and lengthen its callers and parents: again until nothing moves
     *
     * @param order rank of each bar, null to take the current vertical order of the siblings
     * @return false if bars still move
     */
    private boolean separateAll(Map<ActivationBarNode, Integer> order)
    {
        int maxPassCount = 2 * this.bars.size() + 2;
        for (int i = 0; i < maxPassCount; i++)
        {
            boolean isChanged = false;
            for (List<ActivationBarNode> siblings : this.barsByParent.values())
            {
                isChanged |= separate(siblings, order);
            }
            if (!isChanged)
            {
                return true;
            }
        }
        return false;
    }

    private boolean separate(List<ActivationBarNode> siblings, Map<ActivationBarNode, Integer> order)
    {
        Map<ActivationBarNode, Double> keys = new HashMap<>();
        for (ActivationBarNode aBar : siblings)
        {
            keys.put(aBar, order == null ? aBar.getOwnLocation().getY() : order.get(aBar));
        }
        List<ActivationBarNode> sortedBars = order == null ? sort(siblings, keys) : new ArrayList<>(siblings);
        if (order != null)
        {
            sortedBars.sort(Comparator.comparingDouble(keys::get));
        }
        boolean isChanged = false;
        for (int i = 0; i < sortedBars.size(); i++)
        {
            isChanged |= moveBelow(sortedBars.get(i), sortedBars.subList(0, i));
        }
        return isChanged;
    }

    private boolean moveBelow(ActivationBarNode bar, List<ActivationBarNode> barsAbove)
    {
        double ownY = bar.getOwnLocation().getY();
        double y = ownY;
        for (ActivationBarNode aBarAbove : barsAbove)
        {
            if (!isInner(bar, aBarAbove) && !isInner(aBarAbove, bar))
            {
                y = Math.max(y, aBarAbove.getBounds().getMaxY() + GAP);
            }
        }
        y = moveBelowNestedBarsOfCaller(bar, y);
        y = moveBelowMessages(bar, y);
        double shift = y - ownY;
        if (Math.abs(shift - bar.getSiblingShift()) < 0.001)
        {
            return false;
        }
        bar.setSiblingShift(shift);
        for (ActivationBarNode aBar : this.bars)
        {
            aBar.resetLocationCache();
        }
        return true;
    }

    /**
     * @param y where the bar would be in its parent
     * @return y below the calls and returns of its parent it would hide, if any
     */
    private double moveBelowMessages(ActivationBarNode bar, double y)
    {
        double parentY = bar.getParent().getLocationOnGraph().getY();
        double height = bar.getBounds().getHeight();
        List<Double> messageYs = new ArrayList<>();
        for (ReturnEdge aReturn : getReturns(bar.getParent()))
        {
            INode callee = aReturn.getStartNode();
            if (callee instanceof ActivationBarNode && !isInner((ActivationBarNode) callee, bar)
                    && !isInner(bar, (ActivationBarNode) callee))
            {
                messageYs.add(callee.getLocationOnGraph().getY() + callee.getBounds().getHeight() - parentY);
            }
        }
        for (ActivationBarNode aCallee : getCalleesOnOtherLifelines(bar.getParent()))
        {
            if (!isInner(aCallee, bar) && !isInner(bar, aCallee) && !isCallingAbove(bar, aCallee))
            {
                messageYs.add(aCallee.getLocationOnGraph().getY() - parentY);
            }
        }
        messageYs.sort(null);
        double result = y;
        for (double messageY : messageYs)
        {
            if (result < messageY + MESSAGE_GAP && result + height > messageY - MESSAGE_GAP)
            {
                result = messageY + MESSAGE_GAP;
            }
        }
        return result;
    }

    /**
     * @param y where the bar would be in its parent
     * @return y below the bars nested in its caller that call a bar above this one
     */
    private double moveBelowNestedBarsOfCaller(ActivationBarNode bar, double y)
    {
        double result = y;
        ActivationBarNode caller = findCaller(bar);
        if (caller == null || caller.getImplicitParameter() == bar.getImplicitParameter())
        {
            return result;
        }
        double parentY = bar.getParent().getLocationOnGraph().getY();
        for (INode aNode : caller.getChildren())
        {
            if (aNode instanceof ActivationBarNode && isCallingAbove((ActivationBarNode) aNode, bar)
                    && !isInner(bar, (ActivationBarNode) aNode) && !isInner((ActivationBarNode) aNode, bar))
            {
                double bottom = aNode.getLocationOnGraph().getY() + aNode.getBounds().getHeight();
                result = Math.max(result, bottom + MESSAGE_GAP - parentY);
            }
        }
        return result;
    }

    /**
     * @return true if a sibling of the callee, above it, runs during the bar
     */
    private boolean isCallingAbove(ActivationBarNode bar, ActivationBarNode callee)
    {
        return this.innerBars.get(bar).stream().anyMatch(
                b -> b.getParent() == callee.getParent() && b.getOwnLocation().getY() < callee.getOwnLocation().getY());
    }

    private List<ActivationBarNode> getCalleesOnOtherLifelines(INode caller)
    {
        List<ActivationBarNode> callees = new ArrayList<>();
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getStartNode() == caller && anEdge.getEndNode() instanceof ActivationBarNode)
            {
                ActivationBarNode callee = (ActivationBarNode) anEdge.getEndNode();
                if (callee.getImplicitParameter() != ((ActivationBarNode) caller).getImplicitParameter())
                {
                    callees.add(callee);
                }
            }
        }
        return callees;
    }

    private boolean isLinkedToOtherLifelines(INode parent)
    {
        return !getReturns(parent).isEmpty() || !getCalleesOnOtherLifelines(parent).isEmpty();
    }

    private List<ReturnEdge> getReturns(INode caller)
    {
        List<ReturnEdge> returns = new ArrayList<>();
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof ReturnEdge && anEdge.getEndNode() == caller)
            {
                returns.add((ReturnEdge) anEdge);
            }
        }
        return returns;
    }

    /**
     * Sorts bars by key, but a bar that has to come before another one comes first
     */
    private List<ActivationBarNode> sort(Collection<ActivationBarNode> barsToSort, Map<ActivationBarNode, Double> keys)
    {
        List<ActivationBarNode> remaining = new ArrayList<>(barsToSort);
        remaining.sort(Comparator.comparingDouble(keys::get));
        List<ActivationBarNode> result = new ArrayList<>();
        while (!remaining.isEmpty())
        {
            ActivationBarNode next = remaining.get(0);
            for (ActivationBarNode aBar : remaining)
            {
                if (remaining.stream().noneMatch(b -> b != aBar && isBefore(b, aBar)))
                {
                    next = aBar;
                    break;
                }
            }
            remaining.remove(next);
            result.add(next);
        }
        return result;
    }

    /**
     * @return true if a lifeline used during the second bar is created during the first one
     */
    private boolean isBefore(ActivationBarNode bar1, ActivationBarNode bar2)
    {
        return !isInner(bar1, bar2) && !isInner(bar2, bar1) && this.creators.get(bar2).stream().anyMatch(c -> c == bar1 || isInner(c, bar1));
    }

    /**
     * @return the bars that create the lifelines used during the given one
     */
    private Set<ActivationBarNode> findCreators(ActivationBarNode bar)
    {
        Set<ActivationBarNode> result = new HashSet<>();
        Set<ActivationBarNode> remaining = new HashSet<>(this.innerBars.get(bar));
        remaining.add(bar);
        while (!remaining.isEmpty())
        {
            Set<ActivationBarNode> found = new HashSet<>();
            for (ActivationBarNode aBar : remaining)
            {
                ActivationBarNode creator = findCaller(aBar.getImplicitParameter());
                if (creator != null && result.add(creator))
                {
                    found.add(creator);
                }
            }
            remaining = found;
        }
        return result;
    }

    private ActivationBarNode findCaller(INode node)
    {
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getEndNode() == node && anEdge.getStartNode() instanceof ActivationBarNode)
            {
                return (ActivationBarNode) anEdge.getStartNode();
            }
        }
        return null;
    }

    /**
     * @return rank of each bar: callers from top to bottom, each one followed by the bars it calls or contains
     */
    private Map<ActivationBarNode, Integer> getCallOrder()
    {
        Map<ActivationBarNode, List<ActivationBarNode>> barsByOuter = new HashMap<>();
        Map<ActivationBarNode, Double> keys = new HashMap<>();
        for (ActivationBarNode aBar : this.bars)
        {
            ActivationBarNode outer = findCaller(aBar);
            if (outer == null && aBar.getParent() instanceof ActivationBarNode)
            {
                outer = (ActivationBarNode) aBar.getParent();
            }
            barsByOuter.computeIfAbsent(outer, k -> new ArrayList<>()).add(aBar);
            keys.put(aBar, aBar.getLocationOnGraph().getY());
        }
        Map<ActivationBarNode, Integer> result = new HashMap<>();
        collectCallOrder(null, barsByOuter, keys, result);
        for (ActivationBarNode aBar : this.bars)
        {
            result.putIfAbsent(aBar, result.size()); // loop of an older file
        }
        return result;
    }

    private void collectCallOrder(ActivationBarNode outer, Map<ActivationBarNode, List<ActivationBarNode>> barsByOuter,
            Map<ActivationBarNode, Double> keys, Map<ActivationBarNode, Integer> result)
    {
        for (ActivationBarNode aBar : sort(barsByOuter.getOrDefault(outer, new ArrayList<>()), keys))
        {
            if (result.putIfAbsent(aBar, result.size()) == null)
            {
                collectCallOrder(aBar, barsByOuter, keys, result);
            }
        }
    }

    private boolean isInner(ActivationBarNode bar, ActivationBarNode outerBar)
    {
        return this.innerBars.get(outerBar).contains(bar);
    }

    /**
     * Collects the bars whose length is included in the one of the given bar: its children and the bars it calls, and so on
     */
    private void collectInnerBars(ActivationBarNode bar, Set<ActivationBarNode> result)
    {
        List<INode> nodes = new ArrayList<>(bar.getChildren());
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (anEdge instanceof CallEdge && anEdge.getStartNode() == bar)
            {
                nodes.add(anEdge.getEndNode());
            }
        }
        for (INode aNode : nodes)
        {
            if (aNode instanceof ActivationBarNode && result.add((ActivationBarNode) aNode))
            {
                collectInnerBars((ActivationBarNode) aNode, result);
            }
        }
    }

    private final SequenceDiagramGraph graph;

    private final List<ActivationBarNode> bars = new ArrayList<>();

    /** Bars of each lifeline or bar that has several ones */
    private final Map<INode, List<ActivationBarNode>> barsByParent = new LinkedHashMap<>();

    /** Bars that create the lifelines used during each bar */
    private final Map<ActivationBarNode, Set<ActivationBarNode>> creators = new HashMap<>();

    /** Bars running during each bar */
    private final Map<ActivationBarNode, Set<ActivationBarNode>> innerBars = new HashMap<>();
}
