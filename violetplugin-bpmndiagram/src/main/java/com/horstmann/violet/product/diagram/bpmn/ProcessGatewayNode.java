package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;

import com.horstmann.violet.product.diagram.abstracts.node.DiamondNode;
import com.horstmann.violet.product.diagram.bpmn.type.GatewayType;

/**
 * A BPMN gateway: a diamond where the flow splits or merges, its marker telling how (exclusive, parallel, inclusive, on an
 * event)
 */
public class ProcessGatewayNode extends DiamondNode
{

    @Override
    protected void drawMarker(Graphics2D g2)
    {
        getGatewayType().drawMarker(g2, getBounds(), DRAWING.getLineWidth(g2));
    }

    /**
     * Sets the gatewayType property value.
     *
     * @param newValue the kind of gateway
     */
    public void setGatewayType(GatewayType newValue)
    {
        this.gatewayType = newValue;
    }

    /**
     * Gets the gatewayType property value.
     *
     * @return the kind of gateway, exclusive by default
     */
    public GatewayType getGatewayType()
    {
        if (this.gatewayType == null)
        {
            this.gatewayType = GatewayType.EXCLUSIVE;
        }
        return this.gatewayType;
    }

    @Override
    public ProcessGatewayNode clone()
    {
        return (ProcessGatewayNode) super.clone();
    }

    private GatewayType gatewayType;

    private static final ProcessDrawing DRAWING = new ProcessDrawing();

}
