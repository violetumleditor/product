package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Edits the kind of a gateway
 */
public class GatewayTypeEditor extends BpmnChoiceEditor
{

    public GatewayTypeEditor()
    {
        super(GatewayType.values(), "gatewaytype");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        Path2D.Double diamond = new Path2D.Double();
        diamond.moveTo(5, 7);
        diamond.lineTo(12, 0);
        diamond.lineTo(19, 7);
        diamond.lineTo(12, 14);
        diamond.closePath();
        g2.draw(diamond);
        ((GatewayType) choice).drawMarker(g2, new Rectangle2D.Double(5, 0, 14, 14), 0.6f);
    }

}
