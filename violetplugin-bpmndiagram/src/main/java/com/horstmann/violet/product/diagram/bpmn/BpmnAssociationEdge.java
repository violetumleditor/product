package com.horstmann.violet.product.diagram.bpmn;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A BPMN association: a dotted line linking a text annotation to what it explains, or data to the activity that reads it or
 * writes it. Toward or from data, it ends with an open arrow showing which way the data goes.
 */
public class BpmnAssociationEdge extends SegmentedLineEdge
{

    @Override
    public LineStyle getLineStyle()
    {
        return LineStyle.DOTTED;
    }

    @Override
    public ArrowHead getStartArrowHead()
    {
        return ArrowHead.NONE;
    }

    @Override
    public ArrowHead getEndArrowHead()
    {
        return isData(getStartNode()) || isData(getEndNode()) ? ArrowHead.V : ArrowHead.NONE;
    }

    private boolean isData(INode node)
    {
        return node instanceof BpmnDataObjectNode || node instanceof BpmnDataStoreNode;
    }

}
