package com.horstmann.violet.product.diagram.sequence;

import java.util.ArrayList;
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
 */
class ActivationBarSpacing
{
    /** Vertical space between two bars */
    static final double GAP = ActivationBarNode.CALL_YGAP;

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
        this.barsByParent.values().removeIf(siblings -> siblings.size() < 2);
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
        // Moving a bar may move the ones it calls and lengthen its callers and parents: again until nothing moves
        int maxPassCount = 2 * this.bars.size() + 2;
        for (int i = 0; i < maxPassCount; i++)
        {
            boolean isChanged = false;
            for (List<ActivationBarNode> siblings : this.barsByParent.values())
            {
                isChanged |= separate(siblings);
            }
            if (!isChanged)
            {
                return;
            }
        }
    }

    private boolean separate(List<ActivationBarNode> siblings)
    {
        Map<ActivationBarNode, Double> ownYs = new HashMap<>();
        for (ActivationBarNode aBar : siblings)
        {
            ownYs.put(aBar, aBar.getOwnLocation().getY());
        }
        List<ActivationBarNode> sortedBars = new ArrayList<>(siblings);
        sortedBars.sort(Comparator.comparingDouble(ownYs::get));
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

    /** Bars running during each bar */
    private final Map<ActivationBarNode, Set<ActivationBarNode>> innerBars = new HashMap<>();
}
