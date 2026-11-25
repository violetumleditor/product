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

import com.horstmann.violet.product.diagram.abstracts.IGraph;
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

    ActivationBarSpacing(IGraph graph)
    {
        this.messages = new Messages(graph);
        for (INode aNode : graph.getAllNodes())
        {
            if (aNode instanceof ActivationBarNode)
            {
                ActivationBarNode bar = (ActivationBarNode) aNode;
                this.bars.add(bar);
                this.barsByParent.computeIfAbsent(bar.getParent(), k -> new ArrayList<>()).add(bar);
            }
        }
        this.barsByParent.remove(null);
        this.barsByParent.entrySet().removeIf(e -> e.getValue().size() < 2 && !isLinkedToOtherLifelines(e.getKey()));
        this.bars.forEach(b -> this.creators.put(b, findCreators(b)));
    }

    void apply()
    {
        resetShifts();
        if (separateAll(null))
        {
            return;
        }
        resetShifts();
        if (!separateAll(getCallOrder()))
        {
            resetShifts(); // no possible place: better bars that overlap than endless ones
        }
    }

    private void resetShifts()
    {
        this.bars.forEach(b -> b.setSiblingShift(0));
    }

    private boolean isLinkedToOtherLifelines(INode parent)
    {
        return !this.messages.getReturningBars(parent).isEmpty() || !this.messages.getCalleesOnOtherLifelines(parent).isEmpty();
    }

    /**
     * Moving a bar may move the ones it calls and lengthen its callers and parents: again until nothing moves
     *
     * @param order rank of each bar, null to take the current vertical order of the siblings
     * @return false if bars still move
     */
    private boolean separateAll(Map<ActivationBarNode, Double> order)
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

    private boolean separate(List<ActivationBarNode> siblings, Map<ActivationBarNode, Double> order)
    {
        Map<ActivationBarNode, Double> keys = order;
        if (keys == null)
        {
            keys = new HashMap<>();
            for (ActivationBarNode aBar : siblings)
            {
                keys.put(aBar, aBar.getOwnLocation().getY());
            }
        }
        List<ActivationBarNode> sortedBars = sort(siblings, keys);
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
            if (!this.messages.isRunningTogether(bar, aBarAbove))
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
        this.bars.forEach(ActivationBarNode::resetLocationCache);
        return true;
    }

    /**
     * @param y where the bar would be in its parent
     * @return y below the bars nested in its caller that call a bar above this one
     */
    private double moveBelowNestedBarsOfCaller(ActivationBarNode bar, double y)
    {
        ActivationBarNode caller = this.messages.getCaller(bar);
        if (caller == null || caller.getImplicitParameter() == bar.getImplicitParameter())
        {
            return y;
        }
        double result = y;
        for (ActivationBarNode nestedBar : this.messages.getBars(caller.getChildren()))
        {
            if (isCallingAbove(nestedBar, bar) && !this.messages.isRunningTogether(bar, nestedBar))
            {
                result = Math.max(result, getBottomOnGraph(nestedBar) + MESSAGE_GAP - bar.getParent().getLocationOnGraph().getY());
            }
        }
        return result;
    }

    /**
     * @param y where the bar would be in its parent
     * @return y below the calls and returns of its parent it would hide, if any
     */
    private double moveBelowMessages(ActivationBarNode bar, double y)
    {
        INode parent = bar.getParent();
        double parentY = parent.getLocationOnGraph().getY();
        double height = bar.getBounds().getHeight();
        List<Double> messageYs = new ArrayList<>();
        for (ActivationBarNode aBar : this.messages.getReturningBars(parent))
        {
            if (!this.messages.isRunningTogether(aBar, bar))
            {
                messageYs.add(getBottomOnGraph(aBar) - parentY);
            }
        }
        for (ActivationBarNode aCallee : this.messages.getCalleesOnOtherLifelines(parent))
        {
            if (!this.messages.isRunningTogether(aCallee, bar) && !isCallingAbove(bar, aCallee))
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
     * @return true if a sibling of the callee, above it, runs during the bar
     */
    private boolean isCallingAbove(ActivationBarNode bar, ActivationBarNode callee)
    {
        return this.messages.getInnerBars(bar).stream().anyMatch(
                b -> b.getParent() == callee.getParent() && b.getOwnLocation().getY() < callee.getOwnLocation().getY());
    }

    private double getBottomOnGraph(ActivationBarNode bar)
    {
        return bar.getLocationOnGraph().getY() + bar.getBounds().getHeight();
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
            ActivationBarNode next = remaining.stream().filter(b -> remaining.stream().noneMatch(other -> isBefore(other, b)))
                    .findFirst().orElse(remaining.get(0));
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
        return !this.messages.isRunningTogether(bar1, bar2)
                && this.creators.get(bar2).stream().anyMatch(this.messages.getInnerBars(bar1)::contains);
    }

    /**
     * @return the bars that create the lifelines used during the given one, directly or not
     */
    private Set<ActivationBarNode> findCreators(ActivationBarNode bar)
    {
        Set<ActivationBarNode> result = new HashSet<>();
        List<ActivationBarNode> remaining = new ArrayList<>(this.messages.getInnerBars(bar));
        for (int i = 0; i < remaining.size(); i++)
        {
            ActivationBarNode creator = this.messages.getCaller(remaining.get(i).getImplicitParameter());
            if (creator != null && result.add(creator))
            {
                remaining.add(creator);
            }
        }
        return result;
    }

    /**
     * @return rank of each bar: callers from top to bottom, each one followed by the bars it calls or contains
     */
    private Map<ActivationBarNode, Double> getCallOrder()
    {
        Map<ActivationBarNode, List<ActivationBarNode>> barsByOuter = new HashMap<>();
        Map<ActivationBarNode, Double> keys = new HashMap<>();
        for (ActivationBarNode aBar : this.bars)
        {
            ActivationBarNode outer = this.messages.getCaller(aBar);
            if (outer == null && aBar.getParent() instanceof ActivationBarNode)
            {
                outer = (ActivationBarNode) aBar.getParent();
            }
            barsByOuter.computeIfAbsent(outer, k -> new ArrayList<>()).add(aBar);
            keys.put(aBar, aBar.getLocationOnGraph().getY());
        }
        Map<ActivationBarNode, Double> result = new HashMap<>();
        collectCallOrder(null, barsByOuter, keys, result);
        this.bars.forEach(b -> result.putIfAbsent(b, (double) result.size())); // loop of an older file
        return result;
    }

    private void collectCallOrder(ActivationBarNode outer, Map<ActivationBarNode, List<ActivationBarNode>> barsByOuter,
            Map<ActivationBarNode, Double> keys, Map<ActivationBarNode, Double> result)
    {
        for (ActivationBarNode aBar : sort(barsByOuter.getOrDefault(outer, List.of()), keys))
        {
            if (result.putIfAbsent(aBar, (double) result.size()) == null)
            {
                collectCallOrder(aBar, barsByOuter, keys, result);
            }
        }
    }

    private final Messages messages;

    private final List<ActivationBarNode> bars = new ArrayList<>();

    /** Bars of each lifeline or bar that has several ones, or messages that a nested bar may hide */
    private final Map<INode, List<ActivationBarNode>> barsByParent = new LinkedHashMap<>();

    /** Bars that create the lifelines used during each bar */
    private final Map<ActivationBarNode, Set<ActivationBarNode>> creators = new HashMap<>();
}
