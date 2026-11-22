package com.horstmann.violet.product.diagram.activity;

import com.horstmann.violet.product.diagram.abstracts.edge.AutoRouteChooser;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Routes the automatic transitions together, as the BPMN flows, a decision being a decision (see {@link AutoRouteChooser}).
 * The transitions reaching or leaving a synchronization bar aren't routed here: the bar decides their sides.
 */
class ActivityRouteChooser extends AutoRouteChooser
{

    /**
     * @param edge the transition to route
     */
    ActivityRouteChooser(SegmentedLineEdge edge)
    {
        super(edge);
    }

    @Override
    protected boolean isRoutedKind(IEdge anEdge)
    {
        return anEdge instanceof ActivityTransitionEdge && !(anEdge.getStartNode() instanceof SynchronizationBarNode)
                && !(anEdge.getEndNode() instanceof SynchronizationBarNode);
    }

    @Override
    protected boolean isDecision(INode node)
    {
        return node instanceof DecisionNode;
    }

}
