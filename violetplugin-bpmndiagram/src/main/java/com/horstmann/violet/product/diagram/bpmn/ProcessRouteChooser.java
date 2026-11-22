package com.horstmann.violet.product.diagram.bpmn;

import com.horstmann.violet.product.diagram.abstracts.edge.AutoRouteChooser;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Routes the automatic sequence and message flows together, a gateway being a decision (see {@link AutoRouteChooser}).
 */
class ProcessRouteChooser extends AutoRouteChooser
{

    /**
     * @param edge the flow to route
     */
    ProcessRouteChooser(SegmentedLineEdge edge)
    {
        super(edge);
    }

    @Override
    protected boolean isRoutedKind(IEdge anEdge)
    {
        return anEdge instanceof ProcessSequenceFlowEdge || anEdge instanceof ProcessMessageFlowEdge;
    }

    @Override
    protected boolean isDecision(INode node)
    {
        return node instanceof ProcessGatewayNode;
    }

}
