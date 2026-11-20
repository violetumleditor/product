package com.horstmann.violet.product.diagram.bpmn.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.bpmn.type.EventResultType;

/**
 * Edits the result of an end event: what the process produces when it ends there
 */
public class EndEventResultEditor extends ProcessChoiceEditor
{

    public EndEventResultEditor()
    {
        super(EventResultType.values(), "result");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        g2.draw(new Ellipse2D.Double(5, 0, 14, 14));
        // In outline: the icon's background is unknown, so a filled marker would lose its details
        ((EventResultType) choice).drawMarker(g2, new Rectangle2D.Double(8, 3, 8, 8), false, TRANSPARENT, 1);
    }

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

}
